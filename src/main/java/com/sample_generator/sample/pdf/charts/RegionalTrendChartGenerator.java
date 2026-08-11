package com.sample_generator.sample.pdf.charts;



import org.springframework.stereotype.Component;



import javax.imageio.ImageIO;

import java.awt.BasicStroke;

import java.awt.Color;

import java.awt.Font;

import java.awt.FontMetrics;

import java.awt.Graphics2D;

import java.awt.RenderingHints;

import java.awt.geom.Line2D;

import java.awt.geom.Rectangle2D;

import java.io.ByteArrayOutputStream;

import java.io.IOException;



@Component

public class RegionalTrendChartGenerator {



    private static final Color NAVY = new Color(0, 32, 96);

    private static final Color HISTORIC_BAR = new Color(192, 192, 192);

    private static final Color HISTORIC_BAR_EDGE = new Color(160, 160, 160);

    private static final Color NAVY_BAR_EDGE = new Color(0, 22, 70);

    private static final Color GOLD = new Color(255, 193, 7);

    private static final Color GRID = new Color(220, 220, 220);

    private static final Color AXIS_TEXT = new Color(60, 60, 60);



    public byte[] renderComboChart(

            int[] years,

            double[] revenueUsdMillion,

            double[] growthPercent,

            int forecastStartYear) throws IOException {

        int width = 1200;

        int height = 480;

        var image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);

        g.fillRect(0, 0, width, height);



        int left = 88;

        int right = width - 88;

        int top = 48;

        int bottom = height - 110;

        int chartW = right - left;

        int chartH = bottom - top;



        double maxRev = 1;

        for (double v : revenueUsdMillion) {

            maxRev = Math.max(maxRev, v);

        }



        double minGrowth = 0;

        double maxGrowth = 5;

        for (double v : growthPercent) {

            minGrowth = Math.min(minGrowth, v);

            maxGrowth = Math.max(maxGrowth, v);

        }

        if (maxGrowth - minGrowth < 4) {

            double mid = (maxGrowth + minGrowth) / 2.0;

            minGrowth = mid - 2;

            maxGrowth = mid + 2;

        }

        double growthSpan = Math.max(maxGrowth - minGrowth, 1);



        g.setColor(GRID);

        g.setStroke(new BasicStroke(1f));

        for (int i = 0; i <= 5; i++) {

            int y = top + (chartH * i / 5);

            g.drawLine(left, y, right, y);

        }



        Font axisFont = new Font("SansSerif", Font.PLAIN, 11);

        g.setFont(axisFont);

        g.setColor(AXIS_TEXT);

        for (int i = 0; i <= 5; i++) {

            int y = bottom - (chartH * i / 5);

            double label = maxRev * i / 5.0;

            String text = formatAxisValue(label);

            g.drawString(text, left - 52, y + 4);

        }

        for (int i = 0; i <= 5; i++) {

            int y = bottom - (chartH * i / 5);

            double label = minGrowth + (growthSpan * i / 5.0);

            String text = String.format("%.0f%%", label);

            FontMetrics fm = g.getFontMetrics();

            g.drawString(text, right + 10, y + fm.getAscent() / 2);

        }



        int n = years.length;

        double barSlot = (double) chartW / Math.max(n, 1);

        double barWidth = barSlot * 0.52;



        for (int i = 0; i < n; i++) {

            double h = revenueUsdMillion[i] / maxRev * chartH;

            double x = left + i * barSlot + (barSlot - barWidth) / 2;

            double y = bottom - h;

            boolean forecast = years[i] >= forecastStartYear;

            Color fill = forecast ? NAVY : HISTORIC_BAR;

            Color edge = forecast ? NAVY_BAR_EDGE : HISTORIC_BAR_EDGE;

            g.setColor(fill);

            g.fill(new Rectangle2D.Double(x, y, barWidth, h));

            g.setColor(edge);

            g.fill(new Rectangle2D.Double(x + barWidth * 0.82, y + 2, barWidth * 0.18, Math.max(h - 2, 0)));

        }



        g.setStroke(new BasicStroke(2.4f));

        g.setColor(GOLD);

        for (int i = 0; i < n; i++) {

            double gx = left + i * barSlot + barSlot / 2;

            double gy = bottom - ((growthPercent[i] - minGrowth) / growthSpan) * chartH;

            if (i > 0) {

                double prevGx = left + (i - 1) * barSlot + barSlot / 2;

                double prevGy = bottom - ((growthPercent[i - 1] - minGrowth) / growthSpan) * chartH;

                g.draw(new Line2D.Double(prevGx, prevGy, gx, gy));

            }

            double marker = 7;

            g.fill(new Rectangle2D.Double(gx - marker / 2, gy - marker / 2, marker, marker));

        }



        g.setColor(AXIS_TEXT);

        g.setFont(axisFont);

        for (int i = 0; i < n; i++) {

            if (n <= 12 || i % 2 == 0 || i == n - 1) {

                String label = String.valueOf(years[i]);

                FontMetrics fm = g.getFontMetrics();

                float x = (float) (left + i * barSlot + barSlot / 2 - fm.stringWidth(label) / 2.0);

                g.drawString(label, x, bottom + 22);

            }

        }



        g.drawLine(left, bottom, right, bottom);



        Font labelFont = new Font("SansSerif", Font.PLAIN, 12);

        g.setFont(labelFont);

        g.setColor(AXIS_TEXT);

        g.drawString("Year", left + chartW / 2 - 18, bottom + 44);



        g.rotate(-Math.PI / 2);

        g.drawString("Revenue", -top - chartH / 2 - 30, 28);

        g.rotate(Math.PI / 2);



        g.rotate(-Math.PI / 2);

        g.drawString("Growth Rate (%)", -top - chartH / 2 - 44, width - 24);

        g.rotate(Math.PI / 2);



        int legendY = height - 36;

        int legendX = width / 2 - 120;

        g.setColor(NAVY);

        g.fill(new Rectangle2D.Double(legendX, legendY, 12, 12));

        g.setColor(AXIS_TEXT);

        g.drawString("Revenue", legendX + 18, legendY + 11);

        g.setColor(GOLD);

        g.setStroke(new BasicStroke(2.4f));

        g.drawLine(legendX + 100, legendY + 6, legendX + 118, legendY + 6);

        g.fill(new Rectangle2D.Double(legendX + 108, legendY + 2.5, 7, 7));

        g.setColor(AXIS_TEXT);

        g.drawString("Growth Rate", legendX + 126, legendY + 11);



        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ImageIO.write(image, "png", out);

        return out.toByteArray();

    }



    private static String formatAxisValue(double value) {

        if (value >= 1000) {

            return String.format("%.0f", value);

        }

        if (value >= 100) {

            return String.format("%.0f", value);

        }

        if (value >= 10) {

            return String.format("%.1f", value);

        }

        return String.format("%.2f", value);

    }

}

