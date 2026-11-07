package dev.property.maintenance.maintenancerequest.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class MaintenanceRequestNotFoundException extends ErrorResponseException {

    public MaintenanceRequestNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Maintenance request " + id + " was not found."),
                null);
    }
}
