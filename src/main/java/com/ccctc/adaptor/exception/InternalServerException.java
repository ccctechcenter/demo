package com.ccctc.adaptor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerException extends CollegeAdaptorException {

    private Errors code;
    private String message;

    public InternalServerException(String message) {
        this.code = Errors.internalServerError;
        this.message = message;
    }

    public InternalServerException(Errors code, String message) {
        this.code = code;
        this.message = message;
    }

    public Errors getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public enum Errors {
        sisQueryError, internalServerError
    }
}