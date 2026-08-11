package com.sample_generator.sample.pdf.assets;

import com.sample_generator.sample.pdf.models.ReportRenderContext;
import com.sample_generator.sample.pdf.theme.ThemeContext;

/**
 * Resolves and prepares assets for a render context and theme.
 */
public interface AssetManager {

    AssetBundle resolve(ReportRenderContext context, ThemeContext theme);
}
