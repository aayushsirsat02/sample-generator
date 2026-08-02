package com.sample_generator.sample.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "company_name", nullable = false)
    private String companyName;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_report_id", nullable = false)
    private SampleReport sampleReport;


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getCompanyName() {
        return companyName;
    }


    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    public SampleReport getSampleReport() {
        return sampleReport;
    }


    public void setSampleReport(SampleReport sampleReport) {
        this.sampleReport = sampleReport;
    }
}