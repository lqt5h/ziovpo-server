// TicketResponse.java
package com.example.demo.dto;

public class TicketResponse {
    private Ticket ticket;
    private String signature; // ЭЦП (цифровая подпись тикета)

    public TicketResponse(Ticket ticket, String signature) {
        this.ticket = ticket;
        this.signature = signature;
    }

    public Ticket getTicket() { return ticket; }
    public String getSignature() { return signature; }
}
