package dev.property.maintenance.maintenancerequest.service;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceRequest;
import dev.property.maintenance.maintenancerequest.dto.CreateMaintenanceRequestRequest;
import dev.property.maintenance.maintenancerequest.dto.MaintenanceRequestResponse;
import dev.property.maintenance.maintenancerequest.repository.MaintenanceRequestRepository;
import dev.property.maintenance.unit.api.UnitNotFoundException;
import dev.property.maintenance.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository repository;
    private final UnitRepository unitRepository;

    public MaintenanceRequestService(MaintenanceRequestRepository repository, UnitRepository unitRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
    }

    @Transactional
    public MaintenanceRequestResponse create(Long unitId, CreateMaintenanceRequestRequest request) {
        var unit = unitRepository.findById(unitId).orElseThrow(() -> new UnitNotFoundException(unitId));
        var maintenanceRequest = new MaintenanceRequest(unit, request.description(), request.reportedUrgency());
        return MaintenanceRequestResponse.from(repository.save(maintenanceRequest));
    }

    public List<MaintenanceRequestResponse> findByUnitId(Long unitId) {
        if (!unitRepository.existsById(unitId)) {
            throw new UnitNotFoundException(unitId);
        }
        return repository.findByUnitId(unitId).stream().map(MaintenanceRequestResponse::from).toList();
    }

    public Optional<MaintenanceRequestResponse> findById(Long id) {
        return repository.findById(id).map(MaintenanceRequestResponse::from);
    }
}
