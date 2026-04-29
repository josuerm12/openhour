package com.openhour.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "availability_slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_availability_slot", columnNames = {"slotDate", "slotTime"})
)
public class AvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate slotDate;
    private String slotTime;
    private boolean open;

    public AvailabilitySlot() {
    }

    public AvailabilitySlot(LocalDate slotDate, String slotTime, boolean open) {
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.open = open;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public String getSlotTime() {
        return slotTime;
    }

    public void setSlotTime(String slotTime) {
        this.slotTime = slotTime;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
