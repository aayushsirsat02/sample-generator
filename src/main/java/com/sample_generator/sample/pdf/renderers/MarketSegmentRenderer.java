package com.sample_generator.sample.pdf.renderers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.ThemeRenderer;
import com.sample_generator.sample.pdf.layout.BodyFigureLayout;
import com.sample_generator.sample.pdf.table.BodyTableStyling;
import com.sample_generator.sample.pdf.values.MarketValueSeriesProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarketSegmentRenderer {

    private static final String SOURCE_LINE =
            "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.";
    private static final String DELIVERABLE_NOTE =
            "*The deliverable report copy post purchase will include qualitative information along with historical, current & future trends, graphs, figures, and tables as well as recent developments/advances in each segment as represented above";
    private static final String ILLUSTRATION_NOTE = "Note: Charts & Figures only for Illustration purpose";

    /** Vertical reserve so chapter/share headings stay with the share chart. */
    private static final float SHARE_FIGURE_RESERVE_PT = 120f;
    /** Vertical reserve so leaf heading + subsection stay with the trend chart. */
    private static final float LEAF_FIGURE_RESERVE_PT = 100f;

    private static final String[] TREND_CHART_IMAGES = {
            "assets/images/fig2.jpg",
            "assets/images/fig3.jpg",
            "assets/images/graph_year.jpg",
            "assets/images/fig5.jpg"
    };
    private static final String[] SHARE_CHART_IMAGES = {
            "assets/images/graph_year.jpg",
            "assets/images/fig4.png",
            "assets/images/graph_year.png"
    };

    private final ThemeRenderer themeRenderer;
    private final MarketValueSeriesProvider valueSeriesProvider;

    public MarketSegmentRenderer(
            ThemeRenderer themeRenderer,
            MarketValueSeriesProvider valueSeriesProvider) {
        this.themeRenderer = themeRenderer;
        this.valueSeriesProvider = valueSeriesProvider;
    }

    public void render(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            int firstChapter,
            TocSectionRecorder toc) throws IOException {

        if (roots == null || roots.isEmpty()) {
            return;
        }

        int chapter = firstChapter;
        for (MarketSegment root : roots) {
            if (shouldSkipRoot(root)) {
                continue;
            }
            renderChapter(document, report, root, chapter++, toc);
        }
    }

    private boolean shouldSkipRoot(MarketSegment root) {
        return root == null
                || root.getSegmentName() == null
                || root.getSegmentName().isBlank()
                || root.getSegmentName().equalsIgnoreCase("Region");
    }

    private void renderChapter(
            Document document,
            SampleReport report,
            MarketSegment root,
            int chapter,
            TocSectionRecorder toc) throws IOException {

        document.add(new AreaBreak());

        Paragraph chapterHeading = new Paragraph(
                "CHAPTER " + chapter + "  " + report.getKeyName() + " - " + root.getSegmentName())
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setUnderline()
                .setKeepWithNext(true);
        toc.recordChapter(document, chapter, chapterHeading);

        renderRootBlock(document, report, root, String.valueOf(chapter), toc);
    }

    private void renderRootBlock(
            Document document,
            SampleReport report,
            MarketSegment segment,
            String numberPrefix,
            TocSectionRecorder toc) throws IOException {

        String segmentName = segment.getSegmentName();
        String market = report.getKeyName();
        int historicYear = historicYear(report);
        int baseYear = report.getBaseYear();
        int forecastYear = report.getForecastYear();
        String yearSpan = historicYear + "-" + forecastYear;
        String yearPair = baseYear + " & " + forecastYear;

        String overviewNumber = numberPrefix + ".1";
        toc.recordSection(document, "toc." + overviewNumber,
                new Paragraph(overviewNumber + " Global " + market + " - " + segmentName + " Overview")
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        String shareNumber = numberPrefix + ".2";
        String shareTitle = shareNumber
                + " Global "
                + market
                + " Share, by "
                + segmentName
                + ", "
                + yearPair
                + " Value (USD Million)";

        toc.recordSection(document, "toc." + shareNumber,
                new Paragraph(shareTitle)
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer,
                "FIGURE  ",
                "Global "
                        + market
                        + " Share, by "
                        + segmentName
                        + ", "
                        + yearPair
                        + " Value (USD Million)"));

        addImage(document, SHARE_CHART_IMAGES[Math.floorMod(segmentName.hashCode(), SHARE_CHART_IMAGES.length)],
                SHARE_FIGURE_RESERVE_PT);
        sourceParagraph(document, SOURCE_LINE);
        document.add(new Paragraph(ILLUSTRATION_NOTE)
                .setFont(themeRenderer.regular())
                .setFontSize(9)
                .setItalic());

        List<MarketSegment> children = segment.getChildren();
        List<String> tableRows = tableRowLabels(segment, children);

        document.add(new AreaBreak());

        document.add(new Paragraph(
                "TABLE  Global "
                        + market
                        + ", by "
                        + segmentName
                        + ", "
                        + yearSpan
                        + " Value (USD Million)")
                .setFont(themeRenderer.bold())
                .setFontSize(11)
                .setKeepWithNext(true));

        addWideValueTable(document, report, segmentName, tableRows, market, segmentName, SOURCE_LINE);

        if (children == null || children.isEmpty()) {
            return;
        }

        MarketSegment firstChild = null;
        List<MarketSegment> remainingChildren = new ArrayList<>();
        for (MarketSegment child : children) {
            if (child == null || child.getSegmentName() == null || child.getSegmentName().isBlank()) {
                continue;
            }
            if (firstChild == null) {
                firstChild = child;
            } else {
                remainingChildren.add(child);
            }
        }

        if (firstChild != null) {
            renderLeafSegmentFull(document, report, firstChild, numberPrefix + ".3", toc);
        }

        if (!remainingChildren.isEmpty()) {
            int childIndex = 4;
            for (MarketSegment child : remainingChildren) {
                String number = numberPrefix + "." + childIndex;
                toc.recordSection(document, "toc." + number,
                        new Paragraph(number + " " + child.getSegmentName())
                                .setFont(themeRenderer.semiBold())
                                .setFontSize(12)
                                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                                .setKeepWithNext(true));
                childIndex++;
            }
            document.add(new AreaBreak());

        }
    }

    /**
     * Sample-report pattern: only the first subsegment gets the full heading + chart unit.
     */
    private void renderLeafSegmentFull(
            Document document,
            SampleReport report,
            MarketSegment segment,
            String numberPrefix,
            TocSectionRecorder toc) throws IOException {

        document.add(new AreaBreak());

        String market = report.getKeyName();
        int historicYear = historicYear(report);
        int forecastYear = report.getForecastYear();
        String yearSpan = historicYear + "-" + forecastYear;

        toc.recordSection(document, "toc." + numberPrefix,
                new Paragraph(numberPrefix + " " + segment.getSegmentName())
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        String chartSection = numberPrefix + ".1";
        toc.recordSection(document, "toc." + chartSection,
                new Paragraph(chartSection
                        + " "
                        + market
                        + " by "
                        + segment.getSegmentName()
                        + ", "
                        + yearSpan
                        + " Value (USD Million)")
                        .setFont(themeRenderer.regular())
                        .setFontSize(10)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer,
                "FIGURE  ",
                market
                        + " by "
                        + segment.getSegmentName()
                        + ", "
                        + yearSpan
                        + " Value (USD Million)"));

        addImage(document,
                TREND_CHART_IMAGES[Math.floorMod(segment.getSegmentName().hashCode(), TREND_CHART_IMAGES.length)],
                LEAF_FIGURE_RESERVE_PT);
        sourceParagraph(document, SOURCE_LINE);
        document.add(new Paragraph(DELIVERABLE_NOTE)
                .setFont(themeRenderer.regular())
                .setFontSize(9)
                .setItalic());
    }

    private List<String> tableRowLabels(MarketSegment segment, List<MarketSegment> children) {
        if (children != null && !children.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (MarketSegment child : children) {
                if (child != null && child.getSegmentName() != null && !child.getSegmentName().isBlank()) {
                    names.add(child.getSegmentName());
                }
            }
            return names;
        }
        List<String> self = new ArrayList<>();
        self.add(segment.getSegmentName());
        return self;
    }

    private void addWideValueTable(
            Document document,
            SampleReport report,
            String columnTitle,
            List<String> rowLabels,
            String market,
            String dimension,
            String sourceText) throws IOException {

        int historicYear = historicYear(report);
        int forecastYear = report.getForecastYear();

        List<String> yearColumns = new ArrayList<>();
        for (int year = historicYear; year <= forecastYear; year++) {
            yearColumns.add(String.valueOf(year));
        }

        int yearCount = yearColumns.size();
        float[] widths = new float[yearCount + 2];
        widths[0] = 22f;
        widths[yearCount + 1] = 14f;
        float yearWidth = 64f / yearCount;
        for (int i = 1; i <= yearCount; i++) {
            widths[i] = yearWidth;
        }

        Table table = BodyTableStyling.newBodyTable(widths);

        table.addHeaderCell(BodyTableStyling.headerCell(themeRenderer, columnTitle, TextAlignment.LEFT));
        for (String year : yearColumns) {
            table.addHeaderCell(BodyTableStyling.headerCell(themeRenderer, year, TextAlignment.CENTER));
        }
        table.addHeaderCell(BodyTableStyling.headerCell(
                themeRenderer,
                "CAGR\n(" + report.getBaseYear() + "-" + forecastYear + ")",
                TextAlignment.CENTER));

        List<double[]> rowSeries = new ArrayList<>();
        if (rowLabels != null) {
            for (String label : rowLabels) {
                if (label == null || label.isBlank()) {
                    continue;
                }
                double[] series = valueSeriesProvider.yearlyValuesUsdMillion(report, market, dimension, label);
                rowSeries.add(series);
                addWideTableDataRow(table, label, series, report);
            }
        }

        double[] total = new double[yearCount];
        for (double[] series : rowSeries) {
            for (int i = 0; i < Math.min(yearCount, series.length); i++) {
                total[i] += series[i];
            }
        }
        addWideTableDataRow(table, "Total", total, report);
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, sourceText));
    }

    private void addWideTableDataRow(Table table, String label, double[] series, SampleReport report)
            throws IOException {
        table.addCell(BodyTableStyling.labelCell(themeRenderer, label));
        for (double value : series) {
            table.addCell(BodyTableStyling.valueCell(themeRenderer, valueSeriesProvider.formatValue(value)));
        }
        table.addCell(BodyTableStyling.valueCell(
                themeRenderer,
                valueSeriesProvider.formatPercent(valueSeriesProvider.cagrPercent(series, report))));
    }

    private void addImage(Document document, String imagePath, float reservePt) throws IOException {
        BodyFigureLayout.addClasspathFigure(document, imagePath, reservePt);
    }

    private void sourceParagraph(Document document, String text) throws IOException {
        document.add(new Paragraph(text)
                .setFont(themeRenderer.regular())
                .setFontSize(9));
    }

    private int historicYear(SampleReport report) {
        return report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
    }
}
