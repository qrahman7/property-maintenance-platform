package dev.property.maintenance.maintenancerequest;

import dev.property.maintenance.maintenancerequest.domain.MaintenanceStatus;
import dev.property.maintenance.maintenancerequest.domain.ReportedUrgency;
import dev.property.maintenance.maintenancerequest.dto.CreateMaintenanceRequestRequest;
import dev.property.maintenance.maintenancerequest.dto.MaintenanceRequestResponse;
import dev.property.maintenance.maintenancerequest.repository.MaintenanceRequestRepository;
import dev.property.maintenance.property.domain.Property;
import dev.property.maintenance.property.repository.PropertyRepository;
import dev.property.maintenance.unit.domain.Unit;
import dev.property.maintenance.unit.repository.UnitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests the API and persistence against local PostgreSQL, rolling back test data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class MaintenanceRequestApiIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MaintenanceRequestRepository repository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(ReportedUrgency.class)
    void createsPersistsAndRetrievesRequest(ReportedUrgency urgency) throws Exception {
        var unit = createUnit();
        long unitId = unit.getId();
        long propertyId = unit.getProperty().getId();
        var before = Instant.now().minusSeconds(1);
        var response = createRequest(unitId, "Demo faucet is dripping.", urgency);
        var after = Instant.now().plusSeconds(1);

        assertThat(response.id()).isPositive();
        assertThat(response.unitId()).isEqualTo(unitId);
        assertThat(response.unitNumber()).isEqualTo("Demo-101");
        assertThat(response.propertyId()).isEqualTo(propertyId);
        assertThat(response.status()).isEqualTo(MaintenanceStatus.OPEN);
        assertThat(response.createdAt()).isBetween(before, after);

        entityManager.flush();
        entityManager.clear();
        var saved = repository.findById(response.id()).orElseThrow();
        assertThat(saved.getDescription()).isEqualTo("Demo faucet is dripping.");
        assertThat(saved.getReportedUrgency()).isEqualTo(urgency);
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.OPEN);
        assertThat(saved.getCreatedAt()).isEqualTo(response.createdAt());
        assertThat(saved.getUnit().getId()).isEqualTo(unitId);
        assertThat(saved.getUnit().getProperty().getId()).isEqualTo(propertyId);
        var storedEnums = (Object[]) entityManager.createNativeQuery(
                        "select reported_urgency, status from maintenance_requests where id = :id")
                .setParameter("id", response.id())
                .getSingleResult();
        assertThat(storedEnums).containsExactly(urgency.name(), "OPEN");
        entityManager.clear();

        mvc.perform(get("/api/maintenance-requests/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.unitId").value(unitId))
                .andExpect(jsonPath("$.unitNumber").value("Demo-101"))
                .andExpect(jsonPath("$.propertyId").value(propertyId))
                .andExpect(jsonPath("$.description").value("Demo faucet is dripping."))
                .andExpect(jsonPath("$.reportedUrgency").value(urgency.name()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").value(response.createdAt().toString()))
                .andExpect(jsonPath("$.unit").doesNotExist())
                .andExpect(jsonPath("$.property").doesNotExist());
    }

    @Test
    void listsOnlyRequestsForRequestedUnit() throws Exception {
        var unit = createUnit();
        var otherUnit = unitRepository.saveAndFlush(new Unit("Demo-102", unit.getProperty()));
        createRequest(unit.getId(), "Demo faucet repair", ReportedUrgency.LOW);
        createRequest(unit.getId(), "Demo light repair", ReportedUrgency.HIGH);
        createRequest(otherUnit.getId(), "Other demo repair", ReportedUrgency.MEDIUM);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/units/{unitId}/maintenance-requests", unit.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].description", containsInAnyOrder("Demo faucet repair", "Demo light repair")))
                .andExpect(jsonPath("$[0].unitId").value(unit.getId()))
                .andExpect(jsonPath("$[1].unitId").value(unit.getId()))
                .andExpect(jsonPath("$[0].unit").doesNotExist())
                .andExpect(jsonPath("$[0].property").doesNotExist());
    }

    @Test
    void existingUnitWithoutRequestsReturnsEmptyArray() throws Exception {
        mvc.perform(get("/api/units/{unitId}/maintenance-requests", createUnit().getId()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void cannotCreateUnderMissingUnit() throws Exception {
        long countBefore = repository.count();
        mvc.perform(post("/api/units/-1/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"description":"Demo repair","reportedUrgency":"LOW"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unit -1 was not found."));
        assertThat(repository.count()).isEqualTo(countBefore);
    }

    @Test
    void listingMissingUnitReturnsNotFound() throws Exception {
        mvc.perform(get("/api/units/-1/maintenance-requests"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unit -1 was not found."));
    }

    @Test
    void gettingMissingRequestReturnsNotFound() throws Exception {
        mvc.perform(get("/api/maintenance-requests/-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Maintenance request -1 was not found."));
    }

    private Unit createUnit() {
        var property = propertyRepository.saveAndFlush(new Property("Example Property", "100 Example Ave",
                "Sampletown", "GA", "30000"));
        return unitRepository.saveAndFlush(new Unit("Demo-101", property));
    }

    private MaintenanceRequestResponse createRequest(long unitId, String description, ReportedUrgency urgency)
            throws Exception {
        var result = mvc.perform(post("/api/units/{unitId}/maintenance-requests", unitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMaintenanceRequestRequest(description, urgency))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unit").doesNotExist())
                .andExpect(jsonPath("$.property").doesNotExist())
                .andReturn();
        var response = objectMapper.readValue(result.getResponse().getContentAsString(), MaintenanceRequestResponse.class);
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost/api/maintenance-requests/" + response.id());
        return response;
    }
}
