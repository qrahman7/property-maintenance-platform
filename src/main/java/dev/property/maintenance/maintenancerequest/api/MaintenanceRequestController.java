package dev.property.maintenance.maintenancerequest.api;

import dev.property.maintenance.maintenancerequest.dto.CreateMaintenanceRequestRequest;
import dev.property.maintenance.maintenancerequest.dto.MaintenanceRequestResponse;
import dev.property.maintenance.maintenancerequest.service.MaintenanceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }

    @PostMapping("/units/{unitId}/maintenance-requests")
    public ResponseEntity<MaintenanceRequestResponse> create(@PathVariable Long unitId,
            @Valid @RequestBody CreateMaintenanceRequestRequest request) {
        var maintenanceRequest = service.create(unitId, request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/maintenance-requests/{id}")
                .buildAndExpand(maintenanceRequest.id())
                .toUri();
        return ResponseEntity.created(location).body(maintenanceRequest);
    }

    @GetMapping("/units/{unitId}/maintenance-requests")
    public List<MaintenanceRequestResponse> findByUnitId(@PathVariable Long unitId) {
        return service.findByUnitId(unitId);
    }

    @GetMapping("/maintenance-requests/{id}")
    public MaintenanceRequestResponse findById(@PathVariable Long id) {
        return service.findById(id).orElseThrow(() -> new MaintenanceRequestNotFoundException(id));
    }
}
