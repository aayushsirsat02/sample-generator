package com.sample_generator.sample.pdf;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final PdfRenderer pdfRenderer;

    public byte[] generatePdf() {
        return pdfRenderer.generateHelloWorldPdf();
    }
}