package com.sample_generator.sample.pdf.outline;

import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.regional.RegionalGeoCatalog;

import java.util.ArrayList;
import java.util.List;

public final class RegionalOutlineBuilder {

    private RegionalOutlineBuilder() {
    }

    public static List<TocOutlineEntry> buildTocEntries(
            SampleReport report,
            List<MarketSegment> roots,
            int chapter) {

        List<TocOutlineEntry> entries = new ArrayList<>();
        String market = report.getKeyName();
        String chapterPrefix = String.valueOf(chapter);

        entries.add(entry(
                chapterPrefix + ".1",
                "Global " + market + " - Regional Overview",
                1,
                "toc." + chapterPrefix + ".1"));
        entries.add(entry(
                chapterPrefix + ".2",
                "Global " + market + " Share, by Region, " + yearPair(report) + " Value (USD Million)",
                1,
                "toc." + chapterPrefix + ".2"));

        List<MarketSegment> regions = resolveRegions(roots);
        List<MarketSegment> segmentDimensions = segmentDimensions(roots);
        int regionIndex = 3;

        if (regions.isEmpty()) {
            for (String regionName : RegionalGeoCatalog.defaultRegionOrder()) {
                appendRegionToc(entries, report, regionName, List.of(), segmentDimensions, chapter, regionIndex++);
            }
        } else {
            for (MarketSegment region : regions) {
                if (region == null || region.getSegmentName() == null || region.getSegmentName().isBlank()) {
                    continue;
                }
                appendRegionToc(
                        entries,
                        report,
                        region.getSegmentName(),
                        region.getChildren(),
                        segmentDimensions,
                        chapter,
                        regionIndex++);
            }
        }

        return entries;
    }

    private static void appendRegionToc(
            List<TocOutlineEntry> entries,
            SampleReport report,
            String regionName,
            List<MarketSegment> countries,
            List<MarketSegment> segmentDimensions,
            int chapter,
            int regionIndex) {

        String market = report.getKeyName();
        String regionPrefix = chapter + "." + regionIndex;
        String yearSpan = yearSpan(report);

        entries.add(entry(regionPrefix, regionName, 1, "toc." + regionPrefix));

        entries.add(entry(
                regionPrefix + ".1",
                regionName + " " + market + ", " + yearSpan + " Value (USD Million)",
                2,
                "toc." + regionPrefix + ".1"));
        entries.add(entry(
                regionPrefix + ".1.1",
                regionName + " " + market + ", by country, " + yearSpan + " Value (USD Million)",
                3,
                "toc." + regionPrefix + ".1.1"));

        int section = 2;
        for (MarketSegment dimension : segmentDimensions) {
            if (dimension == null || dimension.getSegmentName() == null) {
                continue;
            }
            String dim = dimension.getSegmentName();
            entries.add(entry(
                    regionPrefix + "." + section,
                    regionName + " " + market + ", by " + dim + ", " + yearSpan,
                    2,
                    "toc." + regionPrefix + "." + section));
            entries.add(entry(
                    regionPrefix + "." + section + ".1",
                    regionName + " " + market + ", by " + dim + ", " + yearSpan + " Value (USD Million)",
                    3,
                    "toc." + regionPrefix + "." + section + ".1"));
            section++;
        }

        List<MarketSegment> countryNodes = countries;
        if (countryNodes == null || countryNodes.isEmpty()) {
            countryNodes = stubCountries(regionName);
        }
        appendGeoTocEntries(entries, countryNodes, regionPrefix, section, market, report, 2);
    }

    private static List<MarketSegment> stubCountries(String regionName) {
        List<MarketSegment> stubs = new ArrayList<>();
        for (String country : RegionalGeoCatalog.countriesForRegion(regionName)) {
            MarketSegment stub = new MarketSegment();
            stub.setSegmentName(country);
            stubs.add(stub);
        }
        return stubs;
    }

    private static void appendGeoTocEntries(
            List<TocOutlineEntry> entries,
            List<MarketSegment> nodes,
            String regionPrefix,
            int startSection,
            String market,
            SampleReport report,
            int level) {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        int section = startSection;
        for (MarketSegment node : nodes) {
            if (node == null || node.getSegmentName() == null || node.getSegmentName().isBlank()) {
                continue;
            }
            String sectionNumber = regionPrefix + "." + section;
            entries.add(entry(sectionNumber, node.getSegmentName(), level, "toc." + sectionNumber));

            List<MarketSegment> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                appendGeoTocEntries(entries, children, sectionNumber, 1, market, report, level + 1);
            } else {
                String chartTitle = node.getSegmentName() + " " + market + ", " + yearSpan(report) + " Value (USD Million)";
                entries.add(entry(sectionNumber + ".1", chartTitle, level + 1, "toc." + sectionNumber + ".1"));
            }
            section++;
        }
    }

    public static List<MarketSegment> resolveRegions(List<MarketSegment> roots) {
        MarketSegment regionRoot = findRegionRoot(roots);
        if (regionRoot != null
                && regionRoot.getChildren() != null
                && !regionRoot.getChildren().isEmpty()) {
            return regionRoot.getChildren();
        }
        return List.of();
    }

    public static MarketSegment findRegionRoot(List<MarketSegment> roots) {
        if (roots == null) {
            return null;
        }
        for (MarketSegment root : roots) {
            if (root != null
                    && root.getSegmentName() != null
                    && root.getSegmentName().equalsIgnoreCase("Region")) {
                return root;
            }
        }
        return null;
    }

    public static List<MarketSegment> segmentDimensions(List<MarketSegment> roots) {
        List<MarketSegment> dimensions = new ArrayList<>();
        if (roots == null) {
            return dimensions;
        }
        for (MarketSegment root : roots) {
            if (ReportChapterLayout.isSegmentChapterRoot(root)) {
                dimensions.add(root);
            }
        }
        return dimensions;
    }

    private static TocOutlineEntry entry(String number, String title, int level, String dest) {
        return new TocOutlineEntry(number, title, level, dest);
    }

    private static String yearSpan(SampleReport report) {
        int historic = report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
        return historic + "-" + report.getForecastYear();
    }

    private static String yearPair(SampleReport report) {
        return report.getBaseYear() + " & " + report.getForecastYear();
    }
}
