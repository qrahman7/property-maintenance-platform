package dev.property.maintenance.maintenancerequest.repository;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceAnalysisRepository extends JpaRepository<MaintenanceAnalysis, Long> {

    Optional<MaintenanceAnalysis> findByMaintenanceRequestId(Long maintenanceRequestId);
}
