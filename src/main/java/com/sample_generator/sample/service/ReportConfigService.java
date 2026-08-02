package com.sample_generator.sample.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.repository.SampleReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ReportConfigService(SampleReportRepository reportRepository, ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
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
        } catch (Exception e) {
            log.error("Failed to save working report config for report {}", report.getId(), e);
            throw new RuntimeException("Failed to save report configuration", e);
        }
    }

    public Map<String, Object> resolveWorkingModel(SampleReport report) {
        report = ensureOriginalAndWorkingCopy(report);
        return resolveConfigJson(report, report.getReportConfig(), "pdf");
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

        if (!model.containsKey("settings")) {
            model.put("settings", new LinkedHashMap<>());
        }
        if (!model.containsKey("toc")) {
            model.put("toc", true);
        }
        if (!model.containsKey("theme")) {
            model.put("theme", Map.of(
                    "name", "Blue Corporate",
                    "primaryColor", "#002060",
                    "secondaryColor", "#2B6CB0",
                    "accentColor", "#ECC94B"));
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
        model.put("settings", new LinkedHashMap<>());
        model.put("toc", true);
        model.put("theme", Map.of(
                "name", "Blue Corporate",
                "primaryColor", "#002060",
                "secondaryColor", "#2B6CB0",
                "accentColor", "#ECC94B"));
        model.put("cover", Map.of("keyId", report.getKeyId()));
        model.put("sections", java.util.List.of());
        return model;
    }

    private String writeJson(Map<String, Object> model) {
        try {
            return objectMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report model", e);
        }
    }
}
