package com.ccctc.adaptor.exception;

public abstract class CollegeAdaptorException extends RuntimeException {

    public abstract Object getCode();

    public abstract String getMessage();
}
