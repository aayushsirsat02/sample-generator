package com.sample_generator.sample.pdf.regional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionalGeoCatalog {

    private static final Map<String, List<String>> COUNTRIES_BY_REGION = new LinkedHashMap<>();
    private static final Map<String, List<String>> DIMENSION_ROWS = new LinkedHashMap<>();

    static {
        COUNTRIES_BY_REGION.put("North America", List.of("U.S.", "Canada", "Mexico"));
        COUNTRIES_BY_REGION.put("Europe", List.of(
                "Germany", "France", "UK", "Italy", "Spain", "Rest of Europe"));
        COUNTRIES_BY_REGION.put("APAC", List.of(
                "China", "Japan", "India", "South Korea", "Australia", "Rest of APAC"));
        COUNTRIES_BY_REGION.put("Asia Pacific", List.of(
                "China", "Japan", "India", "South Korea", "Australia", "Rest of APAC"));
        COUNTRIES_BY_REGION.put("South America", List.of("Brazil", "Argentina", "Rest of South America"));
        COUNTRIES_BY_REGION.put("Latin America", List.of("Brazil", "Argentina", "Rest of South America"));
        COUNTRIES_BY_REGION.put("MEA", List.of("GCC Countries", "South Africa", "Rest of MEA"));
        COUNTRIES_BY_REGION.put("Middle East & Africa", List.of("GCC Countries", "South Africa", "Rest of MEA"));
        COUNTRIES_BY_REGION.put("The Middle-East and Africa", List.of("GCC Countries", "South Africa", "Rest of MEA"));

        DIMENSION_ROWS.put("Component", List.of("Hardware", "Software", "Services"));
        DIMENSION_ROWS.put("Data Center Type", List.of(
                "Hyperscale Data Centers",
                "Enterprise Data Centers",
                "Colocation Data Centers",
                "Edge Data Centers",
                "Modular & Portable Data Centers"));
        DIMENSION_ROWS.put("Industry", List.of(
                "Healthcare",
                "Retail",
                "IT and Telecom",
                "BFSI",
                "Automotive",
                "Media & Entertainment"));
    }

    private RegionalGeoCatalog() {
    }

    public static List<String> defaultRegionOrder() {
        return List.of("North America", "Europe", "APAC", "South America", "MEA");
    }

    public static List<String> countriesForRegion(String regionName) {
        if (regionName == null) {
            return List.of();
        }
        List<String> rows = COUNTRIES_BY_REGION.get(regionName);
        if (rows != null) {
            return rows;
        }
        for (Map.Entry<String, List<String>> entry : COUNTRIES_BY_REGION.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(regionName)) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    public static List<String> rowsForDimension(String dimensionName) {
        if (dimensionName == null) {
            return Collections.emptyList();
        }
        List<String> rows = DIMENSION_ROWS.get(dimensionName);
        if (rows != null) {
            return rows;
        }
        for (Map.Entry<String, List<String>> entry : DIMENSION_ROWS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(dimensionName)) {
                return entry.getValue();
            }
        }
        return List.of();
    }
}
