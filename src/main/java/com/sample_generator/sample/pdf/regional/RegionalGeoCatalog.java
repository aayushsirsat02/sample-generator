package com.sample_generator.sample.pdf.regional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionalGeoCatalog {

    public static final String SETTINGS_COUNTRIES_KEY = "countriesByRegion";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, List<String>> COUNTRIES_BY_REGION = new LinkedHashMap<>();
    private static final Map<String, List<String>> DIMENSION_ROWS = new LinkedHashMap<>();
    private static final List<List<String>> REGION_ALIAS_GROUPS = List.of(
            List.of("North America"),
            List.of("Europe"),
            List.of("APAC", "Asia Pacific"),
            List.of("South America", "Latin America"),
            List.of("MEA", "Middle East & Africa", "The Middle-East and Africa"));

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

    /**
     * Configured countries when the report has a saved selection; otherwise catalog defaults.
     * A present (even empty) selection is used as-is so removed countries are omitted.
     */
    public static List<String> countriesForRegion(String regionName, SampleReport report) {
        List<String> configured = configuredCountries(report, regionName);
        if (configured != null) {
            return configured;
        }
        return countriesForRegion(regionName);
    }

    /**
     * When the report has a saved country selection for this region, use that ordered list.
     * Otherwise keep existing segment children, or catalog defaults if children are empty.
     */
    public static List<MarketSegment> countryNodesForRegion(
            String regionName, SampleReport report, List<MarketSegment> existing) {
        List<String> configured = configuredCountries(report, regionName);
        if (configured != null) {
            return stubCountryNodes(configured);
        }
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        return stubCountryNodes(countriesForRegion(regionName));
    }

    public static List<String> configuredCountries(SampleReport report, String regionName) {
        Map<String, List<String>> configured = readConfiguredCountries(report);
        if (configured == null || configured.isEmpty() || regionName == null || regionName.isBlank()) {
            return null;
        }
        for (String key : lookupKeys(regionName.trim())) {
            if (containsKeyIgnoreCase(configured, key)) {
                return configured.get(resolveKey(configured, key));
            }
        }
        return null;
    }

    public static Map<String, List<String>> readConfiguredCountries(SampleReport report) {
        if (report == null || report.getReportConfig() == null || report.getReportConfig().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> model = OBJECT_MAPPER.readValue(
                    report.getReportConfig(),
                    new TypeReference<Map<String, Object>>() {
                    });
            Object raw = null;
            Object settings = model.get("settings");
            if (settings instanceof Map<?, ?> settingsMap) {
                raw = settingsMap.get(SETTINGS_COUNTRIES_KEY);
            }
            if (raw == null) {
                raw = model.get(SETTINGS_COUNTRIES_KEY);
            }
            Map<String, List<String>> parsed = parseCountriesByRegion(raw);
            return parsed == null || parsed.isEmpty() ? null : parsed;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Map<String, List<String>> parseCountriesByRegion(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String region = String.valueOf(entry.getKey()).trim();
            if (region.isEmpty()) {
                continue;
            }
            result.put(region, sanitizeCountryList(entry.getValue()));
        }
        return result.isEmpty() ? null : result;
    }

    public static List<String> sanitizeCountryList(Object raw) {
        List<String> countries = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return countries;
        }
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String name = String.valueOf(item).trim();
            if (name.isEmpty() || "null".equalsIgnoreCase(name)) {
                continue;
            }
            boolean duplicate = false;
            for (String existing : countries) {
                if (existing.equalsIgnoreCase(name)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                countries.add(name);
            }
        }
        return countries;
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

    private static List<MarketSegment> stubCountryNodes(List<String> countries) {
        List<MarketSegment> stubs = new ArrayList<>();
        if (countries == null) {
            return stubs;
        }
        for (String country : countries) {
            if (country == null || country.isBlank()) {
                continue;
            }
            MarketSegment stub = new MarketSegment();
            stub.setSegmentName(country);
            stubs.add(stub);
        }
        return stubs;
    }

    private static List<String> lookupKeys(String regionName) {
        List<String> keys = new ArrayList<>();
        keys.add(regionName);
        for (List<String> group : REGION_ALIAS_GROUPS) {
            boolean match = false;
            for (String alias : group) {
                if (alias.equalsIgnoreCase(regionName)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                for (String alias : group) {
                    if (!alias.equalsIgnoreCase(regionName)) {
                        keys.add(alias);
                    }
                }
                break;
            }
        }
        return keys;
    }

    private static boolean containsKeyIgnoreCase(Map<String, List<String>> map, String key) {
        return resolveKey(map, key) != null;
    }

    private static String resolveKey(Map<String, List<String>> map, String key) {
        if (map.containsKey(key)) {
            return key;
        }
        for (String existing : map.keySet()) {
            if (existing != null && existing.equalsIgnoreCase(key)) {
                return existing;
            }
        }
        return null;
    }
}
