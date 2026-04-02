package com.example.demo.dto;

public class TicketResponse {
    private Ticket ticket;
    private String signature;

    public TicketResponse(Ticket ticket, String signature) {
        this.ticket = ticket;
        this.signature = signature;
    }

    public Ticket getTicket() { return ticket; }
    public String getSignature() { return signature; }

    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public void setSignature(String signature) { this.signature = signature; }
}
