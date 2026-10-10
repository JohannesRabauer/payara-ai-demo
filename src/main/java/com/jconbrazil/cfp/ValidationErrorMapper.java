package com.jconbrazil.cfp;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Map;

// Turns validation errors into {"message": "...", "errors": ["abstractText: size must be ..."]}
@Provider
public class ValidationErrorMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations().stream()
                .map(ValidationErrorMapper::describe)
                .sorted()
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", "Please fix the form", "errors", errors))
                .build();
    }

    // The path looks like "create.newSubmission.email"; keep only the field name.
    private static String describe(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.substring(path.lastIndexOf('.') + 1);
        return field + ": " + violation.getMessage();
    }
}
