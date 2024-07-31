package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by jrscanlon on 12/9/15.
 */
@ApiModel(description = "Method instructor uses to interact with students and the number of hours this method is used.")
public class CourseContact implements Serializable {

    @ApiModelProperty( value = "Method instructor uses to interact with students.")
    private InstructionalMethod instructionalMethod;
    @ApiModelProperty( value = "Number of hours of instruction using the specified instructional method")
    private Float hours;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    private CourseContact(){
    }

    private CourseContact(Builder builder) {
        setInstructionalMethod(builder.instructionalMethod);
        setHours(builder.hours);
    }

    public InstructionalMethod getInstructionalMethod() {
        return instructionalMethod;
    }

    public void setInstructionalMethod(InstructionalMethod instructionalMethod) {
        this.instructionalMethod = instructionalMethod;
    }

    public Float getHours() {
        return hours;
    }

    public void setHours(Float hours) {
        this.hours = hours;
    }

    public static final class Builder {
        private InstructionalMethod instructionalMethod;
        private Float hours;

        public Builder() {
        }

        public Builder instructionalMethod(InstructionalMethod val) {
            instructionalMethod = val;
            return this;
        }

        public Builder hours(Float val) {
            hours = val;
            return this;
        }

        public CourseContact build() {
            return new CourseContact(this);
        }
    }

    @Override
    public String toString() {
        return "CourseContact{" +
                "instructionalMethod=" + instructionalMethod +
                ", hours=" + hours +
                '}';
    }
}
