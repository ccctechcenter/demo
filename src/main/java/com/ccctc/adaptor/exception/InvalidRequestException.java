package com.ccctc.adaptor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends CollegeAdaptorException {

    private Errors code;
    private String message;

    public InvalidRequestException(String message) {
        this.code = Errors.invalidRequest;
        this.message = message;
    }

    public InvalidRequestException(Errors code, String message) {
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
        // general errors
        invalidRequest, invalidSearchCriteria, multipleResultsFound,
        // not found
        misCodeNotFound, collegeNotFound, studentNotFound, personNotFound, termNotFound, courseNotFound, sectionNotFound, enrollmentNotFound,
        // invalid values
        invalidTestName, invalidTestMapping, invalidPlacementCourse, invalidCohort,
        // enrollment errors
        generalEnrollmentError, alreadyEnrolled,
        // other errors
        noFinancialAidRecord, sisQueryError
    }
}