// Ticket.java
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Ticket {
    private LocalDateTime serverDate;       // текущая дата сервера
    private long ticketLifetimeSeconds;     // время жизни тикета
    private LocalDate activationDate;       // дата активации лицензии
    private LocalDate expirationDate;       // дата истечения лицензии
    private Long userId;                    // идентификатор пользователя
    private Long deviceId;                  // идентификатор устройства
    private boolean licenseBlocked;        // флаг блокировки лицензии

    public Ticket(LocalDateTime serverDate, long ticketLifetimeSeconds,
                  LocalDate activationDate, LocalDate expirationDate,
                  Long userId, Long deviceId, boolean licenseBlocked) {
        this.serverDate = serverDate;
        this.ticketLifetimeSeconds = ticketLifetimeSeconds;
        this.activationDate = activationDate;
        this.expirationDate = expirationDate;
        this.userId = userId;
        this.deviceId = deviceId;
        this.licenseBlocked = licenseBlocked;
    }

    public LocalDateTime getServerDate() { return serverDate; }
    public long getTicketLifetimeSeconds() { return ticketLifetimeSeconds; }
    public LocalDate getActivationDate() { return activationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Long getUserId() { return userId; }
    public Long getDeviceId() { return deviceId; }
    public boolean isLicenseBlocked() { return licenseBlocked; }
}
