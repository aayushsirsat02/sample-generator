package com.sample_generator.sample.dto;

import java.util.List;

public class SampleReportDetailResponse {

    private Long id;
    private String keyId;
    private String keyName;
    private String scope;
    private String scopeName;
    private String valueVolume;
    private String unit;
    private String language;
    private Integer historicYear;
    private Integer baseYear;
    private Integer forecastYear;

    /*
     * NEW FIELDS
     */
    private Double marketValueBaseYear;
    private Double marketValueForecastYear;
    private String category;

    /*
     * READ-ONLY INFO (for the right-side info panel in the edit view)
     */
    private String createdAt;
    private String createdByUsername;

    private List<SegmentRequest> segments;
    private List<CompanyRequest> companies;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public String getValueVolume() {
        return valueVolume;
    }

    public void setValueVolume(String valueVolume) {
        this.valueVolume = valueVolume;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getHistoricYear() {
        return historicYear;
    }

    public void setHistoricYear(Integer historicYear) {
        this.historicYear = historicYear;
    }

    public Integer getBaseYear() {
        return baseYear;
    }

    public void setBaseYear(Integer baseYear) {
        this.baseYear = baseYear;
    }

    public Integer getForecastYear() {
        return forecastYear;
    }

    public void setForecastYear(Integer forecastYear) {
        this.forecastYear = forecastYear;
    }

    public Double getMarketValueBaseYear() {
        return marketValueBaseYear;
    }

    public void setMarketValueBaseYear(Double marketValueBaseYear) {
        this.marketValueBaseYear = marketValueBaseYear;
    }

    public Double getMarketValueForecastYear() {
        return marketValueForecastYear;
    }

    public void setMarketValueForecastYear(Double marketValueForecastYear) {
        this.marketValueForecastYear = marketValueForecastYear;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public List<SegmentRequest> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentRequest> segments) {
        this.segments = segments;
    }

    public List<CompanyRequest> getCompanies() {
        return companies;
    }

    public void setCompanies(List<CompanyRequest> companies) {
        this.companies = companies;
    }
}