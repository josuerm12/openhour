package com.openhour.backend.repository;

import com.openhour.backend.model.AvailabilitySlot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findBySlotDateBetweenOrderBySlotDateAscSlotTimeAsc(LocalDate start, LocalDate end);

    List<AvailabilitySlot> findBySlotDateOrderBySlotTimeAsc(LocalDate slotDate);

    Optional<AvailabilitySlot> findBySlotDateAndSlotTime(LocalDate slotDate, String slotTime);
}
