package com.example.demo.dto;

public class ActivateLicenseRequest {
    private String activationKey;
    private String deviceMac;
    private String deviceName;

    public String getActivationKey() { return activationKey; }
    public String getDeviceMac() { return deviceMac; }
    public String getDeviceName() { return deviceName; }

    public void setActivationKey(String activationKey) { this.activationKey = activationKey; }
    public void setDeviceMac(String deviceMac) { this.deviceMac = deviceMac; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}
