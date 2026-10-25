package dev.property.maintenance.property;

import dev.property.maintenance.property.api.PropertyController;
import dev.property.maintenance.property.dto.CreatePropertyRequest;
import dev.property.maintenance.property.dto.PropertyResponse;
import dev.property.maintenance.property.service.PropertyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PropertyController.class)
class PropertyControllerTests {

    private static final String CREATE_REQUEST = """
            {
              "name": "Example Property",
              "addressLine1": "100 Example Ave",
              "city": "Sampletown",
              "state": "GA",
              "zipCode": "30000"
            }
            """;

    private static final PropertyResponse PROPERTY = new PropertyResponse(42L, "Example Property",
            "100 Example Ave", "Sampletown", "GA", "30000");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService service;

    @Test
    void createsPropertyAndReturnsAbsoluteLocation() throws Exception {
        var expectedRequest = new CreatePropertyRequest("Example Property", "100 Example Ave",
                "Sampletown", "GA", "30000");
        given(service.create(expectedRequest)).willReturn(PROPERTY);

        mvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/properties/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Example Property"))
                .andExpect(jsonPath("$.addressLine1").value("100 Example Ave"))
                .andExpect(jsonPath("$.city").value("Sampletown"))
                .andExpect(jsonPath("$.state").value("GA"))
                .andExpect(jsonPath("$.zipCode").value("30000"));
    }

    @Test
    void retrievesExistingProperty() throws Exception {
        given(service.findById(42L)).willReturn(Optional.of(PROPERTY));

        mvc.perform(get("/api/properties/{id}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Example Property"));
    }

    @Test
    void listsProperties() throws Exception {
        given(service.findAll()).willReturn(List.of(PROPERTY));

        mvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].name").value("Example Property"));
    }

    @Test
    void returnsProblemDetailForNonexistentProperty() throws Exception {
        given(service.findById(99L)).willReturn(Optional.empty());

        mvc.perform(get("/api/properties/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Property 99 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/properties/99"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "addressLine1", "city", "state", "zipCode"})
    void rejectsBlankRequiredFields(String field) throws Exception {
        var request = (ObjectNode) objectMapper.readTree(CREATE_REQUEST);
        request.put(field, "   ");
        assertValidationProblem(request, field, "must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "addressLine1", "city", "state", "zipCode"})
    void rejectsMissingRequiredFields(String field) throws Exception {
        var request = (ObjectNode) objectMapper.readTree(CREATE_REQUEST);
        request.remove(field);
        assertValidationProblem(request, field, "must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "addressLine1", "city", "state", "zipCode"})
    void rejectsFieldsExceedingColumnLength(String field) throws Exception {
        var request = (ObjectNode) objectMapper.readTree(CREATE_REQUEST);
        request.put(field, "a".repeat(256));
        assertValidationProblem(request, field, "size must be between 0 and 255");
    }

    private void assertValidationProblem(ObjectNode request, String field, String message) throws Exception {
        mvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));
        then(service).should(never()).create(any());
    }
}
