package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "license_history")
public class LicenseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status;

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;

    private String description;

    public Long getId() { return id; }
    public License getLicense() { return license; }
    public User getUser() { return user; }
    public String getStatus() { return status; }
    public LocalDateTime getChangeDate() { return changeDate; }
    public String getDescription() { return description; }
    public void setLicense(License license) { this.license = license; }
    public void setUser(User user) { this.user = user; }
    public void setStatus(String status) { this.status = status; }
    public void setChangeDate(LocalDateTime changeDate) { this.changeDate = changeDate; }
    public void setDescription(String description) { this.description = description; }
}
