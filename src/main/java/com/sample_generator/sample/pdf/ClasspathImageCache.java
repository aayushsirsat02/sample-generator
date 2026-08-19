package com.sample_generator.sample.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide cache of decoded classpath {@link ImageData}.
 * ImageData is treated as read-only after load; each PDF still wraps it in its own XObject.
 */
public final class ClasspathImageCache {

    private static final ConcurrentHashMap<String, ImageData> IMAGE_DATA = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, byte[]> BYTES = new ConcurrentHashMap<>();

    private ClasspathImageCache() {
    }

    public static ImageData get(String classpathPath) throws IOException {
        ImageData cached = IMAGE_DATA.get(classpathPath);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = bytes(classpathPath);
        ImageData created = ImageDataFactory.create(bytes);
        ImageData raced = IMAGE_DATA.putIfAbsent(classpathPath, created);
        return raced != null ? raced : created;
    }

    public static byte[] bytes(String classpathPath) throws IOException {
        byte[] cached = BYTES.get(classpathPath);
        if (cached != null) {
            return cached;
        }
        byte[] loaded = new ClassPathResource(classpathPath).getInputStream().readAllBytes();
        byte[] raced = BYTES.putIfAbsent(classpathPath, loaded);
        return raced != null ? raced : loaded;
    }

    public static ImageData fromBytes(String cacheKey, byte[] bytes) {
        ImageData cached = IMAGE_DATA.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        ImageData created = ImageDataFactory.create(bytes);
        ImageData raced = IMAGE_DATA.putIfAbsent(cacheKey, created);
        return raced != null ? raced : created;
    }
}
