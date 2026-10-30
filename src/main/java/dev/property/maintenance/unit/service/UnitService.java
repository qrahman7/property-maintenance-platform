package dev.property.maintenance.unit.service;

import dev.property.maintenance.property.api.PropertyNotFoundException;
import dev.property.maintenance.property.repository.PropertyRepository;
import dev.property.maintenance.unit.domain.Unit;
import dev.property.maintenance.unit.dto.CreateUnitRequest;
import dev.property.maintenance.unit.dto.UnitResponse;
import dev.property.maintenance.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository repository;
    private final PropertyRepository propertyRepository;

    public UnitService(UnitRepository repository, PropertyRepository propertyRepository) {
        this.repository = repository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional
    public UnitResponse create(Long propertyId, CreateUnitRequest request) {
        var property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));
        return UnitResponse.from(repository.save(new Unit(request.unitNumber(), property)));
    }

    public List<UnitResponse> findByPropertyId(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new PropertyNotFoundException(propertyId);
        }
        return repository.findByPropertyId(propertyId).stream().map(UnitResponse::from).toList();
    }

    public Optional<UnitResponse> findById(Long id) {
        return repository.findById(id).map(UnitResponse::from);
    }
}
