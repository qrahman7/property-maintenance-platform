package dev.property.maintenance.maintenancerequest.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import dev.property.maintenance.maintenancerequest.domain.ReportedUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceRequestRequest(
        @NotBlank @Size(max = 2000) String description,
        @NotNull ReportedUrgency reportedUrgency
) {
    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unsupported request field: " + name);
    }
}
