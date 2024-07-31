package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by zekeo on 10/3/2016.
 */
@ApiModel
public class StudentHomeCollege implements Serializable {
    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;
    @ApiModelProperty(value = "MIS Code of the student's home college", example="111")
    private String misCode;

    public StudentHomeCollege() {
    }

    private StudentHomeCollege(Builder builder) {
        setCccid(builder.cccid);
        setMisCode(builder.misCode);
    }

    public String getCccid() {
        return cccid;
    }

    public void setCccid(String cccid) {
        this.cccid = cccid;
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public static final class Builder {
        private String cccid;
        private String misCode;

        public Builder() {
        }

        public Builder cccid(String val) {
            cccid = val;
            return this;
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public StudentHomeCollege build() {
            return new StudentHomeCollege(this);
        }
    }
}
