package com.sample_generator.sample.pdf.engine;

import com.sample_generator.sample.pdf.models.ReportRenderContext;
import com.sample_generator.sample.pdf.renderers.ComponentRenderer;

import java.util.List;

/**
 * Supplies renderers in the order they must run for a given context.
 */
public interface RendererRegistry {

    List<ComponentRenderer> getRenderersInOrder(ReportRenderContext context);
}
