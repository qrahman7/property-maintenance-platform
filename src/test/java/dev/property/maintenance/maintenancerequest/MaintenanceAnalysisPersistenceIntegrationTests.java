package dev.property.maintenance.maintenancerequest;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceAnalysis;
import dev.property.maintenance.maintenancerequest.domain.MaintenanceCategory;
import dev.property.maintenance.maintenancerequest.domain.MaintenanceRequest;
import dev.property.maintenance.maintenancerequest.domain.ReportedUrgency;
import dev.property.maintenance.maintenancerequest.domain.Severity;
import dev.property.maintenance.maintenancerequest.domain.SuggestedPriority;
import dev.property.maintenance.maintenancerequest.repository.MaintenanceAnalysisRepository;
import dev.property.maintenance.maintenancerequest.repository.MaintenanceRequestRepository;
import dev.property.maintenance.property.domain.Property;
import dev.property.maintenance.property.repository.PropertyRepository;
import dev.property.maintenance.unit.domain.Unit;
import dev.property.maintenance.unit.repository.UnitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests MaintenanceAnalysis mappings against local PostgreSQL, rolling back test data. */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class MaintenanceAnalysisPersistenceIntegrationTests {

    @Autowired
    private MaintenanceAnalysisRepository analysisRepository;

    @Autowired
    private MaintenanceRequestRepository requestRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private MaintenanceRequest request;

    @BeforeEach
    void createRequest() {
        var property = propertyRepository.saveAndFlush(new Property("Example Property", "100 Example Ave",
                "Sampletown", "GA", "30000"));
        var unit = unitRepository.saveAndFlush(new Unit("Demo-101", property));
        request = requestRepository.saveAndFlush(
                new MaintenanceRequest(unit, "Demo faucet is dripping.", ReportedUrgency.MEDIUM));
    }

    @Test
    void persistsAndFindsAnalysisByMaintenanceRequestId() {
        var saved = analysisRepository.saveAndFlush(new MaintenanceAnalysis(request, MaintenanceCategory.PLUMBING,
                SuggestedPriority.HIGH, Severity.MEDIUM, "Demo leak under the sink."));
        entityManager.clear();

        var loaded = analysisRepository.findByMaintenanceRequestId(request.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getCategory()).isEqualTo(MaintenanceCategory.PLUMBING);
        assertThat(loaded.getSuggestedPriority()).isEqualTo(SuggestedPriority.HIGH);
        assertThat(loaded.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(loaded.getSummary()).isEqualTo("Demo leak under the sink.");
        assertThat(loaded.getAnalyzedAt()).isNotNull().isEqualTo(saved.getAnalyzedAt());
        assertThat(loaded.getMaintenanceRequest().getId()).isEqualTo(request.getId());
        assertThat(loaded.getMaintenanceRequest().getDescription()).isEqualTo("Demo faucet is dripping.");
        var storedEnums = (Object[]) entityManager.createNativeQuery(
                        "select category, suggested_priority, severity from maintenance_analyses where id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(storedEnums).containsExactly("PLUMBING", "HIGH", "MEDIUM");
    }

    @Test
    void rejectsSecondAnalysisForSameMaintenanceRequest() {
        analysisRepository.saveAndFlush(new MaintenanceAnalysis(request, MaintenanceCategory.PLUMBING,
                SuggestedPriority.HIGH, Severity.MEDIUM, "First demo analysis."));

        assertThatThrownBy(() -> analysisRepository.saveAndFlush(new MaintenanceAnalysis(request,
                MaintenanceCategory.OTHER, SuggestedPriority.LOW, Severity.LOW, "Second demo analysis.")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOfSatisfying(SQLException.class, exception ->
                        assertThat(exception.getSQLState()).isEqualTo("23505"))
                .hasMessageContaining("maintenance_request_id");
    }
}
