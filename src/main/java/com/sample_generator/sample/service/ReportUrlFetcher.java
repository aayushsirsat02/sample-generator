package com.sample_generator.sample.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ReportUrlFetcher {

    private static final int TIMEOUT = 15000;

    public Document fetch(String url) throws IOException {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Report URL cannot be empty");
        }

        return Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/151.0.0.0 Safari/537.36"
                )
                .timeout(TIMEOUT)
                .followRedirects(true)
                .get();
    }
}