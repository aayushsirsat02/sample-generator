package com.sample_generator.sample.controller;

import com.sample_generator.sample.dto.ExtractedReportData;
import com.sample_generator.sample.service.ReportParser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestParserController {

    private final ReportParser reportParser;

    public TestParserController(ReportParser reportParser) {
        this.reportParser = reportParser;
    }

    @GetMapping("/parse")
    public ExtractedReportData parse(
            @RequestParam String url) {

        try {

            return reportParser.parse(url);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse report: " + e.getMessage(),
                    e
            );
        }
    }
}