package com.sample_generator.sample.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.PdfGenerationService;
import com.sample_generator.sample.repository.SampleReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages immutable original report configuration versus the editable working
 * copy.
 */
@Service
public class ReportConfigService {

    private static final Logger log = LoggerFactory.getLogger(ReportConfigService.class);

    private final SampleReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final PdfGenerationService pdfGenerationService;

    public ReportConfigService(SampleReportRepository reportRepository, ObjectMapper objectMapper,
            @Lazy PdfGenerationService pdfGenerationService) {
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
        this.pdfGenerationService = pdfGenerationService;
    }

    @Transactional
    public SampleReport ensureOriginalAndWorkingCopy(SampleReport report) {
        if (report.getOriginalConfig() != null && !report.getOriginalConfig().isBlank()) {
            if (report.getReportConfig() == null || report.getReportConfig().isBlank()) {
                report.setReportConfig(report.getOriginalConfig());
                report.setIsEdited(false);
                return reportRepository.save(report);
            }
            return report;
        }

        Map<String, Object> baseline = buildDefaultModel(report);
        String json = writeJson(baseline);
        report.setOriginalConfig(json);
        report.setReportConfig(json);
        report.setIsEdited(false);
        return reportRepository.save(report);
    }

    @Transactional
    public SampleReport initializeNewReportConfig(SampleReport report) {
        Map<String, Object> baseline = buildDefaultModel(report);
        String json = writeJson(baseline);
        report.setOriginalConfig(json);
        report.setReportConfig(json);
        report.setIsEdited(false);
        return report;
    }

    @Transactional
    public void saveWorkingConfig(SampleReport report, Map<String, Object> configPayload) {
        report = ensureOriginalAndWorkingCopy(report);
        try {
            String configString = objectMapper.writeValueAsString(configPayload);
            report.setReportConfig(configString);
            report.setIsEdited(!configString.equals(report.getOriginalConfig()));
            reportRepository.save(report);
            pdfGenerationService.requestGeneration(report.getId());
        } catch (Exception e) {
            log.error("Failed to save working report config for report {}", report.getId(), e);
            throw new RuntimeException("Failed to save report configuration", e);
        }
    }

    @Transactional
    public SampleReport resetWorkingConfig(SampleReport report) {
        report = ensureOriginalAndWorkingCopy(report);
        report.setReportConfig(report.getOriginalConfig());
        report.setIsEdited(false);
        SampleReport saved = reportRepository.save(report);
        pdfGenerationService.requestGeneration(saved.getId());
        return saved;
    }

    public Map<String, Object> resolveWorkingModel(SampleReport report) {
        report = ensureOriginalAndWorkingCopy(report);
        return resolveConfigJson(report, report.getReportConfig(), "pdf");
    }

    /**
     * Working theme over original defaults. Blank/null edited values keep the DEFAULT.
     * Does not persist missing configs, so legacy reports stay unchanged.
     */
    public Map<String, Object> resolveMergedTheme(SampleReport report) {
        Map<String, Object> merged = new LinkedHashMap<>(
                CategoryThemeDefaults.themeMapFor(report != null ? report.getCategory() : null));
        overlayNonBlank(merged, themeMap(parseQuiet(report.getOriginalConfig())));
        overlayNonBlank(merged, themeMap(parseQuiet(report.getReportConfig())));

        String coverImage = firstNonBlank(
                coverImage(parseQuiet(report.getReportConfig())),
                coverImage(parseQuiet(report.getOriginalConfig())),
                stringValue(merged.get("coverImage")));
        if (coverImage != null) {
            merged.put("coverImage", coverImage);
        }
        return merged;
    }

    public Map<String, Object> resolveOriginalModel(SampleReport report) {
        report = ensureOriginalAndWorkingCopy(report);
        return resolveConfigJson(report, report.getOriginalConfig(), "pdf");
    }

    public Map<String, Object> resolveConfigJson(SampleReport report, String configJson,
            String outputFormat) {
        if (configJson != null && !configJson.isBlank() && !configJson.trim().equals("{}")) {
            try {
                Map<String, Object> model = objectMapper.readValue(configJson,
                        new TypeReference<Map<String, Object>>() {
                        });
                model.put("format", outputFormat);
                ensureMetadata(model, report);
                return model;
            } catch (Exception e) {
                log.warn("Malformed stored config for report {}, falling back to default model",
                        report.getId(), e);
            }
        }
        return buildDefaultModel(report);
    }

    private void ensureMetadata(Map<String, Object> model, SampleReport report) {
        if (!model.containsKey("metadata") || model.get("metadata") == null) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("keyId", report.getKeyId());
            metadata.put("keyName", report.getKeyName());
            model.put("metadata", metadata);
        }

        if (!model.containsKey("settings") || !(model.get("settings") instanceof Map<?, ?>)) {
            model.put("settings", defaultMeasurementSettings());
        }
        if (!model.containsKey("toc")) {
            model.put("toc", true);
        }
        if (!model.containsKey("theme")) {
            model.put("theme", CategoryThemeDefaults.themeMapFor(report.getCategory()));
        }
        if (!model.containsKey("cover") || model.get("cover") == null) {
            model.put("cover", CategoryThemeDefaults.coverMapFor(report.getCategory(), report.getKeyId()));
        }
        if (!model.containsKey("sections")) {
            model.put("sections", java.util.List.of());
        }
    }

    private Map<String, Object> buildDefaultModel(SampleReport report) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("format", "pdf");
        model.put("metadata", Map.of(
                "keyId", report.getKeyId(),
                "keyName", report.getKeyName()));
        model.put("settings", defaultMeasurementSettings());
        model.put("toc", true);
        model.put("theme", CategoryThemeDefaults.themeMapFor(report.getCategory()));
        model.put("cover", CategoryThemeDefaults.coverMapFor(report.getCategory(), report.getKeyId()));
        model.put("sections", java.util.List.of());
        return model;
    }

    @SuppressWarnings("unchecked")
    public void mergeMeasurementSettings(
            SampleReport report,
            String measurementType,
            String currency,
            String unit) {
        if (isBlank(measurementType) && isBlank(currency) && isBlank(unit)) {
            return;
        }
        report = ensureOriginalAndWorkingCopy(report);
        Map<String, Object> model = parseQuiet(report.getReportConfig());
        if (model.isEmpty()) {
            model = buildDefaultModel(report);
        } else {
            model = new LinkedHashMap<>(model);
        }

        Map<String, Object> settings;
        Object existing = model.get("settings");
        if (existing instanceof Map<?, ?> map) {
            settings = new LinkedHashMap<>((Map<String, Object>) map);
        } else {
            settings = defaultMeasurementSettings();
        }
        if (!isBlank(measurementType)) {
            settings.put("measurementType",
                    "Volume".equalsIgnoreCase(measurementType.trim()) ? "Volume" : "Value");
        }
        if (!isBlank(currency)) {
            settings.put("currency", currency.trim());
        }
        if (!isBlank(unit)) {
            settings.put("unit", "Billion".equalsIgnoreCase(unit.trim()) ? "Billion" : "Million");
        }
        model.put("settings", settings);
        saveWorkingConfig(report, model);
    }

    public void syncOriginalConfigWithWorking(SampleReport report) {
        if (report.getReportConfig() != null && !report.getReportConfig().isBlank()) {
            report.setOriginalConfig(report.getReportConfig());
            report.setIsEdited(false);
            reportRepository.save(report);
        }
    }

    private static Map<String, Object> defaultMeasurementSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("measurementType", "Value");
        settings.put("currency", "USD");
        settings.put("unit", "Million");
        return settings;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String writeJson(Map<String, Object> model) {
        try {
            return objectMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report model", e);
        }
    }

    private Map<String, Object> parseQuiet(String json) {
        if (json == null || json.isBlank() || json.trim().equals("{}")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Malformed stored config while resolving theme, ignoring");
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> themeMap(Map<String, Object> model) {
        Object theme = model.get("theme");
        if (theme instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static String coverImage(Map<String, Object> model) {
        String fromTheme = stringValue(themeMap(model).get("coverImage"));
        Object cover = model.get("cover");
        if (cover instanceof Map<?, ?> coverMap) {
            String background = stringValue(((Map<String, Object>) coverMap).get("backgroundImage"));
            if (background != null) {
                return background;
            }
        }
        return fromTheme;
    }

    private static void overlayNonBlank(Map<String, Object> target, Map<String, Object> overlay) {
        for (Map.Entry<String, Object> entry : overlay.entrySet()) {
            String value = stringValue(entry.getValue());
            if (value != null) {
                target.put(entry.getKey(), value);
            }
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }
}
