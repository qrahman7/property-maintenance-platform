package dev.property.maintenance.unit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(@NotBlank @Size(max = 50) String unitNumber) {
}
