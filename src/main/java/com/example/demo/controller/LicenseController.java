package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.model.License;
import com.example.demo.service.LicenseService;
import com.example.demo.signature.SigningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;
    private final SigningService signingService;

    public LicenseController(LicenseService licenseService,
                             SigningService signingService) {
        this.licenseService = licenseService;
        this.signingService = signingService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<License> createLicense(
            @RequestBody CreateLicenseRequest request,
            @AuthenticationPrincipal User admin) {
        License license = licenseService.createLicense(request, admin.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(license);
    }

    @PostMapping("/activate")
    public ResponseEntity<TicketResponse> activateLicense(
            @RequestBody ActivateLicenseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(licenseService.activateLicense(request, user.getId()));
    }

    @PostMapping("/renew")
    public ResponseEntity<TicketResponse> renewLicense(
            @RequestBody RenewLicenseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(licenseService.renewLicense(request, user.getId()));
    }

    @PostMapping("/check")
    public ResponseEntity<TicketResponse> checkLicense(
            @RequestBody CheckLicenseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(licenseService.checkLicense(request, user.getId()));
    }

    // Публичный ключ для проверки подписи на стороне клиента
    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() {
        return ResponseEntity.ok(signingService.getPublicKeyBase64());
    }
}
