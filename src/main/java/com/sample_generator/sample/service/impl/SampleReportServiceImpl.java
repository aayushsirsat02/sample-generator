package com.sample_generator.sample.service.impl;

import com.sample_generator.sample.Entity.Company;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.Entity.User;
import com.sample_generator.sample.dto.*;
import com.sample_generator.sample.repository.SampleReportRepository;
import com.sample_generator.sample.repository.UserRepository;
import com.sample_generator.sample.service.SampleReportService;
import com.sample_generator.sample.service.ReportConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class SampleReportServiceImpl implements SampleReportService {

        private final SampleReportRepository sampleReportRepository;

        private final UserRepository userRepository;

        private final ReportConfigService reportConfigService;

        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        public SampleReportServiceImpl(SampleReportRepository sampleReportRepository,
                        UserRepository userRepository, ReportConfigService reportConfigService) {

                this.sampleReportRepository = sampleReportRepository;

                this.userRepository = userRepository;
                this.reportConfigService = reportConfigService;
        }

        /*
         * ================================================ GET MY REPORTS (current
         * user)
         * ================================================
         */

        @Override
        @Transactional(readOnly = true)
        public List<SampleReportListResponse> getMyReports(String username) {

                return sampleReportRepository.findByCreatedByUsernameOrderByCreatedAtDesc(username)
                                .stream()
                                .map(report -> new SampleReportListResponse(report.getId(),
                                                report.getKeyId(), report.getKeyName(),
                                                report.getCreatedAt() != null
                                                                ? report.getCreatedAt().format(
                                                                                DISPLAY_FORMATTER)
                                                                : ""))
                                .toList();
        }

        /*
         * ================================================ GET ALL REPORTS (ADMIN)
         * ================================================
         */

        @Override
        @Transactional(readOnly = true)
        public List<SampleReportListResponse> getAllReports() {

                return sampleReportRepository.findAllByOrderByCreatedAtDesc().stream()
                                .map(report -> new SampleReportListResponse(report.getId(),
                                                report.getKeyId(), report.getKeyName(),
                                                report.getCreatedAt() != null
                                                                ? report.getCreatedAt().format(
                                                                                DISPLAY_FORMATTER)
                                                                : ""))
                                .toList();
        }

        /*
         * ================================================ CREATE SAMPLE REPORT
         * ================================================
         */

        @Override
        @Transactional
        public Long createSampleReport(CreateSampleReportRequest request, String username) {

                /*
                 * CHECK DUPLICATE KEY ID
                 */

                if (sampleReportRepository.existsByKeyId(request.getKeyId())) {

                        throw new RuntimeException(
                                        "Report already exists with Key ID: " + request.getKeyId());
                }

                /*
                 * GET LOGGED-IN USER
                 */

                User user = userRepository.findByUsername(username).orElseThrow(
                                () -> new RuntimeException("User not found: " + username));

                /*
                 * CREATE REPORT ENTITY
                 */

                SampleReport report = new SampleReport();

                report.setKeyId(request.getKeyId());

                report.setKeyName(request.getKeyName());

                report.setScope(request.getScope());

                report.setScopeName(request.getScopeName());

                report.setValueVolume(request.getValueVolume());

                report.setUnit(request.getUnit());

                report.setLanguage(request.getLanguage());

                report.setHistoricYear(request.getHistoricYear());

                report.setBaseYear(request.getBaseYear());

                report.setForecastYear(request.getForecastYear());

                report.setMarketValueBaseYear(request.getMarketValueBaseYear());

                report.setMarketValueForecastYear(request.getMarketValueForecastYear());

                // Auto-set category if blank
                String category = request.getCategory();
                if (category == null || category.isBlank()) {
                        category = "General";
                }
                report.setCategory(category);

                /*
                 * SET LOGGED-IN USER
                 */

                report.setCreatedBy(user);

                /*
                 * CREATE SEGMENTS
                 */

                for (SegmentRequest segmentRequest : request.getSegments()) {

                        MarketSegment segment = createSegment(segmentRequest, report, null);

                        report.getMarketSegments().add(segment);
                }

                /*
                 * CREATE COMPANIES
                 */

                for (CompanyRequest companyRequest : request.getCompanies()) {

                        Company company = new Company();

                        company.setCompanyName(companyRequest.getCompanyName());

                        company.setSampleReport(report);

                        report.getCompanies().add(company);
                }

                /*
                 * SAVE REPORT
                 *
                 * CascadeType.ALL saves:
                 *
                 * Report Segments Child Segments Companies
                 */

                SampleReport savedReport = sampleReportRepository.save(report);

                reportConfigService.initializeNewReportConfig(savedReport);
                sampleReportRepository.save(savedReport);

                return savedReport.getId();
        }

        /*
         * ================================================ UPDATE SAMPLE REPORT
         * ================================================
         */

        @Override
        @Transactional
        public Long updateSampleReport(Long reportId, CreateSampleReportRequest request,
                        String username) {

                /*
                 * GET EXISTING REPORT
                 */

                SampleReport report = sampleReportRepository.findById(reportId).orElseThrow(
                                () -> new RuntimeException("Report not found: " + reportId));

                /*
                 * CHECK DUPLICATE KEY ID (excluding self)
                 */

                if (request.getKeyId() != null && !request.getKeyId().equals(report.getKeyId())
                                && sampleReportRepository.existsByKeyIdAndIdNot(request.getKeyId(),
                                                reportId)) {

                        throw new RuntimeException("Another report already exists with Key ID: "
                                        + request.getKeyId());
                }

                /*
                 * UPDATE FIELDS
                 */

                if (request.getKeyId() != null)
                        report.setKeyId(request.getKeyId());
                if (request.getKeyName() != null)
                        report.setKeyName(request.getKeyName());
                if (request.getScope() != null)
                        report.setScope(request.getScope());
                if (request.getScopeName() != null)
                        report.setScopeName(request.getScopeName());
                if (request.getValueVolume() != null)
                        report.setValueVolume(request.getValueVolume());
                if (request.getUnit() != null)
                        report.setUnit(request.getUnit());
                if (request.getLanguage() != null)
                        report.setLanguage(request.getLanguage());
                if (request.getHistoricYear() != null)
                        report.setHistoricYear(request.getHistoricYear());
                if (request.getBaseYear() != null)
                        report.setBaseYear(request.getBaseYear());
                if (request.getForecastYear() != null)
                        report.setForecastYear(request.getForecastYear());
                if (request.getMarketValueBaseYear() != null)
                        report.setMarketValueBaseYear(request.getMarketValueBaseYear());
                if (request.getMarketValueForecastYear() != null)
                        report.setMarketValueForecastYear(request.getMarketValueForecastYear());

                String category = request.getCategory();
                if (category != null && !category.isBlank()) {
                        report.setCategory(category);
                }

                /*
                 * REPLACE SEGMENTS
                 *
                 * Clear old segments and replace with new ones. orphanRemoval = true handles
                 * deletion automatically.
                 */

                report.getMarketSegments().clear();

                for (SegmentRequest segmentRequest : request.getSegments()) {

                        MarketSegment segment = createSegment(segmentRequest, report, null);

                        report.getMarketSegments().add(segment);
                }

                /*
                 * REPLACE COMPANIES
                 */

                report.getCompanies().clear();

                for (CompanyRequest companyRequest : request.getCompanies()) {

                        Company company = new Company();

                        company.setCompanyName(companyRequest.getCompanyName());

                        company.setSampleReport(report);

                        report.getCompanies().add(company);
                }

                SampleReport savedReport = sampleReportRepository.save(report);

                return savedReport.getId();
        }

        /*
         * RECURSIVE SEGMENT CREATION
         */

        private MarketSegment createSegment(SegmentRequest request, SampleReport report,
                        MarketSegment parent) {

                MarketSegment segment = new MarketSegment();

                segment.setSegmentName(request.getSegmentName());

                segment.setSampleReport(report);

                segment.setParent(parent);

                /*
                 * CREATE CHILDREN RECURSIVELY
                 */

                for (SegmentRequest childRequest : request.getChildren()) {

                        MarketSegment child = createSegment(childRequest, report, segment);

                        segment.getChildren().add(child);
                }

                return segment;
        }

        /*
         * ================================================ GET REPORT BY ID
         * ================================================
         */

        @Override
        @Transactional(readOnly = true)
        public SampleReportDetailResponse getReportById(Long reportId, String username) {

                SampleReport report = sampleReportRepository.findById(reportId).orElseThrow(
                                () -> new RuntimeException("Report not found: " + reportId));

                SampleReportDetailResponse response = new SampleReportDetailResponse();

                response.setId(report.getId());

                response.setKeyId(report.getKeyId());

                response.setKeyName(report.getKeyName());

                response.setScope(report.getScope());

                response.setScopeName(report.getScopeName());

                response.setValueVolume(report.getValueVolume());

                response.setUnit(report.getUnit());

                response.setLanguage(report.getLanguage());

                response.setHistoricYear(report.getHistoricYear());

                response.setBaseYear(report.getBaseYear());

                response.setForecastYear(report.getForecastYear());

                response.setMarketValueBaseYear(report.getMarketValueBaseYear());

                response.setMarketValueForecastYear(report.getMarketValueForecastYear());

                response.setCategory(report.getCategory());

                // Read-only info panel fields
                if (report.getCreatedAt() != null) {
                        response.setCreatedAt(report.getCreatedAt().format(DISPLAY_FORMATTER));
                }

                if (report.getCreatedBy() != null) {
                        response.setCreatedByUsername(report.getCreatedBy().getUsername());
                }

                /*
                 * CONVERT SEGMENTS
                 */

                List<SegmentRequest> segments = report.getMarketSegments().stream()
                                .filter(seg -> seg.getParent() == null)
                                .map(this::convertSegmentToDTO).toList();

                response.setSegments(segments);

                /*
                 * CONVERT COMPANIES
                 */

                List<CompanyRequest> companies = report.getCompanies().stream().map(company -> {

                        CompanyRequest dto = new CompanyRequest();

                        dto.setCompanyName(company.getCompanyName());

                        return dto;

                }).toList();

                response.setCompanies(companies);

                return response;
        }

        /*
         * ================================================ GENERATE WORD REPORT
         * ================================================
         */

        @Override
        @Transactional(readOnly = true)
        public byte[] generateWordReport(Long reportId, String username) throws Exception {
                throw new UnsupportedOperationException(
                                "Word generation has been moved to the future PPT/DOCX engine roadmap.");
        }

        @Override
        @Transactional(readOnly = true)
        public Map<String, Object> getReportConfigExport(Long reportId, String username) {
                SampleReport report = sampleReportRepository.findById(reportId).orElseThrow(
                                () -> new RuntimeException("Report not found: " + reportId));
                report = reportConfigService.ensureOriginalAndWorkingCopy(report);
                return reportConfigService.resolveWorkingModel(report);
        }

        /*
         * ================================================ SEARCH REPORTS
         * ================================================
         */

        @Override
        @Transactional(readOnly = true)
        public List<SampleReportListResponse> searchMyReports(String query, String username,
                        boolean isAdmin) {

                List<SampleReport> results;
                if (isAdmin) {
                        results = sampleReportRepository
                                        .findByKeyIdContainingIgnoreCaseOrKeyNameContainingIgnoreCase(
                                                        query, query);
                } else {
                        results = sampleReportRepository
                                        .findByCreatedByUsernameAndKeyIdContainingIgnoreCaseOrCreatedByUsernameAndKeyNameContainingIgnoreCase(
                                                        username, query, username, query);
                }

                return results.stream().map(report -> new SampleReportListResponse(report.getId(),
                                report.getKeyId(), report.getKeyName(),
                                report.getCreatedAt() != null
                                                ? report.getCreatedAt().format(DISPLAY_FORMATTER)
                                                : ""))
                                .toList();
        }

        private SegmentRequest convertSegmentToDTO(MarketSegment segment) {

                SegmentRequest dto = new SegmentRequest();

                dto.setSegmentName(segment.getSegmentName());

                List<SegmentRequest> children = segment.getChildren().stream()
                                .map(this::convertSegmentToDTO).toList();

                dto.setChildren(children);

                return dto;
        }

}
