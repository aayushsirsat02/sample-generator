package com.sample_generator.sample.pdf;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ThemeRenderer {

    private final ThreadLocal<PdfFont> regularFont = new ThreadLocal<>();
    private final ThreadLocal<PdfFont> boldItalicFont = new ThreadLocal<>();
    private final ThreadLocal<PdfFont> semiBoldFont = new ThreadLocal<>();
    private final ThreadLocal<PdfFont> boldFont = new ThreadLocal<>();

    public PdfFont regular() throws IOException {
        return cachedFont(regularFont, "assets/fonts/Euclid Circular A Regular.ttf");
    }

    public PdfFont boldItalic() throws IOException {
        return cachedFont(boldItalicFont, "assets/fonts/Euclid Circular A Bold Italic.ttf");
    }

    public PdfFont semiBold() throws IOException {
        return cachedFont(semiBoldFont, "assets/fonts/Euclid Circular A Semibold.ttf");
    }

    public PdfFont bold() throws IOException {
        return cachedFont(boldFont, "assets/fonts/Euclid Circular A Bold.ttf");
    }

    private static PdfFont cachedFont(ThreadLocal<PdfFont> slot, String classpathLocation)
            throws IOException {
        PdfFont font = slot.get();
        if (font == null || isFlushed(font)) {
            font = PdfFontFactory.createFont(
                    ClasspathImageCache.bytes(classpathLocation),
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            slot.set(font);
        }
        return font;
    }

    private static boolean isFlushed(PdfFont font) {
        return font.getPdfObject() != null && font.getPdfObject().isFlushed();
    }
}
