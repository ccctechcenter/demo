package com.ccctc.adaptor.model;

/**
 * Created by sideadmin on 1/28/16.
 */
public class ErrorResource {

    String code;

    String message;

    public ErrorResource() {}

    public ErrorResource(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
