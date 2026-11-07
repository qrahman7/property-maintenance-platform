package dev.property.maintenance.maintenancerequest.repository;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByUnitId(Long unitId);
}
