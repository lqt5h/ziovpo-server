package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private LicenseType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_activation_date")
    private LocalDate firstActivationDate;

    @Column(name = "ending_date")
    private LocalDate endingDate;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(name = "device_count", nullable = false)
    private int deviceCount;

    private String description;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Product getProduct() { return product; }
    public LicenseType getType() { return type; }
    public User getOwner() { return owner; }
    public User getUser() { return user; }
    public LocalDate getFirstActivationDate() { return firstActivationDate; }
    public LocalDate getEndingDate() { return endingDate; }
    public boolean isBlocked() { return blocked; }
    public int getDeviceCount() { return deviceCount; }
    public String getDescription() { return description; }

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setProduct(Product product) { this.product = product; }
    public void setType(LicenseType type) { this.type = type; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setUser(User user) { this.user = user; }
    public void setFirstActivationDate(LocalDate d) { this.firstActivationDate = d; }
    public void setEndingDate(LocalDate d) { this.endingDate = d; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void setDeviceCount(int deviceCount) { this.deviceCount = deviceCount; }
    public void setDescription(String description) { this.description = description; }
}
