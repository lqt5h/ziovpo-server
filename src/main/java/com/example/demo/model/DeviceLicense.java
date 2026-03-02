package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "device_license")
public class DeviceLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "activation_date", nullable = false)
    private LocalDate activationDate;

    public Long getId() { return id; }
    public License getLicense() { return license; }
    public Device getDevice() { return device; }
    public LocalDate getActivationDate() { return activationDate; }
    public void setLicense(License license) { this.license = license; }
    public void setDevice(Device device) { this.device = device; }
    public void setActivationDate(LocalDate activationDate) { this.activationDate = activationDate; }
}
