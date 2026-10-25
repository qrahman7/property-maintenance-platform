package dev.property.maintenance.property.service;

import dev.property.maintenance.property.domain.Property;
import dev.property.maintenance.property.dto.CreatePropertyRequest;
import dev.property.maintenance.property.dto.PropertyResponse;
import dev.property.maintenance.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository repository;

    public PropertyService(PropertyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PropertyResponse create(CreatePropertyRequest request) {
        var property = new Property(request.name(), request.addressLine1(), request.city(),
                request.state(), request.zipCode());
        return PropertyResponse.from(repository.save(property));
    }

    public List<PropertyResponse> findAll() {
        return repository.findAll().stream().map(PropertyResponse::from).toList();
    }

    public Optional<PropertyResponse> findById(Long id) {
        return repository.findById(id).map(PropertyResponse::from);
    }
}
