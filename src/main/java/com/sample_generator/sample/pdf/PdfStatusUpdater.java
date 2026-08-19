package com.sample_generator.sample.pdf;

import com.sample_generator.sample.repository.SampleReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdfStatusUpdater {

    private final SampleReportRepository sampleReportRepository;

    public PdfStatusUpdater(SampleReportRepository sampleReportRepository) {
        this.sampleReportRepository = sampleReportRepository;
    }

    @Transactional
    public void update(Long reportId, PdfStatus status, String error) {
        sampleReportRepository.findById(reportId).ifPresent(report -> {
            report.setPdfStatus(status);
            report.setPdfError(error);
            sampleReportRepository.save(report);
        });
    }
}
