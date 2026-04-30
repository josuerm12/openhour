package com.openhour.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openhour.backend.dto.AvailabilitySlotResponse;
import com.openhour.backend.exception.BadRequestException;
import com.openhour.backend.model.AppointmentStatus;
import com.openhour.backend.model.AvailabilitySlot;
import com.openhour.backend.repository.AppointmentRepository;
import com.openhour.backend.repository.AvailabilitySlotRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {
    private FakeAvailabilitySlotRepository slots;
    private FakeAppointmentRepository appointments;
    private FakeActivityService activity;
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        slots = new FakeAvailabilitySlotRepository();
        appointments = new FakeAppointmentRepository();
        activity = new FakeActivityService();
        availabilityService = new AvailabilityService(slots.repository(), appointments.repository(), activity);
    }

    @Test
    void listSeedsDefaultSlotsForDateRangeAndMarksSundayClosed() {
        LocalDate monday = LocalDate.of(2026, 5, 11);
        LocalDate saturday = LocalDate.of(2026, 5, 16);
        LocalDate sunday = LocalDate.of(2026, 5, 17);

        List<AvailabilitySlotResponse> responses = availabilityService.list(monday, sunday);

        assertThat(responses).hasSize(231);
        assertThat(responses)
                .filteredOn(response -> response.date().equals(monday))
                .allMatch(AvailabilitySlotResponse::open);
        assertThat(responses)
                .filteredOn(response -> response.date().equals(saturday))
                .allMatch(AvailabilitySlotResponse::open);
        assertThat(responses)
                .filteredOn(response -> response.date().equals(sunday))
                .noneMatch(AvailabilitySlotResponse::open);
        assertThat(responses)
                .filteredOn(response -> response.date().equals(saturday))
                .extracting(AvailabilitySlotResponse::time)
                .containsExactlyElementsOf(AvailabilityService.DEFAULT_TIMES);
    }

    @Test
    void isOpenAndUnbookedReturnsFalseWhenConfirmedAppointmentUsesSlot() {
        LocalDate date = LocalDate.of(2026, 5, 16);
        appointments.bookedSlots.add(slotKey(date, "10:00 AM"));

        boolean available = availabilityService.isOpenAndUnbooked(date, "10:00 AM");

        assertThat(available).isFalse();
    }

    @Test
    void setOpenRejectsUnsupportedTimes() {
        assertThatThrownBy(() -> availabilityService.setOpen(LocalDate.of(2026, 5, 16), "7:30 AM", true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported time slot");

        assertThat(slots.saved).isEmpty();
    }

    @Test
    void setOpenRejectsBlockingBookedSlots() {
        LocalDate date = LocalDate.of(2026, 5, 16);
        appointments.bookedSlots.add(slotKey(date, "10:00 AM"));

        assertThatThrownBy(() -> availabilityService.setOpen(date, "10:00 AM", false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booked slots cannot be blocked");

        assertThat(slots.saved).isEmpty();
        assertThat(activity.messages).isEmpty();
    }

    @Test
    void setOpenCreatesOrUpdatesSlotAndRecordsActivity() {
        LocalDate date = LocalDate.of(2026, 5, 18);

        AvailabilitySlotResponse response = availabilityService.setOpen(date, "12:00 PM", true);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.time()).isEqualTo("12:00 PM");
        assertThat(response.open()).isTrue();
        assertThat(response.booked()).isFalse();
        assertThat(slots.saved).hasSize(1);
        assertThat(activity.messages).containsExactly("Opened 12:00 PM on 2026-05-18.");
    }

    private static String slotKey(LocalDate date, String time) {
        return date + "|" + time;
    }

    private static class FakeAvailabilitySlotRepository {
        private final Map<String, AvailabilitySlot> slotsByKey = new HashMap<>();
        private final List<AvailabilitySlot> saved = new ArrayList<>();

        AvailabilitySlotRepository repository() {
            return (AvailabilitySlotRepository) Proxy.newProxyInstance(
                    AvailabilitySlotRepository.class.getClassLoader(),
                    new Class<?>[]{AvailabilitySlotRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findBySlotDateAndSlotTime" -> Optional.ofNullable(slotsByKey.get(slotKey((LocalDate) args[0], (String) args[1])));
                        case "findBySlotDateBetweenOrderBySlotDateAscSlotTimeAsc" -> findBetween((LocalDate) args[0], (LocalDate) args[1]);
                        case "findBySlotDateOrderBySlotTimeAsc" -> findByDate((LocalDate) args[0]);
                        case "save" -> save((AvailabilitySlot) args[0]);
                        case "toString" -> "FakeAvailabilitySlotRepository";
                        default -> throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                    }
            );
        }

        private AvailabilitySlot save(AvailabilitySlot slot) {
            slotsByKey.put(slotKey(slot.getSlotDate(), slot.getSlotTime()), slot);
            saved.add(slot);
            return slot;
        }

        private List<AvailabilitySlot> findBetween(LocalDate start, LocalDate end) {
            return slotsByKey.values().stream()
                    .filter(slot -> !slot.getSlotDate().isBefore(start) && !slot.getSlotDate().isAfter(end))
                    .sorted(slotComparator())
                    .toList();
        }

        private List<AvailabilitySlot> findByDate(LocalDate date) {
            return slotsByKey.values().stream()
                    .filter(slot -> slot.getSlotDate().equals(date))
                    .sorted(slotComparator())
                    .toList();
        }

        private Comparator<AvailabilitySlot> slotComparator() {
            return Comparator.comparing(AvailabilitySlot::getSlotDate)
                    .thenComparing(slot -> AvailabilityService.DEFAULT_TIMES.indexOf(slot.getSlotTime()));
        }
    }

    private static class FakeAppointmentRepository {
        private final List<String> bookedSlots = new ArrayList<>();

        AppointmentRepository repository() {
            return (AppointmentRepository) Proxy.newProxyInstance(
                    AppointmentRepository.class.getClassLoader(),
                    new Class<?>[]{AppointmentRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByAppointmentDateAndAppointmentTimeAndStatus" -> bookedSlots.contains(slotKey((LocalDate) args[0], (String) args[1]))
                                && args[2] == AppointmentStatus.CONFIRMED;
                        case "toString" -> "FakeAppointmentRepository";
                        default -> throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                    }
            );
        }
    }

    private static class FakeActivityService extends ActivityService {
        private final List<String> messages = new ArrayList<>();

        FakeActivityService() {
            super(null);
        }

        @Override
        public void record(String message) {
            messages.add(message);
        }
    }
}
