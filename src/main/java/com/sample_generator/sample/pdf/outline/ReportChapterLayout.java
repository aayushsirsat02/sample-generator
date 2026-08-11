package com.sample_generator.sample.pdf.outline;

import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;

import java.util.ArrayList;
import java.util.List;

public final class ReportChapterLayout {

    private static final int EXECUTIVE_CHAPTER = 1;
    private static final int FIRST_SEGMENT_CHAPTER = 2;

    private final int segmentChapterCount;
    private final int regionalChapter;
    private final List<String> segmentChapterTitles;
    private final List<TocOutlineEntry> regionalTocEntries;
    private final List<TocOutlineEntry> segmentTocEntries;

    private ReportChapterLayout(
            int segmentChapterCount,
            List<String> segmentChapterTitles,
            List<TocOutlineEntry> segmentTocEntries,
            List<TocOutlineEntry> regionalTocEntries) {
        this.segmentChapterCount = segmentChapterCount;
        this.segmentChapterTitles = segmentChapterTitles;
        this.segmentTocEntries = segmentTocEntries;
        this.regionalTocEntries = regionalTocEntries;
        this.regionalChapter = FIRST_SEGMENT_CHAPTER + segmentChapterCount;
    }

    public static ReportChapterLayout build(SampleReport report, List<MarketSegment> roots) {
        List<String> titles = new ArrayList<>();
        List<TocOutlineEntry> segmentEntries = new ArrayList<>();
        int count = 0;
        if (roots != null) {
            for (MarketSegment root : roots) {
                if (isSegmentChapterRoot(root)) {
                    count++;
                    String title = report.getKeyName() + " - " + root.getSegmentName();
                    titles.add(title);
                    segmentEntries.addAll(buildSegmentTocEntries(report, root, count));
                }
            }
        }
        int regionalChapter = FIRST_SEGMENT_CHAPTER + count;
        List<TocOutlineEntry> regionalEntries =
                RegionalOutlineBuilder.buildTocEntries(report, roots, regionalChapter);
        return new ReportChapterLayout(count, titles, segmentEntries, regionalEntries);
    }

    private static List<TocOutlineEntry> buildSegmentTocEntries(
            SampleReport report,
            MarketSegment root,
            int chapter) {
        List<TocOutlineEntry> entries = new ArrayList<>();
        String market = report.getKeyName();
        String chapterPrefix = String.valueOf(chapter);
        String yearPair = report.getBaseYear() + " & " + report.getForecastYear();
        int historic = report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
        String yearSpan = historic + "-" + report.getForecastYear();

        entries.add(new TocOutlineEntry(
                chapterPrefix + ".1",
                "Global " + market + " - " + root.getSegmentName() + " Overview",
                1,
                "toc." + chapterPrefix + ".1"));
        entries.add(new TocOutlineEntry(
                chapterPrefix + ".2",
                "Global " + market + " Share, by " + root.getSegmentName() + ", " + yearPair + " Value (USD Million)",
                1,
                "toc." + chapterPrefix + ".2"));

        List<MarketSegment> children = root.getChildren();
        if (children != null) {
            int childIndex = 3;
            boolean firstChild = true;
            for (MarketSegment child : children) {
                if (child == null || child.getSegmentName() == null || child.getSegmentName().isBlank()) {
                    continue;
                }
                String childPrefix = chapterPrefix + "." + childIndex;
                entries.add(new TocOutlineEntry(
                        childPrefix,
                        child.getSegmentName(),
                        1,
                        "toc." + childPrefix));
                // Sample-report pattern: only the first subsegment includes the value subsection.
                if (firstChild) {
                    entries.add(new TocOutlineEntry(
                            childPrefix + ".1",
                            "Global " + market + " by " + child.getSegmentName() + ", " + yearSpan
                                    + " Value (USD Million)",
                            2,
                            "toc." + childPrefix + ".1"));
                    firstChild = false;
                }
                childIndex++;
            }
        }
        return entries;
    }

    public static boolean isSegmentChapterRoot(MarketSegment segment) {
        if (segment == null || segment.getSegmentName() == null || segment.getSegmentName().isBlank()) {
            return false;
        }
        return !segment.getSegmentName().equalsIgnoreCase("Region");
    }

    public int firstSegmentChapter() {
        return FIRST_SEGMENT_CHAPTER;
    }

    public int executiveChapter() {
        return EXECUTIVE_CHAPTER;
    }

    public int regionalChapter() {
        return regionalChapter;
    }

    public String regionalChapterDestination() {
        return destinationForChapter(regionalChapter);
    }

    public int competitiveChapter() {
        return regionalChapter + 1;
    }

    public int companyProfilesChapter() {
        return regionalChapter + 2;
    }

    public int industryAnalysisChapter() {
        return regionalChapter + 3;
    }

    public int marketStrategyChapter() {
        return regionalChapter + 4;
    }

    public int conclusionsChapter() {
        return regionalChapter + 5;
    }

    public int methodologyChapter() {
        return regionalChapter + 6;
    }

    public String destinationForChapter(int chapter) {
        return "toc.ch" + chapter;
    }

    public List<String> getSegmentChapterTitles() {
        return segmentChapterTitles;
    }

    public List<TocOutlineEntry> getSegmentTocEntries() {
        return segmentTocEntries;
    }

    public List<TocOutlineEntry> getRegionalTocEntries() {
        return regionalTocEntries;
    }

    public int segmentChapterCount() {
        return segmentChapterCount;
    }
}
