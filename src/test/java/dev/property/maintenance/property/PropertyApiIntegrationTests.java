package dev.property.maintenance.property;

import dev.property.maintenance.property.domain.Property;
import dev.property.maintenance.property.repository.PropertyRepository;
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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests through HTTP, the service and JPA against the local PostgreSQL database.
 * Uses the {@code local} profile so the schema exists; data is rolled back after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class PropertyApiIntegrationTests {

    private static final String CREATE_REQUEST = """
            {
              "name": "Example Property",
              "addressLine1": "100 Example Ave",
              "city": "Sampletown",
              "state": "GA",
              "zipCode": "30000"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PropertyRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void createsPropertyWithGeneratedIdAndPersistsFields() throws Exception {
        var result = mvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(id).isPositive();
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost/api/properties/" + id);
        entityManager.flush();
        entityManager.clear();
        var saved = repository.findById(id).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Example Property");
        assertThat(saved.getAddressLine1()).isEqualTo("100 Example Ave");
        assertThat(saved.getCity()).isEqualTo("Sampletown");
        assertThat(saved.getState()).isEqualTo("GA");
        assertThat(saved.getZipCode()).isEqualTo("30000");
    }

    @Test
    void retrievesAndListsPersistedProperty() throws Exception {
        var property = repository.saveAndFlush(new Property("Example Property", "100 Example Ave",
                "Sampletown", "GA", "30000"));

        mvc.perform(get("/api/properties/{id}", property.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(property.getId()))
                .andExpect(jsonPath("$.zipCode").value("30000"));

        mvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(property.getId().intValue())));
    }

    @Test
    void returnsNotFoundForNonexistentProperty() throws Exception {
        mvc.perform(get("/api/properties/{id}", -1L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
