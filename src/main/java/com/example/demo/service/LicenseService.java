package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.signature.SigningService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final UserRepository userRepository;
    private final SigningService signingService;

    private static final long TICKET_LIFETIME_SECONDS = 3600;

    public LicenseService(LicenseRepository licenseRepository,
                          ProductRepository productRepository,
                          LicenseTypeRepository licenseTypeRepository,
                          DeviceRepository deviceRepository,
                          DeviceLicenseRepository deviceLicenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          UserRepository userRepository,
                          SigningService signingService) {
        this.licenseRepository = licenseRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.userRepository = userRepository;
        this.signingService = signingService;
    }

    // ===== 2.1 Создание лицензии =====

    @Transactional
    public License createLicense(CreateLicenseRequest request, Long adminId) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));

        LicenseType type = licenseTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "type not found"));

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "admin not found"));

        License license = new License();
        license.setCode(UUID.randomUUID().toString());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setUser(null);
        license.setDeviceCount(request.getDeviceCount());
        license.setDescription(request.getDescription());
        license.setBlocked(false);

        licenseRepository.save(license);
        saveHistory(license, admin, "CREATED", "License created");

        return license;
    }

    // ===== 2.2 Активация лицензии =====

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, Long userId) {
        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));

        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "license owned by another user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setMacAddress(request.getDeviceMac());
                    d.setName(request.getDeviceName() != null ? request.getDeviceName() : request.getDeviceMac());
                    d.setUser(user);
                    return deviceRepository.save(d);
                });

        boolean isFirstActivation = license.getUser() == null;

        if (isFirstActivation) {
            license.setUser(user);
            license.setFirstActivationDate(LocalDate.now());
            license.setEndingDate(LocalDate.now().plusDays(license.getType().getDefaultDurationInDays()));
            licenseRepository.save(license);
            createDeviceLicense(license, device);
            saveHistory(license, user, "ACTIVATED", "First activation");
        } else {
            boolean alreadyActivated = deviceLicenseRepository
                    .existsByLicenseAndDevice_MacAddress(license, request.getDeviceMac());

            if (!alreadyActivated) {
                long activatedCount = deviceLicenseRepository.countByLicense(license);
                if (activatedCount >= license.getDeviceCount()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "device limit reached");
                }
                createDeviceLicense(license, device);
            }
            saveHistory(license, user, "ACTIVATED", "Re-activation on device " + request.getDeviceMac());
        }

        return buildTicketResponse(license, device);
    }

    // ===== 2.3 Продление лицензии =====

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) {
        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        boolean canRenew = license.getEndingDate() == null
                || license.getEndingDate().isBefore(LocalDate.now().plusDays(8));

        if (!canRenew) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "renewal not allowed: license is active and expires in more than 7 days");
        }

        license.setEndingDate(
                (license.getEndingDate() != null ? license.getEndingDate() : LocalDate.now())
                        .plusDays(license.getType().getDefaultDurationInDays())
        );
        licenseRepository.save(license);
        saveHistory(license, user, "RENEWED", "License renewed");

        Device device = deviceLicenseRepository.findAll().stream()
                .filter(dl -> dl.getLicense().getId().equals(license.getId()))
                .map(DeviceLicense::getDevice)
                .findFirst()
                .orElse(null);

        return buildTicketResponse(license, device);
    }

    // ===== 2.4 Проверка лицензии =====

    public TicketResponse checkLicense(CheckLicenseRequest request, Long userId) {
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "device not found"));

        License license = licenseRepository
                .findActiveByDeviceUserAndProduct(device, userId, request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));

        return buildTicketResponse(license, device);
    }

    // ===== Вспомогательные методы =====

    private void createDeviceLicense(License license, Device device) {
        DeviceLicense dl = new DeviceLicense();
        dl.setLicense(license);
        dl.setDevice(device);
        dl.setActivationDate(LocalDate.now());
        deviceLicenseRepository.save(dl);
    }

    private void saveHistory(License license, User user, String status, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus(status);
        history.setChangeDate(LocalDateTime.now());
        history.setDescription(description);
        licenseHistoryRepository.save(history);
    }

    private TicketResponse buildTicketResponse(License license, Device device) {
        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                TICKET_LIFETIME_SECONDS,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.getUser() != null ? license.getUser().getId() : null,
                device != null ? device.getId() : null,
                license.isBlocked()
        );
        String signature = signingService.sign(ticket);
        return new TicketResponse(ticket, signature);
    }
}
