package dev.property.maintenance.unit.api;

import dev.property.maintenance.unit.dto.CreateUnitRequest;
import dev.property.maintenance.unit.dto.UnitResponse;
import dev.property.maintenance.unit.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UnitController {

    private final UnitService service;

    public UnitController(UnitService service) {
        this.service = service;
    }

    @PostMapping("/properties/{propertyId}/units")
    public ResponseEntity<UnitResponse> create(@PathVariable Long propertyId,
            @Valid @RequestBody CreateUnitRequest request) {
        var unit = service.create(propertyId, request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/units/{id}")
                .buildAndExpand(unit.id())
                .toUri();
        return ResponseEntity.created(location).body(unit);
    }

    @GetMapping("/properties/{propertyId}/units")
    public List<UnitResponse> findByPropertyId(@PathVariable Long propertyId) {
        return service.findByPropertyId(propertyId);
    }

    @GetMapping("/units/{id}")
    public UnitResponse findById(@PathVariable Long id) {
        return service.findById(id).orElseThrow(() -> new UnitNotFoundException(id));
    }
}
