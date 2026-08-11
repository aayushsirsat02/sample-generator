package com.sample_generator.sample.pdf.engine;

import com.sample_generator.sample.pdf.assets.AssetBundle;
import com.sample_generator.sample.pdf.assets.AssetManager;
import com.sample_generator.sample.pdf.models.ReportRenderContext;
import com.sample_generator.sample.pdf.renderers.ComponentRenderer;
import com.sample_generator.sample.pdf.theme.ThemeContext;
import com.sample_generator.sample.pdf.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Entry point for PDF generation. Orchestrates theme resolution, asset binding,
 * document session lifecycle, and sequential renderer execution.
 */
@Component
@ConditionalOnBean({ThemeManager.class, AssetManager.class, RendererRegistry.class})
public class PdfEngine {

    private static final Logger log = LoggerFactory.getLogger(PdfEngine.class);

    private final ThemeManager themeManager;
    private final AssetManager assetManager;
    private final RendererRegistry rendererRegistry;

    public PdfEngine(
            ThemeManager themeManager,
            AssetManager assetManager,
            RendererRegistry rendererRegistry) {
        this.themeManager = Objects.requireNonNull(themeManager, "themeManager");
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.rendererRegistry = Objects.requireNonNull(rendererRegistry, "rendererRegistry");
    }

    /**
     * Generates a PDF for the given render context.
     *
     * @param context immutable inputs and metadata for this generation run
     * @return complete PDF document bytes
     * @throws IOException if the document cannot be written or closed
     * @throws IllegalStateException if rendering fails in a non-recoverable way
     */
    public byte[] generate(ReportRenderContext context) throws IOException {
        Objects.requireNonNull(context, "context");

        log.debug("Starting PDF generation");

        ThemeContext theme = themeManager.resolve(context);
        AssetBundle assets = assetManager.resolve(context, theme);

        try (PdfDocumentSession session = PdfDocumentSession.open(context, theme, assets)) {
            List<ComponentRenderer> renderers = rendererRegistry.getRenderersInOrder(context);

            for (ComponentRenderer renderer : renderers) {
                Objects.requireNonNull(renderer, "renderer");
                log.trace("Executing renderer: {}", renderer.getClass().getSimpleName());
                renderer.render(context, theme, assets, session);
            }

            byte[] pdfBytes = session.toPdfBytes();
            log.debug("PDF generation finished, size={} bytes", pdfBytes.length);
            return pdfBytes;
        }
    }
}
