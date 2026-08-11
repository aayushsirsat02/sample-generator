package com.sample_generator.sample.pdf.models;

import java.util.Objects;

/**
 * Immutable inputs for a single PDF generation run.
 */
public final class ReportRenderContext {

    private final String themeId;

    public ReportRenderContext(String themeId) {
        this.themeId = Objects.requireNonNull(themeId, "themeId");
    }

    public String getThemeId() {
        return themeId;
    }
}
