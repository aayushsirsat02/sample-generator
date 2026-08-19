package com.sample_generator.sample.pdf.layout;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.sample_generator.sample.pdf.ClasspathImageCache;
import com.sample_generator.sample.pdf.PdfDocumentImageCache;
import com.sample_generator.sample.pdf.PdfGenTimer;
import com.sample_generator.sample.pdf.PdfRenderPass;
import com.sample_generator.sample.pdf.ThemeRenderer;

import java.io.IOException;

/**
 * Servlet / golden-PDF alignment for body figures and chart images (width, spacing).
 */
public final class BodyFigureLayout {

    public static final float HORIZONTAL_MARGIN_PT = 70f;
    public static final float VERTICAL_MARGIN_PT = 80f;
    public static final DeviceRgb FIGURE_LABEL_COLOR = new DeviceRgb(0, 32, 96);
    /** Golden PDF numbered section/subsection heading color (#0070C0). */
    public static final DeviceRgb NUMBERED_SECTION_HEADING_COLOR = new DeviceRgb(0, 112, 192);

    /** Native size of generated regional combo charts (must match the chart generator). */
    public static final float GENERATED_CHART_NATIVE_WIDTH = 1200f;
    public static final float GENERATED_CHART_NATIVE_HEIGHT = 480f;

    /** Leave room for caption + source within the body content band. */
    private static final float FIGURE_BLOCK_OVERHEAD_PT = 56f;
    private static final float FIGURE_CAPTION_SIZE_PT = 10f;
    private static final float FIGURE_TOP_MARGIN_PT = 2f;
    private static final float FIGURE_BOTTOM_MARGIN_PT = 4f;
    private static final float CAPTION_BOTTOM_MARGIN_PT = 6f;

    private BodyFigureLayout() {
    }

    /**
     * Style numbered body section/subsection headings: navy color + keep-with-next so
     * headings flow naturally and stay with following content (no forced page break).
     */
    public static void breakBeforeNumberedHeading(Document document, Paragraph heading) {
        heading.setFontColor(NUMBERED_SECTION_HEADING_COLOR);
        //heading.setKeepWithNext(true);
    }

    public static float contentWidthPt() {
        return PageSize.A4.rotate().getWidth() - (2f * HORIZONTAL_MARGIN_PT);
    }

    public static float contentHeightPt() {
        return PageSize.A4.rotate().getHeight() - (2f * VERTICAL_MARGIN_PT);
    }

    /**
     * Scale an image to fit the body content band without upscaling past its native size.
     */
    public static Image scaleForBody(Image image) {
        return scaleForBody(image, 0f);
    }

    /**
     * Scale an image leaving extra vertical room for headings / captions already on the page.
     * Prefer shrinking the chart over orphaning a heading or forcing another AreaBreak.
     */
    public static Image scaleForBody(Image image, float additionalReservePt) {
        float maxWidth = contentWidthPt();
        float reserve = FIGURE_BLOCK_OVERHEAD_PT + Math.max(0f, additionalReservePt);
        float maxHeight = Math.max(110f, contentHeightPt() - reserve);

        float nativeWidth = image.getImageWidth();
        float nativeHeight = image.getImageHeight();
        if (nativeWidth > 0f && nativeHeight > 0f) {
            float scale = Math.min(1f, Math.min(maxWidth / nativeWidth, maxHeight / nativeHeight));
            image.setWidth(nativeWidth * scale);
            image.setHeight(nativeHeight * scale);
        } else {
            image.scaleToFit(maxWidth, maxHeight);
        }

        image.setHorizontalAlignment(HorizontalAlignment.CENTER);
        image.setMarginTop(FIGURE_TOP_MARGIN_PT);
        image.setMarginBottom(FIGURE_BOTTOM_MARGIN_PT);
        return image;
    }

    public static void addClasspathFigure(Document document, String classpathPath) throws IOException {
        addClasspathFigure(document, classpathPath, 0f);
    }

    public static void addClasspathFigure(Document document, String classpathPath, float additionalReservePt)
            throws IOException {
        PdfGenTimer.time("images.classpath", () -> {
            ImageData imageData = ClasspathImageCache.get(classpathPath);
            if (PdfRenderPass.isTocIndexing()) {
                addScaledPlaceholder(document, imageData.getWidth(), imageData.getHeight(), additionalReservePt);
                return;
            }
            addScaledFigure(document, PdfDocumentImageCache.image(classpathPath, imageData), additionalReservePt);
        });
    }

    public static void addPngFigure(Document document, byte[] pngBytes) {
        addChartFigure(document, com.itextpdf.io.image.ImageDataFactory.create(pngBytes));
    }

    public static void addChartFigure(Document document, ImageData imageData) {
        if (PdfRenderPass.isTocIndexing()) {
            addChartPlaceholder(document);
            return;
        }
        addScaledFigure(document, new Image(imageData), 0f);
    }

    public static void addChartPlaceholder(Document document) {
        addScaledPlaceholder(document, GENERATED_CHART_NATIVE_WIDTH, GENERATED_CHART_NATIVE_HEIGHT, 0f);
    }

    private static void addScaledFigure(Document document, Image image, float additionalReservePt) {
        // Keep the figure on one page and with the following source line when possible.
        // Captions already use keepWithNext, so caption + figure + source move together.
        Div block = new Div();
        block.setKeepTogether(true);
        block.setKeepWithNext(true);
        block.add(scaleForBody(image, additionalReservePt));
        document.add(block);
    }

    private static void addScaledPlaceholder(
            Document document,
            float nativeWidth,
            float nativeHeight,
            float additionalReservePt) {
        float maxWidth = contentWidthPt();
        float reserve = FIGURE_BLOCK_OVERHEAD_PT + Math.max(0f, additionalReservePt);
        float maxHeight = Math.max(110f, contentHeightPt() - reserve);

        float width;
        float height;
        if (nativeWidth > 0f && nativeHeight > 0f) {
            float scale = Math.min(1f, Math.min(maxWidth / nativeWidth, maxHeight / nativeHeight));
            width = nativeWidth * scale;
            height = nativeHeight * scale;
        } else {
            width = maxWidth;
            height = maxHeight;
        }

        Div figure = new Div();
        figure.setWidth(width);
        figure.setHeight(height);
        figure.setHorizontalAlignment(HorizontalAlignment.CENTER);
        figure.setMarginTop(FIGURE_TOP_MARGIN_PT);
        figure.setMarginBottom(FIGURE_BOTTOM_MARGIN_PT);

        Div block = new Div();
        block.setKeepTogether(true);
        block.setKeepWithNext(true);
        block.add(figure);
        document.add(block);
    }

    /**
     * Caption pattern: bold navy {@code FIGURE n} + regular black title (golden PDF).
     */
    public static Paragraph figureCaption(ThemeRenderer theme, int figureNumber, String title) throws IOException {
        Paragraph caption = new Paragraph()
                .setMarginTop(0)
                .setMarginBottom(CAPTION_BOTTOM_MARGIN_PT)
                .setKeepWithNext(true);
        caption.add(new Text("FIGURE " + figureNumber + " ")
                .setFont(theme.semiBold())
                .setFontSize(FIGURE_CAPTION_SIZE_PT)
                .setFontColor(FIGURE_LABEL_COLOR));
        caption.add(new Text(title)
                .setFont(theme.regular())
                .setFontSize(FIGURE_CAPTION_SIZE_PT)
                .setFontColor(ColorConstants.BLACK));
        return caption;
    }

    /**
     * Unnumbered figure line (e.g. competitive landscape) — bold label + regular title.
     */
    public static Paragraph figureCaptionUnnumbered(ThemeRenderer theme, String figureLabel, String title)
            throws IOException {
        Paragraph caption = new Paragraph()
                .setMarginTop(0)
                .setMarginBottom(CAPTION_BOTTOM_MARGIN_PT);
        caption.add(new Text(figureLabel)
                .setFont(theme.semiBold())
                .setFontSize(FIGURE_CAPTION_SIZE_PT)
                .setFontColor(FIGURE_LABEL_COLOR));
        caption.add(new Text(title)
                .setFont(theme.regular())
                .setFontSize(FIGURE_CAPTION_SIZE_PT)
                .setFontColor(ColorConstants.BLACK));
        return caption;
    }

    /**
     * Render a figure with a strict height cap (e.g., 10f, 50f, 80f).
     */
    public static void addClasspathFigureWithMaxHeight(Document document, String classpathPath, float maxHeightPt)
            throws IOException {
        PdfGenTimer.time("images.classpathMaxHeight", () -> {
            ImageData imageData = ClasspathImageCache.get(classpathPath);
            float nativeWidth = imageData.getWidth();
            float nativeHeight = imageData.getHeight();

            if (PdfRenderPass.isTocIndexing()) {
                Div figure = new Div();
                if (nativeWidth > 0f && nativeHeight > 0f) {
                    float ratio = maxHeightPt / nativeHeight;
                    figure.setHeight(maxHeightPt);
                    figure.setWidth(nativeWidth * ratio);
                } else {
                    figure.setHeight(maxHeightPt);
                }
                figure.setHorizontalAlignment(HorizontalAlignment.CENTER);
                figure.setMarginTop(FIGURE_TOP_MARGIN_PT);
                figure.setMarginBottom(FIGURE_BOTTOM_MARGIN_PT);
                Div block = new Div();
                block.setKeepTogether(true);
                block.setHorizontalAlignment(HorizontalAlignment.CENTER);
                block.add(figure);
                document.add(block);
                return;
            }

            Image image = PdfDocumentImageCache.image(classpathPath, imageData);

            if (nativeWidth > 0f && nativeHeight > 0f) {
                float ratio = maxHeightPt / nativeHeight;
                float targetWidth = nativeWidth * ratio;

                image.setHeight(maxHeightPt);
                image.setWidth(targetWidth);
                image.setAutoScale(false);
            } else {
                image.setMaxHeight(maxHeightPt);
                image.setAutoScaleWidth(true);
            }

            image.setHorizontalAlignment(HorizontalAlignment.CENTER);
            image.setMarginTop(FIGURE_TOP_MARGIN_PT);
            image.setMarginBottom(FIGURE_BOTTOM_MARGIN_PT);

            Div block = new Div();
            block.setKeepTogether(true);
            block.setHorizontalAlignment(HorizontalAlignment.CENTER);
            block.add(image);

            document.add(block);
        });
    }
}
