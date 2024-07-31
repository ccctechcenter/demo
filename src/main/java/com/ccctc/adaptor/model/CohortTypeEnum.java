package com.ccctc.adaptor.model;

/**
 * Created by User on 4/17/2017.
 */
public enum CohortTypeEnum {

    COURSE_EXCHANGE("Course Exchange");

    private final String description;

    private CohortTypeEnum(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
