package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by jrscanlon on 3/2/16.
 */
@ApiModel
public class PrerequisiteStatus {

    @ApiModelProperty(required = true, value = "Status of the Prerequisite ", example = "Complete")
    private PrerequisiteStatusEnum status;

    @ApiModelProperty(required = false, value = "Message indicating why a student may or may not meet the prerequisite requirements")
    private String message;

    public PrerequisiteStatus() {
    }

    private PrerequisiteStatus(Builder builder) {
        setStatus(builder.status);
        setMessage(builder.message);
    }

    public PrerequisiteStatusEnum getStatus() {
        return status;
    }

    public void setStatus(PrerequisiteStatusEnum status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static final class Builder {
        private PrerequisiteStatusEnum status;
        private String message;

        public Builder() {
        }

        public Builder status(PrerequisiteStatusEnum val) {
            status = val;
            return this;
        }

        public Builder message(String val) {
            message = val;
            return this;
        }

        public PrerequisiteStatus build() {
            return new PrerequisiteStatus(this);
        }
    }
}
