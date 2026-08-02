package com.sample_generator.sample.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfRenderer {

    public byte[] generateHelloWorldPdf() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(outputStream);

        PdfDocument pdfDocument = new PdfDocument(writer);

        Document document = new Document(
                pdfDocument,
                com.itextpdf.kernel.geom.PageSize.A4.rotate()
        );

        document.add(new Paragraph("Hello World!"));

        document.add(new Paragraph("Welcome to iText 7 with Spring Boot."));

        document.close();

        return outputStream.toByteArray();
    }
}