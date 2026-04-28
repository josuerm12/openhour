package com.openhour.backend.dto;

public class CancellationRequest {
    private String bookingId;

    public CancellationRequest() {}

    public CancellationRequest(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}
