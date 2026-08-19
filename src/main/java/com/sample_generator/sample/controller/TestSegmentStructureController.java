package com.sample_generator.sample.controller;

import com.sample_generator.sample.service.ReportUrlFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestSegmentStructureController {

    private final ReportUrlFetcher reportUrlFetcher;

    public TestSegmentStructureController(ReportUrlFetcher reportUrlFetcher) {
        this.reportUrlFetcher = reportUrlFetcher;
    }

    @GetMapping("/segment-structure")
    public String inspect(@RequestParam String url) {

        try {

            Document document = reportUrlFetcher.fetch(url);

            StringBuilder result = new StringBuilder();

            Elements strongElements = document.select("strong");

            for (Element strong : strongElements) {

                String text = strong.text().trim();

                if (text.equalsIgnoreCase("Market Segment")) {

                    result.append("\n====================================\n");
                    result.append("FOUND MARKET SEGMENT\n");
                    result.append("====================================\n");

                    Element parent = strong.parent();

                    result.append("\nSTRONG:\n");
                    result.append(strong.outerHtml());

                    result.append("\n\nPARENT:\n");
                    result.append(parent != null
                            ? parent.outerHtml()
                            : "null");

                    /*
                     * Show the next 10 elements after the parent.
                     */
                    result.append("\n\nNEXT ELEMENTS:\n");

                    Element current = parent;

                    for (int i = 1; i <= 10; i++) {

                        if (current == null) {
                            break;
                        }

                        current = current.nextElementSibling();

                        if (current == null) {
                            break;
                        }

                        result.append("\n--- NEXT ")
                                .append(i)
                                .append(" ---\n");

                        result.append("TAG: ")
                                .append(current.tagName())
                                .append("\n");

                        result.append("TEXT: ")
                                .append(current.text())
                                .append("\n");

                        result.append("HTML:\n")
                                .append(current.outerHtml());
                    }

                    /*
                     * We only need the first Market Segment.
                     */
                    break;
                }
            }

            return result.toString();

        } catch (Exception e) {

            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/company-structure")
    public String inspectCompanies(@RequestParam String url) {

        try {

            Document document = reportUrlFetcher.fetch(url);

            StringBuilder result = new StringBuilder();

            Elements elements = document.select("strong");

            for (Element strong : elements) {

                String text = strong.text().trim();

                if (text.toLowerCase().contains("compan")
                        || text.toLowerCase().contains("player")) {

                    result.append("\n====================================\n");
                    result.append("FOUND POSSIBLE COMPANY SECTION\n");
                    result.append("====================================\n");

                    result.append("TAG: ")
                            .append(strong.tagName())
                            .append("\n");

                    result.append("TEXT: ")
                            .append(text)
                            .append("\n");

                    Element parent = strong.parent();

                    result.append("\nPARENT HTML:\n");

                    if (parent != null) {
                        result.append(parent.outerHtml());
                    }

                    result.append("\n\nNEXT 10 ELEMENTS:\n");

                    Element current = parent;

                    for (int i = 1; i <= 10; i++) {

                        if (current == null) {
                            break;
                        }

                        current = current.nextElementSibling();

                        if (current == null) {
                            break;
                        }

                        result.append("\n--- NEXT ")
                                .append(i)
                                .append(" ---\n");

                        result.append("TAG: ")
                                .append(current.tagName())
                                .append("\n");

                        result.append("TEXT: ")
                                .append(current.text())
                                .append("\n");

                        result.append("HTML:\n")
                                .append(current.outerHtml());
                    }

                    result.append("\n\n");
                }
            }

            return result.toString();

        } catch (Exception e) {

            return "Error: " + e.getMessage();
        }
    }


}