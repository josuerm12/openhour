package com.openhour.backend.dto;

import java.time.LocalDateTime;

public class BookingDTO {

    private String customerName;
    private String serviceType;
    private LocalDateTime appointmentDate;

    // Default constructor
    public BookingDTO() {}

    // Parameterized constructor
    public BookingDTO(String customerName, String serviceType, LocalDateTime appointmentDate) {
        this.customerName = customerName;
        this.serviceType = serviceType;
        this.appointmentDate = appointmentDate;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "BookingDTO{" +
                "customerName='" + customerName + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", appointmentDate=" + appointmentDate +
                '}';
    }
}