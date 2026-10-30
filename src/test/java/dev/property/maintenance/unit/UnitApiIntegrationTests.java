package dev.property.maintenance.unit;

import dev.property.maintenance.unit.dto.CreateUnitRequest;
import dev.property.maintenance.unit.dto.UnitResponse;
import dev.property.maintenance.unit.repository.UnitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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
class UnitApiIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UnitRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void createsPersistsAndRetrievesUnitUnderProperty() throws Exception {
        long propertyId = createProperty();
        var unit = createUnit(propertyId, "Demo-101");
        assertThat(unit.id()).isPositive();
        assertThat(unit.propertyId()).isEqualTo(propertyId);

        entityManager.flush();
        entityManager.clear();
        var saved = repository.findById(unit.id()).orElseThrow();
        assertThat(saved.getUnitNumber()).isEqualTo("Demo-101");
        assertThat(saved.getProperty().getId()).isEqualTo(propertyId);
        entityManager.clear();

        mvc.perform(get("/api/units/{id}", unit.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(unit.id()))
                .andExpect(jsonPath("$.unitNumber").value("Demo-101"))
                .andExpect(jsonPath("$.propertyId").value(propertyId))
                .andExpect(jsonPath("$.property").doesNotExist());
    }

    @Test
    void listsOnlyUnitsBelongingToRequestedProperty() throws Exception {
        long propertyId = createProperty();
        createUnit(propertyId, "Demo-101");
        createUnit(propertyId, "Demo-102");
        createUnit(createProperty(), "Other-201");
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/properties/{propertyId}/units", propertyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].unitNumber", containsInAnyOrder("Demo-101", "Demo-102")))
                .andExpect(jsonPath("$[0].propertyId").value(propertyId))
                .andExpect(jsonPath("$[1].propertyId").value(propertyId));
    }

    @Test
    void listsEmptyArrayForExistingPropertyWithoutUnits() throws Exception {
        mvc.perform(get("/api/properties/{propertyId}/units", createProperty()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void cannotCreateUnitUnderNonexistentProperty() throws Exception {
        long countBefore = repository.count();

        mvc.perform(post("/api/properties/-1/units")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"unitNumber": "Demo-101"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Property -1 was not found."));

        assertThat(repository.count()).isEqualTo(countBefore);
    }

    @Test
    void listingNonexistentPropertyReturnsNotFoundInsteadOfEmptyArray() throws Exception {
        mvc.perform(get("/api/properties/-1/units"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Property -1 was not found."));
    }

    private long createProperty() throws Exception {
        var result = mvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "name": "Example Property",
                                  "addressLine1": "100 Example Ave",
                                  "city": "Sampletown",
                                  "state": "GA",
                                  "zipCode": "30000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private UnitResponse createUnit(long propertyId, String unitNumber) throws Exception {
        var result = mvc.perform(post("/api/properties/{propertyId}/units", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUnitRequest(unitNumber))))
                .andExpect(status().isCreated())
                .andReturn();
        var unit = objectMapper.readValue(result.getResponse().getContentAsString(), UnitResponse.class);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/api/units/" + unit.id());
        return unit;
    }
}
