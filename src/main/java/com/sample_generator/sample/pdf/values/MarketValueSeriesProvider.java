package com.sample_generator.sample.pdf.values;

import com.sample_generator.sample.Entity.SampleReport;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Derives per-year market values and CAGR from report-level anchors and stable segment path keys.
 */
@Component
public class MarketValueSeriesProvider {

    public double[] yearlyValuesUsdMillion(SampleReport report, String... pathSegments) {
        int historic = historicYear(report);
        int forecast = report.getForecastYear();
        int span = forecast - historic + 1;
        double[] values = new double[span];

        double start = anchorStart(report, pathSegments);
        double end = anchorEnd(report, pathSegments);
        if (start <= 0 || end <= 0) {
            start = Math.max(start, 1.0);
            end = Math.max(end, start * 1.5);
        }

        double logStart = Math.log(start);
        double logEnd = Math.log(end);
        for (int i = 0; i < span; i++) {
            int year = historic + i;
            double t = span <= 1 ? 1.0 : (double) (year - historic) / (double) (forecast - historic);
            values[i] = Math.exp(logStart + (logEnd - logStart) * t);
        }
        return values;
    }

    public double cagrPercent(double[] values, SampleReport report) {
        int historic = historicYear(report);
        int base = report.getBaseYear();
        int forecast = report.getForecastYear();
        int startIndex = base - historic;
        int endIndex = forecast - historic;
        if (values == null || values.length == 0 || startIndex < 0 || endIndex >= values.length || startIndex >= endIndex) {
            return 0;
        }
        double start = values[startIndex];
        double end = values[endIndex];
        if (start <= 0 || end <= 0) {
            return 0;
        }
        int years = forecast - base;
        if (years <= 0) {
            return 0;
        }
        return (Math.pow(end / start, 1.0 / years) - 1.0) * 100.0;
    }

    public double[] yearOverYearGrowthPercent(double[] values) {
        if (values == null || values.length < 2) {
            return new double[0];
        }
        double[] growth = new double[values.length];
        growth[0] = 0;
        for (int i = 1; i < values.length; i++) {
            double prev = values[i - 1];
            growth[i] = prev <= 0 ? 0 : ((values[i] - prev) / prev) * 100.0;
        }
        return growth;
    }

    public String formatValue(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    public double sum(double[] values) {
        double total = 0;
        if (values == null) {
            return 0;
        }
        for (double v : values) {
            total += v;
        }
        return total;
    }

    private int historicYear(SampleReport report) {
        return report.getHistoricYear() != null ? report.getHistoricYear() : report.getBaseYear();
    }

    private double anchorStart(SampleReport report, String... pathSegments) {
        double global = report.getMarketValueBaseYear() != null ? report.getMarketValueBaseYear() : 1000.0;
        return global * weight(pathSegments);
    }

    private double anchorEnd(SampleReport report, String... pathSegments) {
        double global = report.getMarketValueForecastYear() != null ? report.getMarketValueForecastYear() : 2500.0;
        return global * weight(pathSegments);
    }

    private double weight(String... pathSegments) {
        if (pathSegments == null || pathSegments.length == 0) {
            return 1.0;
        }
        StringBuilder key = new StringBuilder();
        for (String segment : pathSegments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (key.length() > 0) {
                key.append('|');
            }
            key.append(segment.trim().toLowerCase(Locale.ROOT));
        }
        if (key.length() == 0) {
            return 1.0;
        }
        int hash = key.toString().hashCode();
        double fraction = (hash & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return 0.02 + fraction * 0.35;
    }
}
