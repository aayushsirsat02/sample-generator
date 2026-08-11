package com.sample_generator.sample.pdf.renderers;

import com.sample_generator.sample.pdf.assets.AssetBundle;
import com.sample_generator.sample.pdf.engine.PdfDocumentSession;
import com.sample_generator.sample.pdf.models.ReportRenderContext;
import com.sample_generator.sample.pdf.theme.ThemeContext;

import java.io.IOException;

/**
 * Renders one logical block of report content into an open document session.
 */
public interface ComponentRenderer {

    void render(
            ReportRenderContext context,
            ThemeContext theme,
            AssetBundle assets,
            PdfDocumentSession session) throws IOException;
}
