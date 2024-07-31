package com.ccctc.adaptor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends CollegeAdaptorException {

    private static final String CODE = "serviceUnavailable";
    private String message;

    public ServiceUnavailableException(String message) {
        this.message = message;
    }

    public String getCode() {
        return CODE;
    }

    public String getMessage() {
        return message;
    }
}