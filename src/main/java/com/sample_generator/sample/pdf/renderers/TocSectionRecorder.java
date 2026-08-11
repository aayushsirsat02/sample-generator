package com.sample_generator.sample.pdf.renderers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public interface TocSectionRecorder {

    void recordChapter(Document document, int chapter, Paragraph heading);

    void recordSection(Document document, String destinationKey, Paragraph heading);
}
