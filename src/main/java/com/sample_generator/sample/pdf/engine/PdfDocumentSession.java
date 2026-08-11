package com.sample_generator.sample.pdf.engine;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.sample_generator.sample.pdf.assets.AssetBundle;
import com.sample_generator.sample.pdf.models.ReportRenderContext;
import com.sample_generator.sample.pdf.theme.ThemeContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Owns the iText document lifecycle for one generation run.
 */
public final class PdfDocumentSession implements AutoCloseable {

    private final ByteArrayOutputStream outputStream;
    private final PdfWriter writer;
    private final PdfDocument pdfDocument;
    private final Document document;
    private boolean closed;

    private PdfDocumentSession(
            ByteArrayOutputStream outputStream,
            PdfWriter writer,
            PdfDocument pdfDocument,
            Document document) {
        this.outputStream = outputStream;
        this.writer = writer;
        this.pdfDocument = pdfDocument;
        this.document = document;
    }

    public static PdfDocumentSession open(
            ReportRenderContext context,
            ThemeContext theme,
            AssetBundle assets) throws IOException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(assets, "assets");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4.rotate());

        return new PdfDocumentSession(outputStream, writer, pdfDocument, document);
    }

    public Document getDocument() {
        ensureOpen();
        return document;
    }

    public PdfDocument getPdfDocument() {
        ensureOpen();
        return pdfDocument;
    }

    public byte[] toPdfBytes() throws IOException {
        ensureOpen();
        close();
        return outputStream.toByteArray();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        document.close();
        pdfDocument.close();
        writer.close();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("PdfDocumentSession is already closed");
        }
    }
}
