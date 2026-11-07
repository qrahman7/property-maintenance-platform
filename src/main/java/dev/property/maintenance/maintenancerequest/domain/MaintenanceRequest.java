package dev.property.maintenance.maintenancerequest.domain;

import dev.property.maintenance.unit.domain.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportedUrgency reportedUrgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MaintenanceRequest() {
    }

    public MaintenanceRequest(Unit unit, String description, ReportedUrgency reportedUrgency) {
        this.unit = unit;
        this.description = description;
        this.reportedUrgency = reportedUrgency;
        this.status = MaintenanceStatus.OPEN;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Unit getUnit() { return unit; }
    public String getDescription() { return description; }
    public ReportedUrgency getReportedUrgency() { return reportedUrgency; }
    public MaintenanceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
