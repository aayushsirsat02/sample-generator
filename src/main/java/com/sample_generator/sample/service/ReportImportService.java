package com.sample_generator.sample.service;

import com.sample_generator.sample.dto.ExtractedReportData;
import com.sample_generator.sample.dto.SegmentData;
import com.sample_generator.sample.Entity.Company;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.repository.SampleReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sample_generator.sample.Entity.User;
import com.sample_generator.sample.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sample_generator.sample.Entity.SampleReportSource;
import java.net.URI;
import java.time.LocalDateTime;

@Service
public class ReportImportService {

    private final SampleReportRepository sampleReportRepository;
    private final UserRepository userRepository;

    public ReportImportService(
            SampleReportRepository sampleReportRepository,
            UserRepository userRepository) {

        this.sampleReportRepository = sampleReportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SampleReport createSampleReport(
            ExtractedReportData data) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new IllegalStateException(
                    "No authenticated user found"
            );
        }

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found: " + username
                        )
                );

        SampleReport sampleReport = new SampleReport();

        sampleReport.setCreatedBy(user);

        sampleReport.setKeyId(data.getReportId());
        sampleReport.setKeyName(data.getReportTitle());


        /*
         * Default Sample Report values
         */
        sampleReport.setLanguage("English");

        sampleReport.setCategory("General");

        sampleReport.setScope("Global");

        sampleReport.setScopeName("Global");

        //sampleReport.setHistoricYear(2020);

        //sampleReport.setBaseYear(2025);

        //sampleReport.setForecastYear(2035);

        sampleReport.setUnit("Value");

        sampleReport.setValueVolume("Value");

        //sampleReport.setMarketValueBaseYear(0.0);

        //sampleReport.setMarketValueForecastYear(0.0);

        sampleReport.setHistoricYear(
                data.getHistoricYear() != null
                        ? data.getHistoricYear()
                        : 2020
        );

        sampleReport.setBaseYear(
                data.getBaseYear() != null
                        ? data.getBaseYear()
                        : 2025
        );

        sampleReport.setForecastYear(
                data.getForecastYear() != null
                        ? data.getForecastYear()
                        : 2035
        );

        sampleReport.setMarketValueBaseYear(
                data.getMarketValueBaseYear() != null
                        ? data.getMarketValueBaseYear()
                        : 0.0
        );

        sampleReport.setMarketValueForecastYear(
                data.getMarketValueForecastYear() != null
                        ? data.getMarketValueForecastYear()
                        : 0.0
        );

        sampleReport.setCagr(
                data.getCagr() != null
                        ? data.getCagr()
                        : 0.0
        );

        sampleReport.setIsEdited(false);


        String reportConfig = createDefaultReportConfig(
                data.getReportId(),
                data.getReportTitle()
        );

        sampleReport.setReportConfig(reportConfig);
        sampleReport.setOriginalConfig(reportConfig);


        /*
         * SOURCE INFORMATION
         *
         * Stores where this sample report was imported from.
         */
        SampleReportSource source = new SampleReportSource();

        source.setSourceUrl(data.getSourceUrl());

        try {
            source.setSourceDomain(
                    URI.create(data.getSourceUrl()).getHost()
            );
        } catch (Exception e) {
            source.setSourceDomain(null);
        }

        source.setSourceReportId(data.getReportId());

        source.setFetchedAt(
                LocalDateTime.now()
        );

        source.setExtractionStatus("SUCCESS");

        source.setSampleReport(sampleReport);

        sampleReport.setSource(source);


        /*
         * Import segments
         */
        for (SegmentData segmentData : data.getSegments()) {

            MarketSegment parentSegment =
                    new MarketSegment();

            parentSegment.setSegmentName(
                    segmentData.getName()
            );

            parentSegment.setSampleReport(
                    sampleReport
            );


            /*
             * Import child segment values
             */
            for (String value : segmentData.getValues()) {

                MarketSegment childSegment =
                        new MarketSegment();

                childSegment.setSegmentName(value);

                childSegment.setSampleReport(
                        sampleReport
                );

                childSegment.setParent(
                        parentSegment
                );

                parentSegment.getChildren()
                        .add(childSegment);
            }


            sampleReport.getMarketSegments()
                    .add(parentSegment);
        }


        /*
         * Import companies
         */
        for (String companyName : data.getCompanies()) {

            Company company = new Company();

            company.setCompanyName(companyName);

            company.setSampleReport(
                    sampleReport
            );

            sampleReport.getCompanies()
                    .add(company);
        }


        /*
         * Save complete report.
         *
         * CascadeType.ALL will save:
         *
         * SampleReport
         * ├── SampleReportSource
         * ├── MarketSegments
         * │     └── Child Segments
         * └── Companies
         */
        return sampleReportRepository.save(sampleReport);
    }

    private String createDefaultReportConfig(
            String keyId,
            String keyName) {

        return """
            {
              "format": "pdf",
              "metadata": {
                "keyName": "%s",
                "keyId": "%s"
              },
              "settings": {},
              "toc": true,
              "theme": {
                "name": "Blue Corporate",
                "accentColor": "#ECC94B",
                "primaryColor": "#002060",
                "secondaryColor": "#2B6CB0"
              },
              "cover": {
                "keyId": "%s"
              },
              "sections": []
            }
            """.formatted(
                escapeJson(keyName),
                escapeJson(keyId),
                escapeJson(keyId)
        );
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }



}