package com.sample_generator.sample.pdf;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async entry point for PDF generation. Prefer {@link PdfGenerationService#requestGeneration(Long)}
 * so work is scheduled after the report transaction commits.
 */
@Component
public class PdfGenerationWorker {

    private final PdfGenerationService pdfGenerationService;

    public PdfGenerationWorker(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    @Async("pdfGenerationExecutor")
    public void generate(Long reportId) {
        pdfGenerationService.generateInBackground(reportId);
    }
}
