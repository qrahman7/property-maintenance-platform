package dev.property.maintenance.maintenancerequest.dto;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMaintenanceRequestStatusRequest(@NotNull MaintenanceStatus status) {
}
