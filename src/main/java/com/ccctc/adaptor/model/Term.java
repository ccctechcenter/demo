package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by jrscanlon on 12/4/15.
 */
@ApiModel
@PrimaryKey({"misCode", "sisTermId"})
public class Term implements Serializable {

    @ApiModelProperty(value = "3 digit college MIS code", example = "111")
    private String misCode;

    // Colleague - 7 character termCode - 2015FA, 15/FA
    // Banner - 6 character termCode - 201510
    // PS - 4 character STerm - 1501, 0115
    @ApiModelProperty(required = true, value = "Identifier/code representing a term in the SIS. This should be something a school or district administrator will know.", example="2018SP")
    private String sisTermId;

    // PS - 4 Year alpha numeric 2018, 2017, 2016 etc
    // last two digits tell even aid year or odd aid year

    @ApiModelProperty( value = "4 digits Academic Year", example="2018")
    private Integer year;

    // Spring, Fall, Winter, Summer
    @ApiModelProperty( value = "Term session. Spring/Fall/Winter/Summer for Term based Schools. Spring Qtr/Fall Qtr/Winter Qtr/Summer Qt", example = "Fall",dataType = "TermSession" )
    private TermSession session;

    // Semester, Quarter
    @ApiModelProperty( value = "Term Type", example = "Semester", dataType = "TermType")
    private TermType type;

    // Colleague - terms.term_start_date, terms.term_end_date
    // Banner - stvterm.stvterm_start_date, stvterm.stvterm_end_date
    // PeopleSoft - termBeginningDate, termEndDate

    @ApiModelProperty( value = "Term Begin Date", example = "2018-08-27")
    private Date start;
    @ApiModelProperty( value = "Term Ending Date", example = "2018-12-15")
    private Date end;

    // Colleague - terms.term_prereg_start_date, terms.term_prereg_end_date
    // Banner -
    // PeopleSoft - appt_start_date, appt_end_date
    @ApiModelProperty( value = " Enrollment Appointment Start Date ", example = "2018-07-27")
    private Date preRegistrationStart;
    @ApiModelProperty( value = " Enrollment Appointment End Date ", example = "2018-07-30")
    private Date preRegistrationEnd;

    // Colleague - terms.term_reg_start_date, terms.term_reg_end_date
    // Banner - SORRTRM_START_DATE, SORRTRM_END_DATE
    // PeopleSoft - open_enrollment_date, last_date_to_enroll
    @ApiModelProperty( value = "First Date to Enroll ", example = "2018-04-15")
    private Date registrationStart;
    @ApiModelProperty( value = "Last Date to Enroll ", example = "2018-08-10")
    private Date registrationEnd;

    // Colleague -
    // Banner -
    // PeopleSoft -
    @ApiModelProperty( value = "Last date to Enroll ", example = "2018-08-10")
    private Date addDeadline;

    // Colleague -
    // Banner -
    // PeopleSoft -
    @ApiModelProperty( value = "Last Date to Drop from class with no charges ", example = "2018-08-10")
    private Date dropDeadline;

    // Colleague -
    // Banner -
    // PeopleSoft -
    @ApiModelProperty( value = "Last Date to with draw from the class with class fee refund", example = "2018-08-10")
    private Date withdrawalDeadline;

    // we may need to have this defined this in the UI
    // Colleague -
    // Banner -
    // PeopleSoft -
    @ApiModelProperty( value = "Class fee Due Date", example = "2018-08-01")
    private Date feeDeadline;

    // used for 320 reporting
    // Colleague -
    // Banner -
    // PeopleSoft -
    @ApiModelProperty( value = "Census Date ", example = "2018-09-22")
    private Date censusDate;

    // Colleague -
    // Banner -
    // PeopleSoft -
    // Spring 2018, Fall 2018, 2018 Fall, ...
    @ApiModelProperty( value = "Term Description", example = "2018 Spring")
    private String description;

    private Term(Builder builder) {
        setMisCode(builder.misCode);
        setSisTermId(builder.sisTermId);
        setYear(builder.year);
        setSession(builder.session);
        setType(builder.type);
        setStart(builder.start);
        setEnd(builder.end);
        setPreRegistrationStart(builder.preRegistrationStart);
        setPreRegistrationEnd(builder.preRegistrationEnd);
        setRegistrationStart(builder.registrationStart);
        setRegistrationEnd(builder.registrationEnd);
        setAddDeadline(builder.addDeadline);
        setDropDeadline(builder.dropDeadline);
        setWithdrawalDeadline(builder.withdrawalDeadline);
        setFeeDeadline(builder.feeDeadline);
        setCensusDate(builder.censusDate);
        setDescription(builder.description);
    }

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public Term(){}

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public TermSession getSession() {
        return session;
    }

    public void setSession(TermSession session) {
        this.session = session;
    }

    public TermType getType() {
        return type;
    }

    public void setType(TermType type) {
        this.type = type;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Date getPreRegistrationStart() {
        return preRegistrationStart;
    }

    public void setPreRegistrationStart(Date preRegistrationStart) {
        this.preRegistrationStart = preRegistrationStart;
    }

    public Date getPreRegistrationEnd() {
        return preRegistrationEnd;
    }

    public void setPreRegistrationEnd(Date preRegistrationEnd) {
        this.preRegistrationEnd = preRegistrationEnd;
    }

    public Date getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
    }

    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    public Date getAddDeadline() {
        return addDeadline;
    }

    public void setAddDeadline(Date addDeadline) {
        this.addDeadline = addDeadline;
    }

    public Date getDropDeadline() {
        return dropDeadline;
    }

    public void setDropDeadline(Date dropDeadline) {
        this.dropDeadline = dropDeadline;
    }

    public Date getWithdrawalDeadline() {
        return withdrawalDeadline;
    }

    public void setWithdrawalDeadline(Date withdrawalDeadline) {
        this.withdrawalDeadline = withdrawalDeadline;
    }

    public Date getFeeDeadline() {
        return feeDeadline;
    }

    public void setFeeDeadline(Date feeDeadline) {
        this.feeDeadline = feeDeadline;
    }

    public Date getCensusDate() {
        return censusDate;
    }

    public void setCensusDate(Date censusDate) {
        this.censusDate = censusDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Term{" +
                "misCode='" + misCode + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", year=" + year +
                ", session=" + session +
                ", type=" + type +
                ", start=" + start +
                ", end=" + end +
                ", preRegistrationStart=" + preRegistrationStart +
                ", preRegistrationEnd=" + preRegistrationEnd +
                ", registrationStart=" + registrationStart +
                ", registrationEnd=" + registrationEnd +
                ", addDeadline=" + addDeadline +
                ", dropDeadline=" + dropDeadline +
                ", withdrawalDeadline=" + withdrawalDeadline +
                ", feeDeadline=" + feeDeadline +
                ", censusDate=" + censusDate +
                ", description='" + description + '\'' +
                '}';
    }

    public static final class Builder {
        private String misCode;
        private String sisTermId;
        private Integer year;
        private TermSession session;
        private TermType type;
        private Date start;
        private Date end;
        private Date preRegistrationStart;
        private Date preRegistrationEnd;
        private Date registrationStart;
        private Date registrationEnd;
        private Date addDeadline;
        private Date dropDeadline;
        private Date withdrawalDeadline;
        private Date feeDeadline;
        private Date censusDate;
        private String description;

        public Builder() {
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public Builder sisTermId(String val) {
            sisTermId = val;
            return this;
        }

        public Builder year(Integer val) {
            year = val;
            return this;
        }

        public Builder session(TermSession val) {
            session = val;
            return this;
        }

        public Builder type(TermType val) {
            type = val;
            return this;
        }

        public Builder start(Date val) {
            start = val;
            return this;
        }

        public Builder end(Date val) {
            end = val;
            return this;
        }

        public Builder preRegistrationStart(Date val) {
            preRegistrationStart = val;
            return this;
        }

        public Builder preRegistrationEnd(Date val) {
            preRegistrationEnd = val;
            return this;
        }

        public Builder registrationStart(Date val) {
            registrationStart = val;
            return this;
        }

        public Builder registrationEnd(Date val) {
            registrationEnd = val;
            return this;
        }

        public Builder addDeadline(Date val) {
            addDeadline = val;
            return this;
        }

        public Builder dropDeadline(Date val) {
            dropDeadline = val;
            return this;
        }

        public Builder withdrawalDeadline(Date val) {
            withdrawalDeadline = val;
            return this;
        }

        public Builder feeDeadline(Date val) {
            feeDeadline = val;
            return this;
        }

        public Builder censusDate(Date val) {
            censusDate = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Term build() {
            return new Term(this);
        }
    }
}
