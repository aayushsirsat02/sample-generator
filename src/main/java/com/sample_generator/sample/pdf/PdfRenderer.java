package com.sample_generator.sample.pdf;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.sample_generator.sample.pdf.renderers.MarketSegmentRenderer;
import com.sample_generator.sample.pdf.renderers.RegionalAnalysisRenderer;
import com.sample_generator.sample.pdf.renderers.TocSectionRecorder;
import com.sample_generator.sample.pdf.outline.ReportChapterLayout;
import com.sample_generator.sample.pdf.outline.RegionalOutlineBuilder;
import com.sample_generator.sample.pdf.outline.TocOutlineEntry;
import com.sample_generator.sample.pdf.table.BodyTableStyling;
import com.sample_generator.sample.pdf.layout.BodyFigureLayout;
import com.sample_generator.sample.pdf.values.MarketValueSeriesProvider;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;

import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;

import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.kernel.font.PdfFont;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;

import com.sample_generator.sample.Entity.Company;
import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class PdfRenderer {

    PdfFont dingbatsFont = PdfFontFactory.createFont("ZapfDingbats");

    // 2. Define your blue color:
// Custom RGB (e.g., standard blue, royal blue, or dark blue)
    DeviceRgb blueColor = new DeviceRgb(0, 102, 204);

// Or simply use the preset constant:
// com.itextpdf.kernel.colors.ColorConstants.BLUE

    private static final float BODY_HORIZONTAL_MARGIN = 70f;
    private static final String LIST_SYMBOL_IMAGE = "assets/images/listSymbol.png";

    private final ThemeRenderer themeRenderer;
    private final MarketSegmentRenderer marketSegmentRenderer;
    private final RegionalAnalysisRenderer regionalAnalysisRenderer;
    private final MarketValueSeriesProvider valueSeriesProvider;

    private PdfDocument headerFooterRegisteredDocument;
    private final Set<Integer> pagesWithoutHeaderFooter = new HashSet<>();

    private boolean tocIndexingPass;
    private final Map<String, Integer> tocPageByDestination = new HashMap<>();
    private String pendingTocDestination;

    public PdfRenderer(
            ThemeRenderer themeRenderer,
            MarketSegmentRenderer marketSegmentRenderer,
            RegionalAnalysisRenderer regionalAnalysisRenderer,
            MarketValueSeriesProvider valueSeriesProvider) throws IOException {
        this.themeRenderer = themeRenderer;
        this.marketSegmentRenderer = marketSegmentRenderer;
        this.regionalAnalysisRenderer = regionalAnalysisRenderer;
        this.valueSeriesProvider = valueSeriesProvider;
    }

    public byte[] generatePdf(SampleReport report, List<MarketSegment> roots) throws IOException {
        tocPageByDestination.clear();
        tocIndexingPass = true;
        writePdf(new ByteArrayOutputStream(), report, roots);

        tocIndexingPass = false;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writePdf(outputStream, report, roots);
        return outputStream.toByteArray();
    }

    private void writePdf(ByteArrayOutputStream outputStream, SampleReport report, List<MarketSegment> roots)
            throws IOException {

        headerFooterRegisteredDocument = null;
        pagesWithoutHeaderFooter.clear();
        pendingTocDestination = null;

        WriterProperties writerProperties = new WriterProperties()
                .useSmartMode()
                .setFullCompressionMode(true)
                .setCompressionLevel(CompressionConstants.BEST_COMPRESSION);
        PdfWriter writer = new PdfWriter(outputStream, writerProperties);

        PdfDocument pdfDocument = new PdfDocument(writer);

        PageSize pageSize = PageSize.A4.rotate();

        Document document = new Document(pdfDocument, pageSize);

        document.setMargins(0, 0, 0, 0);

        addCoverPage(document, report);

        document.setMargins(
                80, // Top — clear header band
                BODY_HORIZONTAL_MARGIN, // Right — align with servlet body band
                80, // Bottom — clear footer band
                BODY_HORIZONTAL_MARGIN  // Left — align with servlet body band
        );

        document.add(new AreaBreak());

        addHeaderAndFooter(document, report, 2);

        //addReportSummary(document, report);
        //document.add(new AreaBreak());
        addHeaderAndFooter(document, report, 2);

        addReportSummaryOverview(document, report);
        document.add(new AreaBreak());

        addRegionMapSection(document, report, roots);

        addPreliminarySegmentationOverviews(document, report, roots);
        //document.add(new AreaBreak());

//        marketSegmentRenderer.render(
//                document,
//                report,
//                roots
//        );

        document.add(new AreaBreak());

        addCompanyList(document, report);
        document.add(new AreaBreak());

        ReportChapterLayout chapterLayout = ReportChapterLayout.build(report, roots);
        addTableOfContents(document, report, chapterLayout);
        document.add(new AreaBreak());

        addListOfFigures(document, report, roots, chapterLayout);
        document.add(new AreaBreak());

        addListOfTables(document, report, roots, chapterLayout);
        document.add(new AreaBreak());

        TocSectionRecorder tocRecorder = createTocSectionRecorder();

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.executiveChapter());
        addExecutiveSummary(document, report, roots);

        marketSegmentRenderer.render(
                document,
                report,
                roots,
                chapterLayout.firstSegmentChapter(),
                tocRecorder);

        pendingTocDestination = chapterLayout.regionalChapterDestination();
        regionalAnalysisRenderer.render(
                document,
                report,
                roots,
                chapterLayout.regionalChapter(),
                tocRecorder);
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.competitiveChapter());
        addCompetitiveLandscape(document, report, chapterLayout.competitiveChapter());
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.companyProfilesChapter());
        addCompanyProfiles(document, report, chapterLayout.companyProfilesChapter());
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.industryAnalysisChapter());
        addIndustryAnalysis(document, report, roots, chapterLayout.industryAnalysisChapter());
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.marketStrategyChapter());
        addMarketStrategyAnalysis(document, report, chapterLayout.marketStrategyChapter());
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.conclusionsChapter());
        addReportConclusions(document, report, chapterLayout.conclusionsChapter());
        document.add(new AreaBreak());

        pendingTocDestination = chapterLayout.destinationForChapter(chapterLayout.methodologyChapter());
        addResearchMethodology(document, report, chapterLayout.methodologyChapter());
        document.add(new AreaBreak());

        addAboutUs(document, report);
        document.add(new AreaBreak());
        addDisclaimer(document);

        document.close();
    }

    //These below are all methods which user in our main pdf generator method
    //
    //
    //

    private void addCoverPage(Document document, SampleReport report) throws IOException {

        ClassPathResource resource = new ClassPathResource("assets/images/cover.png");
        ImageData imageData = ImageDataFactory.create(resource.getInputStream().readAllBytes());
        Image coverImage = new Image(imageData);

        PageSize pageSize = PageSize.A4.rotate();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();

        // Full-bleed cover
        coverImage.scaleAbsolute(pageWidth, pageHeight);
        coverImage.setFixedPosition(1, 0f, 0f);

        float left = 60f;
        float textWidth = pageWidth - (left * 2);

        com.itextpdf.kernel.colors.Color coverNavy = new com.itextpdf.kernel.colors.DeviceRgb(27, 58, 92);
        com.itextpdf.kernel.colors.Color coverMuted = new com.itextpdf.kernel.colors.DeviceRgb(90, 98, 110);

        String keyName = report.getKeyName() != null ? report.getKeyName() : "";

        // 1. DYNAMIC FONT SIZE CALCULATION (Auto-adjusts based on text length)
        float titleFontSize = 32f; // Default for normal titles
        if (keyName.length() > 90) {
            titleFontSize = 20f;
        } else if (keyName.length() > 65) {
            titleFontSize = 24f;
        } else if (keyName.length() > 40) {
            titleFontSize = 28f;
        }

        // 2. CONTAINER DIV (Positioned at bottom left of cover)
        // Using a Div prevents overlapping between Category, Title, Subtitle, and Date
        Div container = new Div();
        container.setFixedPosition(1, left, 50f, textWidth); // x=70, y=50 from bottom



        // Main Title Line
        Paragraph title = new Paragraph(keyName)
                .setFont(themeRenderer.semiBold())
                .setFontSize(titleFontSize) // Dynamic font size applied here
                .setFontColor(coverNavy)
                .setMargin(0)
                .setMarginBottom(6f)
                .setMultipliedLeading(1.05f);
        container.add(title);

        // Subtitle Line
        Paragraph subtitle = new Paragraph("Forecast to " + report.getForecastYear())
                .setFont(themeRenderer.semiBold())
                .setFontSize(22)
                .setFontColor(coverNavy)
                .setMargin(0)
                .setMarginBottom(8f);
        container.add(subtitle);

        // Date Line
        if (report.getCreatedAt() != null) {
            String published = report.getCreatedAt()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
            Paragraph dateLine = new Paragraph(published)
                    .setFont(themeRenderer.regular())
                    .setFontSize(11)
                    .setFontColor(coverMuted)
                    .setMargin(0);
            container.add(dateLine);
        }

        // Category Line (Above Title)
        String category = report.getCategory();
        if (category != null && !category.isBlank()) {
            Paragraph categoryLine = new Paragraph("Category: "+category.trim().toUpperCase())
                    .setFont(themeRenderer.semiBold())
                    .setFontSize(12)
                    .setFontColor(coverMuted)
                    .setMargin(0)
                    .setMarginBottom(4f);
            container.add(categoryLine);
        }

        // Render cover background first, then the text container on top
        document.add(coverImage);
        document.add(container);
    }
    private void addReportSummary(Document document, SampleReport report) throws IOException {

        Paragraph heading = new Paragraph("REPORT SUMMARY")
                .setBold()
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER);



        document.add(heading);
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65}));

        table.setWidth(UnitValue.createPercentValue(100));

        addRow(table, "Report Title", report.getKeyName());

        addRow(table, "Category", report.getCategory());

        addRow(table, "Scope", report.getScope());

        addRow(table, "Scope Name", report.getScopeName());

        addRow(table, "Language", report.getLanguage());

        addRow(table, "Base Year",
                String.valueOf(report.getBaseYear()));

        addRow(table, "Forecast Year",
                String.valueOf(report.getForecastYear()));

        addRow(table, "Market Value (Base Year)",
                String.valueOf(report.getMarketValueBaseYear()));

        addRow(table, "Market Value (Forecast Year)",
                String.valueOf(report.getMarketValueForecastYear()));

        document.add(table);


    }

    private void addReportSummaryOverview(Document document, SampleReport report) throws IOException {
        int historicYear = report.getHistoricYear() != null
                ? report.getHistoricYear()
                : report.getBaseYear();
        String market = report.getKeyName();
        String unit = report.getUnit() != null && !report.getUnit().isBlank()
                ? report.getUnit()
                : ""+report.getUnit()+"";

        document.add(new Paragraph("Report Summary:\n")
                .setFont(themeRenderer.bold())
                .setFontSize(16));

        document.add(new Paragraph(
                "This detailed business research report, focused on the " + market
                        + " business, fundamentally describes the concepts bifurcated by key segments in terms of Value ("
                        + unit + ") and key players, and stakeholders in the " + market + " comprehensively. "
                        + "Assessment of the current and historical " + market + " business situation ("
                        + historicYear + "-" + report.getBaseYear()
                        + ") is indicated, conflated with competitive landscape and consumer patterns, "
                        + market + " benefits and drawbacks, industry trends, and statistical predictions ("
                        + report.getBaseYear() + "-" + report.getForecastYear() + ").\n\n")
                .setFont(themeRenderer.regular())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.JUSTIFIED));

        document.add(new Paragraph(
                "Regional " + market + " industry characteristics and macroeconomic policies, industrial policies have also been included. "
                        + "From raw materials to downstream buyers of this " + market
                        + " industry will be analyzed statistically; the features of product distribution and sales channel will be presented as well.\n\n")
                .setFont(themeRenderer.regular())
                .setFontSize(10)).setTextAlignment(TextAlignment.JUSTIFIED);

        String[] bullets = {
                "Main manufacturers/suppliers/vendors of " + market
                        + " Worldwide, with company and product introduction, position in the " + market,
                "Market status and development trends of " + market
                        + " by key segments among others are bifurcated and evaluated analytically",
                "Gross margin status of " + market + " and marketing status",
                market + " business growth drivers, restraints, opportunities, and challenges\n"
        };
        // 1. Create ZapfDingbats font
        // 1. Create font passing the name directly as a string (no special constants import needed!)
        PdfFont dingbatsFont = PdfFontFactory.createFont("ZapfDingbats");
        DeviceRgb blueColor = new DeviceRgb(0, 102, 204);

        for (String bullet : bullets) {
            // 2. \u0075 is '♦' in ZapfDingbats
            Text diamondSymbol = new Text("\u0075 ")
                    .setFont(dingbatsFont)
                    .setFontColor(blueColor);

            // 2. Create the main text string
            Text bulletText = new Text(bullet);

            // 3. Combine both into the Paragraph
            Paragraph p = new Paragraph()
                    .add(diamondSymbol)
                    .add(bulletText)
                    .setFont(themeRenderer.regular()) // Ensure themeRenderer.regular() uses IDENTITY_H encoding!
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setFontSize(10)
                    .setMarginLeft(13);

            document.add(p);
        }

        document.add(new Paragraph(
                "This professional and validated report study supports to establish a 360-degree perspective of industrial developments, "
                        + "trends, and characteristics of the " + market
                        + " which vividly aids to address all the business issues comprehensively.\n\n")
                .setFont(themeRenderer.regular())
                .setTextAlignment(TextAlignment.JUSTIFIED)
                        .setFontColor(ColorConstants.BLUE)
                .setFontSize(10));
    }

    private void addRegionMapSection(Document document, SampleReport report, List<MarketSegment> roots)
            throws IOException {
        // Full-page region map with no header/footer (map artwork includes its own chrome)
        document.setMargins(0, 0, 0, 0);

        PdfDocument pdfDocument = document.getPdfDocument();
        // Ensure we are on a dedicated page (AreaBreak before this call may not have flushed yet).
        int mapPageNumber = pdfDocument.getNumberOfPages();
        pagesWithoutHeaderFooter.add(mapPageNumber);

        ClassPathResource mapResource = new ClassPathResource("assets/images/map.jpg");
        ImageData mapData = ImageDataFactory.create(mapResource.getInputStream().readAllBytes());
        Image mapImage = new Image(mapData);

        PageSize pageSize = PageSize.A4.rotate();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        mapImage.scaleAbsolute(pageWidth, pageHeight);
        mapImage.setFixedPosition(mapPageNumber, 0f, 0f);
        document.add(mapImage);

        // Also suppress HF if the fixed image landed on a newly created following page.
        int afterAdd = pdfDocument.getNumberOfPages();
        if (afterAdd != mapPageNumber) {
            pagesWithoutHeaderFooter.add(afterAdd);
        }

        // Restore body margins, then region value/growth table on the following page
        document.setMargins(80, BODY_HORIZONTAL_MARGIN, 80, BODY_HORIZONTAL_MARGIN);
        document.add(new AreaBreak());

        addSegmentValueGrowthTablePage(
                document,
                report,
                "Region",
                resolvePreliminaryRegionLabels(roots),
                "Region");
    }

    private List<String> resolvePreliminaryRegionLabels(List<MarketSegment> roots) {
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
        return Arrays.asList(
                "North America",
                "Europe",
                "Asia Pacific",
                "Latin America",
                "The Middle-East and Africa");
    }

    /**
     * Reusable market-segment section: hierarchy, table, and source on one logical page.
     */
    private void addMarketSegmentSection(
            Document document,
            SampleReport report,
            String segmentTitle,
            List<String> segmentNames,
            String tableColumnTitle) throws IOException {

        document.add(new AreaBreak());
        addSegmentIntroductionPage(document, segmentTitle, segmentNames);
        addSegmentValueGrowthTablePage(
                document,
                report,
                segmentTitle,
                segmentNames,
                tableColumnTitle);
    }

    private void addMarketSegmentSection(
            Document document,
            SampleReport report,
            MarketSegment segmentRoot,
            String tableColumnTitle) throws IOException {

        document.add(new AreaBreak());
        List<MarketSegment> children = segmentRoot.getChildren();
        addSegmentHierarchyIntroduction(
                document,
                segmentRoot.getSegmentName(),
                children);
        addSegmentValueGrowthTablePage(
                document,
                report,
                segmentRoot.getSegmentName(),
                collectLeafSegmentNames(children),
                tableColumnTitle);
    }

    private void addSegmentIntroductionPage(
            Document document,
            String segmentTitle,
            List<String> segmentNames) throws IOException {

        document.add(new Paragraph("By " + segmentTitle)
                .setFont(themeRenderer.semiBold())
                .setFontSize(16)
                .setFontColor(ColorConstants.BLACK)
                .setMarginBottom(8f)
                .setKeepWithNext(true));

        if (segmentNames == null) {
            return;
        }

        for (String name : segmentNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            addSegmentDiamondBullet(document, name);
        }
    }

    private void addSegmentHierarchyIntroduction(
            Document document,
            String segmentTitle,
            List<MarketSegment> childSegments) throws IOException {

        document.add(new Paragraph("By " + segmentTitle)
                .setFont(themeRenderer.semiBold())
                .setFontSize(16)
                .setFontColor(ColorConstants.BLACK)
                .setMarginBottom(8f)
                .setKeepWithNext(true));

        if (childSegments == null) {
            return;
        }

        for (MarketSegment child : childSegments) {
            addSegmentHierarchyNode(document, child, 0);
        }
    }

    private void addSegmentHierarchyNode(
            Document document,
            MarketSegment segment,
            int depth) throws IOException {

        if (segment == null || segment.getSegmentName() == null || segment.getSegmentName().isBlank()) {
            return;
        }

        List<MarketSegment> children = segment.getChildren();
        boolean hasChildren = children != null && !children.isEmpty();

        if (depth == 0) {
            addSegmentDiamondBullet(document, segment.getSegmentName());
        }

        if (hasChildren) {
            addSegmentHierarchyBranches(document, children, depth == 0 ? 1 : depth);
        }
    }

    private void addSegmentHierarchyBranches(
            Document document,
            List<MarketSegment> siblings,
            int depth) throws IOException {

        if (siblings == null || siblings.isEmpty()) {
            return;
        }

        for (int i = 0; i < siblings.size(); i++) {
            MarketSegment segment = siblings.get(i);
            if (segment == null || segment.getSegmentName() == null || segment.getSegmentName().isBlank()) {
                continue;
            }

            boolean lastSibling = i == siblings.size() - 1;
            String branch = lastSibling ? "└── " : "├── ";
            float marginLeft = 12f + (Math.max(0, depth - 1) * 18f);

            document.add(new Paragraph(branch + segment.getSegmentName())
                    .setFont(themeRenderer.regular())
                    .setFontSize(10)
                    .setMarginLeft(marginLeft)
                    .setKeepWithNext(true));

            List<MarketSegment> children = segment.getChildren();
            if (children != null && !children.isEmpty()) {
                addSegmentHierarchyBranches(document, children, depth + 1);
            }
        }
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

    private void addSegmentValueGrowthTablePage(
            Document document,
            SampleReport report,
            String segmentTitle,
            List<String> segmentNames,
            String tableColumnTitle) throws IOException {

        int historicYear = report.getHistoricYear() != null
                ? report.getHistoricYear()
                : report.getBaseYear();
        int baseYear = report.getBaseYear();
        int forecastYear = report.getForecastYear();
        String market = report.getKeyName();

        document.add(new Paragraph(
                "Global " + market
                        + " Value ("+report.getUnit()+") and Growth Rate (%), By " + segmentTitle)
                .setFont(themeRenderer.semiBold())
                .setFontSize(12)
                .setFontColor(ColorConstants.BLACK)
                .setMarginTop(14f)
                .setKeepWithNext(true));

        Table table = BodyTableStyling.newBodyTable(new float[]{40, 20, 20, 20});

        addSegmentTableHeaderRow(
                table,
                tableColumnTitle,
                String.valueOf(historicYear),
                String.valueOf(baseYear),
                String.valueOf(forecastYear));

        double totalHistoric = 0;
        double totalBase = 0;
        double totalForecast = 0;

        if (segmentNames != null) {
            for (String name : segmentNames) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                double[] series = valueSeriesProvider.yearlyValuesUsdMillion(
                        report, market, segmentTitle, name);
                double historicValue = valueAtYear(series, report, historicYear);
                double baseValue = valueAtYear(series, report, baseYear);
                double forecastValue = valueAtYear(series, report, forecastYear);
                totalHistoric += historicValue;
                totalBase += baseValue;
                totalForecast += forecastValue;

                addSegmentTableValueRow(
                        table,
                        name,
                        valueSeriesProvider.formatValue(historicValue),
                        valueSeriesProvider.formatValue(baseValue),
                        valueSeriesProvider.formatValue(forecastValue));

                double[] growth = valueSeriesProvider.yearOverYearGrowthPercent(series);
                addSegmentTableGrowthRow(
                        table,
                        valueSeriesProvider.formatPercent(growthAtYear(growth, report, historicYear)),
                        valueSeriesProvider.formatPercent(growthAtYear(growth, report, baseYear)),
                        valueSeriesProvider.formatPercent(valueSeriesProvider.cagrPercent(series, report)));
            }
        }

        addSegmentTableValueRow(
                table,
                "Total",
                valueSeriesProvider.formatValue(totalHistoric),
                valueSeriesProvider.formatValue(totalBase),
                valueSeriesProvider.formatValue(totalForecast));
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(
                        themeRenderer,
                        "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026."));
    }

    private double valueAtYear(double[] series, SampleReport report, int year) {
        int historic = report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
        int index = year - historic;
        if (series == null || index < 0 || index >= series.length) {
            return 0;
        }
        return series[index];
    }

    private double growthAtYear(double[] growth, SampleReport report, int year) {
        int historic = report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
        int index = year - historic;
        if (growth == null || index < 0 || index >= growth.length) {
            return 0;
        }
        return growth[index];
    }

    private void addSegmentTableHeaderRow(Table table, String c1, String c2, String c3, String c4)
            throws IOException {
        table.addHeaderCell(segmentHeaderCell(c1, TextAlignment.LEFT));
        table.addHeaderCell(segmentHeaderCell(c2, TextAlignment.CENTER));
        table.addHeaderCell(segmentHeaderCell(c3, TextAlignment.CENTER));
        table.addHeaderCell(segmentHeaderCell(c4, TextAlignment.CENTER));
    }

    private Cell segmentHeaderCell(String text, TextAlignment alignment) throws IOException {
        return BodyTableStyling.headerCell(themeRenderer, text, alignment);
    }

    private void addSegmentTableValueRow(Table table, String label, String y1, String y2, String y3)
            throws IOException {
        table.addCell(BodyTableStyling.labelCell(themeRenderer, label));
        table.addCell(segmentValueCell(y1));
        table.addCell(segmentValueCell(y2));
        table.addCell(segmentValueCell(y3));
    }

    private void addSegmentTableGrowthRow(Table table, String y1, String y2, String y3)
            throws IOException {
        table.addCell(BodyTableStyling.labelCell(themeRenderer, "Growth Rate"));
        table.addCell(segmentValueCell(y1));
        table.addCell(segmentValueCell(y2));
        table.addCell(segmentValueCell(y3));
    }

    private Cell segmentValueCell(String value) throws IOException {
        return BodyTableStyling.valueCell(themeRenderer, value);
    }

    private void addPreliminarySegmentationOverviews(
            Document document,
            SampleReport report,
            List<MarketSegment> roots) throws IOException {

        if (roots == null || roots.isEmpty()) {
            return;
        }

        for (MarketSegment root : roots) {
            if (root.getSegmentName() != null
                    && root.getSegmentName().equalsIgnoreCase("Region")) {
                continue;
            }

            String title = root.getSegmentName();
            addMarketSegmentSection(document, report, root, title);
        }
    }

    private void addCompetitiveLandscape(Document document, SampleReport report, int chapter)
            throws IOException {
        String market = report.getKeyName();
        addChapterHeadings(document, report, "CHAPTER " + chapter + "  " + market + " - Competitive Landscape");

        Paragraph section61 = new Paragraph(chapter + ".1 Competitor Market Share - Revenue")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section61);
        section61.setDestination("toc." + chapter + ".1");
        document.add(section61);
        recordTocDestinationPage(document, "toc." + chapter + ".1");

        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Competitor Market Share - Revenue"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/Graph2.png", 90f);
        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");
        document.add(new Paragraph("Note: Charts & Figures only for Illustration purpose")
                .setFont(themeRenderer.regular())
                .setFontSize(9)
                .setItalic());

        int startYear = Math.max(report.getBaseYear() - 3, historicYear(report));
        int endYear = report.getBaseYear();
        List<Integer> years = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            years.add(year);
        }

        document.add(new AreaBreak());
        document.add(new Paragraph("TABLE  Global " + market + " - Company Revenue Analysis, "
                + startYear + "-" + endYear + " Value ("+report.getUnit()+")")
                .setFont(themeRenderer.bold())
                .setFontSize(12));
        addCompanyRevenueTable(document, report, years, false);

        document.add(new AreaBreak());
        document.add(new Paragraph("TABLE  Global " + market + " - Company Revenue Share Analysis, "
                + startYear + "-" + endYear + " (%)")
                .setFont(themeRenderer.bold())
                .setFontSize(12));
        addCompanyRevenueTable(document, report, years, true);

        document.add(new AreaBreak());
        Paragraph section62 = new Paragraph(chapter + ".2 Strategic Developments")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section62);
        section62.setDestination("toc." + chapter + ".2");
        document.add(section62);
        recordTocDestinationPage(document, "toc." + chapter + ".2");

        addStrategicDevelopmentSubsection(document, chapter + ".2.1", "Acquisitions and Mergers",
                "Date", "Company", "Development");
        document.add(new AreaBreak());
        addStrategicDevelopmentSubsection(document, chapter + ".2.2", "New Products",
                "Date", "Company", "Product");
        document.add(new AreaBreak());
        addStrategicDevelopmentSubsection(document, chapter + ".2.3", "Research & Development Activities",
                "Date", "Company", "Research & Development");
    }

    private void addCompanyRevenueTable(
            Document document,
            SampleReport report,
            List<Integer> years,
            boolean sharePercent) throws IOException {

        float[] widths = new float[years.size() + 1];
        widths[0] = 40f;
        float yearWidth = 60f / years.size();
        for (int i = 1; i <= years.size(); i++) {
            widths[i] = yearWidth;
        }

        Table table = BodyTableStyling.newBodyTable(widths);
        table.addHeaderCell(segmentHeaderCell("Company", TextAlignment.LEFT));
        for (Integer year : years) {
            table.addHeaderCell(segmentHeaderCell(String.valueOf(year), TextAlignment.CENTER));
        }

        List<Company> companies = report.getCompanies() != null ? report.getCompanies() : List.of();
        double[] totals = new double[years.size()];
        List<double[]> seriesList = new ArrayList<>();

        for (Company company : companies) {
            if (company == null || company.getCompanyName() == null) {
                continue;
            }
            double[] series = valueSeriesProvider.yearlyValuesUsdMillion(
                    report, report.getKeyName(), "Company", company.getCompanyName());
            seriesList.add(series);
            for (int i = 0; i < years.size(); i++) {
                totals[i] += valueAtYear(series, report, years.get(i));
            }
        }

        int companyIndex = 0;
        for (Company company : companies) {
            if (company == null || company.getCompanyName() == null) {
                continue;
            }
            double[] series = seriesList.get(companyIndex++);
            table.addCell(BodyTableStyling.labelCell(themeRenderer, company.getCompanyName()));
            for (int i = 0; i < years.size(); i++) {
                double value = valueAtYear(series, report, years.get(i));
                String cell = sharePercent
                        ? valueSeriesProvider.formatPercent(totals[i] <= 0 ? 0 : (value / totals[i]) * 100.0)
                        : valueSeriesProvider.formatValue(value);
                table.addCell(segmentValueCell(cell));
            }
        }

        table.addCell(BodyTableStyling.labelCell(themeRenderer, "Others"));
        for (int i = 0; i < years.size(); i++) {
            table.addCell(segmentValueCell(sharePercent ? "—" : "—"));
        }

        table.addCell(BodyTableStyling.labelCell(themeRenderer, "Total"));
        for (int i = 0; i < years.size(); i++) {
            table.addCell(segmentValueCell(sharePercent
                    ? "100.0%"
                    : valueSeriesProvider.formatValue(totals[i])));
        }

        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer,
                        "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026."));
    }

    private void addStrategicDevelopmentSubsection(
            Document document,
            String number,
            String title,
            String c1,
            String c2,
            String c3) throws IOException {

        Paragraph heading = new Paragraph(number + " " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(14);
        BodyFigureLayout.breakBeforeNumberedHeading(document, heading);
        heading.setDestination("toc." + number);
        document.add(heading);
        recordTocDestinationPage(document, "toc." + number);

        document.add(new Paragraph("TABLE  " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(11));

        Table table = BodyTableStyling.newBodyTable(new float[]{25, 35, 40});
        addHeaderRow3Col(table, c1, c2, c3);
        String[] sampleDates = {
                "June " + java.time.Year.now().getValue(),
                "April " + java.time.Year.now().getValue(),
                "February " + java.time.Year.now().getValue(),
                "December " + (java.time.Year.now().getValue() - 1),
                "October " + (java.time.Year.now().getValue() - 1)
        };
        for (String date : sampleDates) {
            addRow3Col(table, date, "", "", false, false);
        }
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, reportPlaceholderSource()));
    }

    private String reportPlaceholderSource() {
        return "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.";
    }

    private void addCompanyProfiles(Document document, SampleReport report, int chapter)
            throws IOException {
        addChapterHeadings(document, report, "CHAPTER " + chapter + "  Company Profiles");

        if (report.getCompanies() == null || report.getCompanies().isEmpty()) {
            document.add(new Paragraph(
                    "Company profiles are not available for this report. Add companies to the sample report to populate this section.")
                    .setFont(themeRenderer.regular())
                    .setFontSize(11));
            return;
        }

        Company firstCompany = null;
        List<Company> remainingCompanies = new ArrayList<>();
        for (Company company : report.getCompanies()) {
            if (company == null || company.getCompanyName() == null || company.getCompanyName().isBlank()) {
                continue;
            }
            if (firstCompany == null) {
                firstCompany = company;
            } else {
                remainingCompanies.add(company);
            }
        }

        if (firstCompany == null) {
            return;
        }

        // Sample report: only the first company receives the full detailed profile.
        addDetailedCompanyProfile(document, report, firstCompany, chapter, 1);

        document.add(new AreaBreak());
        int companySection = 2;
        for (Company company : remainingCompanies) {
            Paragraph companyHeading = new Paragraph(chapter + "." + companySection + " " + company.getCompanyName())
                    .setFont(themeRenderer.bold())
                    .setFontSize(11)
                    .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                    .setKeepWithNext(true);
            companyHeading.setDestination("toc." + chapter + "." + companySection);
            document.add(companyHeading);
            recordTocDestinationPage(document, "toc." + chapter + "." + companySection);
            companySection++;
        }


       document.add(new AreaBreak());

//        Paragraph others = new Paragraph(chapter + "." + companySection + " Others")
//                .setFont(themeRenderer.bold())
//                .setFontSize(11)
//                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR);
//        others.setDestination("toc." + chapter + "." + companySection);
//        document.add(others);
        recordTocDestinationPage(document, "toc." + chapter + "." + companySection);
    }

    private void addDetailedCompanyProfile(
            Document document,
            SampleReport report,
            Company company,
            int chapter,
            int companySection) throws IOException {

        String companyName = company.getCompanyName();
        String market = report.getKeyName();
        String sectionPrefix = chapter + "." + companySection;

        Paragraph companyHeading = new Paragraph(sectionPrefix + " " + companyName)
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR)
                .setKeepWithNext(true);
        companyHeading.setDestination("toc." + sectionPrefix);
        document.add(companyHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix);

        Paragraph overviewHeading = new Paragraph(sectionPrefix + ".1 Company Overview")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, overviewHeading);
        overviewHeading.setDestination("toc." + sectionPrefix + ".1");
        document.add(overviewHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".1");
        document.add(new Paragraph("TABLE  Company Overview")
                .setFont(themeRenderer.bold())
                .setFontSize(11)
                .setKeepWithNext(true));

        Table overviewTable = BodyTableStyling.newBodyTable(new float[]{15, 30, 55});
        addHeaderRow3Col(overviewTable, "Sr.No.", "ITEM", "DESCRIPTION");
        addRow3Col(overviewTable, "1", "Company Name", companyName, false, false);
        addRow3Col(overviewTable, "2", "Website", "XXXXXXXXXX", false, false);
        addRow3Col(overviewTable, "3", "Established Year", "XXX", false, false);
        addRow3Col(overviewTable, "4", "Headquarters", "XXXXXXXXX", false, false);
        addRow3Col(overviewTable, "5", "Key Products", "1) XXXXXXXXX\n2) XXXXXXXXXX", false, false);
        addRow3Col(overviewTable, "6", "Company Profile",
                "XXXXXXX XXXXXXXXXX", false, false);
        addRow3Col(overviewTable, "7", "Key Regions of Sales",
                report.getScopeName() != null ? report.getScopeName() : "XXXXXXX", false, false);
        addRow3Col(overviewTable, "8", "Key Competitors",
                "1) XXXX\n2) XXXXXXX\n3) XXXX", false, false);
        document.add(overviewTable);

        document.add(new AreaBreak());

        Paragraph portfolioHeading = new Paragraph(sectionPrefix + ".2 Product/Service Portfolio")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, portfolioHeading);
        portfolioHeading.setDestination("toc." + sectionPrefix + ".2");
        document.add(portfolioHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".2");
        document.add(new Paragraph("TABLE  Product/Service Portfolio")
                .setFont(themeRenderer.bold())
                .setFontSize(11)
                .setKeepWithNext(true));
        Table productsTable = BodyTableStyling.newBodyTable(new float[]{30, 40, 30});
        addHeaderRow3Col(productsTable, "PRODUCT/SERVICE TYPE", "FEATURES / DESCRIPTION", "APPLICATION");
        addRow3Col(productsTable, "----", "----", "----", false, false);
        addRow3Col(productsTable, "----", "----", "----", false, false);
        addRow3Col(productsTable, "----", "----", "----", false, false);
        document.add(productsTable);

        document.add(new AreaBreak());

        Paragraph metricsHeading = new Paragraph(sectionPrefix + ".3 " + companyName
                + " Revenue, Market Share, YoY Growth Rate")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, metricsHeading);
        metricsHeading.setDestination("toc." + sectionPrefix + ".3");
        document.add(metricsHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".3");
        document.add(new Paragraph("TABLE  Revenue, Market Share, YoY Growth Rate")
                .setFont(themeRenderer.bold())
                .setFontSize(11)
                .setKeepWithNext(true));

        int startYear = Math.max(report.getBaseYear() - 3, historicYear(report));
        int endYear = report.getBaseYear();
        List<Integer> metricYears = new ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            metricYears.add(year);
        }
        float[] metricWidths = new float[metricYears.size() + 1];
        metricWidths[0] = 40f;
        float yearWidth = 60f / metricYears.size();
        for (int i = 1; i <= metricYears.size(); i++) {
            metricWidths[i] = yearWidth;
        }
        Table metricsTable = BodyTableStyling.newBodyTable(metricWidths);
        metricsTable.addHeaderCell(segmentHeaderCell("PARAMETER", TextAlignment.LEFT));
        for (Integer year : metricYears) {
            metricsTable.addHeaderCell(segmentHeaderCell(String.valueOf(year), TextAlignment.CENTER));
        }
        metricsTable.addCell(BodyTableStyling.labelCell(themeRenderer, "Revenue ("+report.getUnit()+")"));
        for (int i = 0; i < metricYears.size(); i++) {
            metricsTable.addCell(segmentValueCell("----"));
        }
        metricsTable.addCell(BodyTableStyling.labelCell(themeRenderer, "Market Share (%)"));
        for (int i = 0; i < metricYears.size(); i++) {
            metricsTable.addCell(segmentValueCell("----"));
        }
        metricsTable.addCell(BodyTableStyling.labelCell(themeRenderer, "YoY Growth Rate (%)"));
        for (int i = 0; i < metricYears.size(); i++) {
            metricsTable.addCell(segmentValueCell("----"));
        }
        BodyTableStyling.addTableWithSource(
                document,
                metricsTable,
                BodyTableStyling.sourceLine(themeRenderer, reportPlaceholderSource()));

        document.add(new AreaBreak());
        Paragraph revenueHeading = new Paragraph(sectionPrefix + ".4 " + companyName + " Revenue and Growth Rate")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, revenueHeading);
        revenueHeading.setDestination("toc." + sectionPrefix + ".4");
        document.add(revenueHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".4");
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", companyName + " Revenue and Growth Rate"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/fig3.jpg", 90f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        Paragraph shareHeading = new Paragraph(sectionPrefix + ".5 " + companyName + " Market Share")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, shareHeading);
        shareHeading.setDestination("toc." + sectionPrefix + ".5");
        document.add(shareHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".5");
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", companyName + " Market Share"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/japanese/graph_year.png", 90f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        Paragraph initiativesHeading = new Paragraph(sectionPrefix
                + ".6 Recent Initiatives, Funding/VC Activities and Technological Innovations")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, initiativesHeading);
        initiativesHeading.setDestination("toc." + sectionPrefix + ".6");
        document.add(initiativesHeading);
        recordTocDestinationPage(document, "toc." + sectionPrefix + ".6");
        document.add(new Paragraph(
                "Recent strategic initiatives for " + companyName
                        + " in the " + market + " are assessed through primary interviews and secondary research.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        sourceParagraph(document, report, reportPlaceholderSource());
    }

    private void addIndustryAnalysis(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            int chapter) throws IOException {

        String market = report.getKeyName();
        addChapterHeadings(document, report, "CHAPTER " + chapter + "  " + market + " - Industry Analysis");



        addNumberedSection(document, chapter + ".1", "Introduction and Taxonomy",
                "This chapter provides taxonomy, key trends, value chain, regulatory mandates, technology roadmap, "
                        + "SWOT, and attractiveness analysis for the " + market + ".");

        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_3.png", 100f);
        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_3.png", 100f);


        document.add(new AreaBreak());
        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".2", market + " - Key Trends", null);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(themeRenderer, "FIGURE  ", "Market Dynamics"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/fig4.png", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addIndustryImpactTable(document, chapter + ".2.1", "Market Drivers", "MARKET DRIVERS", report);
        document.add(new AreaBreak());
        addIndustryImpactTable(document, chapter + ".2.2", "Market Restraints", "MARKET RESTRAINTS", report);
        document.add(new AreaBreak());
        addIndustryImpactTable(document, chapter + ".2.3", "Market Opportunities", "MARKET OPPORTUNITIES", report);
        document.add(new AreaBreak());
        addIndustryImpactTable(document, chapter + ".2.4", "Market Challenges", "MARKET CHALLENGES", report);

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".3", market + " Value Chain Analysis", null);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", market + " – Value Chain Analysis"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_4.jpg", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".4", "Key Mandates and Regulations", null);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Key Mandates and Regulations"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_15.png", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".5", "Technology Roadmap and Timeline", null);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Technology Roadmap and Timeline"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_16.png", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".6", "SWOT Analysis", null);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(themeRenderer, "FIGURE  ", "SWOT Analysis"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/img_7.png", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".7", market + " - Attractiveness Analysis", null);

        List<MarketSegment> dimensions = RegionalOutlineBuilder.segmentDimensions(roots);
        int attractSection = 1;
        if (!dimensions.isEmpty()) {
            MarketSegment firstDim = dimensions.get(0);
            Paragraph attractDimHeading = new Paragraph(
                    chapter + ".7." + attractSection + " By " + firstDim.getSegmentName())
                    .setFont(themeRenderer.semiBold())
                    .setFontSize(12);
            BodyFigureLayout.breakBeforeNumberedHeading(document, attractDimHeading);
            attractDimHeading.setDestination("toc." + chapter + ".7." + attractSection);
            document.add(attractDimHeading);
            recordTocDestinationPage(document, "toc." + chapter + ".7." + attractSection);
            document.add(BodyFigureLayout.figureCaptionUnnumbered(
                    themeRenderer,
                    "FIGURE  ",
                    "Market Attractiveness Analysis – By " + firstDim.getSegmentName()));
            BodyFigureLayout.addClasspathFigure(document, "assets/images/fig5.jpg", 90f);
            sourceParagraph(document, report, reportPlaceholderSource());
            document.add(new Paragraph("Note: Charts & Figures only for Illustration purpose")
                    .setFont(themeRenderer.regular())
                    .setFontSize(9)
                    .setItalic());
            attractSection++;
        }

        document.add(new AreaBreak());
        Paragraph attractRegionHeading = new Paragraph(chapter + ".7." + attractSection + " By Region")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, attractRegionHeading);
        attractRegionHeading.setDestination("toc." + chapter + ".7." + attractSection);
        document.add(attractRegionHeading);
        recordTocDestinationPage(document, "toc." + chapter + ".7." + attractSection);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Market Attractiveness Analysis – By Region"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/map.jpg", 90f);
        sourceParagraph(document, report, reportPlaceholderSource());
        document.add(new Paragraph("Note: Charts & Figures only for Illustration purpose")
                .setFont(themeRenderer.regular())
                .setFontSize(9)
                .setItalic());
    }

    private void addIndustryImpactTable(
            Document document,
            String number,
            String title,
            String header,
            SampleReport report) throws IOException {

        int historic = historicYear(report);
        int base = report.getBaseYear();
        int forecast = report.getForecastYear();

        Paragraph impactHeading = new Paragraph(number + " " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(14);
        BodyFigureLayout.breakBeforeNumberedHeading(document, impactHeading);
        impactHeading.setDestination("toc." + number);
        document.add(impactHeading);
        recordTocDestinationPage(document, "toc." + number);

        document.add(new Paragraph("TABLE  " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(11));

        Table table = BodyTableStyling.newBodyTable(new float[]{40, 20, 20, 20});
        table.addHeaderCell(segmentHeaderCell(header, TextAlignment.LEFT));
        table.addHeaderCell(segmentHeaderCell(historic + "-" + Math.min(historic + 2, base), TextAlignment.CENTER));
        table.addHeaderCell(segmentHeaderCell(Math.max(base - 1, historic) + "-" + base, TextAlignment.CENTER));
        table.addHeaderCell(segmentHeaderCell((base + 1) + "-" + forecast, TextAlignment.CENTER));

        for (int i = 1; i <= 4; i++) {
            table.addCell(BodyTableStyling.labelCell(themeRenderer, title + " " + i));
            table.addCell(segmentValueCell("High"));
            table.addCell(segmentValueCell("Medium"));
            table.addCell(segmentValueCell("High"));
        }
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, reportPlaceholderSource()));
    }

    private void addMarketStrategyAnalysis(Document document, SampleReport report, int chapter)
            throws IOException {
        String market = report.getKeyName();
        addChapterHeadings(document, report,
                "CHAPTER " + chapter + "  Marketing Strategy Analysis, Distributors");

        addNumberedSection(document, chapter + ".1", "Marketing Channel",
                "With the advancement of technology, immense changes are opted for business strategies by manufacturers of the "
                        + market + ". Manufacturers of " + market
                        + " have utilized diversified marketing to sell their products. The development of global competition, technology, and "
                        + "advertising industry have made maintenance, pricing and promotion strategies for the product more complicated. "
                        + "Concerning these challenges, the importance of the marketing channel, as a strategic tool, has rapidly grown.");
        document.add(BodyFigureLayout.figureCaptionUnnumbered(themeRenderer, "FIGURE  ", "Market Channels"));
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_10.png", 200f);
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".2", "Direct Marketing", null);
        addNumberedSection(document, chapter + ".3", "Indirect Marketing", null);
        addNumberedSection(document, chapter + ".4", "Marketing Channel Development Trends", null);

        document.add(new AreaBreak());
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Marketing Channel Development Trend"));
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_11.png", 320f);
        sourceParagraph(document, report, reportPlaceholderSource());
    }

    private void addReportConclusions(Document document, SampleReport report, int chapter)
            throws IOException {
        addChapterHeadings(document, report, "CHAPTER " + chapter + "  Report Conclusion & Key Insights");

        addNumberedSection(document, chapter + ".1",
                "Key Insights from Primary Interviews & Survey's Respondents",
                "");
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_3.png", 110f);
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_3.png", 110f);

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".2",
                "Key Takeaways from Analysts, Consultants, and Industry Leaders",
                "");

        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_3.png", 110f);
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document, "assets/images/img_3.png", 110f);

    }

    private void addResearchMethodology(Document document, SampleReport report, int chapter)
            throws IOException {
        String market = report.getKeyName();
        int historic = historicYear(report);
        int base = report.getBaseYear();
        int forecast = report.getForecastYear();

        addChapterHeadings(document, report, "CHAPTER " + chapter + "  Research Approach & Methodology");

        addNumberedSection(document, chapter + ".1", "Report Description",
                "The report covers a forecast and an analysis of the " + market
                        + " on a global and regional level as well as country level trends and market sizes. "
                        + "The study provides historical data from " + historic + " to " + forecast
                        + " along with projections from " + base + " to " + forecast
                        + " based on revenue Value ("+report.getUnit()+"). The study includes the drivers and restraints of the "
                        + market + " along with their impact on the demand over the forecast period. Additionally, "
                        + "the report includes the study of opportunities available in the " + market + ". \n\n"
                        + "The report provides qualitative industry analysis further quantified by their impacts over the historical timeline "
                        + "as well as over the projected timeline. The study encompasses a market attractiveness analysis, wherein all the "
                        + "segments and regions are benchmarked based on their market size, growth rate, and general attractiveness for attractive investment pockets. \n\n"
                        + "In order to give the users of this report a comprehensive view of the " + market
                        + ", we have included a competitive landscape for the market. The report provides company market share analysis to give a broader overview of the key players in the market."
                        + "In addition, the report also covers key strategic developments of the competitive market including acquisitions & mergers, new product launches, agreements, partnerships,"
                        + "collaborations & joint ventures, research & development activities, and regional expansion of major and emerging participants involved in the market on a global and regional basis.\n\n"


                        +"The study provides a decisive view of the "+ market +" by segmenting it based on sales channel, application, and region. All the segments"
                        +"have been analyzed based on present and future trends and the market is estimated from "+ historic +" to "+ forecast +" with actual data from "+ historic +" to "+ base +"."
                        +"regional segment includes the current and forecast demand for North America, Europe, Asia Pacific, Latin America, and Middle East & Africa which"
                        +"are further bifurcated into individual countries.");

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".2", "Research Scope",
                "This report provides market size of " + market
                        + " for the past years and forecasts for the next years. The global " + market
                        + " size is given in terms of revenue value and volume. Market revenue is defined in "+report.getUnit()+". "
                        + "Market numbers are given on the basis of different segments. Market size and forecasts for each major segment is provided in the context of each region viz. "
                        + "North America, Europe, Asia Pacific, Latin America, and Middle East & Africa.\n\n"
                        + "The numbers and data provided in this report are derived on the basis of demand as well as supply for "
                        + market + " from different countries across the globe.");

        document.add(new AreaBreak());
        addNumberedSection(document, chapter + ".3", "Research Methodology", "The process of market research at spherical insights is an iterative process in nature and usually follows following robust path. Information from" +
                "secondary used to build data models, then results from data models are validated from primary participants. Then cycle repeats where, according" +
                "to inputs from primary participants, additional secondary research is done and new information is again incorporated into data model. The" +
                "processes continue till desired level of information is not generated.\n\n"+
                "To calculate the market size, the report considers the revenue generated from the sales/subscription of "+market+". The revenue" +
                "generated from the sales of "+market+" has been calculated through primary and secondary research. The report also presents the key" +
                "players operating in the "+market+" across the globe identified through secondary research and a corresponding detailed analysis of the" +
                "top vendors in the market. The market size calculation also includes segmentation determined using secondary products and verified through" +
                "primary products.");
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document,"assets/images/img_10.png", 180f);

        document.add(new AreaBreak());
        Paragraph secondaryHeading = new Paragraph(chapter + ".3.1 Secondary Research")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, secondaryHeading);
        document.add(secondaryHeading);
        document.add(new Paragraph(
                "The secondary research products that are typically referred to include, but are not limited to:")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        String[] secondary = {
                "Company websites, annual reports, financial reports, broker reports, investor presentations and SEC filings",
                "Internal and external proprietary databases, relevant patent and regulatory databases",
                "National government documents, statistical databases and market reports",
                "News articles, press releases and web-casts specific to the companies operating in the market"
        };
        // 1. Create the ZapfDingbats font for standard PDF symbol support
        PdfFont symbolFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.ZAPFDINGBATS);

        for (String item : secondary) {
            Paragraph p = new Paragraph()
                    .setFontSize(11)
                    .setMarginLeft(12)
                    .setMarginBottom(3f);

            // 2. BLUE DIAMOND ONLY ('u' in ZapfDingbats renders as a solid diamond ♦)
            Text diamondSymbol = new Text("u  ")
                    .setFont(symbolFont)
                    .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR); // Blue (#0070C0)

            // 3. BLACK TEXT ONLY
            Text itemText = new Text(item)
                    .setFont(themeRenderer.regular())
                    .setFontColor(ColorConstants.BLACK); // Keeps text black

            // Add both elements to the paragraph
            p.add(diamondSymbol);
            p.add(itemText);

            document.add(p);
        }

        document.add(new AreaBreak());
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer,
                "FIGURE  ",
                "The products for secondary research include but are not limited to: Factiva, Hoovers, Statista"));
        addImageToPages(document, report, "assets/images/img_15.png");
        sourceParagraph(document, report, reportPlaceholderSource());

        document.add(new AreaBreak());
        // Reuse the existing symbolFont variable already declared at the top of addResearchMethodology
// (If not initialized yet, just do: symbolFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.ZAPFDINGBATS);)

// --- 11.3.2 Primary Research ---
        Paragraph primaryHeading = new Paragraph(chapter + ".3.2 Primary Research")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, primaryHeading);
        document.add(primaryHeading);

        document.add(new Paragraph(
                "We conduct primary interviews on an ongoing basis with industry participants and commentators in order to validate data and analysis. "
                        + "A typical research interview fulfills the following functions:")
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setMarginBottom(6f));

// Primary Research Bullet List 1
        List<String> primaryFunctions = Arrays.asList(
                "It provides first-hand information on the market size, market trends, growth trends, competitive landscape, future outlook etc.",
                "Helps in validating and strengthening the secondary research findings",
                "Further develops the analysis team’s expertise and market understanding",
                "Primary research involves E-mail interactions, telephonic interviews as well as face-to-face interviews for each market, category, segment and sub-segment across geographies"
        );

        for (String item : primaryFunctions) {
            Paragraph p = new Paragraph()
                    .setFontSize(11)
                    .setMarginLeft(12)
                    .setMarginBottom(3f);

            Text diamond = new Text("u  ")
                    .setFont(symbolFont)
                    .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR);

            Text content = new Text(item)
                    .setFont(themeRenderer.regular())
                    .setFontColor(ColorConstants.BLACK);

            p.add(diamond).add(content);
            document.add(p);
        }

        document.add(new Paragraph(
                "The participants who typically take part in such a power rating include, but are not limited to:")
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setMarginTop(6f)
                .setMarginBottom(6f));

// Primary Research Bullet List 2 (Participants)
        List<String> participants = Arrays.asList(
                "Industry participants: CEOs, VPs, marketing/ type managers, market intelligence managers and national sales managers",
                "Purchasing managers, technical personnel, distributors and resellers",
                "Outside experts: Investment bankers, valuation experts, research analysts specializing in specific markets",
                "Key opinion leaders specializing in different areas corresponding to different industry applications"
        );

        for (String item : participants) {
            Paragraph p = new Paragraph()
                    .setFontSize(11)
                    .setMarginLeft(12)
                    .setMarginBottom(3f);

            Text diamond = new Text("u  ")
                    .setFont(symbolFont)
                    .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR);

            Text content = new Text(item)
                    .setFont(themeRenderer.regular())
                    .setFontColor(ColorConstants.BLACK);

            p.add(diamond).add(content);
            document.add(p);
        }


// --- 11.3.3 Statistical Models ---
        Paragraph statisticalHeading = new Paragraph(chapter + ".3.3 Statistical Models")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12)
                .setMarginTop(10f);
        BodyFigureLayout.breakBeforeNumberedHeading(document, statisticalHeading);
        document.add(statisticalHeading);

        document.add(new Paragraph(
                "Where no hard data is available, we use modeling and estimates in order to produce comprehensive data sets. "
                        + "A rigorous methodology is adopted in which the available hard data is cross referenced with the following data types to produce estimates:")
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setMarginBottom(6f));

// Statistical Models Bullet List
        List<String> dataTypes = Arrays.asList(
                "Demographic data: Population split by segment",
                "Macro-economic indicators: GDP, etc.",
                "Industry indicators: Expenditure, Product stage & infrastructure, sector growth and facilities."
        );

        for (String item : dataTypes) {
            Paragraph p = new Paragraph()
                    .setFontSize(11)
                    .setMarginLeft(12)
                    .setMarginBottom(3f);

            Text diamond = new Text("u  ")
                    .setFont(symbolFont)
                    .setFontColor(BodyFigureLayout.NUMBERED_SECTION_HEADING_COLOR);

            Text content = new Text(item)
                    .setFont(themeRenderer.regular())
                    .setFontColor(ColorConstants.BLACK);

            p.add(diamond).add(content);
            document.add(p);
        }

        document.add(new Paragraph("Data is then cross checked by the expert panel.")
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setMarginTop(6f));

        document.add(new AreaBreak());
        Paragraph companyShareHeading = new Paragraph(chapter + ".3.3.1 Company Share Analysis Model")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, companyShareHeading);
        document.add(companyShareHeading);
        document.add(new Paragraph(
                "Company share analysis is used to derive the size of global market. Study of revenues of companies for last three to five years also provide "
                        + "the base for forecasting the market size and its growth rate.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Company Share Analysis Model"));
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document,"assets/images/img_16.png", 220f);

        document.add(new AreaBreak());
        Paragraph revenueModelHeading = new Paragraph(chapter + ".3.3.2 Revenue Based Modelling")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);



        BodyFigureLayout.breakBeforeNumberedHeading(document, revenueModelHeading);
        document.add(revenueModelHeading);
        document.add(new Paragraph("Revenue based models can be built in two ways Top-Down or Bottom-Up irrespective of industry. Market size estimated from company share" +
                        "analysis acts as a validation point for bottom-up approach where as it acts as starting point for top-down approach.")).setFont(themeRenderer.regular())
                .setFontSize(10);
        document.add(BodyFigureLayout.figureCaptionUnnumbered(
                themeRenderer, "FIGURE  ", "Revenue Based Modeling"));
        BodyFigureLayout.addClasspathFigureWithMaxHeight(document,"assets/images/img_17.png", 260f);

        document.add(new AreaBreak());
        Paragraph limitationsHeading = new Paragraph(chapter + ".3.4 Research Limitations")
                .setFont(themeRenderer.semiBold())
                .setFontSize(12);
        BodyFigureLayout.breakBeforeNumberedHeading(document, limitationsHeading);
        document.add(limitationsHeading);
        document.add(new Paragraph(
                "Inflation is not a part of pricing analysis and revenue calculations in this report. Prices of "
                        + market + " and its derivatives vary in each region and hence similar revenue ratio does not follow for each individual region. "
                        + "The weighted average price for each type has been taken into account while estimating and forecasting market revenue on a global basis. "
                        + "This report provides market size of " + market + " for the past years and forecasts for the next years. "
                        + "Market revenue is defined in "+report.getUnit()+".")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
    }

    private void addAboutUs(Document document, SampleReport report) throws IOException {
        document.add(new Paragraph("About Us")
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setUnderline());
        document.add(new Paragraph(
                "Spherical Insights aims at providing actionable insights through data analytics for companies to improve their business acumen. "
                        + "We have a robust forecasting and estimation model to meet the clients' objectives of high-quality output within a short span of time.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(new Paragraph(
                "We provide both customized (clients' specific) and syndicate reports. Our repository of syndicate reports is diverse across all the categories and sub-categories across domains. "
                        + "Our customized Components are tailored to meet the clients' requirement whether they are looking to expand or planning to launch a new product in the global market.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(new Paragraph(
                "We publish market research reports after thorough analysis from both primary and secondary sources, which comprises of market overview, market sizing estimation, "
                        + "competitive landscape, major player, key trends, and current market scenario.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(new Paragraph(
                "We have a dedicated team of highly expert analysts and consultants specific to each domain to provide an in-depth analysis of the different industries. "
                        + "Our teams have experience in keeping track of recent development across industries and interpret them to provide the clients with the most recent analysis and market sizing forecast.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(new Paragraph(
                "Some of the industries we track include Hospitality & transportation, energy & power, Food & Beverages, chemicals & materials, information & communication services, and healthcare.")
                .setFont(themeRenderer.regular())
                .setFontSize(11));
        document.add(new AreaBreak());
        addImageToPages(document, report, "assets/images/img_18.png");

        document.add(new Paragraph(
                "Copyright © 2025 Spherical Insights\n " +
                        "All Rights Reserved. This document contains highly confidential information and is the sole property of Spherical Insights. No part of it may be circulated, copied, quoted, or otherwise\n" +
                        "reproduced without the approval of Spherical Insights")
                .setFont(themeRenderer.regular())
                        .setFontColor(ColorConstants.RED)
                .setFontSize(8));


    }

    private void addDisclaimer(Document document) throws IOException {
        document.add(new Paragraph("Disclaimer")
                .setFont(themeRenderer.bold())
                .setFontSize(16));
        document.add(new Paragraph(
                "Any part of this report cannot be reproduced, kept in a retrieval system or transmitted in any kind by any means, electronic, mechanical, "
                        + "photocopying, recording or otherwise, without the prior permission of the publisher, Spherical Insights.")
                .setFont(themeRenderer.regular())
                .setFontSize(11)).setTextAlignment(TextAlignment.JUSTIFIED);
        document.add(new Paragraph(
                "The TOC is subject to change during final deliverable depending upon the feasibility, availability of data, and customization of contents from either party.")
                .setFont(themeRenderer.regular())
                .setFontSize(11)).setTextAlignment(TextAlignment.JUSTIFIED);
        document.add(new Paragraph(
                "The information and opinions in this report were prepared by Spherical Insights. The information herein is believed to be reliable and has been obtained from public sources believed to be reliable. "
                        + "All info provided in this report is provided for information purposes solely.")
                .setFont(themeRenderer.regular())
                .setFontSize(11)).setTextAlignment(TextAlignment.JUSTIFIED);
        document.add(new Paragraph("The facts of this report are believed to be correct at the time of publication however cannot be assured. Please note that the judgments, conclusions" +
                "and recommendations that Spherical Insights delivers can be based on information gathered in good faith from each primary and secondary source, " +
                "we have a tendency to be always in a position to ensure.")).setTextAlignment(TextAlignment.JUSTIFIED);

        document.add(new Paragraph("Research can initiate, update and cease coverage alone at the discretion of the Spherical Insights."));

        document.add(new Paragraph(
                "Spherical Insights has no obligation to notify a recipient thereof within the event that any estimation, forecast, changes or subsequently becomes inaccurate. "
                        + "As such Spherical Insights will accept no liability whatever for actions taken based mostly on any information that could subsequently prove to be incorrect.")
                .setFont(themeRenderer.regular())
                .setFontSize(11)).setTextAlignment(TextAlignment.JUSTIFIED);

        document.add(new AreaBreak());
        document.add(new Paragraph("DISCLAIMER")
                .setFont(themeRenderer.bold())
                .setFontSize(16));

        document.add(new Paragraph(
                """
                        Spherical Insights is an organization offering strategic business/market research and analysis source. Research report includes research analysis,\
                        research recommendation or an opinion based on primary information gathered from industry participants and secondary data source available\
                        publicly/internal data/ other reliable source believed to be true. Spherical Insights does not give any warranty, guarantee or official confirmation of\
                        the accuracy of the data and information presented in the research reports, as the findings are based on primary information gathered from\
                        industry participants and secondary data source. Spherical Insights does not take any responsibility of incorrect information or data supplied by\
                        primary interviewees or the manufacturers we contacted during the course of research. The clients buying our report are not allowed to reproduce,\
                        lend, resale or disclose this document to any other third-party entity. This can only be done with prior permission from Spherical Insights.\
                        Reproduction Or/and transmission of this document by any means or in any form including mechanical, electronic, photocopying, recording or\
                        otherwise is prohibited unless and until authorization is received from Spherical Insights.
                        
                        \n\n""")
                .setFont(themeRenderer.regular())
                .setFontSize(11));

        document.add(new Paragraph(
                "For information regarding permissions,\n")
                .setFont(themeRenderer.regular())

                .setFontSize(11));

        document.add(new Paragraph("Contact: Email: sales@sphericalinsights.com").setFont(themeRenderer.regular())
                .setFontSize(11));

    }

    private void addNumberedSection(Document document, String number, String title, String body)
            throws IOException {
        Paragraph heading = new Paragraph(number + " " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(11);
        BodyFigureLayout.breakBeforeNumberedHeading(document, heading);
        heading.setDestination("toc." + number);
        document.add(heading);
        recordTocDestinationPage(document, "toc." + number);
        if (body != null && !body.isBlank()) {
            document.add(new Paragraph(body)
                    .setFont(themeRenderer.regular())
                    .setFontSize(11));
        }
    }

    private int historicYear(SampleReport report) {
        return report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
    }

    private void addPlaceholderSegmentTable(
            Document document,
            SampleReport report,
            String dimension,
            String... rows) throws IOException {

        Table table = BodyTableStyling.newBodyTable(new float[]{40, 30, 30});
        int base = report.getBaseYear();
        int forecast = report.getForecastYear();
        addHeaderRow3Col(table, dimension, String.valueOf(base), String.valueOf(forecast));
        for (String row : rows) {
            double[] series = valueSeriesProvider.yearlyValuesUsdMillion(
                    report, report.getKeyName(), dimension, row);
            addRow3Col(table, row,
                    valueSeriesProvider.formatValue(valueAtYear(series, report, base)),
                    valueSeriesProvider.formatValue(valueAtYear(series, report, forecast)));
            addRow3Col(table, "Growth Rate",
                    valueSeriesProvider.formatPercent(0),
                    valueSeriesProvider.formatPercent(valueSeriesProvider.cagrPercent(series, report)));
        }
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, reportPlaceholderSource()));
    }

    private void addSegmentPage(Document document, MarketSegment segment, SampleReport report) throws IOException {

        Paragraph title = new Paragraph(segment.getSegmentName())
                .setFont(themeRenderer.bold())
                .setFontSize(16);

        document.add(title);


        document.add(new Paragraph(
                "This section provides a comprehensive analysis of "
                        + segment.getSegmentName()

                        + " including market trends, growth drivers, opportunities, and future outlook."
        ));

        Table table = BodyTableStyling.newBodyTable(new float[]{60, 40});

        addRow(table, "Segment", segment.getSegmentName());

        addRow(table,"Base Year",
                String.valueOf(report.getBaseYear()));

        addRow(table,"Forecast",
                String.valueOf(report.getForecastYear()));

        document.add(table);

    }

    private void addSubsegmentTitlePara(Document document, MarketSegment segment, SampleReport report) throws IOException {

        document.add(new Paragraph(
                "These are the subsegments of this segment"
        ));

    }

    private void addSubsegment(Document document, MarketSegment segment, SampleReport report) throws IOException {

        document.add(new Paragraph(
                "These are the subsegments of this segment"
        ));

    }

    private void addCompanyList(Document document, SampleReport report) throws IOException {

        Paragraph heading = new Paragraph("Key Players:")
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setTextAlignment(TextAlignment.LEFT);

        document.add(heading);

        if (report.getCompanies() == null || report.getCompanies().isEmpty()) {
            document.add(new Paragraph("—")
                    .setFont(themeRenderer.regular())
                    .setFontSize(10));
            return;
        }

        // 1. Initialize ZapfDingbats font and Blue color
        PdfFont dingbatsFont = PdfFontFactory.createFont("ZapfDingbats");
        DeviceRgb blueColor = new DeviceRgb(0, 102, 204);

        // 2. Loop through companies with blue diamond bullet
        for (Company company : report.getCompanies()) {
            if (company == null || company.getCompanyName() == null || company.getCompanyName().isBlank()) {
                continue;
            }

            Text diamondSymbol = new Text("\u0075 ")
                    .setFont(dingbatsFont)
                    .setFontColor(blueColor);

            Text companyName = new Text(company.getCompanyName());

            Paragraph p = new Paragraph()
                    .add(diamondSymbol)
                    .add(companyName)
                    .setFont(themeRenderer.regular())
                    .setFontSize(10)
                    .setMarginLeft(13);

            document.add(p);
        }

        // 3. Add "Others" with the blue diamond bullet
        Text diamondSymbol = new Text("\u0075 ")
                .setFont(dingbatsFont)
                .setFontColor(blueColor);

        Text othersText = new Text("Others");

        Paragraph othersParagraph = new Paragraph()
                .add(diamondSymbol)
                .add(othersText)
                .setFont(themeRenderer.regular())
                .setFontSize(10) // Set to 10 to match company list items, or adjust as needed
                .setMarginLeft(13);

        document.add(othersParagraph);
    }
    private void addListOfFigures(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            ReportChapterLayout chapterLayout) throws IOException {

        String market = report.getKeyName().toUpperCase(Locale.ROOT);
        int historicYear = historicYear(report);
        String yearRange = historicYear + "-" + report.getForecastYear();
        String yearPair = report.getBaseYear() + " & " + report.getForecastYear();
        float tabPosition = 718f;

        document.add(new Paragraph("List of Figures")
                .setFont(themeRenderer.bold())
                .setFontSize(24)
                .setMarginTop(0)
                .setMarginBottom(16));

        List<String> captions = new ArrayList<>();
        captions.add("FIGURE 1 COVID-19 IMPACT ANALYSIS");
        captions.add("FIGURE 2 GLOBAL " + market + ", " + yearRange + " VALUE ("+report.getUnit()+")");

        int figure = 3;
        List<MarketSegment> dimensions = RegionalOutlineBuilder.segmentDimensions(roots);
        for (MarketSegment dimension : dimensions) {
            if (dimension == null || dimension.getSegmentName() == null) {
                continue;
            }
            String dim = dimension.getSegmentName().toUpperCase(Locale.ROOT);
            captions.add("FIGURE " + figure++ + " GLOBAL " + market + " SHARE, BY " + dim
                    + ", " + yearPair + " VALUE ("+report.getUnit()+")");
            List<MarketSegment> children = dimension.getChildren();
            if (children != null && !children.isEmpty()) {
                MarketSegment first = children.get(0);
                if (first != null && first.getSegmentName() != null) {
                    captions.add("FIGURE " + figure++ + " GLOBAL " + market + " FOR "
                            + first.getSegmentName().toUpperCase(Locale.ROOT)
                            + ", " + yearRange + " VALUE ("+report.getUnit()+")");
                }
            }
        }

        captions.add("FIGURE " + figure++ + " GLOBAL " + market + " SHARE, BY REGION, "
                + yearPair + " VALUE ("+report.getUnit()+")");

        List<MarketSegment> regions = RegionalOutlineBuilder.resolveRegions(roots);
        if (regions.isEmpty()) {
            for (String region : com.sample_generator.sample.pdf.regional.RegionalGeoCatalog.defaultRegionOrder()) {
                captions.add("FIGURE " + figure++ + " " + region.toUpperCase(Locale.ROOT) + " "
                        + market + ", " + yearRange + " VALUE ("+report.getUnit()+")");
            }
        } else {
            for (MarketSegment region : regions) {
                if (region == null || region.getSegmentName() == null) {
                    continue;
                }
                captions.add("FIGURE " + figure++ + " " + region.getSegmentName().toUpperCase(Locale.ROOT)
                        + " " + market + ", " + yearRange + " VALUE ("+report.getUnit()+")");
                List<MarketSegment> countries = region.getChildren();
                if (countries != null) {
                    for (MarketSegment country : countries) {
                        if (country == null || country.getSegmentName() == null) {
                            continue;
                        }
                        captions.add("FIGURE " + figure++ + " " + country.getSegmentName().toUpperCase(Locale.ROOT)
                                + " " + market + ", " + yearRange + " VALUE ("+report.getUnit()+")");
                    }
                }
            }
        }

        captions.add("FIGURE " + figure++ + " COMPETITOR MARKET SHARE – REVENUE");
        captions.add("FIGURE " + figure++ + " MARKET DYNAMICS");
        captions.add("FIGURE " + figure++ + " GLOBAL " + market + " – VALUE CHAIN ANALYSIS");
        captions.add("FIGURE " + figure++ + " KEY MANDATES AND REGULATIONS");
        captions.add("FIGURE " + figure++ + " TECHNOLOGY ROADMAP AND TIMELINE");
        captions.add("FIGURE " + figure++ + " SWOT ANALYSIS");
        captions.add("FIGURE " + figure++ + " MARKET ATTRACTIVENESS ANALYSIS – BY COMPONENT");
        captions.add("FIGURE " + figure++ + " MARKET ATTRACTIVENESS ANALYSIS – BY REGION");
        captions.add("FIGURE " + figure++ + " MARKET CHANNELS");
        captions.add("FIGURE " + figure++ + " MARKETING CHANNEL DEVELOPMENT TREND");
        captions.add("FIGURE " + figure++ + " THE PRODUCTS FOR SECONDARY RESEARCH INCLUDE BUT ARE NOT LIMITED TO: "
                + "FACTIVA, HOOVERS, STATISTA");
        captions.add("FIGURE " + figure++ + " COMPANY SHARE ANALYSIS MODEL");
        captions.add("FIGURE " + figure + " REVENUE BASED MODELING");

        for (String caption : captions) {
            Paragraph line = new Paragraph()
                    .setMarginTop(2)
                    .setMarginBottom(2);
            line.addTabStops(new TabStop(tabPosition, TabAlignment.RIGHT, new DottedLine()));
            line.add(new Text(caption)
                    .setFont(themeRenderer.regular())
                    .setFontSize(9.5f));
            line.add(new Tab());
            line.add(new Text(" ")
                    .setFont(themeRenderer.regular())
                    .setFontSize(9.5f));
            document.add(line);
        }
    }

    private void addListOfTables(
            Document document,
            SampleReport report,
            List<MarketSegment> roots,
            ReportChapterLayout chapterLayout) throws IOException {

        String market = report.getKeyName().toUpperCase(Locale.ROOT);
        int historicYear = historicYear(report);
        String yearRange = historicYear + " - " + report.getForecastYear();
        float tabPosition = 718f;

        document.add(new Paragraph("List of Tables")
                .setFont(themeRenderer.bold())
                .setFontSize(24)
                .setMarginTop(0)
                .setMarginBottom(16));

        List<String> captions = new ArrayList<>();
        int tableNo = 1;
        captions.add("TABLE " + tableNo++ + " GLOBAL " + market + ", " + yearRange + " VALUE ("+report.getUnit()+")");

        List<MarketSegment> dimensions = RegionalOutlineBuilder.segmentDimensions(roots);
        for (MarketSegment dimension : dimensions) {
            if (dimension == null || dimension.getSegmentName() == null) {
                continue;
            }
            captions.add("TABLE " + tableNo++ + " GLOBAL " + market + ", BY "
                    + dimension.getSegmentName().toUpperCase(Locale.ROOT) + " "
                    + yearRange + " VALUE ("+report.getUnit()+")");
        }

        captions.add("TABLE " + tableNo++ + " GLOBAL " + market + ", BY REGION "
                + yearRange + " VALUE ("+report.getUnit()+")");

        List<MarketSegment> regions = RegionalOutlineBuilder.resolveRegions(roots);
        List<String> regionNames = new ArrayList<>();
        if (regions.isEmpty()) {
            regionNames.addAll(com.sample_generator.sample.pdf.regional.RegionalGeoCatalog.defaultRegionOrder());
        } else {
            for (MarketSegment region : regions) {
                if (region != null && region.getSegmentName() != null) {
                    regionNames.add(region.getSegmentName());
                }
            }
        }
        for (String regionName : regionNames) {
            captions.add("TABLE " + tableNo++ + " " + regionName.toUpperCase(Locale.ROOT) + " "
                    + market + ", BY COUNTRY " + yearRange + " VALUE ("+report.getUnit()+")");
            for (MarketSegment dimension : dimensions) {
                if (dimension == null || dimension.getSegmentName() == null) {
                    continue;
                }
                captions.add("TABLE " + tableNo++ + " " + regionName.toUpperCase(Locale.ROOT) + " "
                        + market + ", BY " + dimension.getSegmentName().toUpperCase(Locale.ROOT)
                        + " " + yearRange + " VALUE ("+report.getUnit()+")");
            }
        }

        captions.add("TABLE " + tableNo++ + " GLOBAL " + market
                + " - COMPANY REVENUE ANALYSIS, VALUE ("+report.getUnit()+")");
        captions.add("TABLE " + tableNo++ + " GLOBAL " + market
                + " - COMPANY REVENUE SHARE ANALYSIS (%)");
        captions.add("TABLE " + tableNo++ + " ACQUISITION AND MERGERS");
        captions.add("TABLE " + tableNo++ + " NEW PRODUCTS");
        captions.add("TABLE " + tableNo++ + " RESEARCH & DEVELOPMENT ACTIVITIES");
        captions.add("TABLE " + tableNo++ + " COMPANY OVERVIEW");
        captions.add("TABLE " + tableNo++ + " PRODUCT/SERVICE PORTFOLIO");
        captions.add("TABLE " + tableNo++ + " REVENUE, MARKET SHARE, YOY GROWTH RATE");
        captions.add("TABLE " + tableNo++ + " MARKET DRIVERS");
        captions.add("TABLE " + tableNo++ + " MARKET RESTRAINTS");
        captions.add("TABLE " + tableNo++ + " MARKET OPPORTUNITIES");
        captions.add("TABLE " + tableNo + " MARKET CHALLENGES");

        for (String caption : captions) {
            Paragraph line = new Paragraph()
                    .setMarginTop(2)
                    .setMarginBottom(2);
            line.addTabStops(new TabStop(tabPosition, TabAlignment.RIGHT, new DottedLine()));
            line.add(new Text(caption)
                    .setFont(themeRenderer.regular())
                    .setFontSize(9.5f));
            line.add(new Tab());
            line.add(new Text(" ")
                    .setFont(themeRenderer.regular())
                    .setFontSize(9.5f));
            document.add(line);
        }
    }

    private void addTableOfContents(Document document, SampleReport report, ReportChapterLayout chapterLayout)
            throws IOException {
        String market = report.getKeyName();
        int baseYear = report.getBaseYear();
        int forecastYear = report.getForecastYear();
        int historic = historicYear(report);

        document.add(new Paragraph("Table of Contents")
                .setFont(themeRenderer.bold())
                .setFontSize(24)
                .setMarginTop(0)
                .setMarginBottom(16));

        addTocChapter(document, 1, "Executive Summary", formatTocPage("toc.ch1"));
        addTocEntry(document, "1.1", "Introduction of " + market, 1, formatTocPage("toc.1.1"));
        addTocEntry(document, "1.1.1", "Global " + market + ", " + baseYear + " & " + forecastYear
                + " Value ("+report.getUnit()+")", 2, formatTocPage("toc.1.1.1"));
        addTocEntry(document, "1.2", "COVID-19 Impacts on " + market + " Industry", 1, formatTocPage("toc.1.2"));
        addTocEntry(document, "1.2.1", "COVID-19 Short-Term Impact & Business Strategies", 2, formatTocPage("toc.1.2.1"));
        addTocEntry(document, "1.2.2", "COVID-19 Mid-Term Impact & Business Strategies", 2, formatTocPage("toc.1.2.2"));
        addTocEntry(document, "1.2.3", "COVID-19 Long-Term Impact & Business Strategies", 2, formatTocPage("toc.1.2.3"));
        addTocEntry(document, "1.3", "Global " + market + ", " + historic + " - " + forecastYear
                + " Value ("+report.getUnit()+")", 1, formatTocPage("toc.1.3"));

        int segmentChapter = chapterLayout.firstSegmentChapter();
        List<String> segmentTitles = chapterLayout.getSegmentChapterTitles();
        for (int i = 0; i < segmentTitles.size(); i++) {
            addTocChapter(document, segmentChapter + i, segmentTitles.get(i),
                    formatTocPage(chapterLayout.destinationForChapter(segmentChapter + i)));
        }
        for (TocOutlineEntry entry : chapterLayout.getSegmentTocEntries()) {
            addTocEntry(document, entry.number(), entry.title(), entry.level(), formatTocPage(entry.destinationKey()));
        }

        int regionalChapter = chapterLayout.regionalChapter();
        addTocChapter(
                document,
                regionalChapter,
                market + " - Regional Analysis",
                formatTocPage(chapterLayout.regionalChapterDestination()));
        for (TocOutlineEntry entry : chapterLayout.getRegionalTocEntries()) {
            addTocEntry(
                    document,
                    entry.number(),
                    entry.title(),
                    entry.level(),
                    formatTocPage(entry.destinationKey()));
        }

        int competitive = chapterLayout.competitiveChapter();
        addTocChapter(document, competitive, market + " - Competitive Landscape",
                formatTocPage(chapterLayout.destinationForChapter(competitive)));
        addTocEntry(document, competitive + ".1", "Competitor Market Share - Revenue", 1,
                formatTocPage("toc." + competitive + ".1"));
        addTocEntry(document, competitive + ".2", "Strategic Developments", 1,
                formatTocPage("toc." + competitive + ".2"));
        addTocEntry(document, competitive + ".2.1", "Acquisitions and Mergers", 2,
                formatTocPage("toc." + competitive + ".2.1"));
        addTocEntry(document, competitive + ".2.2", "New Products", 2,
                formatTocPage("toc." + competitive + ".2.2"));
        addTocEntry(document, competitive + ".2.3", "Research & Development Activities", 2,
                formatTocPage("toc." + competitive + ".2.3"));

        int companiesChapter = chapterLayout.companyProfilesChapter();
        addTocChapter(document, companiesChapter, "Company Profiles",
                formatTocPage(chapterLayout.destinationForChapter(companiesChapter)));
        if (report.getCompanies() != null) {
            int idx = 1;
            for (Company company : report.getCompanies()) {
                if (company == null || company.getCompanyName() == null || company.getCompanyName().isBlank()) {
                    continue;
                }
                addTocEntry(document, companiesChapter + "." + idx, company.getCompanyName(), 1,
                        formatTocPage("toc." + companiesChapter + "." + idx));
                // Sample report: only the first company expands with detailed subsections in the TOC.
                if (idx == 1) {
                    String prefix = companiesChapter + ".1";
                    addTocEntry(document, prefix + ".1", "Company Overview", 2,
                            formatTocPage("toc." + prefix + ".1"));
                    addTocEntry(document, prefix + ".2", "Product/Service Portfolio", 2,
                            formatTocPage("toc." + prefix + ".2"));
                    addTocEntry(document, prefix + ".3",
                            company.getCompanyName() + " Revenue, Market Share, YoY Growth Rate", 2,
                            formatTocPage("toc." + prefix + ".3"));
                    addTocEntry(document, prefix + ".4",
                            company.getCompanyName() + " Revenue and Growth Rate", 2,
                            formatTocPage("toc." + prefix + ".4"));
                    addTocEntry(document, prefix + ".5",
                            company.getCompanyName() + " Market Share", 2,
                            formatTocPage("toc." + prefix + ".5"));
                    addTocEntry(document, prefix + ".6",
                            "Recent Initiatives, Funding/VC Activities and Technological Innovations", 2,
                            formatTocPage("toc." + prefix + ".6"));
                }
                idx++;
            }
            addTocEntry(document, companiesChapter + "." + idx, "Others", 1,
                    formatTocPage("toc." + companiesChapter + "." + idx));
        }

        int industry = chapterLayout.industryAnalysisChapter();
        addTocChapter(document, industry, market + " - Industry Analysis",
                formatTocPage(chapterLayout.destinationForChapter(industry)));
        addTocEntry(document, industry + ".1", "Introduction and Taxonomy", 1, formatTocPage("toc." + industry + ".1"));
        addTocEntry(document, industry + ".2", market + " - Key Trends", 1, formatTocPage("toc." + industry + ".2"));
        addTocEntry(document, industry + ".2.1", "Market Drivers", 2, formatTocPage("toc." + industry + ".2.1"));
        addTocEntry(document, industry + ".2.2", "Market Restraints", 2, formatTocPage("toc." + industry + ".2.2"));
        addTocEntry(document, industry + ".2.3", "Market Opportunities", 2, formatTocPage("toc." + industry + ".2.3"));
        addTocEntry(document, industry + ".3", "Value Chain Analysis", 1, formatTocPage("toc." + industry + ".3"));
        addTocEntry(document, industry + ".4", "Key Mandates and Regulations", 1, formatTocPage("toc." + industry + ".4"));
        addTocEntry(document, industry + ".5", "Technology Roadmap and Timeline", 1, formatTocPage("toc." + industry + ".5"));
        addTocEntry(document, industry + ".6", "SWOT Analysis", 1, formatTocPage("toc." + industry + ".6"));
        addTocEntry(document, industry + ".7", market + " - Attractiveness Analysis", 1, formatTocPage("toc." + industry + ".7"));
        if (!chapterLayout.getSegmentChapterTitles().isEmpty()) {
            String firstSegmentTitle = chapterLayout.getSegmentChapterTitles().get(0);
            String dimName = firstSegmentTitle.contains(" - ")
                    ? firstSegmentTitle.substring(firstSegmentTitle.lastIndexOf(" - ") + 3)
                    : firstSegmentTitle;
            addTocEntry(document, industry + ".7.1", dimName, 2, formatTocPage("toc." + industry + ".7.1"));
        }
        addTocEntry(document, industry + ".7.2", "By Region", 2, formatTocPage("toc." + industry + ".7.2"));

        int strategy = chapterLayout.marketStrategyChapter();
        addTocChapter(document, strategy, "Marketing Strategy Analysis, Distributors",
                formatTocPage(chapterLayout.destinationForChapter(strategy)));
        addTocEntry(document, strategy + ".1", "Marketing Channel", 1, formatTocPage("toc." + strategy + ".1"));
        addTocEntry(document, strategy + ".2", "Direct Marketing", 1, formatTocPage("toc." + strategy + ".2"));
        addTocEntry(document, strategy + ".3", "Indirect Marketing", 1, formatTocPage("toc." + strategy + ".3"));
        addTocEntry(document, strategy + ".4", "Marketing Channel Development Trends", 1, formatTocPage("toc." + strategy + ".4"));

        int conclusions = chapterLayout.conclusionsChapter();
        addTocChapter(document, conclusions, "Report Conclusion & Key Insights",
                formatTocPage(chapterLayout.destinationForChapter(conclusions)));
        addTocEntry(document, conclusions + ".1", "Key Insights from Primary Interviews & Survey's Respondents", 1,
                formatTocPage("toc." + conclusions + ".1"));
        addTocEntry(document, conclusions + ".2", "Key Takeaways from Analysts, Consultants, and Industry Leaders", 1,
                formatTocPage("toc." + conclusions + ".2"));

        int methodology = chapterLayout.methodologyChapter();
        addTocChapter(document, methodology, "Research Approach & Methodology",
                formatTocPage(chapterLayout.destinationForChapter(methodology)));
        addTocEntry(document, methodology + ".1", "Report Description", 1, formatTocPage("toc." + methodology + ".1"));
        addTocEntry(document, methodology + ".2", "Research Scope", 1, formatTocPage("toc." + methodology + ".2"));
        addTocEntry(document, methodology + ".3", "Research Methodology", 1, formatTocPage("toc." + methodology + ".3"));
        addTocEntry(document, methodology + ".3.1", "Secondary Research", 2, formatTocPage("toc." + methodology + ".3.1"));
        addTocEntry(document, methodology + ".3.2", "Primary Research", 2, formatTocPage("toc." + methodology + ".3.2"));
        addTocEntry(document, methodology + ".3.3", "Statistical Models", 2, formatTocPage("toc." + methodology + ".3.3"));
        addTocEntry(document, methodology + ".3.3.1", "Company Share Analysis Model", 3, formatTocPage("toc." + methodology + ".3.3.1"));
        addTocEntry(document, methodology + ".3.3.2", "Revenue Based Modelling", 3, formatTocPage("toc." + methodology + ".3.3.2"));
        addTocEntry(document, methodology + ".3.4", "Research Limitations", 2, formatTocPage("toc." + methodology + ".3.4"));
    }

    private String formatTocPage(String destinationKey) {
        if (tocIndexingPass) {
            return " ";
        }
        Integer page = tocPageByDestination.get(destinationKey);
        if (page == null) {
            page = resolveChapterFallbackPage(destinationKey);
        }
        return page != null ? String.valueOf(page) : " ";
    }

    private Integer resolveChapterFallbackPage(String destinationKey) {
        if (destinationKey.startsWith("toc.ch")) {
            return tocPageByDestination.get(destinationKey);
        }
        if (!destinationKey.startsWith("toc.")) {
            return null;
        }
        String sectionNumber = destinationKey.substring("toc.".length());
        int chapterEnd = sectionNumber.indexOf('.');
        String chapterPart = chapterEnd >= 0 ? sectionNumber.substring(0, chapterEnd) : sectionNumber;
        try {
            int chapter = Integer.parseInt(chapterPart);
            for (int ch = chapter; ch >= 1; ch--) {
                Integer page = tocPageByDestination.get("toc.ch" + ch);
                if (page != null) {
                    return page;
                }
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private void recordTocDestinationPage(Document document, String destinationKey) {
        if (!tocIndexingPass || destinationKey == null || destinationKey.isBlank()) {
            return;
        }
        document.flush();
        tocPageByDestination.put(destinationKey, document.getPdfDocument().getNumberOfPages());
    }

    private TocSectionRecorder createTocSectionRecorder() {
        return new TocSectionRecorder() {
            @Override
            public void recordChapter(Document document, int chapter, Paragraph heading) {
                String dest = "toc.ch" + chapter;
                heading.setDestination(dest);
                document.add(heading);
                recordTocDestinationPage(document, dest);
            }

            @Override
            public void recordSection(Document document, String destinationKey, Paragraph heading) {
                BodyFigureLayout.breakBeforeNumberedHeading(document, heading);
                heading.setDestination(destinationKey);
                document.add(heading);
                recordTocDestinationPage(document, destinationKey);
            }
        };
    }

    private void addTocChapter(Document document, int chapter, String title, String pageNumber)
            throws IOException {
        DeviceRgb navy = new DeviceRgb(27, 58, 92);
        float tabPosition = 718f;

        Paragraph line = new Paragraph()
                .setMarginTop(10)
                .setMarginBottom(2);
        line.addTabStops(new TabStop(tabPosition, TabAlignment.RIGHT, new DottedLine()));
        line.add(new Text("CHAPTER " + chapter)
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setFontColor(navy));
        line.add(new Text("     " + title)
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setFontColor(navy));
        line.add(new Tab());
        line.add(new Text(pageNumber)
                .setFont(themeRenderer.bold())
                .setFontSize(10)
                .setFontColor(navy));
        document.add(line);
    }

    private void addTocEntry(
            Document document,
            String number,
            String title,
            int level,
            String pageNumber) throws IOException {
        DeviceRgb navy = new DeviceRgb(27, 58, 92);
        float tabPosition = 718f;
        float indent = 22f + (Math.max(0, level - 1) * 18f);
        boolean bold = level <= 1;

        PdfFont font = bold ? themeRenderer.semiBold() : themeRenderer.regular();
        float fontSize = level <= 1 ? 9.5f : 9f;

        Paragraph line = new Paragraph()
                .setMarginTop(1)
                .setMarginBottom(1)
                .setPaddingLeft(indent);
        line.addTabStops(new TabStop(tabPosition, TabAlignment.RIGHT, new DottedLine()));
        line.add(new Text(number + "  " + title)
                .setFont(font)
                .setFontSize(fontSize)
                .setFontColor(bold ? navy : ColorConstants.BLACK));
        line.add(new Tab());
        line.add(new Text(pageNumber).setFont(font).setFontSize(fontSize));
        document.add(line);
    }

    private void addExecutiveSummary(Document document, SampleReport report, List<MarketSegment> roots)
            throws IOException {

        addChapterHeadings(document, report, "CHAPTER 1  Executive Summary");

        String market = report.getKeyName();
        String unit = report.getUnit() != null && !report.getUnit().isBlank() ? report.getUnit() : ""+report.getUnit()+"";
        double baseValue = report.getMarketValueBaseYear() != null ? report.getMarketValueBaseYear() : 0;
        double forecastValue = report.getMarketValueForecastYear() != null ? report.getMarketValueForecastYear() : 0;
        double[] globalSeries = valueSeriesProvider.yearlyValuesUsdMillion(report, market);
        if (baseValue <= 0) {
            baseValue = valueAtYear(globalSeries, report, report.getBaseYear());
        }
        if (forecastValue <= 0) {
            forecastValue = valueAtYear(globalSeries, report, report.getForecastYear());
        }
        double cagr = valueSeriesProvider.cagrPercent(globalSeries, report);

        document.add(new Paragraph(
                "The global " + market + " is projected to reach "
                        + valueSeriesProvider.formatValue(forecastValue) + " Value (" + unit + ") by "
                        + report.getForecastYear() + ", from "
                        + valueSeriesProvider.formatValue(baseValue) + " Value (" + unit + ") in "
                        + report.getBaseYear() + " and is anticipated to register a CAGR of "
                        + valueSeriesProvider.formatPercent(cagr) + " between "
                        + report.getBaseYear() + " and " + report.getForecastYear())
                .setFont(themeRenderer.regular())
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                .setFontSize(10));

        Paragraph section11 = new Paragraph("1.1 Introduction of " + market)
                .setFont(themeRenderer.bold())
                .setFontSize(11);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section11);
        section11.setDestination("toc.1.1");
        document.add(section11);
        recordTocDestinationPage(document, "toc.1.1");

        Paragraph section111 = new Paragraph("1.1.1 Global " + market + ", "
                + report.getBaseYear() + " & " + report.getForecastYear() + " Value ("+report.getUnit()+")")
                .setFont(themeRenderer.bold())
                .setFontSize(11);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section111);
        section111.setDestination("toc.1.1.1");
        document.add(section111);
        recordTocDestinationPage(document, "toc.1.1.1");

        addMarketOverviewTable(document, report, roots);

        document.add(new AreaBreak());

        Paragraph section12 = new Paragraph("1.2 COVID-19 Impacts on " + market + " Industry")
                .setFont(themeRenderer.bold())
                .setFontSize(11);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section12);
        section12.setDestination("toc.1.2");
        document.add(section12);
        recordTocDestinationPage(document, "toc.1.2");

        document.add(new Paragraph(
                "This updated and latest version of the report would be considering the impact of COVID-19 on "
                        + market + " across the globe as well as on different regions and individual countries. "
                        + "The effects of COVID-19 pandemic will be analyzed on the overall industry including both the "
                        + "demand side and supply side perspectives. The effects of the pandemic would be studied and analyzed "
                        + "for short-term, mid-term, and long-term scenarios. This would assist to formulate business strategies "
                        + "for the period during pandemic as well as post-pandemic period for all stakeholders involved in "
                        + market + " industry including suppliers, manufacturers, vendors, distributors, and end-users.")
                .setFont(themeRenderer.regular())
                .setFontSize(10)
                .setKeepWithNext(true));

        addNumberedSection(document, "1.2.1", "COVID-19 Short-Term Impact & Business Strategies", null);
        addNumberedSection(document, "1.2.2", "COVID-19 Mid-Term Impact & Business Strategies", null);
        addNumberedSection(document, "1.2.3", "COVID-19 Long-Term Impact & Business Strategies", null);

        // Special figure page: caption + chart + source (keep unit together; shrink if needed).
        document.add(new AreaBreak());
        document.add(new AreaBreak());
        addCovidImages(document, report);
        document.add(new AreaBreak());

        Paragraph section13 = new Paragraph("1.3 Global " + market + ", "
                + historicYear(report) + "-" + report.getForecastYear() + " Value ("+report.getUnit()+")")
                .setFont(themeRenderer.bold())
                .setFontSize(11);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section13);
        section13.setDestination("toc.1.3");
        document.add(section13);
        recordTocDestinationPage(document, "toc.1.3");

        document.add(BodyFigureLayout.figureCaption(
                themeRenderer,
                2,
                "Global " + market + ", "
                        + historicYear(report) + "-" + report.getForecastYear() + " Value ("+report.getUnit()+")"));
        BodyFigureLayout.addClasspathFigure(document, "assets/images/fig2.jpg", 80f);
        sourceParagraph(document, report, reportPlaceholderSource());
    }

    private void addMarketOverviewTable(Document document, SampleReport report, List<MarketSegment> roots)
            throws IOException {

        String market = report.getKeyName();
        int baseYear = report.getBaseYear();
        int forecastYear = report.getForecastYear();

        document.add(new Paragraph("TABLE 1  Global " + market + ", "
                + baseYear + " & " + forecastYear + " Value ("+report.getUnit()+")")
                .setFont(themeRenderer.semiBold())
                .setFontSize(11));

        Table table = BodyTableStyling.newBodyTable(new float[]{50, 25, 25});

        double[] globalSeries = valueSeriesProvider.yearlyValuesUsdMillion(report, market);
        double baseValue = report.getMarketValueBaseYear() != null
                ? report.getMarketValueBaseYear()
                : valueAtYear(globalSeries, report, baseYear);
        double forecastValue = report.getMarketValueForecastYear() != null
                ? report.getMarketValueForecastYear()
                : valueAtYear(globalSeries, report, forecastYear);
        String cagr = valueSeriesProvider.formatPercent(valueSeriesProvider.cagrPercent(globalSeries, report));

        addHeaderRow3Col(table, "Parameter", String.valueOf(baseYear), String.valueOf(forecastYear));
        addRow3Col(table, "Global " + market + " Value ("+report.getUnit()+")",
                valueSeriesProvider.formatValue(baseValue),
                valueSeriesProvider.formatValue(forecastValue));
        addRow3Col(table, "CAGR % (" + baseYear + "-" + forecastYear + ")", cagr, cagr);

        List<MarketSegment> dimensions = RegionalOutlineBuilder.segmentDimensions(roots);
        int dimCount = 0;
        for (MarketSegment dimension : dimensions) {
            if (dimension == null || dimension.getSegmentName() == null) {
                continue;
            }
            String largest = largestChildLabel(report, market, dimension);
            addRow3Col(table,
                    "Largest Market Segment, by " + dimension.getSegmentName() + " ("+report.getUnit()+")",
                    largest, largest);
            dimCount++;
            if (dimCount >= 2) {
                break;
            }
        }

        List<MarketSegment> regions = RegionalOutlineBuilder.resolveRegions(roots);
        String fastestRegion = "—";
        double bestCagr = Double.NEGATIVE_INFINITY;
        List<String> regionNames = new ArrayList<>();
        if (regions.isEmpty()) {
            regionNames.addAll(com.sample_generator.sample.pdf.regional.RegionalGeoCatalog.defaultRegionOrder());
        } else {
            for (MarketSegment region : regions) {
                if (region != null && region.getSegmentName() != null) {
                    regionNames.add(region.getSegmentName());
                }
            }
        }
        for (String regionName : regionNames) {
            double[] series = valueSeriesProvider.yearlyValuesUsdMillion(report, market, "Region", regionName);
            double regionCagr = valueSeriesProvider.cagrPercent(series, report);
            if (regionCagr > bestCagr) {
                bestCagr = regionCagr;
                fastestRegion = regionName + ": " + valueSeriesProvider.formatPercent(regionCagr);
            }
        }
        addRow3Col(table, "Key Growth Market Segment, by region CAGR % (" + baseYear + "-" + forecastYear + ")",
                fastestRegion, "");

        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer, reportPlaceholderSource()));
    }

    private String largestChildLabel(SampleReport report, String market, MarketSegment dimension) {
        List<MarketSegment> children = dimension.getChildren();
        if (children == null || children.isEmpty()) {
            return dimension.getSegmentName();
        }
        String best = children.get(0).getSegmentName();
        double bestValue = Double.NEGATIVE_INFINITY;
        for (MarketSegment child : children) {
            if (child == null || child.getSegmentName() == null) {
                continue;
            }
            double[] series = valueSeriesProvider.yearlyValuesUsdMillion(
                    report, market, dimension.getSegmentName(), child.getSegmentName());
            double value = valueAtYear(series, report, report.getBaseYear());
            if (value > bestValue) {
                bestValue = value;
                best = child.getSegmentName() + ": " + valueSeriesProvider.formatValue(value);
            }
        }
        return best;
    }
    private void addCovidSection(Document document, SampleReport report) {
        Paragraph section13 = new Paragraph("1.2 COVID-19 Impacts on Japan Wine Market Industry");
        section13.setDestination("toc.1.3");
        document.add(section13);
        recordTocDestinationPage(document, "toc.1.3");
        document.add(new Paragraph("This updated and latest version of the report would be considering the impact of COVID-19 on Japan Wine Market Market across the globe as well as\n" +
                "on different regions and individual countries. The effects of COVID-19 pandemic will be analyzed on the overall industry including both the demand\n" +
                "side and supply side perspectives. The effects of the pandemic would be studied and analyzed for short-term, mid-term, and long-term scenarios.\n" +
                "This would assist to formulate business strategies for the period during pandemic as well as post-pandemic period for all stakeholders involved in\n" +
                "Japan Wine Market industry including suppliers, manufacturers, vendors, distributors, and end-users.").setFontSize(14));

        //document.add( new Paragraph("1.2.1 COVID-19 Short-Term Impact & Business Strategies").setFontSize(11));
        document.add( new Paragraph("1.2.2 COVID-19 Mid-Term Impact & Business Strategies").setFontSize(11));
        //document.add( new Paragraph("1.2.3 COVID-19 Long-Term Impact & Business Strategies").setFontSize(11));

    }

    private void addCovidImages(Document document, SampleReport report) throws IOException {
        // Golden special figure page: top caption, horizontally centered ~500×300 fit, source below.

        document.add(BodyFigureLayout.figureCaption(themeRenderer, 1, "COVID-19 Impact Analysis"));

        ClassPathResource resource = new ClassPathResource("assets/images/CIA.png");
        ImageData imageData = ImageDataFactory.create(resource.getInputStream().readAllBytes());

// 1. Create the Image object
        Image image = new Image(imageData);

// 2. Set custom height and top margin
        image.setMarginTop(20f);      // Adds top margin (adjust points as needed)
        image.setMaxHeight(300f);      // Restricts maximum height (or use setHeight(80f))
        image.setAutoScaleHeight(false); // Ensures hard height limits are respected if scaled

// 3. Apply your layout scaling utility and add to document
        document.add(BodyFigureLayout.scaleForBody(image));

        sourceParagraph(document, report, reportPlaceholderSource());
    }
    private void addCovidParagraph2(Document document, SampleReport report) throws IOException {

        document.add(new Paragraph("1.3 " + report.getKeyName()+"," + report.getHistoricYear()+ " - " +report.getForecastYear() + " Value (" +report.getUnit()+ ")"));
        document.add(new Paragraph("FIGURE 2 " + report.getKeyName()+"," + report.getHistoricYear()+ " - " +report.getForecastYear() + " Value (" +report.getUnit()+ ")"));
        addImageToPages(document, report, "assets/images/fig2.jpg");
        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");


    }

    private void addExecutiveMarketDynamics(Document document, SampleReport report) throws IOException {
        String market = report.getKeyName();
        Paragraph section14 = new Paragraph("1.4 " + market + " Dynamics")
                .setFontSize(18);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section14);
        section14.setDestination("toc.1.4");
        document.add(section14);
        recordTocDestinationPage(document, "toc.1.4");

        document.add(new Paragraph(
                "The market dynamics section summarizes the key drivers, restraints, opportunities, and challenges shaping the "
                        + market + " during the forecast period from " + report.getBaseYear()
                        + " to " + report.getForecastYear() + ".")
                .setFontSize(14));

        addExecutiveDynamicsSubsection(document, "1.4.1 Market Drivers", new String[]{
                "Rising adoption of AI workloads and high-performance computing across enterprises.",
                "Expansion of cloud and colocation infrastructure to support digital transformation.",
                "Government incentives for domestic data center and semiconductor investments.",
                "Growing demand for low-latency edge and regional data center capacity."
        });
        addExecutiveDynamicsSubsection(document, "1.4.2 Market Restraints", new String[]{
                "High upfront capital expenditure and long deployment cycles for new facilities.",
                "Power availability constraints and rising electricity costs in key markets.",
                "Supply chain bottlenecks for GPUs, networking, and cooling equipment.",
                "Extended permitting and land acquisition timelines in urban regions."
        });
        addExecutiveDynamicsSubsection(document, "1.4.3 Market Opportunities", new String[]{
                "Liquid cooling, immersion cooling, and energy-efficient facility designs.",
                "Sovereign and industry-specific AI cloud offerings for regulated sectors.",
                "Partnerships between hyperscalers, utilities, and renewable energy providers.",
                "Retrofit and modernization of legacy data center assets for AI readiness."
        });
        addExecutiveDynamicsSubsection(document, "1.4.4 Market Challenges", new String[]{
                "Talent shortages in data center operations, security, and AI infrastructure.",
                "Evolving data residency, privacy, and environmental compliance requirements.",
                "Integration complexity across hybrid multi-cloud and on-premise estates.",
                "Balancing utilization, sustainability targets, and total cost of ownership."
        });

        document.add(new Paragraph("FIGURE 12 MARKET DYNAMICS").setFontSize(16));
        addImageToPages(document, report, "assets/images/fig4.png");
        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");

        addExecutiveDynamicsImpactTable(document, report, "TABLE 13  MARKET DRIVERS - IMPACT ANALYSIS",
                "Driver", "High", "Medium", "Low");
        addExecutiveDynamicsImpactTable(document, report, "TABLE 14  MARKET RESTRAINTS",
                "Restraint", "High", "Medium", "Low");
        addExecutiveDynamicsImpactTable(document, report, "TABLE 15  MARKET OPPORTUNITIES",
                "Opportunity", "High", "Medium", "Low");
        addExecutiveDynamicsImpactTable(document, report, "TABLE 16  MARKET CHALLENGES",
                "Challenge", "High", "Medium", "Low");
    }

    private void addExecutiveDynamicsSubsection(Document document, String title, String[] bullets)
            throws IOException {
        Paragraph heading = new Paragraph(title).setFontSize(16);
        BodyFigureLayout.breakBeforeNumberedHeading(document, heading);
        document.add(heading);
        for (String bullet : bullets) {
            document.add(new Paragraph("♦  " + bullet)
                    .setFontSize(14)
                    .setMarginLeft(12));
        }
    }

    private void addExecutiveDynamicsImpactTable(
            Document document,
            SampleReport report,
            String tableTitle,
            String factorLabel,
            String highLabel,
            String mediumLabel,
            String lowLabel) throws IOException {

        document.add(new Paragraph(tableTitle).setFontSize(14));

        Table table = BodyTableStyling.newBodyTable(new float[]{30, 20, 50});
        addHeaderRow3Col(table, factorLabel, "Impact", "Description");
        addRow3Col(table, marketDynamicsPlaceholderFactor(factorLabel, 1), highLabel,
                "Placeholder short-term impact for " + factorLabel.toLowerCase(Locale.ROOT) + " 1.");
        addRow3Col(table, marketDynamicsPlaceholderFactor(factorLabel, 2), mediumLabel,
                "Placeholder mid-term impact for " + factorLabel.toLowerCase(Locale.ROOT) + " 2.");
        addRow3Col(table, marketDynamicsPlaceholderFactor(factorLabel, 3), lowLabel,
                "Placeholder long-term impact for " + factorLabel.toLowerCase(Locale.ROOT) + " 3.");
        BodyTableStyling.addTableWithSource(
                document,
                table,
                BodyTableStyling.sourceLine(themeRenderer,
                        "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026."));
    }

    private static String marketDynamicsPlaceholderFactor(String factorLabel, int index) {
        String singular = factorLabel;
        if (singular.endsWith("s") && singular.length() > 1) {
            singular = singular.substring(0, singular.length() - 1);
        }
        return singular + " " + index;
    }

    private void addExecutivePortersFiveForces(Document document, SampleReport report) throws IOException {
        Paragraph section15 = new Paragraph("1.5 Porter's Five Forces Analysis")
                .setFontSize(18);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section15);
        section15.setDestination("toc.1.5");
        document.add(section15);
        recordTocDestinationPage(document, "toc.1.5");

        document.add(new Paragraph(
                "Porter's Five Forces framework is used to evaluate the competitive intensity and attractiveness of the "
                        + report.getKeyName() + ".")
                .setFontSize(14));

        Table forcesTable = BodyTableStyling.newBodyTable(new float[]{45, 55});
        addRow(forcesTable, "Threat of New Entrants", "Moderate – capital intensity and technology barriers limit rapid entry.");
        addRow(forcesTable, "Bargaining Power of Suppliers", "Moderate – specialized inputs with limited substitute suppliers.");
        addRow(forcesTable, "Bargaining Power of Buyers", "High – large enterprise buyers negotiate on price and service levels.");
        addRow(forcesTable, "Threat of Substitute Products", "Low to Moderate – alternative solutions exist but with performance trade-offs.");
        addRow(forcesTable, "Competitive Rivalry", "High – established players compete on innovation, scale, and partnerships.");
        document.add(forcesTable);

        document.add(new Paragraph(
                "Overall, competitive rivalry and buyer power are the most influential forces, while supplier power and "
                        + "the threat of new entrants remain moderate given capital and technology requirements.")
                .setFontSize(14));

        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");
    }

    private void addExecutivePestleAnalysis(Document document, SampleReport report) throws IOException {
        Paragraph section16 = new Paragraph("1.6 PESTLE Analysis")
                .setFontSize(18);
        BodyFigureLayout.breakBeforeNumberedHeading(document, section16);
        section16.setDestination("toc.1.6");
        document.add(section16);
        recordTocDestinationPage(document, "toc.1.6");

        document.add(new Paragraph(
                "PESTLE analysis highlights macro-environmental factors influencing the " + report.getKeyName()
                        + " across political, economic, social, technological, legal, and environmental dimensions.")
                .setFontSize(14));

        Table pestleTable = BodyTableStyling.newBodyTable(new float[]{22, 78});
        addRow(pestleTable, "Political", "Government policies, trade regulations, and data sovereignty requirements.");
        addRow(pestleTable, "Economic", "Capital expenditure cycles, inflation, and enterprise IT spending trends.");
        addRow(pestleTable, "Social", "Digital adoption, workforce skills, and sustainability expectations.");
        addRow(pestleTable, "Technological", "AI workloads, edge computing, cooling innovation, and chip advancements.");
        addRow(pestleTable, "Legal", "Compliance, privacy laws, and industry-specific certifications.");
        addRow(pestleTable, "Environmental", "Energy efficiency mandates and carbon reduction targets for data center operations.");
        document.add(pestleTable);

        document.add(new Paragraph(report.getKeyName() + " – PESTLE Framework").setFontSize(16));
        addImageToPages(document, report, "assets/images/img_10.png");
        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");

        document.add(new Paragraph("FIGURE 14 KEY MANDATES AND REGULATIONS").setFontSize(16));
        addImageToPages(document, report, "assets/images/img_5.png");
        sourceParagraph(document, report,
                "Source: Primary Interviews, Surveys, Secondary Sources, In-house & Paid External Databases, Spherical Insights, 2026.");
    }


    ///
    ///
    ///
    /// these are down below all are helper methods

    private void addRow(Table table, String label, String value) throws IOException {
        table.addCell(BodyTableStyling.labelCell(themeRenderer, label));
        table.addCell(BodyTableStyling.bodyCell(
                themeRenderer, value != null ? value : "-", false, TextAlignment.LEFT));
    }



    private void addHeaderRow3Col(Table table, String col1, String col2, String col3) throws IOException {
        table.addHeaderCell(segmentHeaderCell(col1, TextAlignment.LEFT));
        table.addHeaderCell(segmentHeaderCell(col2, TextAlignment.CENTER));
        table.addHeaderCell(segmentHeaderCell(col3, TextAlignment.CENTER));
    }

    private void addRow3Col(Table table, String col1, String col2, String col3) throws IOException {
        addRow3Col(table, col1, col2, col3, false, true);
    }

    private void addRow3Col(
            Table table,
            String col1,
            String col2,
            String col3,
            boolean headerRow,
            boolean firstColSemiBold) throws IOException {
        if (headerRow) {
            addHeaderRow3Col(table, col1, col2, col3);
            return;
        }
        table.addCell(BodyTableStyling.bodyCell(themeRenderer, col1, firstColSemiBold, TextAlignment.LEFT));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, col2));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, col3));
    }

    private void addRowForSegmentTable(Table table, String title, String value_19,
                                       String value_20, String value_21,
                                       String value_22, String value_23,
                                       String value_24, String value_25,
                                       String value_26, String value_27,
                                       String value_28, String value_29,
                                       String value_30, String value_31,
                                       String value_32, String value_33,
                                       String value_34, String value_35,
                                       String cagr) throws IOException {
        table.addCell(BodyTableStyling.labelCell(themeRenderer, title));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_19));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_20));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_21));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_22));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_23));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_24));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_25));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_26));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_27));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_28));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_29));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_30));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_31));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_32));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_33));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_34));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, value_35));
        table.addCell(BodyTableStyling.valueCell(themeRenderer, cagr));
    }

    private void addImageToPages(Document document, SampleReport report, String ImgPath) throws IOException {
        BodyFigureLayout.addClasspathFigure(document, ImgPath);
    }

    private void addHeaderAndFooter(Document document,
                                    SampleReport report,
                                    int pageNumber) throws IOException {

        PdfDocument pdfDocument = document.getPdfDocument();
        if (pdfDocument == headerFooterRegisteredDocument) {
            return;
        }
        headerFooterRegisteredDocument = pdfDocument;

        int startYear = report.getHistoricYear() != null
                ? report.getHistoricYear()
                : report.getBaseYear();
        String headerTitle = report.getKeyName().toUpperCase(Locale.ROOT)
                + ", " + startYear + " - " + report.getForecastYear();

        String category = report.getCategory();

        ImageData logoData = ImageDataFactory.create(
                new ClassPathResource("assets/images/spherical.png").getInputStream().readAllBytes());
        ImageData yellowLineData = ImageDataFactory.create(
                new ClassPathResource("assets/images/yellow_line.png").getInputStream().readAllBytes());

//        logoData.setHeight(500);
//        logoData.setWidth(1000);

        pdfDocument.addEventHandler(
                PdfDocumentEvent.END_PAGE,
                new HeaderFooterPageEventHandler(
                        themeRenderer.regular(),
                        themeRenderer.semiBold(),
                        headerTitle,
                        category,
                        logoData,
                        yellowLineData,
                        pagesWithoutHeaderFooter));
    }

    private static final class HeaderFooterPageEventHandler implements IEventHandler {

        private static final DeviceRgb HEADER_TEXT_COLOR = new DeviceRgb(110, 110, 110);
        private static final DeviceRgb BACK_TO_TOP_COLOR = new DeviceRgb(0, 112, 192);
        private static final DeviceRgb FOOTER_BAR_COLOR = new DeviceRgb(255, 184, 28);
        private static final DeviceRgb SI_WEBSITE_COLOR = new DeviceRgb(3, 14, 79);

        private static final float HORIZONTAL_MARGIN = 70f;
        private static final String FOOTER_TEXT =
                "Copyright \u00a9 Spherical Insights | sales@sphericalinsights.com | www.sphericalinsights.com";

        private final PdfFont regularFont;
        private final PdfFont semiBoldFont;
        private final String headerTitle;
        private final String category;
        private final ImageData logoData;
        private final ImageData yellowLineData;
        private final Set<Integer> pagesWithoutHeaderFooter;

        private HeaderFooterPageEventHandler(
                PdfFont regularFont,
                PdfFont semiBoldFont,
                String headerTitle,
                String category,
                ImageData logoData,
                ImageData yellowLineData,
                Set<Integer> pagesWithoutHeaderFooter) {
            this.regularFont = regularFont;
            this.semiBoldFont = semiBoldFont;
            this.headerTitle = headerTitle;
            this.category = category;
            this.logoData = logoData;
            this.yellowLineData = yellowLineData;
            this.pagesWithoutHeaderFooter = pagesWithoutHeaderFooter;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNum = pdfDoc.getPageNumber(page);
            if (pageNum <= 1 || pagesWithoutHeaderFooter.contains(pageNum)) {
                return;
            }

            Rectangle pageSize = page.getPageSizeWithRotation();
            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.newContentStreamAfter(),
                    page.getResources(),
                    pdfDoc);

            try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
                float top = pageSize.getTop();
                float right = pageSize.getRight();
                float width = pageSize.getWidth();

                Paragraph headerText = new Paragraph(headerTitle)
                        .setFont(semiBoldFont)
                        .setFontSize(9f)
                        .setFontColor(SI_WEBSITE_COLOR)
                        .setMargin(0);
                canvas.showTextAligned(
                        headerText,
                        pageSize.getLeft() + HORIZONTAL_MARGIN,
                        top - 40f,
                        TextAlignment.LEFT);

                Paragraph categoryText = new Paragraph( "Category: "+category)
                        .setFont(semiBoldFont)
                        .setFontSize(9f)
                        .setFontColor(BACK_TO_TOP_COLOR)
                        .setMargin(0);
                canvas.showTextAligned(
                        categoryText,
                        pageSize.getLeft() + HORIZONTAL_MARGIN,
                        top - 55f,
                        TextAlignment.LEFT);

                pdfCanvas.saveState()
                        .setFillColor(FOOTER_BAR_COLOR)
                        .rectangle(0, 530, 600, 4)
                        .fill()
                        .restoreState();

                float logoWidth = 240f;
                float logoHeight = 170f;
                Image logo = new Image(logoData);
                logo.scaleToFit(logoWidth, logoHeight);
                logo.setFixedPosition(
                        pageNum,
                        right - HORIZONTAL_MARGIN - logoWidth + 120f,
                        top - 73f,
                        logoWidth);
                canvas.add(logo);

                float dividerWidth = width - (HORIZONTAL_MARGIN * 2f);
                Image divider = new Image(yellowLineData);
                divider.scaleToFit(dividerWidth, 4f);
                divider.setFixedPosition(
                        pageNum,
                        pageSize.getLeft() + HORIZONTAL_MARGIN,
                        top - 44f,
                        dividerWidth);
                canvas.add(divider);

                Paragraph backToTop = new Paragraph("Back to Top")
                        .setFont(regularFont)
                        .setBold()
                        .setFontSize(9f)
                        .setFontColor(BACK_TO_TOP_COLOR)
                        .setMargin(0)
                        .setAction(PdfAction.createGoTo(
                                PdfExplicitDestination.createFit(pdfDoc.getPage(1))));
                canvas.showTextAligned(
                        backToTop,
                        pageSize.getLeft() + HORIZONTAL_MARGIN,
                        44f,
                        TextAlignment.LEFT);

                float barLeft = pageSize.getLeft() + 98f;
                float barLeft2 = 125f;
                float barBottom = 40f;
                float barHeight = 16f;
                float barWidth = right - HORIZONTAL_MARGIN - barLeft - 80f;
                float barWidth2 = 550f;

                pdfCanvas.saveState()
                        .setFillColor(FOOTER_BAR_COLOR)
                        .rectangle(barLeft2, barBottom, barWidth2, barHeight)
                        .fill()
                        .restoreState();

                float leftPadding = 25f; // Increase or decrease this value to control spacing from the left

                Paragraph footerText = new Paragraph(FOOTER_TEXT)
                        .setFont(regularFont)
                        .setFontSize(8.5f)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK);

                canvas.showTextAligned(
                        footerText,
                        barLeft + 6f + leftPadding, // <-- Added padding here
                        barBottom + 4f,
                        TextAlignment.LEFT);

                Paragraph pageNumberText = new Paragraph(String.valueOf(pageNum))
                        .setFont(regularFont)
                        .setFontSize(9f)
                        .setFontColor(SI_WEBSITE_COLOR)
                        .setMargin(0);
                canvas.showTextAligned(
                        pageNumberText,
                        right - HORIZONTAL_MARGIN,
                        barBottom + 3.9f,
                        TextAlignment.RIGHT);
            }
        }
    }
    private void addChapterHeadings(Document document, SampleReport report, String heading_c) throws IOException {

        Paragraph heading = new Paragraph(heading_c)
                .setFont(themeRenderer.bold())
                .setFontSize(16)
                .setUnderline()
                .setTextAlignment(TextAlignment.LEFT);

        if (pendingTocDestination != null) {
            heading.setDestination(pendingTocDestination);
        }

        document.add(heading);
        recordTocDestinationPage(document, pendingTocDestination);
        pendingTocDestination = null;

    }
    private void addSegmentDiamondBullet(Document document, String text) throws IOException {
        ImageData bulletData = ImageDataFactory.create(
                new ClassPathResource(LIST_SYMBOL_IMAGE).getInputStream().readAllBytes());
        Image bullet = new Image(bulletData);
        // Golden segment intro bullets are ~12×8pt at the body left margin.
        bullet.scaleAbsolute(12f, 8f);
        bullet.setMarginRight(0f);

        document.add(new Paragraph()
                .add(bullet)
                .add(new Text("  " + text).setFont(themeRenderer.regular()).setFontSize(10))
                .setMarginLeft(0f)
                .setMarginTop(2f)
                .setMarginBottom(2f)
                .setFixedLeading(14f)
                .setKeepWithNext(true));
    }

    private void sourceParagraph(Document document, SampleReport report, String para) throws IOException {
        document.add(new Paragraph(para)
                .setFont(themeRenderer.regular())
                .setFontSize(9));
    }

//    private void addMarketSegments(
//            Document document,
//            List<MarketSegment> roots)
//            throws IOException {
//
//        addChapterHeadings(document, report, "Market Segments");
//
//        for (MarketSegment root : roots) {
//
//            renderSegment(document, root, 1);
//
//        }
//    }

    private void renderSegment(
            Document document,
            MarketSegment segment,
            SampleReport report)
            throws IOException {

        // Render the current segment FIRST
        addSegmentPage(document, segment, report);

        // Then render all children
        for (MarketSegment child : segment.getChildren()) {

            renderSegment(document, child, report);

        }

    }


    //this is class closer curly brace for ending

}