package com.sample_generator.sample.dto;

import java.util.ArrayList;
import java.util.List;

public class SegmentRequest {

    private String segmentName;

    private List<SegmentRequest> children = new ArrayList<>();


    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }


    public List<SegmentRequest> getChildren() {
        return children;
    }

    public void setChildren(List<SegmentRequest> children) {
        this.children = children;
    }
}