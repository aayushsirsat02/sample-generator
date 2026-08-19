package com.sample_generator.sample.controller;

import com.sample_generator.sample.dto.ExtractedReportData;
import com.sample_generator.sample.dto.ReportUrlRequest;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.service.ReportImportService;
import com.sample_generator.sample.service.ReportParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sample-reports")
public class ReportImportController {

    private final ReportParser reportParser;
    private final ReportImportService reportImportService;

    public ReportImportController(
            ReportParser reportParser,
            ReportImportService reportImportService) {

        this.reportParser = reportParser;
        this.reportImportService = reportImportService;
    }


    @PostMapping("/import-url")
    public ResponseEntity<?> importFromUrl(
            @RequestBody ReportUrlRequest request) {

        try {

            /*
             * Validate URL
             */
            if (request == null
                    || request.getUrl() == null
                    || request.getUrl().isBlank()) {

                return ResponseEntity
                        .badRequest()
                        .body("Report URL is required");
            }


            /*
             * STEP 1
             *
             * Fetch webpage and extract:
             *
             * Report ID
             * Report Title
             * Segments
             * Companies
             */
            ExtractedReportData data =
                    reportParser.parse(
                            request.getUrl()
                    );


            /*
             * STEP 2
             *
             * Convert extracted data into:
             *
             * SampleReport
             * MarketSegment
             * Company
             * SampleReportSource
             *
             * and save everything to database.
             */
            SampleReport sampleReport =
                    reportImportService
                            .createSampleReport(data);


            /*
             * SUCCESS RESPONSE
             */
            return ResponseEntity.ok(
                    new ImportResponse(
                            sampleReport.getId(),
                            sampleReport.getKeyId(),
                            sampleReport.getKeyName(),
                            "Report imported successfully"
                    )
            );


        } catch (Exception e) {

            /*
             * For now return the actual error.
             * Later we can replace this with
             * proper global exception handling.
             */
            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Report import failed: "
                                    + e.getMessage()
                    );
        }
    }


    /*
     * Response DTO
     */
    public static class ImportResponse {

        private Long sampleReportId;

        private String reportId;

        private String reportTitle;

        private String message;


        public ImportResponse(
                Long sampleReportId,
                String reportId,
                String reportTitle,
                String message) {

            this.sampleReportId = sampleReportId;
            this.reportId = reportId;
            this.reportTitle = reportTitle;
            this.message = message;
        }


        public Long getSampleReportId() {
            return sampleReportId;
        }


        public String getReportId() {
            return reportId;
        }


        public String getReportTitle() {
            return reportTitle;
        }


        public String getMessage() {
            return message;
        }
    }
}