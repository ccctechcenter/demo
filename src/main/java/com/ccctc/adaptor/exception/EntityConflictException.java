package com.ccctc.adaptor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class EntityConflictException extends CollegeAdaptorException {

    private static final String CODE = "entityAlreadyExists";
    private String message;

    public EntityConflictException(String message) {
        this.message = message;
    }

    public String getCode() {
        return CODE;
    }

    public String getMessage() {
        return message;
    }
}