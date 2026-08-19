package com.sample_generator.sample.controller;

import com.sample_generator.sample.dto.CreateSampleReportRequest;
import com.sample_generator.sample.dto.SampleReportDetailResponse;
import com.sample_generator.sample.dto.SampleReportListResponse;
import com.sample_generator.sample.service.SampleReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class SampleReportController {

        private final SampleReportService sampleReportService;

        public SampleReportController(SampleReportService sampleReportService) {

                this.sampleReportService = sampleReportService;
        }

        /*
         * ================================================ CREATE SAMPLE REPORT POST /api/reports
         * ================================================
         */

        @PostMapping
        public ResponseEntity<Long> createSampleReport(

                        @RequestBody CreateSampleReportRequest request,

                        Authentication authentication

        ) {

                String username = authentication.getName();

                Long reportId = sampleReportService.createSampleReport(request, username);

                return ResponseEntity.ok(reportId);
        }

        /*
         * ================================================ UPDATE SAMPLE REPORT PUT
         * /api/reports/{reportId} ================================================
         */

        @PutMapping("/{reportId}")
        public ResponseEntity<Long> updateSampleReport(

                        @PathVariable Long reportId,

                        @RequestBody CreateSampleReportRequest request,

                        Authentication authentication

        ) {

                String username = authentication.getName();

                Long updatedId = sampleReportService.updateSampleReport(reportId, request,
                                username);

                return ResponseEntity.ok(updatedId);
        }

        /*
         * ================================================ GET MY REPORTS GET
         * /api/reports/my-reports ================================================
         */

        @GetMapping("/my-reports")
        public ResponseEntity<List<SampleReportListResponse>> getMyReports(
                        Authentication authentication) {

                String username = authentication.getName();

                List<SampleReportListResponse> reports = sampleReportService.getMyReports(username);

                return ResponseEntity.ok(reports);
        }

        /*
         * ================================================ GET ALL REPORTS (ADMIN) GET
         * /api/reports/all ================================================
         */

        @GetMapping("/all")
        public ResponseEntity<List<SampleReportListResponse>> getAllReports(
                        Authentication authentication) {

                List<SampleReportListResponse> reports = sampleReportService.getAllReports();

                return ResponseEntity.ok(reports);
        }

        /*
         * ================================================ GET REPORT BY ID GET
         * /api/reports/{reportId} ================================================
         */

        @GetMapping("/{reportId}")
        public ResponseEntity<SampleReportDetailResponse> getReportById(

                        @PathVariable Long reportId,

                        Authentication authentication

        ) {

                String username = authentication.getName();

                SampleReportDetailResponse report =
                                sampleReportService.getReportById(reportId, username);

                return ResponseEntity.ok(report);
        }

        @DeleteMapping("/{reportId}")
        public ResponseEntity<Map<String, String>> deleteSampleReport(

                        @PathVariable Long reportId,

                        Authentication authentication

        ) {

                String username = authentication.getName();
                boolean isAdmin = authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                sampleReportService.deleteSampleReport(reportId, username, isAdmin);

                return ResponseEntity.ok(Map.of("message", "Report deleted successfully."));
        }

        /*
         * ================================================ DOWNLOAD WORD REPORT GET
         * /api/reports/{reportId}/word ================================================
         */

        @GetMapping("/{reportId}/word")
        public ResponseEntity<byte[]> downloadWordReport(

                        @PathVariable Long reportId,

                        Authentication authentication

        ) throws Exception {

                String username = authentication.getName();

                byte[] wordFile = sampleReportService.generateWordReport(reportId, username);

                return ResponseEntity.ok()

                                .header("Content-Disposition",
                                                "attachment; filename=sample-report-" + reportId
                                                                + ".docx")

                                .header("Content-Type",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

                                .body(wordFile);

        }

        /*
         * ================================================ DOWNLOAD PPT REPORT (STUB) GET
         * /api/reports/{reportId}/ppt ================================================
         */

        @GetMapping("/{reportId}/ppt")
        public ResponseEntity<Map<String, String>> downloadPptReport(

                        @PathVariable Long reportId,

                        Authentication authentication

        ) {

                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message",
                                "PPT generation coming soon. Report ID: " + reportId));

        }

        @GetMapping("/{reportId}/export/json")
        public ResponseEntity<Map<String, Object>> exportReportJson(@PathVariable Long reportId,
                        Authentication authentication) {
                return ResponseEntity.ok(sampleReportService.getReportConfigExport(reportId,
                                authentication.getName()));
        }

//        @PutMapping("/{reportId}/export/json")
//        public ResponseEntity<Map<String, Object>> saveReportConfig(@PathVariable Long reportId,
//                        @RequestBody Map<String, Object> configPayload,
//                        Authentication authentication) {
//                return ResponseEntity.ok(sampleReportService.saveReportConfig(reportId, configPayload,
//                                authentication.getName()));
//        }
//
//        @PostMapping("/{reportId}/export/json/reset")
//        public ResponseEntity<Map<String, Object>> resetReportConfig(@PathVariable Long reportId,
//                        Authentication authentication) {
//                return ResponseEntity.ok(sampleReportService.resetReportConfig(reportId,
//                                authentication.getName()));
//        }

        /*
         * ================================================ SEARCH REPORTS GET
         * /api/reports/search?query=... ================================================
         */

        @GetMapping("/search")
        public ResponseEntity<List<SampleReportListResponse>> searchReports(

                        @RequestParam String query,

                        Authentication authentication

        ) {

                String username = authentication.getName();

                boolean isAdmin = authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                List<SampleReportListResponse> reports =
                                sampleReportService.searchMyReports(query, username, isAdmin);

                return ResponseEntity.ok(reports);
        }

}
