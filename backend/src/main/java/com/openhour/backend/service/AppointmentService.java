package com.openhour.backend.service;

import com.openhour.backend.dto.AppointmentRequest;
import com.openhour.backend.dto.AppointmentResponse;
import com.openhour.backend.dto.CheckoutResponse;
import com.openhour.backend.dto.MoveAppointmentRequest;
import com.openhour.backend.dto.ServiceOfferingDTO;
import com.openhour.backend.exception.BadRequestException;
import com.openhour.backend.exception.NotFoundException;
import com.openhour.backend.model.Appointment;
import com.openhour.backend.model.AppointmentStatus;
import com.openhour.backend.model.PaymentStatus;
import com.openhour.backend.repository.AppointmentRepository;
import com.openhour.backend.service.StripeService.StripeCheckoutSession;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final CatalogService catalogService;
    private final StripeService stripeService;
    private final ActivityService activityService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            CatalogService catalogService,
            StripeService stripeService,
            ActivityService activityService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.catalogService = catalogService;
        this.stripeService = stripeService;
        this.activityService = activityService;
    }

    public List<AppointmentResponse> listConfirmed() {
        return appointmentRepository.findByStatusOrderByAppointmentDateAscAppointmentTimeAsc(AppointmentStatus.CONFIRMED).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional
    public CheckoutResponse createCheckout(AppointmentRequest request) {
        if (!availabilityService.isOpenAndUnbooked(request.date(), request.time())) {
            throw new BadRequestException("That appointment slot is no longer available.");
        }

        ServiceOfferingDTO service = catalogService.requireService(request.service());
        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.name());
        appointment.setEmail(request.email());
        appointment.setPhone(request.phone());
        appointment.setServiceName(request.service());
        appointment.setNotes(request.notes());
        appointment.setAppointmentDate(request.date());
        appointment.setAppointmentTime(request.time());
        appointment.setDepositCents(service.depositCents());
        appointment.setDonationCents(request.donationCents());
        appointment = appointmentRepository.save(appointment);

        StripeCheckoutSession checkoutSession = stripeService.createCheckoutSession(
                appointment.getId(),
                appointment.getEmail(),
                appointment.getServiceName(),
                appointment.getDepositCents(),
                appointment.getDonationCents()
        );
        appointment.setStripeSessionId(checkoutSession.id());
        appointment.touch();
        appointmentRepository.save(appointment);
        activityService.record("Started checkout for " + appointment.getCustomerName() + " on " + appointment.getAppointmentDate() + ".");
        return new CheckoutResponse(appointment.getId(), checkoutSession.url(), checkoutSession.id());
    }

    @Transactional
    public AppointmentResponse confirm(Long appointmentId, String sessionId) {
        Appointment appointment = requireAppointment(appointmentId);
        if (!sessionId.equals(appointment.getStripeSessionId())) {
            throw new BadRequestException("Stripe session does not match the appointment.");
        }
        if (!availabilityService.isOpenAndUnbooked(appointment.getAppointmentDate(), appointment.getAppointmentTime())
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setPaymentStatus(PaymentStatus.CANCELLED);
            appointment.touch();
            throw new BadRequestException("That appointment slot was already taken.");
        }

        StripeCheckoutSession checkoutSession = stripeService.retrieveCheckoutSession(sessionId);
        if (!"paid".equals(checkoutSession.paymentStatus())) {
            throw new BadRequestException("Payment has not been completed yet.");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setPaymentStatus(PaymentStatus.PAID);
        appointment.setStripePaymentIntentId(checkoutSession.paymentIntentId());
        appointment.touch();
        activityService.record("Booked " + appointment.getServiceName() + " for " + appointment.getCustomerName()
                + " on " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".");
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long appointmentId) {
        Appointment appointment = requireAppointment(appointmentId);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.touch();
        activityService.record("Cancelled " + appointment.getServiceName() + " for " + appointment.getCustomerName()
                + " on " + appointment.getAppointmentDate() + ".");
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse move(Long appointmentId, MoveAppointmentRequest request) {
        Appointment appointment = requireAppointment(appointmentId);
        if (!availabilityService.isOpenAndUnbooked(request.date(), request.time())) {
            throw new BadRequestException("That appointment slot is not available.");
        }
        appointment.setAppointmentDate(request.date());
        appointment.setAppointmentTime(request.time());
        appointment.touch();
        activityService.record("Changed " + appointment.getCustomerName() + "'s appointment to "
                + request.time() + " on " + request.date() + ".");
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public AppointmentResponse get(Long appointmentId) {
        return AppointmentResponse.from(requireAppointment(appointmentId));
    }

    private Appointment requireAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
    }
}
