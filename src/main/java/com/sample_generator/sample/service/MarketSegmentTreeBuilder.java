package com.sample_generator.sample.service;

import com.sample_generator.sample.Entity.MarketSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketSegmentTreeBuilder {

    public List<MarketSegment> buildTree(List<MarketSegment> segments) {

        Map<Long, MarketSegment> map = new HashMap<>();
        List<MarketSegment> roots = new ArrayList<>();

        // Put every segment in the map
        for (MarketSegment segment : segments) {

            map.put(segment.getId(), segment);

            segment.getChildren().clear();

        }

        // Connect parents and children
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

}