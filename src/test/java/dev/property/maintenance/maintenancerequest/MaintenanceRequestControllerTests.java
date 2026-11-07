package dev.property.maintenance.maintenancerequest;

import dev.property.maintenance.maintenancerequest.api.MaintenanceRequestController;
import dev.property.maintenance.maintenancerequest.domain.MaintenanceStatus;
import dev.property.maintenance.maintenancerequest.domain.ReportedUrgency;
import dev.property.maintenance.maintenancerequest.dto.CreateMaintenanceRequestRequest;
import dev.property.maintenance.maintenancerequest.dto.MaintenanceRequestResponse;
import dev.property.maintenance.maintenancerequest.service.MaintenanceRequestService;
import dev.property.maintenance.unit.api.UnitNotFoundException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceRequestController.class)
class MaintenanceRequestControllerTests {

    private static final String CREATE_JSON = """
            {"description": "Demo kitchen faucet is dripping.", "reportedUrgency": "MEDIUM"}
            """;
    private static final CreateMaintenanceRequestRequest CREATE =
            new CreateMaintenanceRequestRequest("Demo kitchen faucet is dripping.", ReportedUrgency.MEDIUM);
    private static final MaintenanceRequestResponse RESPONSE = new MaintenanceRequestResponse(
            9L, 7L, "Demo-101", 42L, CREATE.description(), ReportedUrgency.MEDIUM,
            MaintenanceStatus.OPEN, Instant.parse("2026-01-01T12:00:00Z"));
    private static final String RESPONSE_JSON = """
            {"id":9,"unitId":7,"unitNumber":"Demo-101","propertyId":42,
             "description":"Demo kitchen faucet is dripping.","reportedUrgency":"MEDIUM",
             "status":"OPEN","createdAt":"2026-01-01T12:00:00Z"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaintenanceRequestService service;

    @Test
    void createsRequestWithAbsoluteLocationAndResponse() throws Exception {
        given(service.create(7L, CREATE)).willReturn(RESPONSE);

        mvc.perform(post("/api/units/7/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/maintenance-requests/9"))
                .andExpect(content().json(RESPONSE_JSON))
                .andExpect(jsonPath("$.unit").doesNotExist())
                .andExpect(jsonPath("$.property").doesNotExist());
    }

    @Test
    void locationUsesRequestOriginAndContextPath() throws Exception {
        given(service.create(7L, CREATE)).willReturn(RESPONSE);

        mvc.perform(post(URI.create("https://example.test:8443/demo/api/units/7/maintenance-requests"))
                        .contextPath("/demo").contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://example.test:8443/demo/api/maintenance-requests/9"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankDescription(String description) throws Exception {
        assertValidationProblem(objectMapper.writeValueAsString(
                new CreateMaintenanceRequestRequest(description, ReportedUrgency.LOW)), "description");
    }

    @Test
    void rejectsMissingDescription() throws Exception {
        assertValidationProblem("{\"reportedUrgency\":\"LOW\"}", "description");
    }

    @Test
    void rejectsDescriptionOverMaximum() throws Exception {
        assertValidationProblem(objectMapper.writeValueAsString(
                new CreateMaintenanceRequestRequest("a".repeat(2001), ReportedUrgency.LOW)), "description");
    }

    @Test
    void acceptsMaximumDescriptionLength() throws Exception {
        var request = new CreateMaintenanceRequestRequest("a".repeat(2000), ReportedUrgency.LOW);
        given(service.create(7L, request)).willReturn(new MaintenanceRequestResponse(
                9L, 7L, "Demo-101", 42L, request.description(), request.reportedUrgency(),
                MaintenanceStatus.OPEN, RESPONSE.createdAt()));

        mvc.perform(post("/api/units/7/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(request.description()));
    }

    @Test
    void rejectsMissingUrgency() throws Exception {
        assertValidationProblem("{\"description\":\"Demo repair\"}", "reportedUrgency");
    }

    @Test
    void rejectsInvalidUrgency() throws Exception {
        mvc.perform(post("/api/units/7/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"description":"Demo repair","reportedUrgency":"INVALID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "unitId", "propertyId", "status", "createdAt"})
    void rejectsCallerSuppliedServerManagedFields(String field) throws Exception {
        var request = objectMapper.createObjectNode();
        request.put("description", CREATE.description());
        request.put("reportedUrgency", "MEDIUM");
        request.put(field, "caller-supplied");

        mvc.perform(post("/api/units/7/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(service);
    }

    @Test
    void createReturnsProblemDetailForMissingUnit() throws Exception {
        given(service.create(99L, CREATE)).willThrow(new UnitNotFoundException(99L));

        mvc.perform(post("/api/units/99/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Unit 99 was not found."));
    }

    @Test
    void listsRequestsForUnit() throws Exception {
        given(service.findByUnitId(7L)).willReturn(List.of(RESPONSE));

        mvc.perform(get("/api/units/7/maintenance-requests"))
                .andExpect(status().isOk())
                .andExpect(content().json("[" + RESPONSE_JSON + "]"));
    }

    @Test
    void returnsEmptyArrayForUnitWithoutRequests() throws Exception {
        given(service.findByUnitId(7L)).willReturn(List.of());

        mvc.perform(get("/api/units/7/maintenance-requests"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listReturnsProblemDetailForMissingUnit() throws Exception {
        given(service.findByUnitId(99L)).willThrow(new UnitNotFoundException(99L));

        mvc.perform(get("/api/units/99/maintenance-requests"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Unit 99 was not found."));
    }

    @Test
    void retrievesRequestById() throws Exception {
        given(service.findById(9L)).willReturn(Optional.of(RESPONSE));

        mvc.perform(get("/api/maintenance-requests/9"))
                .andExpect(status().isOk()).andExpect(content().json(RESPONSE_JSON));
    }

    @Test
    void returnsProblemDetailForMissingRequest() throws Exception {
        given(service.findById(99L)).willReturn(Optional.empty());

        mvc.perform(get("/api/maintenance-requests/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Maintenance request 99 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/maintenance-requests/99"));
    }

    private void assertValidationProblem(String request, String field) throws Exception {
        mvc.perform(post("/api/units/7/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verifyNoInteractions(service);
    }
}
