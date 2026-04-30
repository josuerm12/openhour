package com.openhour.backend.service;

import com.openhour.backend.dto.AvailabilitySlotResponse;
import com.openhour.backend.exception.BadRequestException;
import com.openhour.backend.model.AppointmentStatus;
import com.openhour.backend.model.AvailabilitySlot;
import com.openhour.backend.repository.AppointmentRepository;
import com.openhour.backend.repository.AvailabilitySlotRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {
    public static final List<String> DEFAULT_TIMES = List.of(
            "8:00 AM",
            "8:30 AM",
            "9:00 AM",
            "9:30 AM",
            "10:00 AM",
            "10:30 AM",
            "11:00 AM",
            "11:30 AM",
            "12:00 PM",
            "12:30 PM",
            "1:00 PM",
            "1:30 PM",
            "2:00 PM",
            "2:30 PM",
            "3:00 PM",
            "3:30 PM",
            "4:00 PM",
            "4:30 PM",
            "5:00 PM",
            "5:30 PM",
            "6:00 PM",
            "6:30 PM",
            "7:00 PM",
            "7:30 PM",
            "8:00 PM",
            "8:30 PM",
            "9:00 PM",
            "9:30 PM",
            "10:00 PM",
            "10:30 PM",
            "11:00 PM",
            "11:30 PM",
            "12:00 AM"
    );

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ActivityService activityService;

    public AvailabilityService(
            AvailabilitySlotRepository availabilitySlotRepository,
            AppointmentRepository appointmentRepository,
            ActivityService activityService
    ) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.activityService = activityService;
    }

    public List<AvailabilitySlotResponse> list(LocalDate start, LocalDate end) {
        ensureSeeded(start, end);
        return availabilitySlotRepository.findBySlotDateBetweenOrderBySlotDateAscSlotTimeAsc(start, end).stream()
                .filter(slot -> DEFAULT_TIMES.contains(slot.getSlotTime()))
                .sorted(slotOrder())
                .map(this::toResponse)
                .toList();
    }

    public List<AvailabilitySlotResponse> listForDate(LocalDate date) {
        ensureSeeded(date, date);
        return availabilitySlotRepository.findBySlotDateOrderBySlotTimeAsc(date).stream()
                .filter(slot -> DEFAULT_TIMES.contains(slot.getSlotTime()))
                .sorted(slotOrder())
                .map(this::toResponse)
                .toList();
    }

    public boolean isOpenAndUnbooked(LocalDate date, String time) {
        ensureSeeded(date, date);
        return availabilitySlotRepository.findBySlotDateAndSlotTime(date, time)
                .filter(AvailabilitySlot::isOpen)
                .filter(slot -> !isBooked(date, time))
                .isPresent();
    }

    @Transactional
    public AvailabilitySlotResponse setOpen(LocalDate date, String time, boolean open) {
        if (!DEFAULT_TIMES.contains(time)) {
            throw new BadRequestException("Unsupported time slot.");
        }
        if (!open && isBooked(date, time)) {
            throw new BadRequestException("Booked slots cannot be blocked until the appointment is cancelled.");
        }

        AvailabilitySlot slot = availabilitySlotRepository.findBySlotDateAndSlotTime(date, time)
                .orElseGet(() -> new AvailabilitySlot(date, time, false));
        slot.setOpen(open);
        AvailabilitySlot saved = availabilitySlotRepository.save(slot);
        activityService.record((open ? "Opened " : "Blocked ") + time + " on " + date + ".");
        return toResponse(saved);
    }

    @Transactional
    public void ensureSeeded(LocalDate start, LocalDate end) {
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            for (String time : DEFAULT_TIMES) {
                LocalDate date = cursor;
                availabilitySlotRepository.findBySlotDateAndSlotTime(date, time)
                        .orElseGet(() -> availabilitySlotRepository.save(new AvailabilitySlot(date, time, isDefaultOpen(date))));
            }
            cursor = cursor.plusDays(1);
        }
    }

    private boolean isDefaultOpen(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        return day != 7;
    }

    private AvailabilitySlotResponse toResponse(AvailabilitySlot slot) {
        boolean booked = isBooked(slot.getSlotDate(), slot.getSlotTime());
        return new AvailabilitySlotResponse(slot.getSlotDate(), slot.getSlotTime(), slot.isOpen(), booked);
    }

    private boolean isBooked(LocalDate date, String time) {
        return appointmentRepository.existsByAppointmentDateAndAppointmentTimeAndStatus(date, time, AppointmentStatus.CONFIRMED);
    }

    private Comparator<AvailabilitySlot> slotOrder() {
        return Comparator.comparing(AvailabilitySlot::getSlotDate)
                .thenComparing(slot -> DEFAULT_TIMES.indexOf(slot.getSlotTime()));
    }
}
