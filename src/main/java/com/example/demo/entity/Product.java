package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isBlocked() { return isBlocked; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}
