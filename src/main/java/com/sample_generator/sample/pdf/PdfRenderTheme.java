package com.sample_generator.sample.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.service.CategoryThemeDefaults;

import java.awt.Color;
import java.util.Map;

/**
 * Resolved PDF colors/cover for one generation run. Current Blue Corporate values
 * are the fallback when config is missing or the category is General/unknown.
 */
public final class PdfRenderTheme {

    public static final String DEFAULT_COVER = CategoryThemeDefaults.DEFAULT_COVER_IMAGE;

    public static final DeviceRgb CURRENT_HEADER = new DeviceRgb(0, 112, 192);
    public static final DeviceRgb CURRENT_FOOTER = new DeviceRgb(255, 184, 28);
    public static final DeviceRgb CURRENT_TABLE_HEADER = new DeviceRgb(0, 32, 96);
    public static final DeviceRgb CURRENT_PRIMARY = new DeviceRgb(0, 32, 96);
    public static final DeviceRgb CURRENT_SECONDARY = new DeviceRgb(0, 102, 204);
    public static final DeviceRgb CURRENT_ACCENT = new DeviceRgb(255, 184, 28);

    public static final Color CURRENT_CHART_PRIMARY = new Color(0, 32, 96);
    public static final Color CURRENT_CHART_SECONDARY = new Color(255, 193, 7);

    public static final PdfRenderTheme CURRENT = new PdfRenderTheme(
            DEFAULT_COVER,
            CURRENT_HEADER,
            CURRENT_FOOTER,
            CURRENT_TABLE_HEADER,
            CURRENT_PRIMARY,
            CURRENT_SECONDARY,
            CURRENT_ACCENT,
            CURRENT_CHART_PRIMARY,
            CURRENT_CHART_SECONDARY);

    private final String coverImagePath;
    private final DeviceRgb headerColor;
    private final DeviceRgb footerColor;
    private final DeviceRgb tableHeaderColor;
    private final DeviceRgb primaryColor;
    private final DeviceRgb secondaryColor;
    private final DeviceRgb accentColor;
    private final Color chartPrimaryColor;
    private final Color chartSecondaryColor;

    public PdfRenderTheme(
            String coverImagePath,
            DeviceRgb headerColor,
            DeviceRgb footerColor,
            DeviceRgb tableHeaderColor,
            DeviceRgb primaryColor,
            DeviceRgb secondaryColor,
            DeviceRgb accentColor,
            Color chartPrimaryColor,
            Color chartSecondaryColor) {
        this.coverImagePath = coverImagePath != null && !coverImagePath.isBlank()
                ? coverImagePath
                : DEFAULT_COVER;
        this.headerColor = headerColor != null ? headerColor : CURRENT_HEADER;
        this.footerColor = footerColor != null ? footerColor : CURRENT_FOOTER;
        this.tableHeaderColor = tableHeaderColor != null ? tableHeaderColor : CURRENT_TABLE_HEADER;
        this.primaryColor = primaryColor != null ? primaryColor : CURRENT_PRIMARY;
        this.secondaryColor = secondaryColor != null ? secondaryColor : CURRENT_SECONDARY;
        this.accentColor = accentColor != null ? accentColor : CURRENT_ACCENT;
        this.chartPrimaryColor = chartPrimaryColor != null ? chartPrimaryColor : CURRENT_CHART_PRIMARY;
        this.chartSecondaryColor = chartSecondaryColor != null ? chartSecondaryColor : CURRENT_CHART_SECONDARY;
    }

    public static PdfRenderTheme resolve(SampleReport report, Map<String, Object> mergedTheme) {
        if (report == null) {
            return CURRENT;
        }
        boolean general = CategoryThemeDefaults.forCategory(report.getCategory())
                == CategoryThemeDefaults.FALLBACK;
        boolean edited = Boolean.TRUE.equals(report.getIsEdited());
        if (general && !edited) {
            return CURRENT;
        }
        Map<String, Object> theme = mergedTheme != null ? mergedTheme : Map.of();
        DeviceRgb secondary = rgb(theme, "secondaryColor", CURRENT_SECONDARY);
        DeviceRgb accent = rgb(theme, "accentColor", CURRENT_ACCENT);
        DeviceRgb primary = rgb(theme, "primaryColor", CURRENT_PRIMARY);
        return new PdfRenderTheme(
                firstNonBlank(string(theme.get("coverImage")), DEFAULT_COVER),
                rgb(theme, "headerColor", secondary),
                rgb(theme, "footerColor", accent),
                rgb(theme, "tableHeaderColor", primary),
                primary,
                secondary,
                accent,
                awt(theme, "chartPrimaryColor", CURRENT_CHART_PRIMARY),
                awt(theme, "chartSecondaryColor", CURRENT_CHART_SECONDARY));
    }

    public String coverImagePath() {
        return coverImagePath;
    }

    public DeviceRgb headerColor() {
        return headerColor;
    }

    public DeviceRgb footerColor() {
        return footerColor;
    }

    public DeviceRgb tableHeaderColor() {
        return tableHeaderColor;
    }

    public DeviceRgb primaryColor() {
        return primaryColor;
    }

    public DeviceRgb secondaryColor() {
        return secondaryColor;
    }

    public DeviceRgb accentColor() {
        return accentColor;
    }

    public Color chartPrimaryColor() {
        return chartPrimaryColor;
    }

    public Color chartSecondaryColor() {
        return chartSecondaryColor;
    }

    private static DeviceRgb rgb(Map<String, Object> theme, String key, DeviceRgb fallback) {
        Color parsed = parseHex(string(theme.get(key)));
        if (parsed == null) {
            return fallback;
        }
        return new DeviceRgb(parsed.getRed(), parsed.getGreen(), parsed.getBlue());
    }

    private static Color awt(Map<String, Object> theme, String key, Color fallback) {
        Color parsed = parseHex(string(theme.get(key)));
        return parsed != null ? parsed : fallback;
    }

    static Color parseHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return null;
        }
        try {
            int rgb = Integer.parseInt(value, 16);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
