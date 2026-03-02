// CreateLicenseRequest.java
package com.example.demo.dto;

public class CreateLicenseRequest {
    private Long productId;
    private Long typeId;
    private Long ownerId;
    private int deviceCount;
    private String description;

    public Long getProductId() { return productId; }
    public Long getTypeId() { return typeId; }
    public Long getOwnerId() { return ownerId; }
    public int getDeviceCount() { return deviceCount; }
    public String getDescription() { return description; }
}
