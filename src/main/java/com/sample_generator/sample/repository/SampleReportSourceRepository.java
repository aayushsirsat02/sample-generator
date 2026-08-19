package com.sample_generator.sample.repository;

import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.Entity.SampleReportSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SampleReportSourceRepository
        extends JpaRepository<SampleReportSource, Long> {

    Optional<SampleReportSource> findBySampleReport(SampleReport sampleReport);

    Optional<SampleReportSource> findBySourceUrl(String sourceUrl);

    Optional<SampleReportSource> findBySourceReportId(String sourceReportId);
}