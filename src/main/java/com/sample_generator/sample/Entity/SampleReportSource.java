package com.sample_generator.sample.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sample_report_sources")
public class SampleReportSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_domain", length = 255)
    private String sourceDomain;

    @Column(name = "source_report_id", length = 255)
    private String sourceReportId;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "extraction_status", length = 50)
    private String extractionStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_report_id", nullable = false, unique = true)
    private SampleReport sampleReport;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceDomain() {
        return sourceDomain;
    }

    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    public String getSourceReportId() {
        return sourceReportId;
    }

    public void setSourceReportId(String sourceReportId) {
        this.sourceReportId = sourceReportId;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getExtractionStatus() {
        return extractionStatus;
    }

    public void setExtractionStatus(String extractionStatus) {
        this.extractionStatus = extractionStatus;
    }

    public SampleReport getSampleReport() {
        return sampleReport;
    }

    public void setSampleReport(SampleReport sampleReport) {
        this.sampleReport = sampleReport;
    }
}