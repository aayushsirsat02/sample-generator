package com.sample_generator.sample.dto;

import java.util.ArrayList;
import java.util.List;

public class SegmentData {

    private String name;

    private List<String> values = new ArrayList<>();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}