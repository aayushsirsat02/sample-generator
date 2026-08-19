package com.sample_generator.sample.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class ExtractedReportData {

    @Setter
    @Getter
    private String reportId;

    private String reportTitle;

    private String sourceUrl;

    private List<SegmentData> segments = new ArrayList<>();

    private List<String> companies = new ArrayList<>();


    private Integer historicYear;
    private Integer baseYear;
    private Integer forecastYear;

    private Double marketValueBaseYear;
    private Double marketValueForecastYear;

    private Double cagr;

    private String scope;
    private String scopeName;

    private String unit;
    private String valueVolume;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }


    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }


    public List<SegmentData> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentData> segments) {
        this.segments = segments;
    }


    public List<String> getCompanies() {
        return companies;
    }

    public void setCompanies(List<String> companies) {
        this.companies = companies;
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

    public Double getCagr() {
        return cagr;
    }

    public void setCagr(Double cagr) {
        this.cagr = cagr;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getValueVolume() {
        return valueVolume;
    }

    public void setValueVolume(String valueVolume) {
        this.valueVolume = valueVolume;
    }
}
