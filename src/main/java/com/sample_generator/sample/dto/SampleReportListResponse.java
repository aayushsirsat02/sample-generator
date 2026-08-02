package com.sample_generator.sample.dto;

public class SampleReportListResponse {

    private Long id;

    private String keyId;

    private String keyName;

    private String createdAt;


    public SampleReportListResponse() {
    }


    public SampleReportListResponse(
            Long id,
            String keyId,
            String keyName
    ) {
        this.id = id;
        this.keyId = keyId;
        this.keyName = keyName;
    }

    public SampleReportListResponse(
            Long id,
            String keyId,
            String keyName,
            String createdAt
    ) {
        this.id = id;
        this.keyId = keyId;
        this.keyName = keyName;
        this.createdAt = createdAt;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }


    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }


    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}