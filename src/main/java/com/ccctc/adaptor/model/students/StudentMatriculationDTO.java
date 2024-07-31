package com.ccctc.adaptor.model.students;

import com.ccctc.adaptor.model.*;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.Date;


@ApiModel
@PrimaryKey({"sisTermId", "sisPersonId"})
public class StudentMatriculationDTO implements Serializable {

    private String cccid;
    private String sisPersonId;
    private String sisTermId;

    private OrientationStatus orientationStatus;
    private Boolean hasEducationPlan;
    private ApplicationStatus applicationStatus;
    private Date registrationDate;
    private Boolean isActive;
    private Boolean hasHold;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public StudentMatriculationDTO() {
        // set defaults
        this.orientationStatus = OrientationStatus.UNKNOWN;
        this.hasEducationPlan = false;
        this.applicationStatus = ApplicationStatus.NoApplication;
        this.isActive = false;
        this.hasHold = true;
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

    public Boolean getHasEducationPlan() {
        return hasEducationPlan;
    }

    public void setHasEducationPlan(Boolean hasEducationPlan) {
        this.hasEducationPlan = hasEducationPlan;
    }

    public OrientationStatus getOrientationStatus() {
        return orientationStatus;
    }

    public void setOrientationStatus(OrientationStatus orienStatus) {
        this.orientationStatus = orienStatus;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }

    public Date getRegistrationDate() { return registrationDate; }

    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }

    public Boolean getIsActive() { return isActive; }

    public void setIsActive(Boolean active) { isActive = active; }

    public Boolean getHasHold() { return hasHold; }

    public void setHasHold(Boolean hasHold) { this.hasHold = hasHold; }

}
