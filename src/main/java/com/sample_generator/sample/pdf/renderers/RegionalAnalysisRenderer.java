package com.sample_generator.sample.pdf.renderers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.pdf.ThemeRenderer;
import com.sample_generator.sample.pdf.table.BodyTableStyling;
import com.sample_generator.sample.pdf.charts.RegionalTrendChartGenerator;
import com.sample_generator.sample.pdf.layout.BodyFigureLayout;
import com.sample_generator.sample.pdf.outline.RegionalOutlineBuilder;
import com.sample_generator.sample.pdf.regional.RegionalGeoCatalog;
import com.sample_generator.sample.pdf.values.MarketValueSeriesProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RegionalAnalysisRenderer {

    private static final String SOURCE_LINE =
            "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.";
    private final ThemeRenderer themeRenderer;
    private final MarketValueSeriesProvider valueSeriesProvider;
    private final RegionalTrendChartGenerator chartGenerator;
    private final AtomicInteger figureCounter = new AtomicInteger(9);

    public RegionalAnalysisRenderer(
            ThemeRenderer themeRenderer,
            MarketValueSeriesProvider valueSeriesProvider,
            RegionalTrendChartGenerator chartGenerator) {
        this.themeRenderer = themeRenderer;
        this.valueSeriesProvider = valueSeriesProvider;
        this.chartGenerator = chartGenerator;
    }

    public int render(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            int chapter,
            TocSectionRecorder toc) throws IOException {

        List<MarketSegment> regions = RegionalOutlineBuilder.resolveRegions(roots);
        List<MarketSegment> dimensions = RegionalOutlineBuilder.segmentDimensions(roots);

        document.add(new AreaBreak());

        String market = report.getKeyName();
        String chapterPrefix = String.valueOf(chapter);

        Paragraph chapterHeading = new Paragraph(
                "CHAPTER " + chapter + "  " + market + " - Regional Analysis")
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setUnderline();
        toc.recordChapter(document, chapter, chapterHeading);

        renderChapterIntroduction(document, report, roots, chapter, toc);

        int regionSection = 3;
        if (regions.isEmpty()) {
            for (String regionName : RegionalGeoCatalog.defaultRegionOrder()) {
                renderFallbackRegion(document, report, regionName, dimensions, chapterPrefix, regionSection++, toc);
            }
        } else {
            for (MarketSegment region : regions) {
                if (region == null || region.getSegmentName() == null || region.getSegmentName().isBlank()) {
                    continue;
                }
                renderRegionBlock(document, report, region, dimensions, chapterPrefix, regionSection++, toc);
            }
        }

        return chapter + 1;
    }

    private void renderChapterIntroduction(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            int chapter,
            TocSectionRecorder toc) throws IOException {

        String market = report.getKeyName();
        String yearSpan = yearSpan(report);
        String yearPair = report.getBaseYear() + " & " + report.getForecastYear();
        String chapterPrefix = String.valueOf(chapter);

        recordSectionHeading(document, toc, "toc." + chapterPrefix + ".1",
                new Paragraph(chapterPrefix + ".1 Global " + market + " - Regional Overview")
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        //addClasspathImage(document, "assets/images/map.jpg");

        recordSectionHeading(document, toc, "toc." + chapterPrefix + ".2",
                new Paragraph(chapterPrefix + ".2 Global " + market + " Share, by Region, "
                        + yearPair + " Value (USD Million)")
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        document.add(BodyFigureLayout.figureCaption(
                themeRenderer,
                nextFigureNumber(),
                "Global " + market + " Share, by Region, "
                        + yearPair + " Value (USD Million)"));

        addRegionalShareChart(document, report, roots);
        sourceParagraph(document);

        document.add(new AreaBreak());

        document.add(new Paragraph("TABLE " + chapter + " Global " + market + ", by Region, " + yearSpan
                + " Value (USD Million)")
                .setFont(themeRenderer.bold())
                .setFontSize(12)
                .setKeepWithNext(true));

        List<String> regionLabels = regionRowLabelsFromData(roots);
        addWideValueTable(document, report, "Region", regionLabels, market, "Region");
    }

    private void renderRegionBlock(
            Document document,
            SampleReport report,
            MarketSegment region,
            List<MarketSegment> dimensions,
            String chapterPrefix,
            int regionIndex,
            TocSectionRecorder toc) throws IOException {

        String regionName = region.getSegmentName();
        String market = report.getKeyName();
        String yearSpan = yearSpan(report);
        String regionPrefix = chapterPrefix + "." + regionIndex;

        document.add(new AreaBreak());
        recordSectionHeading(document, toc, "toc." + regionPrefix,
                new Paragraph(regionPrefix + " " + regionName)
                        .setFont(themeRenderer.semiBold())
                        .setFontSize(12)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        String chartSection = regionPrefix + ".1";
        recordSectionHeading(document, toc, "toc." + chartSection,
                new Paragraph(chartSection + " " + regionName + " " + market + ", " + yearSpan + " Value (USD Million)")
                        .setFont(themeRenderer.regular())
                        .setFontSize(10)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        document.add(BodyFigureLayout.figureCaption(
                themeRenderer,
                nextFigureNumber(),
                regionName + " " + market + ", " + yearSpan + " Value (USD Million)"));

        addTrendChart(document, report, market, regionName);
        sourceParagraph(document);

        document.add(new AreaBreak());
        String countryTableSection = chartSection + ".1";
        recordSectionHeading(document, toc, "toc." + countryTableSection,
                new Paragraph(countryTableSection + " " + regionName + " " + market
                        + ", by country, " + yearSpan + " Value (USD Million)")
                        .setFont(themeRenderer.regular())
                        .setFontSize(10)
                        .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                        .setKeepWithNext(true));

        document.add(new Paragraph("TABLE  " + regionName + " " + market + ", by Country, " + yearSpan
                + " Value (USD Million)")
                .setFont(themeRenderer.semiBold())
                .setFontSize(11)
                .setKeepWithNext(true));

        List<String> countryRows = childNames(region.getChildren());
        if (countryRows.isEmpty()) {
            countryRows = RegionalGeoCatalog.countriesForRegion(regionName);
        }
        addWideValueTable(document, report, "Country", countryRows, market, regionName);

        int section = 2;
        for (MarketSegment dimension : dimensions) {
            if (dimension == null || dimension.getSegmentName() == null) {
                continue;
            }
            document.add(new AreaBreak());
            String dimName = dimension.getSegmentName();
            String dimPrefix = regionPrefix + "." + section;
            recordSectionHeading(document, toc, "toc." + dimPrefix,
                    new Paragraph(dimPrefix + " " + regionName + " " + market + ", by " + dimName + ", " + yearSpan)
                            .setFont(themeRenderer.semiBold())
                            .setFontSize(11)
                            .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                            .setKeepWithNext(true));

            String tablePrefix = dimPrefix + ".1";
            recordSectionHeading(document, toc, "toc." + tablePrefix,
                    new Paragraph(tablePrefix + " " + regionName + " " + market + ", by " + dimName + ", "
                            + yearSpan + " Value (USD Million)")
                            .setFont(themeRenderer.regular())
                            .setFontSize(10)
                            .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                            .setKeepWithNext(true));

            document.add(new Paragraph("TABLE  " + regionName + " " + market + ", by " + dimName + ", "
                    + yearSpan + " Value (USD Million)")
                    .setFont(themeRenderer.semiBold())
                    .setFontSize(11)
                    .setKeepWithNext(true));

            List<String> dimRows = collectLeafSegmentNames(dimension.getChildren());
            if (dimRows.isEmpty()) {
                dimRows = RegionalGeoCatalog.rowsForDimension(dimName);
            }
            addWideValueTable(document, report, dimName, dimRows, market, regionName, dimName);
            section++;
        }

        List<MarketSegment> geoNodes = region.getChildren();
        if (geoNodes == null || geoNodes.isEmpty()) {
            geoNodes = new ArrayList<>();
            for (String country : RegionalGeoCatalog.countriesForRegion(regionName)) {
                MarketSegment stub = new MarketSegment();
                stub.setSegmentName(country);
                geoNodes.add(stub);
            }
        }
        renderGeoNodes(document, report, geoNodes, regionPrefix, section, toc, 0, market);
    }

    private void renderFallbackRegion(
            Document document,
            SampleReport report,
            String regionName,
            List<MarketSegment> dimensions,
            String chapterPrefix,
            int regionIndex,
            TocSectionRecorder toc) throws IOException {

        MarketSegment stub = new MarketSegment();
        stub.setSegmentName(regionName);
        stub.setChildren(new ArrayList<>());
        renderRegionBlock(document, report, stub, dimensions, chapterPrefix, regionIndex, toc);
    }

    private void renderGeoNodes(
            Document document,
            SampleReport report,
            List<MarketSegment> nodes,
            String prefix,
            int startSection,
            TocSectionRecorder toc,
            int chartDepth,
            String market) throws IOException {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        String yearSpan = yearSpan(report);
        int section = startSection;
        for (MarketSegment node : nodes) {
            if (node == null || node.getSegmentName() == null || node.getSegmentName().isBlank()) {
                continue;
            }
            String sectionNumber = prefix + "." + section;
            List<MarketSegment> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                recordSectionHeading(document, toc, "toc." + sectionNumber,
                        new Paragraph(sectionNumber + " " + node.getSegmentName())
                                .setFont(themeRenderer.semiBold())
                                .setFontSize(12)
                                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                                .setKeepWithNext(true));
                renderGeoNodes(document, report, children, sectionNumber, 1, toc, chartDepth + 1, market);
            } else {
                document.add(new AreaBreak());
                recordSectionHeading(document, toc, "toc." + sectionNumber,
                        new Paragraph(sectionNumber + " " + node.getSegmentName())
                                .setFont(themeRenderer.semiBold())
                                .setFontSize(12)
                                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                                .setKeepWithNext(true));

                String chartSub = sectionNumber + ".1";
                recordSectionHeading(document, toc, "toc." + chartSub,
                        new Paragraph(chartSub + " " + node.getSegmentName() + " " + market + ", " + yearSpan
                                + " Value (USD Million)")
                                .setFont(themeRenderer.regular())
                                .setFontSize(10)
                                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                                .setKeepWithNext(true));

                document.add(BodyFigureLayout.figureCaption(
                        themeRenderer,
                        nextFigureNumber(),
                        node.getSegmentName() + " " + market + ", " + yearSpan + " Value (USD Million)"));

                addTrendChart(document, report, market, node.getSegmentName());
                sourceParagraph(document);
            }
            section++;
        }
    }

    private void addRegionalShareChart(Document document, SampleReport report, List<MarketSegment> roots)
            throws IOException {
        List<String> labels = regionRowLabelsFromData(roots);
        int baseYear = report.getBaseYear();
        int forecastYear = report.getForecastYear();
        String market = report.getKeyName();

        double[] revenues = new double[labels.size() * 2];
        double[] growth = new double[labels.size() * 2];
        int idx = 0;
        for (String label : labels) {
            double[] series = valueSeriesProvider.yearlyValuesUsdMillion(report, market, "Region", label);
            revenues[idx] = valueAtYear(series, report, baseYear);
            revenues[idx + 1] = valueAtYear(series, report, forecastYear);
            growth[idx] = 0;
            growth[idx + 1] = valueSeriesProvider.cagrPercent(series, report);
            idx += 2;
        }
        int[] chartYears = new int[labels.size() * 2];
        for (int i = 0; i < labels.size(); i++) {
            chartYears[i * 2] = baseYear;
            chartYears[i * 2 + 1] = forecastYear;
        }
        addChartImage(
                document,
                chartGenerator.renderComboChart(chartYears, revenues, growth, report.getBaseYear()));
    }

    private void addTrendChart(Document document, SampleReport report, String market, String entityName)
            throws IOException {
        double[] values = valueSeriesProvider.yearlyValuesUsdMillion(report, market, entityName);
        int historic = historicYear(report);
        int forecast = report.getForecastYear();
        int n = values.length;
        int[] years = new int[n];
        for (int i = 0; i < n; i++) {
            years[i] = historic + i;
        }
        double[] growth = valueSeriesProvider.yearOverYearGrowthPercent(values);
        addChartImage(
                document,
                chartGenerator.renderComboChart(years, values, growth, report.getBaseYear()));
    }

    private void addWideValueTable(
            Document document,
            SampleReport report,
            String columnTitle,
            List<String> rowLabels,
            String market,
            String... pathPrefix) throws IOException {

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
                "CAGR\n(" + report.getBaseYear() + "- " + forecastYear + ")",
                TextAlignment.CENTER));

        List<double[]> rowSeries = new ArrayList<>();
        if (rowLabels != null) {
            for (String label : rowLabels) {
                if (label == null || label.isBlank()) {
                    continue;
                }
                double[] series = valueSeriesProvider.yearlyValuesUsdMillion(report, concat(pathPrefix, market, label));
                rowSeries.add(series);
                addWideTableDataRow(table, label, series, report);
            }
        }

        double[] total = new double[yearCount];
        for (double[] series : rowSeries) {
            for (int i = 0; i < yearCount; i++) {
                total[i] += series[i];
            }
        }
        addWideTableDataRow(table, "Total", total, report);
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, SOURCE_LINE));
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

    private List<String> regionRowLabelsFromData(List<MarketSegment> roots) {
        List<MarketSegment> regions = RegionalOutlineBuilder.resolveRegions(roots);
        List<String> labels = new ArrayList<>();
        for (MarketSegment region : regions) {
            if (region != null && region.getSegmentName() != null && !region.getSegmentName().isBlank()) {
                labels.add(region.getSegmentName());
            }
        }
        if (!labels.isEmpty()) {
            return labels;
        }
        return RegionalGeoCatalog.defaultRegionOrder();
    }

    private List<String> childNames(List<MarketSegment> children) {
        List<String> names = new ArrayList<>();
        if (children == null) {
            return names;
        }
        for (MarketSegment child : children) {
            if (child != null && child.getSegmentName() != null && !child.getSegmentName().isBlank()) {
                names.add(child.getSegmentName());
            }
        }
        return names;
    }

    private List<String> collectLeafSegmentNames(List<MarketSegment> segments) {
        List<String> names = new ArrayList<>();
        collectLeafSegmentNamesRecursive(segments, names);
        return names;
    }

    private void collectLeafSegmentNamesRecursive(List<MarketSegment> segments, List<String> names) {
        if (segments == null) {
            return;
        }
        for (MarketSegment segment : segments) {
            if (segment == null || segment.getSegmentName() == null || segment.getSegmentName().isBlank()) {
                continue;
            }
            List<MarketSegment> children = segment.getChildren();
            if (children == null || children.isEmpty()) {
                names.add(segment.getSegmentName());
            } else {
                collectLeafSegmentNamesRecursive(children, names);
            }
        }
    }

    private void recordSectionHeading(Document document, TocSectionRecorder toc, String dest, Paragraph heading)
            throws IOException {
        toc.recordSection(document, dest, heading);
    }

    private void addClasspathImage(Document document, String path) throws IOException {
        BodyFigureLayout.addClasspathFigure(document, path);
    }

    private void addChartImage(Document document, byte[] png) {
        BodyFigureLayout.addPngFigure(document, png);
    }

    private void sourceParagraph(Document document) throws IOException {
        document.add(BodyTableStyling.sourceLine(themeRenderer, SOURCE_LINE));
    }

    private int nextFigureNumber() {
        return figureCounter.getAndIncrement();
    }

    private double valueAtYear(double[] series, SampleReport report, int year) {
        int historic = historicYear(report);
        int index = year - historic;
        if (index < 0 || index >= series.length) {
            return 0;
        }
        return series[index];
    }

    private String[] concat(String[] prefix, String market, String label) {
        String[] combined = new String[prefix.length + 2];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        combined[prefix.length] = market;
        combined[prefix.length + 1] = label;
        return combined;
    }

    private String yearSpan(SampleReport report) {
        return historicYear(report) + "-" + report.getForecastYear();
    }

    private int historicYear(SampleReport report) {
        return report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
    }
}
