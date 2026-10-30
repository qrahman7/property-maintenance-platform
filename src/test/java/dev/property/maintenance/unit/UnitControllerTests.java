package dev.property.maintenance.unit;

import dev.property.maintenance.property.api.PropertyNotFoundException;
import dev.property.maintenance.unit.api.UnitController;
import dev.property.maintenance.unit.dto.CreateUnitRequest;
import dev.property.maintenance.unit.dto.UnitResponse;
import dev.property.maintenance.unit.service.UnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UnitController.class)
class UnitControllerTests {

    private static final UnitResponse UNIT = new UnitResponse(7L, "Demo-101", 42L);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnitService service;

    @Test
    void createsUnitAndReturnsAbsoluteLocationAndResponse() throws Exception {
        given(service.create(42L, new CreateUnitRequest("Demo-101"))).willReturn(UNIT);

        mvc.perform(post("/api/properties/42/units")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"unitNumber": "Demo-101"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/units/7"))
                .andExpect(content().json("""
                        {"id": 7, "unitNumber": "Demo-101", "propertyId": 42}
                        """));
    }

    @Test
    void locationUsesRequestOriginAndContextPath() throws Exception {
        given(service.create(42L, new CreateUnitRequest("Demo-101"))).willReturn(UNIT);

        mvc.perform(post(URI.create("https://example.test:8443/maintenance/api/properties/42/units"))
                        .contextPath("/maintenance")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"unitNumber": "Demo-101"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://example.test:8443/maintenance/api/units/7"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankUnitNumber(String unitNumber) throws Exception {
        assertValidationProblem(objectMapper.writeValueAsString(new CreateUnitRequest(unitNumber)),
                "must not be blank");
    }

    @Test
    void rejectsMissingUnitNumber() throws Exception {
        assertValidationProblem("{}", "must not be blank");
    }

    @Test
    void rejectsUnitNumberOver50Characters() throws Exception {
        assertValidationProblem(objectMapper.writeValueAsString(new CreateUnitRequest("a".repeat(51))),
                "size must be between 0 and 50");
    }

    @Test
    void acceptsUnitNumberAtMaximumLength() throws Exception {
        var request = new CreateUnitRequest("a".repeat(50));
        given(service.create(42L, request)).willReturn(new UnitResponse(7L, request.unitNumber(), 42L));

        mvc.perform(post("/api/properties/42/units")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturnsProblemDetailForMissingProperty() throws Exception {
        given(service.create(99L, new CreateUnitRequest("Demo-101")))
                .willThrow(new PropertyNotFoundException(99L));

        mvc.perform(post("/api/properties/99/units")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"unitNumber": "Demo-101"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Property 99 was not found."));
    }

    @Test
    void retrievesExistingUnit() throws Exception {
        given(service.findById(7L)).willReturn(Optional.of(UNIT));

        mvc.perform(get("/api/units/7"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id": 7, "unitNumber": "Demo-101", "propertyId": 42}
                        """));
    }

    @Test
    void returnsProblemDetailForMissingUnit() throws Exception {
        given(service.findById(99L)).willReturn(Optional.empty());

        mvc.perform(get("/api/units/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Unit 99 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/units/99"));
    }

    @Test
    void listsUnitsForProperty() throws Exception {
        given(service.findByPropertyId(42L)).willReturn(List.of(UNIT));

        mvc.perform(get("/api/properties/42/units"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id": 7, "unitNumber": "Demo-101", "propertyId": 42}]
                        """));
    }

    @Test
    void listsEmptyArrayForExistingPropertyWithoutUnits() throws Exception {
        given(service.findByPropertyId(42L)).willReturn(List.of());

        mvc.perform(get("/api/properties/42/units"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listReturnsProblemDetailForMissingProperty() throws Exception {
        given(service.findByPropertyId(99L)).willThrow(new PropertyNotFoundException(99L));

        mvc.perform(get("/api/properties/99/units"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Property 99 was not found."));
    }

    private void assertValidationProblem(String request, String message) throws Exception {
        mvc.perform(post("/api/properties/42/units")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("unitNumber"))
                .andExpect(jsonPath("$.errors[0].message").value(message));
        verifyNoInteractions(service);
    }
}
