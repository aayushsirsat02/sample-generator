package com.sample_generator.sample.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Category DEFAULT theme values stored in {@code originalConfig}/{@code reportConfig}.
 * Unknown and General categories keep the current Blue Corporate palette.
 */
public final class CategoryThemeDefaults {

    public static final String DEFAULT_COVER_IMAGE = "assets/images/cover.png";

    public static final ThemePalette FALLBACK = palette(
            "Blue Corporate",
            "#002060",
            "#2B6CB0",
            "#ECC94B",
            "#002060",
            "#002060",
            "#FFC107",
            DEFAULT_COVER_IMAGE);

    private static final Map<String, ThemePalette> BY_NORMALIZED_CATEGORY = new LinkedHashMap<>();

    static {
        register("Aerospace & Defense", palette(
                "Aerospace & Defense",
                "#1B3A4B", "#4A7C9B", "#C9A227",
                "#1B3A4B", "#1B3A4B", "#C9A227",
                coverPath("aerospace-defense")));
        register("Agriculture", palette(
                "Agriculture",
                "#2F5D32", "#5B8C51", "#C4A35A",
                "#2F5D32", "#2F5D32", "#C4A35A",
                coverPath("agriculture")));
        register("Automotive & Transportation", palette(
                "Automotive & Transportation",
                "#1F2933", "#C0392B", "#F39C12",
                "#1F2933", "#1F2933", "#C0392B",
                coverPath("automotive-transportation")));
        register("Banking Financial Services & Insurance", palette(
                "Banking Financial Services & Insurance",
                "#0B3D5C", "#1A6B9A", "#D4AF37",
                "#0B3D5C", "#0B3D5C", "#D4AF37",
                coverPath("bfsi")));
        register("Chemicals & Materials", palette(
                "Chemicals & Materials",
                "#3D2463", "#6B4C9A", "#2A9D8F",
                "#3D2463", "#3D2463", "#2A9D8F",
                coverPath("chemicals-materials")));
        register("Construction & Manufacturing", palette(
                "Construction & Manufacturing",
                "#5C3A21", "#C26E33", "#E0A100",
                "#5C3A21", "#5C3A21", "#C26E33",
                coverPath("construction-manufacturing")));
        register("Consumer Goods", palette(
                "Consumer Goods",
                "#7A2748", "#C45C7A", "#E8B86D",
                "#7A2748", "#7A2748", "#E8B86D",
                coverPath("consumer-goods")));
        register("Electronics & Semiconductor", palette(
                "Electronics & Semiconductor",
                "#12355B", "#1B6CA8", "#00A3A1",
                "#12355B", "#12355B", "#00A3A1",
                coverPath("electronics-semiconductor")));
        register("Energy & Power", palette(
                "Energy & Power",
                "#1E4D2B", "#3D7A4A", "#E09F1F",
                "#1E4D2B", "#1E4D2B", "#E09F1F",
                coverPath("energy-power")));
        register("Food & Beverages", palette(
                "Food & Beverages",
                "#6B2D1B", "#B85C38", "#D4A017",
                "#6B2D1B", "#6B2D1B", "#D4A017",
                coverPath("food-beverages")));
        register("Healthcare", palette(
                "Healthcare",
                "#0E4D64", "#2A9BB0", "#3CB371",
                "#0E4D64", "#0E4D64", "#3CB371",
                coverPath("healthcare")));
        register("Information & Technology", palette(
                "Information & Technology",
                "#1A365D", "#3182CE", "#805AD5",
                "#1A365D", "#1A365D", "#3182CE",
                coverPath("information-technology")));
        register("Machinery & Equipment", palette(
                "Machinery & Equipment",
                "#374151", "#F97316", "#FBBF24",
                "#374151", "#374151", "#F97316",
                coverPath("machinery-equipment")));
        register("Packaging", palette(
                "Packaging",
                "#4B3A2A", "#8B6914", "#2F855A",
                "#4B3A2A", "#4B3A2A", "#8B6914",
                coverPath("packaging")));
        register("Telecom", palette(
                "Telecom",
                "#312E81", "#4F46E5", "#06B6D4",
                "#312E81", "#312E81", "#06B6D4",
                coverPath("telecom")));
        register("Media & Entertainment", palette(
                "Media & Entertainment",
                "#4A1942", "#9B1B6B", "#F59E0B",
                "#4A1942", "#4A1942", "#F59E0B",
                coverPath("media-entertainment")));

        alias("Aerospace", "Aerospace & Defense");
        alias("Aerospace and Defense", "Aerospace & Defense");
        alias("Aerospace & Defence", "Aerospace & Defense");
        alias("Agriculture", "Agriculture");
        alias("Automotive", "Automotive & Transportation");
        alias("Automotive and Transportation", "Automotive & Transportation");
        alias("BFSI", "Banking Financial Services & Insurance");
        alias("Banking", "Banking Financial Services & Insurance");
        alias("Banking, Financial Services and Insurance", "Banking Financial Services & Insurance");
        alias("Chemicals", "Chemicals & Materials");
        alias("Chemicals and Materials", "Chemicals & Materials");
        alias("Construction", "Construction & Manufacturing");
        alias("Construction and Manufacturing", "Construction & Manufacturing");
        alias("Consumer", "Consumer Goods");
        alias("Electronics", "Electronics & Semiconductor");
        alias("Semiconductor", "Electronics & Semiconductor");
        alias("Electronics and Semiconductor", "Electronics & Semiconductor");
        alias("Energy", "Energy & Power");
        alias("Energy and Power", "Energy & Power");
        alias("Food", "Food & Beverages");
        alias("Food and Beverages", "Food & Beverages");
        alias("Health", "Healthcare");
        alias("Information and Technology", "Information & Technology");
        alias("IT", "Information & Technology");
        alias("ICT", "Information & Technology");
        alias("Technology", "Information & Technology");
        alias("Machinery", "Machinery & Equipment");
        alias("Machinery and Equipment", "Machinery & Equipment");
        alias("Telecommunications", "Telecom");
        alias("Media", "Media & Entertainment");
        alias("Media and Entertainment", "Media & Entertainment");
        alias("General", null);
    }

    private CategoryThemeDefaults() {
    }

    public static ThemePalette forCategory(String category) {
        if (category == null || category.isBlank()) {
            return FALLBACK;
        }
        String normalized = normalize(category);
        if (normalized.isEmpty() || "general".equals(normalized) || "unknown".equals(normalized)) {
            return FALLBACK;
        }
        ThemePalette palette = BY_NORMALIZED_CATEGORY.get(normalized);
        return palette != null ? palette : FALLBACK;
    }

    public static Map<String, Object> themeMapFor(String category) {
        return forCategory(category).toThemeMap();
    }

    public static Map<String, Object> coverMapFor(String category, String keyId) {
        Map<String, Object> cover = new LinkedHashMap<>();
        cover.put("keyId", keyId);
        cover.put("backgroundImage", forCategory(category).coverImage());
        return cover;
    }

    private static void register(String category, ThemePalette palette) {
        BY_NORMALIZED_CATEGORY.put(normalize(category), palette);
    }

    private static void alias(String alias, String canonicalCategory) {
        if (canonicalCategory == null) {
            BY_NORMALIZED_CATEGORY.put(normalize(alias), FALLBACK);
            return;
        }
        ThemePalette palette = BY_NORMALIZED_CATEGORY.get(normalize(canonicalCategory));
        if (palette != null) {
            BY_NORMALIZED_CATEGORY.put(normalize(alias), palette);
        }
    }

    private static ThemePalette palette(
            String name,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String tableHeaderColor,
            String chartPrimaryColor,
            String chartSecondaryColor,
            String coverImage) {
        return new ThemePalette(
                name,
                primaryColor,
                secondaryColor,
                accentColor,
                tableHeaderColor,
                chartPrimaryColor,
                chartSecondaryColor,
                coverImage);
    }

    private static String coverPath(String slug) {
        return "assets/images/covers/" + slug + ".png";
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replace(',', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record ThemePalette(
            String name,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String tableHeaderColor,
            String chartPrimaryColor,
            String chartSecondaryColor,
            String coverImage) {

        public Map<String, Object> toThemeMap() {
            Map<String, Object> theme = new LinkedHashMap<>();
            theme.put("name", name);
            theme.put("primaryColor", primaryColor);
            theme.put("secondaryColor", secondaryColor);
            theme.put("accentColor", accentColor);
            theme.put("tableHeaderColor", tableHeaderColor);
            theme.put("chartPrimaryColor", chartPrimaryColor);
            theme.put("chartSecondaryColor", chartSecondaryColor);
            theme.put("coverImage", coverImage);
            return theme;
        }
    }
}
