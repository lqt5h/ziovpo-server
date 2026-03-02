package com.example.demo.model;

import jakarta.persistence.*;
import com.example.demo.entity.User;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getMacAddress() { return macAddress; }
    public User getUser() { return user; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public void setUser(User user) { this.user = user; }
}
