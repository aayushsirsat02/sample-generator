package com.sample_generator.sample.repository;

import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.PdfStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleReportRepository
        extends JpaRepository<SampleReport, Long> {

    boolean existsByKeyId(String keyId);

    boolean existsByKeyIdAndIdNot(String keyId, Long id);

    List<SampleReport>
    findByCreatedByUsernameOrderByCreatedAtDesc(
            String username
    );

    List<SampleReport>
    findAllByOrderByCreatedAtDesc();

    List<SampleReport>
    findByCreatedByUsernameAndKeyIdContainingIgnoreCaseOrCreatedByUsernameAndKeyNameContainingIgnoreCase(
            String username1,
            String keyId,
            String username2,
            String keyName
    );

    List<SampleReport>
    findByKeyIdContainingIgnoreCaseOrKeyNameContainingIgnoreCase(
            String keyId,
            String keyName
    );

    List<SampleReport> findByPdfStatus(PdfStatus pdfStatus);

}