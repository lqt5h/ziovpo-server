// DeviceLicenseRepository.java
package com.example.demo.repository;

import com.example.demo.model.DeviceLicense;
import com.example.demo.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    long countByLicense(License license);
    boolean existsByLicenseAndDevice_MacAddress(License license, String macAddress);
}
