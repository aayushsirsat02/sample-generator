package com.sample_generator.sample.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sample_generator.sample.pdf.PdfService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final PdfService pdfService;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws java.io.IOException {
        long start = System.currentTimeMillis();
        byte[] pdf = pdfService.generatePdf(id);
        long elapsed = System.currentTimeMillis() - start;
        log.info("PDF download time for reportId={}: {} ms", id, elapsed);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("Sample_Report.pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
