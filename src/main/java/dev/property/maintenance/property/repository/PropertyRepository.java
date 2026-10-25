package dev.property.maintenance.property.repository;

import dev.property.maintenance.property.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
}
