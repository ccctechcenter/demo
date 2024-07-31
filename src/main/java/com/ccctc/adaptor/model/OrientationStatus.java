package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;

@ApiModel
public enum OrientationStatus {
    UNKNOWN, // When Student mapping found, but no matching orientation status for given term
    RESTRICTED, // When student has not completed for given term, and should not
    OPTIONAL, // When student has not completed for given term, and could if they desired to
    REQUIRED, // When student has not completed for given term, but must in order to enroll
    COMPLETED // When student has finished all orientation tasks. no further action required
}
