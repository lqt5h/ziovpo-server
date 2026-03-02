package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "license_type")
public class LicenseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_duration_in_days", nullable = false)
    private int defaultDurationInDays;

    private String description;

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getDefaultDurationInDays() { return defaultDurationInDays; }
    public String getDescription() { return description; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDefaultDurationInDays(int days) { this.defaultDurationInDays = days; }
    public void setDescription(String description) { this.description = description; }
}
