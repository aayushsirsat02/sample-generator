package com.sample_generator.sample.Entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sample_reports")
public class SampleReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_id", nullable = false, unique = true)
    private String keyId;

    @Column(name = "key_name", nullable = false, length = 1000)
    private String keyName;

    @Column(name = "scope")
    private String scope;

    @Column(name = "scope_name")
    private String scopeName;

    @Column(name = "value_volume")
    private String valueVolume;

    @Column(name = "unit")
    private String unit;

    @Column(name = "language")
    private String language;

    @Column(name = "historic_year")
    private Integer historicYear;

    @Column(name = "base_year")
    private Integer baseYear;

    @Column(name = "forecast_year")
    private Integer forecastYear;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cagr")
    private Double cagr;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;


    @OneToMany(
            mappedBy = "sampleReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MarketSegment> marketSegments = new ArrayList<>();




    @OneToMany(
            mappedBy = "sampleReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Company> companies = new ArrayList<>();



    @Getter
    @OneToOne(
            mappedBy = "sampleReport",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private SampleReportSource source;

    public void setSource(SampleReportSource source) {
        this.source = source;
    }
    /*
     * NEW FIELDS — Market Values & Category
     *
     * marketValueBaseYear    : market value at base year (optional)
     * marketValueForecastYear: market value at forecast year (optional)
     * category               : report category (optional, can be auto-set)
     */

    @Column(name = "market_value_base_year")
    private Double marketValueBaseYear;

    @Column(name = "market_value_forecast_year")
    private Double marketValueForecastYear;

    @Column(name = "category")
    private String category;

    @Column(name = "report_config", columnDefinition = "LONGTEXT")
    private String reportConfig;

    @Column(name = "original_config", columnDefinition = "LONGTEXT")
    private String originalConfig;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

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

    public boolean isCountryScope() {
        return scope != null && "country".equalsIgnoreCase(scope.trim());
    }

    public boolean isRegionalScope() {
        return scope != null && "regional".equalsIgnoreCase(scope.trim());
    }

    public String geoScopeLabel() {
        if ((isCountryScope() || isRegionalScope()) && scopeName != null && !scopeName.isBlank()) {
            return scopeName.trim();
        }
        return "Global";
    }

    public String geoScopeLabelUpper() {
        return geoScopeLabel().toUpperCase(java.util.Locale.ROOT);
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

    public int getBaseYear() {
        return baseYear;
    }

    public void setBaseYear(Integer baseYear) {
        this.baseYear = baseYear;
    }

    public int getForecastYear() {
        return forecastYear;
    }

    public void setForecastYear(Integer forecastYear) {
        this.forecastYear = forecastYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MarketSegment> getMarketSegments() {
        return marketSegments;
    }

    public void setMarketSegments(List<MarketSegment> marketSegments) {
        this.marketSegments = marketSegments;
    }

    public List<Company> getCompanies() {
        return companies;
    }

    public void setCompanies(List<Company> companies) {
        this.companies = companies;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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

    public String getReportConfig() {
        return reportConfig;
    }

    public void setReportConfig(String reportConfig) {
        this.reportConfig = reportConfig;
    }

    public String getOriginalConfig() {
        return originalConfig;
    }

    public void setOriginalConfig(String originalConfig) {
        this.originalConfig = originalConfig;
    }

    public Boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }

    public Double getCagr() {
        return cagr;
    }

    public void setCagr(Double cagr) {
        this.cagr = cagr;
    }

}