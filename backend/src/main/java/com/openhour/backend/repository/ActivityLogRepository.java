package com.openhour.backend.repository;

import com.openhour.backend.model.ActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop30ByOrderByCreatedAtDesc();
}
