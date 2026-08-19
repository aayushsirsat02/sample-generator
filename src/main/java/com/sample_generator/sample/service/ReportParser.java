package com.sample_generator.sample.service;

import com.sample_generator.sample.dto.ExtractedReportData;
import com.sample_generator.sample.dto.SegmentData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;



import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportParser {

    private final ReportUrlFetcher reportUrlFetcher;

    public ReportParser(ReportUrlFetcher reportUrlFetcher) {
        this.reportUrlFetcher = reportUrlFetcher;
    }

    public ExtractedReportData parse(String url) throws Exception {

        Document document = reportUrlFetcher.fetch(url);

        ExtractedReportData data = new ExtractedReportData();

        data.setSourceUrl(url);

        String reportTitle = extractReportTitle(document);
        String reportId = extractReportId(document);

        data.setReportTitle(reportTitle);
        data.setReportId(reportId);


        /*
         * Extract market segments
         */
        List<SegmentData> segments =
                extractSegments(document);

        data.setSegments(segments);


        /*
         * Extract companies
         */
        List<String> companies =
                extractCompanies(document);

        data.setCompanies(companies);

        /*
         * Extract report metadata / market values
         */
        //data.setHistoricYear(extractHistoricYear(document));
        Integer baseYear =
                extractBaseYear(document);

        data.setBaseYear(baseYear);


        /*
         * Extract Forecast Year
         */
        Integer forecastYear =
                extractForecastYear(document);

        System.out.println(
                "Forecast Year: " + forecastYear
        );

        data.setForecastYear(forecastYear);


        /*
         * Extract Market Value in Base Year
         */
        Double marketValueBaseYear =
                extractBaseYearMarketValue(document);

        System.out.println(
                "Market Value Base Year: " + marketValueBaseYear
        );

        data.setMarketValueBaseYear(
                marketValueBaseYear
        );

        /*
         * Extract Market Value in Forecast Year
         */
        Double marketValueForecastYear =
                extractForecastYearMarketValue(document);

        System.out.println(
                "Market Value Forecast Year: "
                        + marketValueForecastYear
        );

        data.setMarketValueForecastYear(
                marketValueForecastYear
        );

        /*
         * Extract CAGR
         */
        Double cagr =
                extractCagr(document);

        System.out.println(
                "CAGR: " + cagr
        );

        data.setCagr(cagr);

        /*
         * Extract Historical Year
         */
        Integer historicYear =
                extractHistoricYear(document);

        System.out.println(
                "Historic Year: " + historicYear
        );

        data.setHistoricYear(historicYear);

        /*
         * Extract Scope
         */
        String scope =
                extractScope(document);

        System.out.println(
                "Scope: " + scope
        );

        data.setScope(scope);

        data.setScopeName(scope);

//        String language = extractLanguage(document);
//
//        System.out.println("Language: " + language);
//
//        data.setLanguage(language);




//        data.setForecastYear(extractForecastYear(document));
//
//        data.setMarketValueBaseYear(
//                extractBaseYearMarketValue(document)
//        );
//
//        data.setMarketValueForecastYear(
//                extractForecastYearMarketValue(document)
//        );
//
//        data.setCagr(
//                extractCagr(document)
//        );
//
//        data.setScope(
//                extractScope(document)
//        );
//
//        data.setScopeName(
//                extractScopeName(document)
//        );
//
//        data.setUnit(
//                extractUnit(document)
//        );
//
//        data.setValueVolume(
//                extractValueVolume(document)
//        );


        return data;
    }


    private String extractReportTitle(Document document) {

        /*
         * Both websites currently use the main H1
         * for the report title.
         */
        String title = document.select("h1").first() != null
                ? document.select("h1").first().text().trim()
                : null;

        if (title == null || title.isBlank()) {
            throw new IllegalStateException(
                    "Report title could not be found"
            );
        }

        return title;
    }


    private String extractReportId(Document document) {

        String pageText = document.body().text();

        /*
         * Decisions Advisors:
         * Report ID: DAR5191
         *
         * Spherical Insights:
         * REPORT ID SI20327
         *
         * Case-insensitive so both formats work.
         */
        Pattern pattern = Pattern.compile(
                "\\bREPORT\\s*ID\\s*[:\\-]?\\s*([A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(pageText);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        throw new IllegalStateException(
                "Report ID could not be found"
        );
    }





    private boolean isSegmentHeading(Element element) {

        String tag = element.tagName().toLowerCase();

        if (!(tag.equals("h1") ||
                tag.equals("h2") ||
                tag.equals("h3") ||
                tag.equals("h4") ||
                tag.equals("h5") ||
                tag.equals("h6"))) {

            return false;
        }

        return element.text()
                .toLowerCase()
                .contains("by ");
    }

    private List<SegmentData> extractSegments(Document document) {

        List<SegmentData> segments = new ArrayList<>();

        Elements strongElements = document.select("strong");

        for (Element strong : strongElements) {

            String text = strong.text().trim();

            /*
             * Find the Market Segment section
             */
            if (!text.equalsIgnoreCase("Market Segment")
                    && !text.equalsIgnoreCase("Market Segmentation")) {
                continue;
            }

            Element parent = strong.parent();

            if (parent == null) {
                continue;
            }

            /*
             * Start from the element immediately after
             * the Market Segment heading.
             */
            Element current =
                    parent.nextElementSibling();

            while (current != null) {

                String currentText =
                        current.text().trim();

                /*
                 * Stop at regional analysis.
                 */
                if (isRegionalAnalysis(current)) {
                    break;
                }

                /*
                 * Stop at unrelated sections.
                 */
                if (isSegmentStopSection(currentText)) {
                    break;
                }

                /*
                 * Detect segment heading.
                 *
                 * Examples:
                 *
                 * Global Retail Market, By Distribution Channel
                 * Global Retail Market, By Product Category
                 * Global Wi-Fi Range Extender Market, By Type
                 * Global Wi-Fi Range Extender Market, By Product
                 * Global Wi-Fi Range Extender Market, By End User
                 */
                if (isBySegmentHeading(current)) {

                    String segmentName =
                            extractSegmentName(currentText);

                    if (!segmentName.isBlank()) {

                        /*
                         * Find the next UL.
                         */
                        Element valuesElement =
                                current.nextElementSibling();

                        while (valuesElement != null) {

                            /*
                             * Stop if we reach another section
                             * before finding the values.
                             */
                            if (isRegionalAnalysis(valuesElement)
                                    || isSegmentStopSection(
                                    valuesElement.text().trim())) {

                                break;
                            }

                            if (valuesElement.tagName()
                                    .equalsIgnoreCase("ul")) {

                                Elements listItems =
                                        valuesElement.select("> li");

                                SegmentData segment =
                                        new SegmentData();

                                segment.setName(segmentName);

                                for (Element li : listItems) {

                                    String value =
                                            li.text().trim();

                                    if (!value.isBlank()) {
                                        segment.getValues()
                                                .add(value);
                                    }
                                }

                                if (!segment.getValues().isEmpty()) {
                                    segments.add(segment);
                                }

                                break;
                            }

                            valuesElement =
                                    valuesElement.nextElementSibling();
                        }
                    }
                }

                current =
                        current.nextElementSibling();
            }

            /*
             * We found the correct Market Segment section.
             */
            if (!segments.isEmpty()) {
                break;
            }
        }

        return segments;
    }

//    private boolean isBySegmentHeading(Element element) {
//
//        if (!element.tagName().equalsIgnoreCase("p")) {
//            return false;
//        }
//
//        String text = element.text().trim();
//
//        return text.toLowerCase().contains(", by ");
//    }
    private String extractSegmentName(String headingText) {

        int index = headingText.toLowerCase().indexOf(", by ");

        if (index == -1) {
            return headingText.trim();
        }

        return headingText
                .substring(index + 4)
                .trim();
    }
    private boolean isRegionalAnalysis(Element element) {

        String text = element.text()
                .trim()
                .toLowerCase();

        return text.contains("regional analysis")
                || text.contains("regional segment")
                || text.contains("regional segmentation")
                || text.contains("regional market");
    }

    private List<String> extractCompanies(Document document) {

        List<String> companies = new ArrayList<>();

        Elements strongElements = document.select("strong");

        for (Element strong : strongElements) {

            String heading =
                    strong.text().trim().toLowerCase();

            if (!isCompanyHeading(heading)) {
                continue;
            }

            Element parent = strong.parent();

            if (parent == null) {
                continue;
            }

            Element current =
                    parent.nextElementSibling();

            while (current != null) {

                String tag =
                        current.tagName().toLowerCase();

                /*
                 * Company list
                 */
                if (tag.equals("ul")
                        || tag.equals("ol")) {

                    Elements listItems =
                            current.select("> li");

                    for (Element li : listItems) {

                        String companyName =
                                li.text().trim();

                        if (!companyName.isBlank()
                                && !isInvalidCompanyName(companyName)) {

                            companies.add(companyName);
                        }
                    }

                    break;
                }

                /*
                 * Stop at unrelated sections.
                 */
                if (tag.equals("p")
                        || tag.equals("strong")) {

                    String nextText =
                            current.text()
                                    .trim()
                                    .toLowerCase();

                    if (isCompanyStopSection(nextText)) {
                        break;
                    }
                }

                current =
                        current.nextElementSibling();
            }

            /*
             * We found the company section.
             */
            if (!companies.isEmpty()) {
                break;
            }
        }

        return companies;
    }

    private Integer extractBaseYear(Document document) {

        String text = document.body().text();

        Pattern pattern = Pattern.compile(
                "\\bBase\\s+Year\\s*[:\\-]?\\s*(20\\d{2})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private Integer extractForecastYear(Document document) {

        String text = document.body().text();

        Pattern pattern = Pattern.compile(
                "\\bForecast\\s+(?:Period|Year)\\s*[:\\-]?\\s*(?:20\\d{2}\\s*[–\\-]\\s*)?(20\\d{2})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private Double extractBaseYearMarketValue(Document document) {

        String text = document.body().text();

        /*
         * Example:
         *
         * Market Size in 2025 : USD 7.58 billion
         *
         * Revenue 2025
         * USD Billion 4.45
         */

        Pattern pattern = Pattern.compile(
                "(?:Market\\s+Size\\s+in\\s+20\\d{2}|Revenue\\s+20\\d{2})\\s*[:\\-]?\\s*(?:USD\\s*)?(?:Billion|Million|Trillion)?\\s*([0-9]+(?:\\.[0-9]+)?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Double.parseDouble(
                    matcher.group(1)
            );
        }

        /*
         * Try the Decisions Advisors format:
         *
         * REVENUE 2025
         * USD Billion 4.45
         */

        Pattern revenuePattern = Pattern.compile(
                "REVENUE\\s+20\\d{2}\\s+(?:USD\\s*)?(?:Billion|Million|Trillion)\\s*([0-9]+(?:\\.[0-9]+)?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher revenueMatcher =
                revenuePattern.matcher(text);

        if (revenueMatcher.find()) {
            return Double.parseDouble(
                    revenueMatcher.group(1)
            );
        }

        return null;
    }

    private Double extractForecastYearMarketValue(Document document) {

        String text = document.body().text();

        /*
         * Example:
         *
         * 2035 Value Projection: USD 33.02 billion
         */

        Pattern projectionPattern = Pattern.compile(
                "\\b20\\d{2}\\s+Value\\s+Projection\\s*[:\\-]?\\s*"
                        + "(?:USD\\s*)?"
                        + "(?:Billion|Million|Trillion)?\\s*"
                        + "([0-9]+(?:\\.[0-9]+)?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher projectionMatcher =
                projectionPattern.matcher(text);

        if (projectionMatcher.find()) {

            return Double.parseDouble(
                    projectionMatcher.group(1)
            );
        }


        /*
         * Decisions Advisors format:
         *
         * FORECAST 2035
         * USD Billion 6.75
         */

        Pattern forecastPattern = Pattern.compile(
                "\\bFORECAST\\s+20\\d{2}\\s+"
                        + "(?:USD\\s*)?"
                        + "(?:Billion|Million|Trillion)\\s*"
                        + "([0-9]+(?:\\.[0-9]+)?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher forecastMatcher =
                forecastPattern.matcher(text);

        if (forecastMatcher.find()) {

            return Double.parseDouble(
                    forecastMatcher.group(1)
            );
        }


        /*
         * Value not found.
         */
        return null;
    }

    private Double extractCagr(Document document) {

        String text = document.body().text();

        /*
         * Examples:
         *
         * CAGR of 15.85%
         * CAGR: 15.85%
         * CAGR 15.85%
         * 4.25%
         */

        Pattern pattern = Pattern.compile(
                "\\bCAGR\\b(?:\\s+of)?\\s*[:\\-]?\\s*"
                        + "([0-9]+(?:\\.[0-9]+)?)\\s*%",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            return Double.parseDouble(
                    matcher.group(1)
            );
        }

        return null;
    }

    private Integer extractHistoricYear(Document document) {

        String text = document.body().text();

        /*
         * Format 1:
         * Historical Data for: 2020-2024
         *
         * Format 2:
         * Historical Data: 2020 - 2024
         *
         * Format 3:
         * Historical Data for 2020–2024
         */
        Pattern pattern = Pattern.compile(
                "\\bHistorical\\s+Data(?:\\s+for)?\\s*[:\\-]?\\s*"
                        + "(20\\d{2})"
                        + "\\s*[–\\-]"
                        + "\\s*(20\\d{2})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            return Integer.parseInt(
                    matcher.group(1)
            );
        }

        /*
         * Additional format:
         *
         * Historical: 2020-2024
         */
        Pattern historicalPattern = Pattern.compile(
                "\\bHistorical\\s*[:\\-]\\s*"
                        + "(20\\d{2})"
                        + "\\s*[–\\-]"
                        + "\\s*20\\d{2}",
                Pattern.CASE_INSENSITIVE
        );

        Matcher historicalMatcher =
                historicalPattern.matcher(text);

        if (historicalMatcher.find()) {

            return Integer.parseInt(
                    historicalMatcher.group(1)
            );
        }

        return null;
    }

    private String extractScope(Document document) {

        String text = document.body().text();

        Pattern pattern = Pattern.compile(
                "REPORT\\s+COVERAGE\\s+"
                        + "(Global|Regional|Country)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String extractLanguage(Document document) {

        // Current supported report websites are English.
        // Return English as the default.

        return "English";
    }

    private boolean isBySegmentHeading(Element element) {

        String text = element.text()
                .trim()
                .toLowerCase();

        if (text.contains(", by ")) {
            return true;
        }

        /*
         * Also support headings like:
         *
         * By Type
         * By Product
         * By Application
         * By End User
         */
        return text.matches(
                "^by\\s+[a-z0-9 &/\\-]+$"
        );
    }

    private boolean isSegmentStopSection(String text) {

        String value = text
                .trim()
                .toLowerCase();

        return value.equals("key target audience")
                || value.equals("recent development")
                || value.equals("government initiatives")
                || value.equals("frequently asked questions (faq)")
                || value.equals("faq")
                || value.contains("top companies")
                || value.contains("key companies")
                || value.contains("key players")
                || value.contains("major companies")
                || value.contains("leading companies")
                || value.contains("top key players");
    }

    private boolean isCompanyHeading(String text) {

        return text.contains("top companies")
                || text.contains("list of key companies")
                || text.equals("key companies")
                || text.contains("major companies")
                || text.contains("leading companies")
                || text.contains("top key players")
                || text.equals("key players")
                || text.equals("top players")
                || text.contains("major players")
                || text.contains("leading players")
                || text.contains("key market players")
                || text.contains("worldwide top key players")
                || text.contains("worldwide key players");
    }

    private boolean isInvalidCompanyName(String name) {

        String value =
                name.trim().toLowerCase();

        return value.equals("others")
                || value.equals("other companies")
                || value.equals("other players");
    }

    private boolean isCompanyStopSection(String text) {

        String value =
                text.trim().toLowerCase();

        return value.contains("key target audience")
                || value.contains("recent development")
                || value.contains("market segment")
                || value.contains("market segmentation")
                || value.contains("regional analysis")
                || value.contains("government initiatives")
                || value.contains("frequently asked questions")
                || value.equals("faq");
    }

}
