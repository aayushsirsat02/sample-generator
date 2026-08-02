package com.sample_generator.sample.service;

import com.sample_generator.sample.dto.CreateSampleReportRequest;
import com.sample_generator.sample.dto.SampleReportDetailResponse;
import com.sample_generator.sample.dto.SampleReportListResponse;

import java.util.List;

import java.util.Map;

public interface SampleReportService {

        /*
         * CREATE SAMPLE REPORT
         */

        Long createSampleReport(
                        CreateSampleReportRequest request,
                        String username);

        /*
         * GET REPORTS CREATED BY
         * LOGGED-IN USER
         */

        List<SampleReportListResponse> getMyReports(
                        String username);

        /*
         * GET ALL REPORTS (ADMIN)
         */

        List<SampleReportListResponse> getAllReports();

        SampleReportDetailResponse getReportById(
                        Long reportId,
                        String username);

        /*
         * UPDATE SAMPLE REPORT
         */

        Long updateSampleReport(
                        Long reportId,
                        CreateSampleReportRequest request,
                        String username);

        byte[] generateWordReport(
                        Long reportId,
                        String username) throws Exception;

        Map<String, Object> getReportConfigExport(
                        Long reportId,
                        String username);

        List<SampleReportListResponse> searchMyReports(
                        String query,
                        String username,
                        boolean isAdmin);

}