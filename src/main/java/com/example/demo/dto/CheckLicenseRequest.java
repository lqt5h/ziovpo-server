package com.example.demo.dto;

public class CheckLicenseRequest {
    private String deviceMac;
    private Long productId;

    public String getDeviceMac() { return deviceMac; }
    public Long getProductId() { return productId; }

    public void setDeviceMac(String deviceMac) { this.deviceMac = deviceMac; }
    public void setProductId(Long productId) { this.productId = productId; }
}
