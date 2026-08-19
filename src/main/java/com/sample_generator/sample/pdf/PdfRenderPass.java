package com.sample_generator.sample.pdf;

/**
 * Per-thread flag for the TOC indexing pass vs the final visual pass.
 * Safe for concurrent PDF generation; no shared mutable render state.
 */
public final class PdfRenderPass {

    private static final ThreadLocal<Boolean> TOC_INDEXING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private PdfRenderPass() {
    }

    public static void beginTocIndexing() {
        TOC_INDEXING.set(Boolean.TRUE);
    }

    public static void beginFinalPass() {
        TOC_INDEXING.set(Boolean.FALSE);
    }

    public static void clear() {
        TOC_INDEXING.remove();
    }

    public static boolean isTocIndexing() {
        return Boolean.TRUE.equals(TOC_INDEXING.get());
    }
}
