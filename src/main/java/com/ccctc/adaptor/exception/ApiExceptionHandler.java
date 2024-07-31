package com.ccctc.adaptor.exception;

import com.ccctc.adaptor.model.ErrorResource;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

/**
 * Created by sideadmin on 1/28/16.
 */
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({InvalidRequestException.class})
    protected ResponseEntity<Object> handleInvalidRequestException(InvalidRequestException e, WebRequest request) {

        ErrorResource error = new ErrorResource(e.getCode() != null ? e.getCode().toString() : null,
                e.getMessage() == null ? e.getClass().getName() : e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, InvalidRequestException.class.getAnnotation(ResponseStatus.class).value(), request);
    }

    @ExceptionHandler({EntityNotFoundException.class})
    protected ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException e, WebRequest request) {

        ErrorResource error = new ErrorResource(e.getCode(), e.getMessage() == null ? e.getClass().getName() : e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, EntityNotFoundException.class.getAnnotation(ResponseStatus.class).value(), request);
    }

    @ExceptionHandler({InternalServerException.class})
    protected ResponseEntity<Object> handleInternalServerException(InternalServerException e, WebRequest request) {

        ErrorResource error = new ErrorResource(e.getCode() != null ? e.getCode().toString() : null,
                e.getMessage() == null ? e.getClass().getName() : e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, InternalServerException.class.getAnnotation(ResponseStatus.class).value(), request);
    }

    @ExceptionHandler({RuntimeException.class})
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException e, WebRequest request) {

        Throwable cause = e;

        // unwrap invoker exceptions from groovy
        if (cause instanceof InvokerInvocationException)
            cause = e.getCause();

        // unwrap concurrent exceptions
        if (cause instanceof ExecutionException)
            cause = cause.getCause();

        // unwrap undeclared throwable exceptions
        while (cause instanceof UndeclaredThrowableException) {
            if (cause.getCause() == null) break;
            cause = cause.getCause();
        }

        ErrorResource error = new ErrorResource("serverException", cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ServiceUnavailableException.class})
    protected ResponseEntity<Object> handleServiceUnavailableException(ServiceUnavailableException e, WebRequest request) {

        ErrorResource error = new ErrorResource(e.getCode(), e.getMessage() == null ? e.getClass().getName() : e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, ServiceUnavailableException.class.getAnnotation(ResponseStatus.class).value(), request);
    }

    @ExceptionHandler({EntityConflictException.class})
    protected ResponseEntity<Object> handleEntityConflictException(EntityConflictException e, WebRequest request) {

        ErrorResource error = new ErrorResource(e.getCode(), e.getMessage() == null ? e.getClass().getName() : e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        logger.error("Exception encounter: ", e);

        return handleExceptionInternal(e, error, headers, EntityConflictException.class.getAnnotation(ResponseStatus.class).value(), request);
    }
}