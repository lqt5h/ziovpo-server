// LicenseTypeRepository.java
package com.example.demo.repository;

import com.example.demo.model.LicenseType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseTypeRepository extends JpaRepository<LicenseType, Long> {}
