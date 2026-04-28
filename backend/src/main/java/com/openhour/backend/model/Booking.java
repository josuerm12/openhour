package com.openhour.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Booking {
    private String id;
    private String customerName;
    private String serviceType;
    private LocalDateTime appointmentDate;
    private BookingStatus status;

    public enum BookingStatus {
        SCHEDULED, CANCELLED
    }

    public Booking() {
        this.id = UUID.randomUUID().toString();
        this.status = BookingStatus.SCHEDULED;
    }

    public Booking(String customerName, String serviceType, LocalDateTime appointmentDate) {
        this();
        this.customerName = customerName;
        this.serviceType = serviceType;
        this.appointmentDate = appointmentDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", customerName='" + customerName + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", status=" + status +
                '}';
    }
}
