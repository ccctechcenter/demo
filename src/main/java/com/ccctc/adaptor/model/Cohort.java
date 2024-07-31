package com.ccctc.adaptor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by User on 4/17/2017.
 */
@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cohort implements Serializable {

    @ApiModelProperty(value = "Cohort Type", example = "COURSE_EXCHANGE", dataType = "CohortType")
    private CohortTypeEnum name;

    @ApiModelProperty(value = "Description", example="Course Exchange")
    private String description;

    public Cohort() {

    }

    public Cohort(Builder builder) {
        setName(builder.name);
    }

    public CohortTypeEnum getName() {
        return name;
    }

    public String getDescription() {
        return this.name.getDescription();
    }

    public void setName(CohortTypeEnum name) {
        this.name = name;
    }

    public static final class Builder {
        private CohortTypeEnum name;

        public Builder() {
        }

        public Builder name(CohortTypeEnum val) {
            name = val;
            return this;
        }

        public Cohort build() {
            return new Cohort(this);
        }
    }

    @Override
    public String toString() {
        return "Cohort{" +
                "name=" + name +
                ", description='" + description + '\'' +
                '}';
    }
}