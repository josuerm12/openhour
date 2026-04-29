package com.openhour.backend.repository;

import com.openhour.backend.model.Appointment;
import com.openhour.backend.model.AppointmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByStatusOrderByAppointmentDateAscAppointmentTimeAsc(AppointmentStatus status);

    boolean existsByAppointmentDateAndAppointmentTimeAndStatus(LocalDate appointmentDate, String appointmentTime, AppointmentStatus status);

    Optional<Appointment> findByStripeSessionId(String stripeSessionId);
}
