package com.sample_generator.sample.pdf.table;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.sample_generator.sample.pdf.ThemeRenderer;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Servlet-aligned styling for report body tables (width, navy headers, borders, typography).
 */
public final class BodyTableStyling {

    public static final DeviceRgb HEADER_BACKGROUND = new DeviceRgb(0, 32, 96);

    private static final float BODY_FONT_SIZE_PT = 7.5f;
    private static final float HEADER_FONT_SIZE_PT = 8f;
    private static final float CELL_PADDING_PT = 2.5f;
    private static final float HEADER_PADDING_PT = 3f;
    private static final float BORDER_WIDTH_PT = 0.5f;

    private static final Border BODY_CELL_BORDER = new SolidBorder(ColorConstants.BLACK, BORDER_WIDTH_PT);
    private static final Border HEADER_CELL_BORDER = new SolidBorder(ColorConstants.WHITE, BORDER_WIDTH_PT);

    private static final Pattern YEAR_TOKEN = Pattern.compile("^(19|20)\\d{2}$");
    private static final Pattern PURE_PERCENT = Pattern.compile("^[+-]?[\\d,]+(?:\\.\\d+)?\\s*%$");
    private static final Pattern PURE_NUMBER = Pattern.compile("^[+-]?[\\d,]+(?:\\.\\d+)?$");
    private static final Pattern EMBEDDED_PERCENT = Pattern.compile("[+-]?[\\d,]+(?:\\.\\d+)?\\s*%");

    private BodyTableStyling() {
    }

    public static Table newBodyTable(float[] columnWidths) {
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();
        table.setFixedLayout();
        return table;
    }

    public static Cell headerCell(ThemeRenderer theme, String text, TextAlignment alignment) throws IOException {
        Paragraph paragraph = new Paragraph(text != null ? text : "")
                .setFont(theme.semiBold())
                .setFontSize(HEADER_FONT_SIZE_PT)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(alignment)
                .setMargin(0)
                .setMultipliedLeading(1.05f);
        paragraph.setProperty(Property.NO_SOFT_WRAP_INLINE, Boolean.TRUE);
        return new Cell()
                .add(paragraph)
                .setBackgroundColor(HEADER_BACKGROUND)
                .setTextAlignment(alignment)
                .setPadding(HEADER_PADDING_PT)
                .setBorder(HEADER_CELL_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    public static Cell labelCell(ThemeRenderer theme, String text) throws IOException {
        return bodyCell(theme, text, true, TextAlignment.LEFT, false);
    }

    public static Cell valueCell(ThemeRenderer theme, String text) throws IOException {
        return bodyCell(theme, maskNumericBodyValue(text), false, TextAlignment.CENTER, true);
    }

    public static Cell bodyCell(
            ThemeRenderer theme,
            String text,
            boolean semiBold,
            TextAlignment alignment) throws IOException {
        return bodyCell(theme, text, semiBold, alignment, false);
    }

    private static Cell bodyCell(
            ThemeRenderer theme,
            String text,
            boolean semiBold,
            TextAlignment alignment,
            boolean compactNoWrap) throws IOException {
        String safe = text != null ? text : "-";
        Paragraph paragraph = new Paragraph(safe)
                .setFont(semiBold ? theme.semiBold() : theme.regular())
                .setFontSize(BODY_FONT_SIZE_PT)
                .setTextAlignment(alignment)
                .setMargin(0)
                .setMultipliedLeading(1.05f);
        if (compactNoWrap) {
            paragraph.setProperty(Property.NO_SOFT_WRAP_INLINE, Boolean.TRUE);
        }
        Cell cell = new Cell()
                .add(paragraph)
                .setTextAlignment(alignment)
                .setPadding(CELL_PADDING_PT)
                .setBorder(BODY_CELL_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (compactNoWrap) {
            cell.setProperty(Property.NO_SOFT_WRAP_INLINE, Boolean.TRUE);
        }
        return cell;
    }

    /**
     * Hide numeric body values while preserving labels, years, and non-numeric text.
     * Years such as 2019 stay unchanged; CAGR-style percents become {@code xx%}.
     */
    private static String maskNumericBodyValue(String text) {
        if (text == null) {
            return "-";
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed) || "—".equals(trimmed)) {
            return trimmed.isEmpty() ? text : trimmed;
        }
        if ("xx".equalsIgnoreCase(trimmed)) {
            return "xx";
        }
        if ("xx%".equalsIgnoreCase(trimmed)) {
            return "xx%";
        }
        if (YEAR_TOKEN.matcher(trimmed).matches()) {
            return trimmed;
        }
        if (PURE_PERCENT.matcher(trimmed).matches()) {
            return "xx%";
        }
        if (PURE_NUMBER.matcher(trimmed).matches()) {
            return "xx";
        }
        return EMBEDDED_PERCENT.matcher(trimmed).replaceAll("xx%");
    }

    public static Paragraph sourceLine(ThemeRenderer theme, String text) throws IOException {
        return new Paragraph(text)
                .setFont(theme.regular())
                .setFontSize(9)
                .setMarginTop(2f);
    }

    public static void addTableWithSource(Document document, Table table, Paragraph source) {
        Div block = new Div();
        block.setKeepTogether(true);
        table.setKeepTogether(true);
        block.add(table);
        if (source != null) {
            block.add(source);
        }
        document.add(block);
    }
}
