package dev.property.maintenance.maintenancerequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "maintenance_analyses")
public class MaintenanceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_request_id", nullable = false, unique = true)
    private MaintenanceRequest maintenanceRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestedPriority suggestedPriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant analyzedAt;

    protected MaintenanceAnalysis() {
    }

    public MaintenanceAnalysis(MaintenanceRequest maintenanceRequest, MaintenanceCategory category,
            SuggestedPriority suggestedPriority, Severity severity, String summary) {
        this.maintenanceRequest = maintenanceRequest;
        this.category = category;
        this.suggestedPriority = suggestedPriority;
        this.severity = severity;
        this.summary = summary;
        this.analyzedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public MaintenanceRequest getMaintenanceRequest() { return maintenanceRequest; }
    public MaintenanceCategory getCategory() { return category; }
    public SuggestedPriority getSuggestedPriority() { return suggestedPriority; }
    public Severity getSeverity() { return severity; }
    public String getSummary() { return summary; }
    public Instant getAnalyzedAt() { return analyzedAt; }
}
