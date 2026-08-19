package com.sample_generator.sample.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.element.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Reuses {@link PdfImageXObject} instances within a single PDF document so the same
 * asset is not re-encoded on every occurrence (headers, repeated figures, bullets).
 */
public final class PdfDocumentImageCache {

    private static final ThreadLocal<Map<String, PdfImageXObject>> XOBJECTS = new ThreadLocal<>();

    private PdfDocumentImageCache() {
    }

    public static void beginDocument() {
        XOBJECTS.set(new HashMap<>());
    }

    public static void endDocument() {
        XOBJECTS.remove();
    }

    public static Image image(String key, ImageData data) {
        Map<String, PdfImageXObject> map = XOBJECTS.get();
        if (map == null) {
            return new Image(data);
        }
        PdfImageXObject xObject = map.get(key);
        if (xObject == null) {
            xObject = new PdfImageXObject(data);
            map.put(key, xObject);
        }
        return new Image(xObject);
    }
}
