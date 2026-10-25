package dev.property.maintenance.property.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class PropertyNotFoundException extends ErrorResponseException {

    public PropertyNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Property " + id + " was not found."),
                null);
    }
}
