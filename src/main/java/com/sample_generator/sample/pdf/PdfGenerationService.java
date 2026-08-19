package com.sample_generator.sample.pdf;

import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.repository.SampleReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invalidates cached PDFs and generates replacements in the background.
 * Download never calls this generation path.
 */
@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    private final PdfService pdfService;
    private final GeneratedPdfStore generatedPdfStore;
    private final SampleReportRepository sampleReportRepository;
    private final PdfStatusUpdater pdfStatusUpdater;
    private final PdfGenerationWorker pdfGenerationWorker;
    private final ConcurrentHashMap<Long, AtomicInteger> epochs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> inFlight = new ConcurrentHashMap<>();

    public PdfGenerationService(
            PdfService pdfService,
            GeneratedPdfStore generatedPdfStore,
            SampleReportRepository sampleReportRepository,
            PdfStatusUpdater pdfStatusUpdater,
            @Lazy PdfGenerationWorker pdfGenerationWorker) {
        this.pdfService = pdfService;
        this.generatedPdfStore = generatedPdfStore;
        this.sampleReportRepository = sampleReportRepository;
        this.pdfStatusUpdater = pdfStatusUpdater;
        this.pdfGenerationWorker = pdfGenerationWorker;
    }

    /**
     * After the surrounding DB transaction commits, generate and cache the PDF.
     */
    public void requestGeneration(Long reportId) {
        if (reportId == null) {
            return;
        }

        generatedPdfStore.invalidate(reportId);
        pdfStatusUpdater.update(reportId, PdfStatus.GENERATING, null);
        epochs.computeIfAbsent(reportId, id -> new AtomicInteger()).incrementAndGet();

        Runnable enqueue = () -> enqueue(reportId);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
        } else {
            enqueue.run();
        }
    }

    public void discardCachedPdf(Long reportId) {
        generatedPdfStore.invalidate(reportId);
    }

    public PdfStatusView getStatus(Long reportId) {
        SampleReport report = sampleReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (generatedPdfStore.findReady(reportId).isPresent()) {
            if (report.getPdfStatus() != PdfStatus.READY) {
                pdfStatusUpdater.update(reportId, PdfStatus.READY, null);
            }
            return new PdfStatusView(reportId, PdfStatus.READY, null);
        }

        PdfStatus status = report.getPdfStatus();
        if (status == null
                || ((status == PdfStatus.GENERATING || status == PdfStatus.READY)
                        && !inFlight.containsKey(reportId))) {
            if (status != null) {
                log.info("Recovering PDF generation for reportId={} (status={})", reportId, status);
            }
            requestGeneration(reportId);
            return new PdfStatusView(reportId, PdfStatus.GENERATING, "PDF is being prepared");
        }
        String message = status == PdfStatus.FAILED
                ? firstNonBlank(report.getPdfError(), "PDF generation failed")
                : "PDF is being prepared";
        return new PdfStatusView(reportId, status, message);
    }

    public Optional<Path> findReadyFile(Long reportId) {
        if (!sampleReportRepository.existsById(reportId)) {
            throw new RuntimeException("Report not found");
        }
        return generatedPdfStore.findReady(reportId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedGeneration() {
        for (SampleReport report : sampleReportRepository.findByPdfStatus(PdfStatus.GENERATING)) {
            log.info("Recovering interrupted PDF generation for reportId={}", report.getId());
            requestGeneration(report.getId());
        }
    }

    void generateInBackground(Long reportId) {
        try {
            while (true) {
                int epoch = currentEpoch(reportId);
                try {
                    log.info("PDF cache miss for reportId={} (background generation)", reportId);
                    long start = System.currentTimeMillis();
                    byte[] pdf = pdfService.generatePdf(reportId);
                    long generationMs = System.currentTimeMillis() - start;
                    log.info("PDF generation time for reportId={}: {} ms", reportId, generationMs);

                    if (epoch != currentEpoch(reportId)) {
                        log.info("PDF data changed during generation for reportId={}; regenerating", reportId);
                        continue;
                    }

                    generatedPdfStore.store(reportId, pdf);

                    if (epoch != currentEpoch(reportId)) {
                        continue;
                    }

                    pdfStatusUpdater.update(reportId, PdfStatus.READY, null);
                    log.info("PDF cache ready for reportId={}", reportId);
                    if (epoch != currentEpoch(reportId)) {
                        continue;
                    }
                    return;
                } catch (Exception e) {
                    if (epoch != currentEpoch(reportId)) {
                        continue;
                    }
                    log.error("PDF generation failed for reportId={}", reportId, e);
                    pdfStatusUpdater.update(reportId, PdfStatus.FAILED, abbreviate(e.getMessage()));
                    return;
                }
            }
        } finally {
            inFlight.remove(reportId);
            SampleReport report = sampleReportRepository.findById(reportId).orElse(null);
            if (report != null
                    && report.getPdfStatus() == PdfStatus.GENERATING
                    && generatedPdfStore.findReady(reportId).isEmpty()) {
                enqueue(reportId);
            }
        }
    }

    private void enqueue(Long reportId) {
        if (inFlight.putIfAbsent(reportId, Boolean.TRUE) != null) {
            log.info("PDF generation already in progress for reportId={}", reportId);
            return;
        }
        try {
            pdfGenerationWorker.generate(reportId);
        } catch (RejectedExecutionException e) {
            inFlight.remove(reportId);
            log.error("PDF generation queue is full for reportId={}", reportId, e);
            pdfStatusUpdater.update(reportId, PdfStatus.FAILED, "PDF generation queue is full");
        }
    }

    private int currentEpoch(Long reportId) {
        AtomicInteger epoch = epochs.get(reportId);
        return epoch == null ? 0 : epoch.get();
    }

    private static String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "PDF generation failed";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    public record PdfStatusView(Long reportId, PdfStatus status, String message) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "reportId", reportId,
                    "status", status.name(),
                    "message", message == null ? "" : message);
        }
    }
}
