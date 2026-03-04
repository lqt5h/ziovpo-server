package com.example.demo.repository;

import com.example.demo.model.DeviceLicense;
import com.example.demo.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    long countByLicense(License license);
    boolean existsByLicenseAndDevice_MacAddress(License license, String macAddress);
    Optional<DeviceLicense> findFirstByLicense(License license);
}
