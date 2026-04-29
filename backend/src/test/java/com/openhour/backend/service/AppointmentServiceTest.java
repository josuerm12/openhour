package com.openhour.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openhour.backend.dto.AppointmentRequest;
import com.openhour.backend.dto.AppointmentResponse;
import com.openhour.backend.dto.CheckoutResponse;
import com.openhour.backend.dto.MoveAppointmentRequest;
import com.openhour.backend.exception.BadRequestException;
import com.openhour.backend.model.Appointment;
import com.openhour.backend.model.AppointmentStatus;
import com.openhour.backend.model.PaymentStatus;
import com.openhour.backend.repository.AppointmentRepository;
import com.openhour.backend.service.StripeService.StripeCheckoutSession;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AppointmentServiceTest {
    private FakeAppointmentRepository appointments;
    private FakeAvailabilityService availability;
    private FakeStripeService stripe;
    private FakeActivityService activity;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointments = new FakeAppointmentRepository();
        availability = new FakeAvailabilityService();
        stripe = new FakeStripeService();
        activity = new FakeActivityService();

        appointmentService = new AppointmentService(
                appointments.repository(),
                availability,
                new CatalogService(),
                stripe,
                activity
        );
    }

    @Test
    void createCheckoutRejectsUnavailableSlotBeforeSavingOrCallingStripe() {
        AppointmentRequest request = bookingRequest();
        availability.openAndUnbooked = false;

        assertThatThrownBy(() -> appointmentService.createCheckout(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");

        assertThat(appointments.saved).isEmpty();
        assertThat(stripe.createCheckoutCalls).isZero();
    }

    @Test
    void createCheckoutSavesPendingAppointmentAndReturnsStripeCheckout() {
        AppointmentRequest request = bookingRequest();
        availability.openAndUnbooked = true;
        stripe.checkoutSession = new StripeCheckoutSession(
                "cs_test_123",
                "https://checkout.stripe.test/session",
                null
        );

        CheckoutResponse response = appointmentService.createCheckout(request);

        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.test/session");
        assertThat(response.stripeSessionId()).isEqualTo("cs_test_123");

        assertThat(appointments.saved).hasSize(2);
        Appointment savedAppointment = appointments.saved.get(0);
        Appointment updatedAppointment = appointments.saved.get(1);
        assertThat(savedAppointment.getCustomerName()).isEqualTo("Maya Rivera");
        assertThat(savedAppointment.getServiceName()).isEqualTo("Silk Press");
        assertThat(savedAppointment.getDepositCents()).isEqualTo(2500);
        assertThat(savedAppointment.getDonationCents()).isEqualTo(500);
        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.PENDING_PAYMENT);
        assertThat(updatedAppointment.getStripeSessionId()).isEqualTo("cs_test_123");
        assertThat(activity.messages).containsExactly("Started checkout for Maya Rivera on 2026-05-14.");
    }

    @Test
    void confirmRejectsMismatchedStripeSession() {
        Appointment appointment = pendingAppointment();
        appointment.setStripeSessionId("cs_expected");
        appointments.appointmentById = Optional.of(appointment);

        assertThatThrownBy(() -> appointmentService.confirm(42L, "cs_wrong"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");

        assertThat(stripe.retrieveCheckoutCalls).isZero();
        assertThat(appointments.saved).isEmpty();
    }

    @Test
    void confirmMarksAppointmentPaidWhenStripeSessionIsPaid() {
        Appointment appointment = pendingAppointment();
        appointment.setStripeSessionId("cs_paid");
        appointments.appointmentById = Optional.of(appointment);
        availability.openAndUnbooked = true;
        stripe.retrievedSession = new StripeCheckoutSession("cs_paid", null, "paid", "pi_123");

        AppointmentResponse response = appointmentService.confirm(42L, "cs_paid");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(appointments.saved.get(0).getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(activity.messages).containsExactly("Booked Silk Press for Maya Rivera on 2026-05-14 at 10:00 AM.");
    }

    @Test
    void moveRejectsUnavailableDestinationSlot() {
        Appointment appointment = pendingAppointment();
        MoveAppointmentRequest request = new MoveAppointmentRequest(LocalDate.of(2026, 5, 16), "2:00 PM");
        appointments.appointmentById = Optional.of(appointment);
        availability.openAndUnbooked = false;

        assertThatThrownBy(() -> appointmentService.move(42L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");

        assertThat(appointments.saved).isEmpty();
    }

    private AppointmentRequest bookingRequest() {
        return new AppointmentRequest(
                "Maya Rivera",
                "maya@example.com",
                "555-1212",
                "Silk Press",
                "Shoulder length",
                LocalDate.of(2026, 5, 14),
                "10:00 AM",
                500
        );
    }

    private Appointment pendingAppointment() {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("Maya Rivera");
        appointment.setEmail("maya@example.com");
        appointment.setPhone("555-1212");
        appointment.setServiceName("Silk Press");
        appointment.setNotes("Shoulder length");
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 14));
        appointment.setAppointmentTime("10:00 AM");
        appointment.setDepositCents(2500);
        appointment.setDonationCents(500);
        return appointment;
    }

    private static class FakeAppointmentRepository {
        private final List<Appointment> saved = new ArrayList<>();
        private Optional<Appointment> appointmentById = Optional.empty();

        AppointmentRepository repository() {
            return (AppointmentRepository) Proxy.newProxyInstance(
                    AppointmentRepository.class.getClassLoader(),
                    new Class<?>[]{AppointmentRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> {
                            Appointment appointment = (Appointment) args[0];
                            saved.add(appointment);
                            yield appointment;
                        }
                        case "findById" -> appointmentById;
                        case "toString" -> "FakeAppointmentRepository";
                        default -> throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                    }
            );
        }
    }

    private static class FakeAvailabilityService extends AvailabilityService {
        private boolean openAndUnbooked;

        FakeAvailabilityService() {
            super(null, null, null);
        }

        @Override
        public boolean isOpenAndUnbooked(LocalDate date, String time) {
            return openAndUnbooked;
        }
    }

    private static class FakeStripeService extends StripeService {
        private int createCheckoutCalls;
        private int retrieveCheckoutCalls;
        private StripeCheckoutSession checkoutSession;
        private StripeCheckoutSession retrievedSession;

        FakeStripeService() {
            super("", "", RestClient.builder());
        }

        @Override
        public StripeCheckoutSession createCheckoutSession(
                Long appointmentId,
                String customerEmail,
                String serviceName,
                int depositCents,
                int donationCents
        ) {
            createCheckoutCalls += 1;
            return checkoutSession;
        }

        @Override
        public StripeCheckoutSession retrieveCheckoutSession(String sessionId) {
            retrieveCheckoutCalls += 1;
            return retrievedSession;
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
