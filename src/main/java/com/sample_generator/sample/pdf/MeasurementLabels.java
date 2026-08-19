package com.sample_generator.sample.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample_generator.sample.Entity.SampleReport;

import java.util.Map;

/**
 * Market measurement labels for table/chart/figure titles. Missing config keeps
 * the current PDF wording: Value (USD Million).
 */
public final class MeasurementLabels {

    public static final String DEFAULT_MEASUREMENT_TYPE = "Value";
    public static final String DEFAULT_CURRENCY = "USD";
    public static final String DEFAULT_UNIT = "Million";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MeasurementLabels() {
    }

    public static String getMeasurementType(SampleReport report) {
        String value = firstNonBlank(
                setting(report, "measurementType"),
                report != null ? report.getValueVolume() : null);
        if (isVolume(value)) {
            return "Volume";
        }
        return DEFAULT_MEASUREMENT_TYPE;
    }

    public static String getCurrency(SampleReport report) {
        String value = firstNonBlank(
                setting(report, "currency"),
                currencyFromLegacyUnit(report));
        if (value != null && !isMeasurementType(value) && !isMeasurementScale(value)) {
            return value;
        }
        return DEFAULT_CURRENCY;
    }

    public static String getUnit(SampleReport report) {
        String value = firstNonBlank(
                setting(report, "unit"),
                setting(report, "measurementUnit"),
                scaleFromLegacyUnit(report));
        if (isBillion(value)) {
            return "Billion";
        }
        return DEFAULT_UNIT;
    }

    public static String getMeasurementLabel(SampleReport report) {
        String type = getMeasurementType(report);
        String inner = getCurrency(report) + " " + getUnit(report);
        if (inner.isBlank() || isMeasurementType(inner)) {
            inner = DEFAULT_CURRENCY + " " + DEFAULT_UNIT;
        }
        return type + " (" + inner + ")";
    }

    private static String currencyFromLegacyUnit(SampleReport report) {
        String raw = report != null ? report.getUnit() : null;
        if (raw == null || raw.isBlank() || isMeasurementType(raw) || isMeasurementScale(raw)) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length >= 2 && isMeasurementScale(parts[parts.length - 1]) && !isMeasurementType(parts[0])) {
            return parts[0];
        }
        return null;
    }

    private static String scaleFromLegacyUnit(SampleReport report) {
        String raw = report != null ? report.getUnit() : null;
        if (raw == null || raw.isBlank() || isMeasurementType(raw)) {
            return null;
        }
        if (isMeasurementScale(raw)) {
            return raw.trim();
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length >= 2 && isMeasurementScale(parts[parts.length - 1])) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private static boolean isMeasurementType(String value) {
        return isValue(value) || isVolume(value);
    }

    private static boolean isValue(String value) {
        return value != null && "Value".equalsIgnoreCase(value.trim());
    }

    private static boolean isVolume(String value) {
        return value != null && "Volume".equalsIgnoreCase(value.trim());
    }

    private static boolean isBillion(String value) {
        return value != null && "Billion".equalsIgnoreCase(value.trim());
    }

    private static boolean isMeasurementScale(String value) {
        return value != null
                && ("Million".equalsIgnoreCase(value.trim())
                || "Billion".equalsIgnoreCase(value.trim()));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String setting(SampleReport report, String key) {
        if (report == null || report.getReportConfig() == null || report.getReportConfig().isBlank()) {
            return null;
        }
        try {
            Map<String, Object> model = OBJECT_MAPPER.readValue(
                    report.getReportConfig(),
                    new TypeReference<Map<String, Object>>() {
                    });
            Object settings = model.get("settings");
            if (!(settings instanceof Map<?, ?> settingsMap)) {
                return null;
            }
            Object value = settingsMap.get(key);
            if (value == null) {
                return null;
            }
            String text = String.valueOf(value).trim();
            return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
        } catch (Exception ignored) {
            return null;
        }
    }
}
