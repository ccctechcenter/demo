package com.ccctc.adaptor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends CollegeAdaptorException {

    private static final String CODE = "noResultsFound";
    private String message;

    public EntityNotFoundException(String message) {
        this.message = message;
    }

    public String getCode() {
        return CODE;
    }

    public String getMessage() {
        return message;
    }
}