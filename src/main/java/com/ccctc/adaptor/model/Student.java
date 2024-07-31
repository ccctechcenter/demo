package com.ccctc.adaptor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by jrscanlon on 11/20/15.
 */
@ApiModel
@PrimaryKey({"misCode", "sisTermId", "sisPersonId"})
public class Student implements Serializable {

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;
    @ApiModelProperty(value = "ID of the student in the SIS", example = "person1")
    private String sisPersonId;
    @ApiModelProperty(value = "Identifier/code representing a term in the SIS", example = "2018SP")
    private String sisTermId;
    @ApiModelProperty(value = "Cohorts of the student")
    private List<Cohort> cohorts;

    // Eligibility to use Exchange
    @ApiModelProperty(value = "VISA Type of the student", example = "F1")
    private String visaType; // Note: VISA type should be upper case letters and numbers ONLY, no spaces, dashes, etc
    @ApiModelProperty(value = "True/false value indicating whether the student has a California address on file")
    private Boolean hasCaliforniaAddress;
    @ApiModelProperty(value = "True/false value indicating whether the student is incarcerated")
    private Boolean isIncarcerated;
    @ApiModelProperty(value = "True/false value indicating whether the student is concurrently enrolled in K-12 and college")
    private Boolean isConcurrentlyEnrolled;

    // Matriculation eligibility
    @ApiModelProperty(value = "True/false value indicating whether the student has an educational plan on file")
    private Boolean hasEducationPlan;
    @ApiModelProperty(value = "True/false value indicating whether the student has completed the mathematics assessment")
    private Boolean hasMathAssessment;
    @ApiModelProperty(value = "True/false value indicating whether the student has completed the English assessment")
    private Boolean hasEnglishAssessment;

    // Term Based information
    @ApiModelProperty(value = "Status of the student's completion of all orientation tasks", example="COMPLETED")
    private OrientationStatus orientationStatus;

    @ApiModelProperty(value = "Status of the student's application for the term", example = "ApplicationAccepted")
    private ApplicationStatus applicationStatus;
    @ApiModelProperty(value = "Residency status of the student for the term", example = "Resident")
    private ResidentStatus residentStatus;
    @ApiModelProperty(value = "True/false value indicating whether the student has a hold that would prevent registration for the term")
    private Boolean hasHold;
    @ApiModelProperty(value = "Date the student has been assigned to register for the term.")
    private Date registrationDate;
    @ApiModelProperty(value = "True/false value indicating whether the student has any active financial aid award for the term")
    private Boolean hasFinancialAidAward;
    @ApiModelProperty(value = "True/false value indicating whether the student has any active BOG Fee Waiver for the term")
    private Boolean hasBogfw;

    // Other student information
    @ApiModelProperty(value = "Total account balance of the student at the college")
    private Float accountBalance;

    @ApiModelProperty(value = "True/False value indicating whether the student is DSPS Eligible ")
    private Boolean dspsEligible;

    @ApiModelProperty(value = "True/False value indicating whether the student is active at the institution")
    private Boolean isActive;


    private Student(Builder builder) {
        setMisCode(builder.misCode);
        setCccid(builder.cccid);
        setSisPersonId(builder.sisPersonId);
        setSisTermId(builder.sisTermId);
        setCohorts(builder.cohorts);
        setVisaType(builder.visaType);
        setHasCaliforniaAddress(builder.hasCaliforniaAddress);
        setIncarcerated(builder.isIncarcerated);
        setConcurrentlyEnrolled(builder.isConcurrentlyEnrolled);
        setHasEducationPlan(builder.hasEducationPlan);
        setHasMathAssessment(builder.hasMathAssessment);
        setHasEnglishAssessment(builder.hasEnglishAssessment);
        setOrientationStatus(builder.orientationStatus);
        setApplicationStatus(builder.applicationStatus);
        setResidentStatus(builder.residentStatus);
        setHasHold(builder.hasHold);
        setRegistrationDate(builder.registrationDate);
        setHasFinancialAidAward(builder.hasFinancialAidAward);
        setHasBogfw(builder.hasBogfw);
        setAccountBalance(builder.accountBalance);
        setDspsEligible(builder.dspsEligible);
        setIsActive(builder.isActive);
    }

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public Student() {
        this.orientationStatus = OrientationStatus.UNKNOWN;
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getCccid() {
        return cccid;
    }

    public void setCccid(String cccid) {
        this.cccid = cccid;
    }

    public String getSisPersonId() {
        return sisPersonId;
    }

    public void setSisPersonId(String sisPersonId) {
        this.sisPersonId = sisPersonId;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public List<Cohort> getCohorts() {
        return cohorts;
    }

    public void setCohorts(List<Cohort> cohorts) {
        this.cohorts = cohorts;
    }

    public String getVisaType() {
        return visaType;
    }

    public void setVisaType(String visaType) {
        this.visaType = visaType;
    }

    public Boolean getHasCaliforniaAddress() {
        return hasCaliforniaAddress;
    }

    public void setHasCaliforniaAddress(Boolean hasCaliforniaAddress) {
        this.hasCaliforniaAddress = hasCaliforniaAddress;
    }

    public Boolean getIncarcerated() {
        return isIncarcerated;
    }

    public void setIncarcerated(Boolean incarcerated) {
        isIncarcerated = incarcerated;
    }

    public Boolean getConcurrentlyEnrolled() {
        return isConcurrentlyEnrolled;
    }

    public void setConcurrentlyEnrolled(Boolean concurrentlyEnrolled) {
        isConcurrentlyEnrolled = concurrentlyEnrolled;
    }

    public Boolean getHasEducationPlan() {
        return hasEducationPlan;
    }

    public void setHasEducationPlan(Boolean hasEducationPlan) {
        this.hasEducationPlan = hasEducationPlan;
    }

    public Boolean getHasMathAssessment() {
        return hasMathAssessment;
    }

    public void setHasMathAssessment(Boolean hasMathAssessment) {
        this.hasMathAssessment = hasMathAssessment;
    }

    public Boolean getHasEnglishAssessment() {
        return hasEnglishAssessment;
    }

    public void setHasEnglishAssessment(Boolean hasEnglishAssessment) {
        this.hasEnglishAssessment = hasEnglishAssessment;
    }

    public OrientationStatus getOrientationStatus() {
        if(orientationStatus == null) {
            orientationStatus = OrientationStatus.UNKNOWN;
        }
        return orientationStatus;
    }

    public void setOrientationStatus(OrientationStatus orienStatus) {
        if(orienStatus == null) {
            orienStatus = OrientationStatus.UNKNOWN;
        }
        this.orientationStatus = orienStatus;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public ResidentStatus getResidentStatus() {
        return residentStatus;
    }

    public void setResidentStatus(ResidentStatus residentStatus) {
        this.residentStatus = residentStatus;
    }

    public Boolean getHasHold() {
        return hasHold;
    }

    public void setHasHold(Boolean hasHold) {
        this.hasHold = hasHold;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Boolean getHasFinancialAidAward() {
        return hasFinancialAidAward;
    }

    public void setHasFinancialAidAward(Boolean hasFinancialAidAward) {
        this.hasFinancialAidAward = hasFinancialAidAward;
    }

    public Boolean getHasBogfw() {
        return hasBogfw;
    }

    public void setHasBogfw(Boolean hasBogfw) {
        this.hasBogfw = hasBogfw;
    }

    public Float getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(Float accountBalance) {
        this.accountBalance = accountBalance;
    }

    public Boolean getDspsEligible() {
        return dspsEligible;
    }

    public void setDspsEligible(Boolean dspsEligible) {
        this.dspsEligible = dspsEligible;
    }

    public Boolean getIsActive() { return isActive; }

    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public String toString() {
        return "Student{" +
                "misCode='" + misCode + '\'' +
                ", cccid='" + cccid + '\'' +
                ", sisPersonId='" + sisPersonId + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", cohorts=" + cohorts +
                ", visaType='" + visaType + '\'' +
                ", hasCaliforniaAddress=" + hasCaliforniaAddress +
                ", isIncarcerated=" + isIncarcerated +
                ", isConcurrentlyEnrolled=" + isConcurrentlyEnrolled +
                ", hasEducationPlan=" + hasEducationPlan +
                ", hasMathAssessment=" + hasMathAssessment +
                ", hasEnglishAssessment=" + hasEnglishAssessment +
                ", orientationStatus=" + orientationStatus +
                ", applicationStatus=" + applicationStatus +
                ", residentStatus=" + residentStatus +
                ", hasHold=" + hasHold +
                ", registrationDate=" + registrationDate +
                ", hasFinancialAidAward=" + hasFinancialAidAward +
                ", hasBogfw=" + hasBogfw +
                ", accountBalance=" + accountBalance +
                ", dspsEligible=" + dspsEligible +
                ", isActive=" + isActive +
                '}';
    }

    public static final class Builder {
        private String misCode;
        private String cccid;
        private String sisPersonId;
        private String sisTermId;
        private List<Cohort> cohorts;
        private String visaType;
        private Boolean hasCaliforniaAddress;
        private Boolean isIncarcerated;
        private Boolean isConcurrentlyEnrolled;
        private Boolean hasEducationPlan;
        private Boolean hasMathAssessment;
        private Boolean hasEnglishAssessment;
        private OrientationStatus orientationStatus;
        private ApplicationStatus applicationStatus;
        private ResidentStatus residentStatus;
        private Boolean hasHold;
        private Date registrationDate;
        private Boolean hasFinancialAidAward;
        private Boolean hasBogfw;
        private Float accountBalance;
        private Boolean dspsEligible;
        private Boolean isActive;

        public Builder() {
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public Builder cccid(String val) {
            cccid = val;
            return this;
        }

        public Builder sisPersonId(String val) {
            sisPersonId = val;
            return this;
        }

        public Builder sisTermId(String val) {
            sisTermId = val;
            return this;
        }

        public Builder cohorts(List<Cohort> val) {
            cohorts = val;
            return this;
        }

        public Builder visaType(String val) {
            visaType = val;
            return this;
        }

        public Builder hasCaliforniaAddress(Boolean val) {
            hasCaliforniaAddress = val;
            return this;
        }

        public Builder isIncarcerated(Boolean val) {
            isIncarcerated = val;
            return this;
        }

        public Builder isConcurrentlyEnrolled(Boolean val) {
            isConcurrentlyEnrolled = val;
            return this;
        }

        public Builder orientationStatus(OrientationStatus val) {
            orientationStatus = val;
            return this;
        }

        public Builder hasEducationPlan(Boolean val) {
            hasEducationPlan = val;
            return this;
        }

        public Builder hasMathAssessment(Boolean val) {
            hasMathAssessment = val;
            return this;
        }

        public Builder hasEnglishAssessment(Boolean val) {
            hasEnglishAssessment = val;
            return this;
        }

        public Builder applicationStatus(ApplicationStatus val) {
            applicationStatus = val;
            return this;
        }

        public Builder residentStatus(ResidentStatus val) {
            residentStatus = val;
            return this;
        }

        public Builder hasHold(Boolean val) {
            hasHold = val;
            return this;
        }

        public Builder registrationDate(Date val) {
            registrationDate = val;
            return this;
        }

        public Builder hasFinancialAidAward(Boolean val) {
            hasFinancialAidAward = val;
            return this;
        }

        public Builder hasBogfw(Boolean val) {
            hasBogfw = val;
            return this;
        }

        public Builder accountBalance(Float val) {
            accountBalance = val;
            return this;
        }

        public Builder dspsEligible(Boolean val) {
            dspsEligible = val;
            return this;
        }

        public Builder isActive(Boolean val) {
            isActive = val;
            return this;
        }

        public Student build() {
            return new Student(this);
        }
    }
}
