package com.ccctc.adaptor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by jrscanlon on 12/9/15.
 */
@ApiModel
@PrimaryKey({"misCode", "sisPersonId", "sisTermId", "sisSectionId"})
public class Enrollment implements Serializable{

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    // Enrollment specific values
    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;
    @ApiModelProperty(value = "ID of the student in the SIS", example = "person1")
    private String sisPersonId;
    @ApiModelProperty(value = "Identifier/code representing a term in the SIS", example = "2018SP")
    private String sisTermId;
    @ApiModelProperty(value = "Identifier/code representing a section of a course in a term in the SIS.", example = "1234")
    private String sisSectionId;
    @ApiModelProperty(value = "Status of the enrollment", example = "Enrolled,Dropped,Cancelled")
    private EnrollmentStatus enrollmentStatus;
    @ApiModelProperty(value = "Date associated with the enrollment status (the date the student enrolled, dropped or the section was cancelled).")
    private Date enrollmentStatusDate;
    @ApiModelProperty(value = "Units the student enrolled in.")
    private Float units;
    @ApiModelProperty(value = "True/false value indicating whether the student enrolled in the course for a pass/no pass grade.")
    private Boolean passNoPass;
    @ApiModelProperty(value = "True/false value indicating whether the student is auditing the course.")
    private Boolean audit;
    @ApiModelProperty(value = "Grade the student received in the enrollment.")
    private String grade;
    @ApiModelProperty(value = "Date that the grade was assigned to the student.")
    private Date gradeDate;
    @ApiModelProperty(value = "Last date the student attended the course.")
    private Date lastDateAttended;

    // Values from the course to aid in displaying a class schedule
    @ApiModelProperty(value = "Identifier/code representing the course in the SIS.", example = "ENGL-100")
    private String sisCourseId;

    @ApiModelProperty(value = "C-ID.net course identifier.", example = "ENGL 110")
    private String c_id;

    @ApiModelProperty(value = "Course title", example = "English 100 - Introduction to College English")
    private String title;

    public Enrollment() {
    }

    private Enrollment(Builder builder) {
        setMisCode(builder.misCode);
        setCccid(builder.cccid);
        setSisPersonId(builder.sisPersonId);
        setSisTermId(builder.sisTermId);
        setSisSectionId(builder.sisSectionId);
        setEnrollmentStatus(builder.enrollmentStatus);
        setEnrollmentStatusDate(builder.enrollmentStatusDate);
        setUnits(builder.units);
        setPassNoPass(builder.passNoPass);
        setAudit(builder.audit);
        setGrade(builder.grade);
        setGradeDate(builder.gradeDate);
        setLastDateAttended(builder.lastDateAttended);
        setSisCourseId(builder.sisCourseId);
        setC_id(builder.c_id);
        setTitle(builder.title);
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

    public String getSisSectionId() {
        return sisSectionId;
    }

    public void setSisSectionId(String sisSectionId) {
        this.sisSectionId = sisSectionId;
    }

    public EnrollmentStatus getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public Date getEnrollmentStatusDate() {
        return enrollmentStatusDate;
    }

    public void setEnrollmentStatusDate(Date enrollmentStatusDate) {
        this.enrollmentStatusDate = enrollmentStatusDate;
    }

    public Float getUnits() {
        return units;
    }

    public void setUnits(Float units) {
        this.units = units;
    }

    public Boolean getPassNoPass() {
        return passNoPass;
    }

    public void setPassNoPass(Boolean passNoPass) {
        this.passNoPass = passNoPass;
    }

    public Boolean getAudit() {
        return audit;
    }

    public void setAudit(Boolean audit) {
        this.audit = audit;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Date getGradeDate() {
        return gradeDate;
    }

    public void setGradeDate(Date gradeDate) {
        this.gradeDate = gradeDate;
    }

    public Date getLastDateAttended() {
        return lastDateAttended;
    }

    public void setLastDateAttended(Date lastDateAttended) {
        this.lastDateAttended = lastDateAttended;
    }

    public String getSisCourseId() {
        return sisCourseId;
    }

    public void setSisCourseId(String sisCourseId) {
        this.sisCourseId = sisCourseId;
    }

    public String getC_id() {
        return c_id;
    }

    public void setC_id(String c_id) {
        this.c_id = c_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static final class Builder {
        private String misCode;
        private String cccid;
        private String sisPersonId;
        private String sisTermId;
        private String sisSectionId;
        private EnrollmentStatus enrollmentStatus;
        private Date enrollmentStatusDate;
        private Float units;
        private Boolean passNoPass;
        private Boolean audit;
        private String grade;
        private Date gradeDate;
        private Date lastDateAttended;
        private String sisCourseId;
        private String c_id;
        private String title;

        public Builder() {
        }

        public Builder misCode(String val) {
            this.misCode = val;
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

        public Builder sisSectionId(String val) {
            sisSectionId = val;
            return this;
        }

        public Builder enrollmentStatus(EnrollmentStatus val) {
            enrollmentStatus = val;
            return this;
        }

        public Builder enrollmentStatusDate(Date val) {
            enrollmentStatusDate = val;
            return this;
        }

        public Builder units(Float val) {
            units = val;
            return this;
        }

        public Builder passNoPass(Boolean val) {
            passNoPass = val;
            return this;
        }

        public Builder audit(Boolean val) {
            audit = val;
            return this;
        }

        public Builder grade(String val) {
            grade = val;
            return this;
        }

        public Builder gradeDate(Date val) {
            gradeDate = val;
            return this;
        }

        public Builder lastDateAttended(Date val) {
            lastDateAttended = val;
            return this;
        }

        public Builder sisCourseId(String val) {
            sisCourseId = val;
            return this;
        }

        public Builder c_id(String val) {
            c_id = val;
            return this;
        }

        public Builder title(String val) {
            title = val;
            return this;
        }

        public Enrollment build() {
            return new Enrollment(this);
        }
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "misCode='" + misCode + '\'' +
                ", cccid='" + cccid + '\'' +
                ", sisPersonId='" + sisPersonId + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", sisSectionId='" + sisSectionId + '\'' +
                ", enrollmentStatus=" + enrollmentStatus +
                ", enrollmentStatusDate=" + enrollmentStatusDate +
                ", units=" + units +
                ", passNoPass=" + passNoPass +
                ", audit=" + audit +
                ", grade='" + grade + '\'' +
                ", gradeDate=" + gradeDate +
                ", lastDateAttended=" + lastDateAttended +
                ", sisCourseId='" + sisCourseId + '\'' +
                ", c_id='" + c_id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
