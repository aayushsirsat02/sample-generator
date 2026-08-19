package com.sample_generator.sample.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local disk cache of generated PDFs, keyed by report id.
 * Thread-safe per report so concurrent downloads do not regenerate twice.
 */
@Component
public class GeneratedPdfStore {

    private static final Logger log = LoggerFactory.getLogger(GeneratedPdfStore.class);

    private final Path cacheDir;
    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    public GeneratedPdfStore(
            @Value("${pdf.cache.dir:cache/pdfs}") String cacheDir) {
        this.cacheDir = Paths.get(cacheDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.cacheDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create PDF cache directory: " + this.cacheDir, e);
        }
    }

    public Optional<Path> findReady(Long reportId) {
        Path cached = cacheFile(reportId);
        return isUsable(cached) ? Optional.of(cached) : Optional.empty();
    }

    public void store(Long reportId, byte[] pdf) throws IOException {
        Object lock = lockFor(reportId);
        synchronized (lock) {
            writeAtomically(cacheFile(reportId), pdf);
        }
    }

    public void invalidate(Long reportId) {
        if (reportId == null) {
            return;
        }
        Object lock = lockFor(reportId);
        synchronized (lock) {
            Path cached = cacheFile(reportId);
            try {
                boolean deleted = Files.deleteIfExists(cached);
                if (deleted) {
                    log.info("Invalidated cached PDF for reportId={}", reportId);
                }
            } catch (IOException e) {
                log.warn("Failed to invalidate cached PDF for reportId={}", reportId, e);
            }
        }
    }

    private Object lockFor(Long reportId) {
        return locks.computeIfAbsent(reportId, id -> new Object());
    }

    private Path cacheFile(Long reportId) {
        return cacheDir.resolve(reportId + ".pdf");
    }

    private static boolean isUsable(Path file) {
        try {
            return Files.isRegularFile(file) && Files.size(file) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static void writeAtomically(Path target, byte[] pdf) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temp, pdf);
        try {
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }
}
