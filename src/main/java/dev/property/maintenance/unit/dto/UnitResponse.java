package dev.property.maintenance.unit.dto;

import dev.property.maintenance.unit.domain.Unit;

public record UnitResponse(Long id, String unitNumber, Long propertyId) {

    public static UnitResponse from(Unit unit) {
        return new UnitResponse(unit.getId(), unit.getUnitNumber(), unit.getProperty().getId());
    }
}
