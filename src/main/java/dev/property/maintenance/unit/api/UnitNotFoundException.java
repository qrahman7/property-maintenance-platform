package dev.property.maintenance.unit.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class UnitNotFoundException extends ErrorResponseException {

    public UnitNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Unit " + id + " was not found."),
                null);
    }
}
