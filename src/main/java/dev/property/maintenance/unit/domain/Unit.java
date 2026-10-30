package dev.property.maintenance.unit.domain;

import dev.property.maintenance.property.domain.Property;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_number", nullable = false, length = 50)
    private String unitNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    protected Unit() {
    }

    public Unit(String unitNumber, Property property) {
        this.unitNumber = unitNumber;
        this.property = property;
    }

    public Long getId() { return id; }
    public String getUnitNumber() { return unitNumber; }
    public Property getProperty() { return property; }
}
