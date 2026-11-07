package dev.property.maintenance.maintenancerequest.dto;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceRequest;
import dev.property.maintenance.maintenancerequest.domain.MaintenanceStatus;
import dev.property.maintenance.maintenancerequest.domain.ReportedUrgency;

import java.time.Instant;

public record MaintenanceRequestResponse(
        Long id,
        Long unitId,
        String unitNumber,
        Long propertyId,
        String description,
        ReportedUrgency reportedUrgency,
        MaintenanceStatus status,
        Instant createdAt
) {
    public static MaintenanceRequestResponse from(MaintenanceRequest request) {
        var unit = request.getUnit();
        return new MaintenanceRequestResponse(request.getId(), unit.getId(), unit.getUnitNumber(),
                unit.getProperty().getId(), request.getDescription(), request.getReportedUrgency(),
                request.getStatus(), request.getCreatedAt());
    }
}
