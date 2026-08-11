package com.sample_generator.sample.pdf.theme;

import java.util.Objects;

/**
 * Resolved theme applied for one generation run.
 */
public final class ThemeContext {

    private final String themeId;

    public ThemeContext(String themeId) {
        this.themeId = Objects.requireNonNull(themeId, "themeId");
    }

    public String getThemeId() {
        return themeId;
    }
}
