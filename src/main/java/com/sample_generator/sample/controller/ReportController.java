package com.sample_generator.sample.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sample_generator.sample.pdf.PdfGenerationService;
import com.sample_generator.sample.pdf.PdfStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final PdfGenerationService pdfGenerationService;

    @GetMapping("/{id}/pdf-status")
    public ResponseEntity<?> pdfStatus(@PathVariable Long id) {
        return ResponseEntity.ok(pdfGenerationService.getStatus(id).toMap());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadPdf(@PathVariable Long id) throws java.io.IOException {

        long start = System.currentTimeMillis();

        Optional<Path> readyFile = pdfGenerationService.findReadyFile(id);
        if (readyFile.isPresent()) {
            Path pdfPath = readyFile.get();
            log.info("PDF cache hit for reportId={}", id);
            long end = System.currentTimeMillis();
            log.info("PDF download time for reportId={}: {} ms", id, end - start);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("Sample_Report.pdf")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(Files.size(pdfPath))
                    .body(new FileSystemResource(pdfPath));
        }

        var status = pdfGenerationService.getStatus(id);
        if (status.status() == PdfStatus.FAILED) {
            log.warn("PDF download rejected for reportId={}: FAILED", id);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status.toMap());
        }

        log.info("PDF still generating for reportId={}", id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(status.toMap());
    }
}
