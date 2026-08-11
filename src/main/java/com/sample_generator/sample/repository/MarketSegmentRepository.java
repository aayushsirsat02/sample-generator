package com.sample_generator.sample.repository;

import com.sample_generator.sample.Entity.MarketSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketSegmentRepository extends JpaRepository<MarketSegment, Long> {

    List<MarketSegment> findBySampleReportId(Long reportId);

}
