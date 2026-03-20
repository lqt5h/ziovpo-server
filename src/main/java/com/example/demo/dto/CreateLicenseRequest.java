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

    public void setProductId(Long productId) { this.productId = productId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public void setDeviceCount(int deviceCount) { this.deviceCount = deviceCount; }
    public void setDescription(String description) { this.description = description; }
}
