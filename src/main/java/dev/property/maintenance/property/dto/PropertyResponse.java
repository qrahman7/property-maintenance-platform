package dev.property.maintenance.property.dto;

import dev.property.maintenance.property.domain.Property;

public record PropertyResponse(
        Long id,
        String name,
        String addressLine1,
        String city,
        String state,
        String zipCode
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.getId(), property.getName(), property.getAddressLine1(),
                property.getCity(), property.getState(), property.getZipCode());
    }
}
