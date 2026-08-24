package com.sample_generator.sample.pdf;


import com.sample_generator.sample.Entity.MarketSegment;
import com.sample_generator.sample.Entity.SampleReport;
import com.sample_generator.sample.repository.MarketSegmentRepository;
import com.sample_generator.sample.repository.SampleReportRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final PdfRenderer pdfRenderer;
    private final SampleReportRepository sampleReportRepository;
    private final MarketSegmentRepository marketSegmentRepository;

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long reportId) throws IOException {

        SampleReport report = sampleReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        List<MarketSegment> segments =
                marketSegmentRepository.findBySampleReportId(reportId);

        List<MarketSegment> roots = buildTree(segments);

        System.out.println("===== TREE =====");

        for (MarketSegment root : roots) {
            printTree(root, 0);
        }



        long start = System.currentTimeMillis();
        byte[] pdf = pdfRenderer.generatePdf(report, roots);
        log.info("PDF generatePdf() time for reportId={}: {} ms", reportId, System.currentTimeMillis() - start);
        return pdf;
    }


    private List<MarketSegment> buildTree(List<MarketSegment> segments) {

        Map<Long, MarketSegment> map = new HashMap<>();

        List<MarketSegment> roots = new ArrayList<>();

        for (MarketSegment segment : segments) {

            map.put(segment.getId(), segment);

            segment.getChildren().clear();

        }

        for (MarketSegment segment : segments) {

            if (segment.getParent() == null) {

                roots.add(segment);

            } else {

                MarketSegment parent =
                        map.get(segment.getParent().getId());

                parent.getChildren().add(segment);

            }

        }

        return roots;
    }

    private void printTree(MarketSegment segment, int level) {

        for (int i = 0; i < level; i++) {
            System.out.print("    ");
        }

        System.out.println(segment.getSegmentName());

        for (MarketSegment child : segment.getChildren()) {
            printTree(child, level + 1);
        }
    }
}