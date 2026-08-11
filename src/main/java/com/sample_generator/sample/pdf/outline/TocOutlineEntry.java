package com.sample_generator.sample.pdf.outline;

public record TocOutlineEntry(
        String number,
        String title,
        int level,
        String destinationKey) {
}
