package dev.property.maintenance.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String addressLine1,
        @NotBlank @Size(max = 255) String city,
        @NotBlank @Size(max = 255) String state,
        @NotBlank @Size(max = 255) String zipCode
) {
}
