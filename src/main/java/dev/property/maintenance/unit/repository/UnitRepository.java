package dev.property.maintenance.unit.repository;

import dev.property.maintenance.unit.domain.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByPropertyId(Long propertyId);
}
