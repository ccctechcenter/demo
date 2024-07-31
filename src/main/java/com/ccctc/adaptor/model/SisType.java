package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by Rasul on 8/4/16.
 */
@ApiModel
public class SisType {

    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    @ApiModelProperty(required = true, value = "Type of SIS the adaptor is configured for", example = "colleague")
    private String sisType;

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisType() {
        return sisType;
    }

    public void setSisType(String sisType) {
        this.sisType = sisType;
    }

    private SisType(Builder builder) {
        setMisCode(builder.misCode);
        setSisType(builder.sisType);
    }

    public static final class Builder {
        private String misCode;
        private String sisType;

        public Builder() {
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public Builder sisType(String val) {
            sisType = val;
            return this;
        }

        public SisType build() {
            return new SisType(this);
        }
    }
}
