package com.sample_generator.sample.pdf.theme;

import com.sample_generator.sample.pdf.models.ReportRenderContext;

/**
 * Resolves theme configuration for a render context.
 */
public interface ThemeManager {

    ThemeContext resolve(ReportRenderContext context);
}
