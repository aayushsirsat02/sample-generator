package com.sample_generator.sample.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "market_segments")
public class MarketSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "segment_name", nullable = false)
    private String segmentName;


    /*
     * REPORT RELATIONSHIP
     *
     * Many segments can belong to one report.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_report_id", nullable = false)
    private SampleReport sampleReport;


    /*
     * PARENT SEGMENT
     *
     * Example:
     *
     * Machine Learning
     * parent = Technology
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MarketSegment parent;


    /*
     * CHILD SEGMENTS
     *
     * Example:
     *
     * Technology
     *      ↓
     * Machine Learning
     *      ↓
     * Deep Learning
     */

    @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MarketSegment> children = new ArrayList<>();


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getSegmentName() {
        return segmentName;
    }


    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }


    public SampleReport getSampleReport() {
        return sampleReport;
    }


    public void setSampleReport(SampleReport sampleReport) {
        this.sampleReport = sampleReport;
    }


    public MarketSegment getParent() {
        return parent;
    }


    public void setParent(MarketSegment parent) {
        this.parent = parent;
    }


    public List<MarketSegment> getChildren() {
        return children;
    }


    public void setChildren(List<MarketSegment> children) {
        this.children = children;
    }
}