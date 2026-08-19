package com.sample_generator.sample.controller;

import com.sample_generator.sample.service.ReportUrlFetcher;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestFetchController {

    private final ReportUrlFetcher reportUrlFetcher;

    public TestFetchController(ReportUrlFetcher reportUrlFetcher) {
        this.reportUrlFetcher = reportUrlFetcher;
    }

    @GetMapping("/fetch")
    public String fetch(@RequestParam String url) {

        try {

            Document document = reportUrlFetcher.fetch(url);

            return """
                    Fetch successful!

                    URL: %s
                    Title: %s
                    HTML Length: %d
                    """.formatted(
                    url,
                    document.title(),
                    document.html().length()
            );

        } catch (Exception e) {

            return "Fetch failed: " + e.getMessage();
        }
    }
}