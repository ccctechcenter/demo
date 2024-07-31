package com.ccctc.adaptor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@ApiModel
@PrimaryKey({"misCode", "sisTermId", "sisPersonId", "ceEnrollmentMisCode", "ceEnrollmentC_id"})
public class FinancialAidUnits implements Serializable {

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "ID of the student in the SIS", example = "person1")
    private String sisPersonId;

    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;
    @ApiModelProperty(value = "Identifier/code representing a term in the SIS", example = "2018SP")
    private String sisTermId;
    @ApiModelProperty(value = "Student enrollment information at a teaching college")
    private CourseExchangeEnrollment ceEnrollment;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public FinancialAidUnits(){}

    private FinancialAidUnits(Builder builder) {
        setMisCode(builder.misCode);
        setSisPersonId(builder.sisPersonId);
        setCccid(builder.cccid);
        setSisTermId(builder.sisTermId);
        setCeEnrollment(builder.ceEnrollment);
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisPersonId() {
        return sisPersonId;
    }

    public void setSisPersonId(String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }

    public String getCccid() {
        return cccid;
    }

    public void setCccid(String cccid) {
        this.cccid = cccid;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public CourseExchangeEnrollment getCeEnrollment() {
        return ceEnrollment;
    }

    public void setCeEnrollment(CourseExchangeEnrollment ceEnrollment) {
        this.ceEnrollment = ceEnrollment;
    }

    // Methods used by the mock DB to get/set unique values for ceEnrollment
    @JsonIgnore
    public String getCeEnrollmentMisCode() {
        return ceEnrollment != null ? ceEnrollment.getMisCode() : null;
    }

    @JsonIgnore
    public void setCeEnrollmentMisCode(String val) {
        if (ceEnrollment == null)
            ceEnrollment = new CourseExchangeEnrollment();

        ceEnrollment.setMisCode(val);
    }

    @JsonIgnore
    public String getCeEnrollmentC_id() {
        return ceEnrollment != null ? ceEnrollment.getC_id() : null;
    }

    @JsonIgnore
    public void setCeEnrollmentC_id(String val) {
        if (ceEnrollment == null)
            ceEnrollment = new CourseExchangeEnrollment();

        ceEnrollment.setC_id(val);
    }

    @Override
    public String toString() {
        return "FinancialAidUnits{" +
                "misCode='" + misCode + '\'' +
                ", sisPersonId='" + sisPersonId + '\'' +
                ", cccid='" + cccid + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", ceEnrollment=" + ceEnrollment +
                '}';
    }

    public static final class Builder {
        private String misCode;
        private String sisPersonId;
        private String cccid;
        private String sisTermId;
        private CourseExchangeEnrollment ceEnrollment;

        public Builder() {
        }

        public Builder misCode(String misCode) {
            this.misCode = misCode;
            return this;
        }

        public Builder sisPersonId(String sisPersonId) {
            this.sisPersonId = sisPersonId;
            return this;
        }

        public Builder cccid(String cccid) {
            this.cccid = cccid;
            return this;
        }

        public Builder sisTermId(String sisTermId) {
            this.sisTermId = sisTermId;
            return this;
        }

        public Builder ceEnrollment(CourseExchangeEnrollment ceEnrollment) {
            this.ceEnrollment = ceEnrollment;
            return this;
        }

        public FinancialAidUnits build() {
            return new FinancialAidUnits(this);
        }
    }
}
