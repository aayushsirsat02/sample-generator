package com.sample_generator.sample.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.PdfGenerationService;
import com.sample_generator.sample.repository.SampleReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AssetReferenceService {

    private final SampleReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final PdfGenerationService pdfGenerationService;

    public AssetReferenceService(SampleReportRepository reportRepository, ObjectMapper objectMapper,
            PdfGenerationService pdfGenerationService) {
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
        this.pdfGenerationService = pdfGenerationService;
    }

    public int countUsage(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            return 0;
        }
        int count = 0;
        for (SampleReport report : reportRepository.findAll()) {
            if (configContainsAsset(report.getReportConfig(), assetPath)
                    || configContainsAsset(report.getOriginalConfig(), assetPath)) {
                count++;
            }
        }
        return count;
    }

    public List<Map<String, Object>> findUsageDetails(String assetPath) {
        List<Map<String, Object>> usages = new ArrayList<>();
        for (SampleReport report : reportRepository.findAll()) {
            boolean inWorking = configContainsAsset(report.getReportConfig(), assetPath);
            boolean inOriginal = configContainsAsset(report.getOriginalConfig(), assetPath);
            if (inWorking || inOriginal) {
                usages.add(Map.of(
                        "reportId", report.getId(),
                        "keyId", report.getKeyId(),
                        "keyName", report.getKeyName(),
                        "inWorkingCopy", inWorking,
                        "inOriginal", inOriginal
                ));
            }
        }
        return usages;
    }

    @Transactional
    public void removeAssetReferences(String assetPath) {
        for (SampleReport report : reportRepository.findAll()) {
            boolean changed = false;
            if (report.getReportConfig() != null && configContainsAsset(report.getReportConfig(), assetPath)) {
                report.setReportConfig(stripAsset(report.getReportConfig(), assetPath));
                changed = true;
            }
            if (report.getOriginalConfig() != null && configContainsAsset(report.getOriginalConfig(), assetPath)) {
                report.setOriginalConfig(stripAsset(report.getOriginalConfig(), assetPath));
                changed = true;
            }
            if (changed) {
                reportRepository.save(report);
                pdfGenerationService.requestGeneration(report.getId());
            }
        }
    }

    private boolean configContainsAsset(String configJson, String assetPath) {
        return configJson != null && configJson.contains(assetPath);
    }

    private String stripAsset(String configJson, String assetPath) {
        try {
            Map<String, Object> model = objectMapper.readValue(configJson, new TypeReference<>() {});
            stripFromModel(model, assetPath);
            return objectMapper.writeValueAsString(model);
        } catch (Exception e) {
            return configJson.replace(assetPath, "");
        }
    }

    @SuppressWarnings("unchecked")
    private void stripFromModel(Map<String, Object> model, String assetPath) {
        Object cover = model.get("cover");
        if (cover instanceof Map<?, ?> coverMap) {
            clearIfMatches((Map<String, Object>) coverMap, "backgroundImage", assetPath);
            clearIfMatches((Map<String, Object>) coverMap, "logoImage", assetPath);
        }
        stripFromSections(model.get("sections"), assetPath);
    }

    @SuppressWarnings("unchecked")
    private void stripFromSections(Object sectionsObj, String assetPath) {
        if (!(sectionsObj instanceof Iterable<?> sections)) {
            return;
        }
        for (Object secObj : sections) {
            if (!(secObj instanceof Map<?, ?> section)) {
                continue;
            }
            clearIfMatches((Map<String, Object>) section, "backgroundImage", assetPath);
            Object content = section.get("content");
            if (content instanceof Iterable<?> blocks) {
                for (Object blockObj : blocks) {
                    if (blockObj instanceof Map<?, ?> block && "image".equals(block.get("type"))) {
                        clearIfMatches((Map<String, Object>) block, "src", assetPath);
                    }
                }
            }
            stripFromSections(section.get("subsections"), assetPath);
        }
    }

    private void clearIfMatches(Map<String, Object> map, String field, String assetPath) {
        Object val = map.get(field);
        if (val != null && assetPath.equals(String.valueOf(val))) {
            map.put(field, "");
        }
    }
}
