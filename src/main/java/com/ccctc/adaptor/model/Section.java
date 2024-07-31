package com.ccctc.adaptor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by jrscanlon on 6/24/15.
 */
@ApiModel
@PrimaryKey({"misCode", "sisTermId", "sisSectionId"})
public class Section implements Serializable {

    // @TODO - ignore this for now as it was not part of the original model. Consider adding?
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "111")
    private String misCode;

    // Colleague - course_section.sec_synonym
    @ApiModelProperty(required = true, value = "Identifier/code representing a section of a course in a term in the SIS. This should be something a school or district administrator will know. This combined with the course and term will uniquely identify the section.", example = "1234")
    private String sisSectionId;

    // Colleague - terms.terms_id
    @ApiModelProperty(required = true, value = "Identifier/code representing the term in the SIS. This should be something a school or district administrator will know.", example = "2108SP")
    private String sisTermId;

    // Colleague - courses.crs_name
    @ApiModelProperty(required = true, value = "Identifier/code representing the course in the SIS. This should be something a school or district administrator will know.", example = "ENGL-100")
    private String sisCourseId;

    // Colleague - course_sections_ls.sec_faculty -> course_sec_faculty.csf_faculty -> person.*
    @ApiModelProperty(value = "Instructor(s) associated with the section.")
    private List<Instructor> instructors;

    // Colleague - course_sections.sec_capacity
    @ApiModelProperty(value = "Maximum number of enrollments allowed in the section.", example="30")
    private Integer maxEnrollments;

    // Colleague - course_sections.sec_waitlist_max
    @ApiModelProperty(value = "Maximum number of students allowed on the waitlist.", example="10")
    private Integer maxWaitlist;

    // Colleague - course_sections.sec_min_cred
    @ApiModelProperty(required = true, value = "Minimum number of units for the section. If the section is fixed unit, minimum and maximum will be the same.", example="3.0")
    private Float minimumUnits;

    // Colleague - course_sections.sec_max_cred (when variable unit) or course_sections.sec_min_cred (when fixed unit)
    @ApiModelProperty(required = true, value = "Maximum number of units for the section. If the section is fixed unit, minimum and maximum will be the same.", example="3.0")
    private Float maximumUnits;

    // Colleague - course_sections_ls.sec_meeting -> course_sec_meeting.*
    @ApiModelProperty(value = "Meeting information for the section including days and times the section meets.")
    private List<MeetingTime> meetingTimes;

    // Colleague - course_sections.sec_no_weeks
    @ApiModelProperty(value = "Number of weeks the section meets for.", example="18")
    private Integer weeksOfInstruction;

    // Colleague - course_sections.sec_location -> locations.loc_desc
    @ApiModelProperty(value = "Campus/location where the section meets.", example="Main Campus")
    private String campus;

    // Colleague - terms.term_start_date, terms.term_end_date
    // Banner - stvterm.stvterm_start_date, stvterm.stvterm_end_date
    // PeopleSoft - termBeginningDate, termEndDate
    @ApiModelProperty(required = true, value = "Start date of the section", example="8/24/2018")
    private Date start;
    @ApiModelProperty(required = true, value = "End date of the section", example="12/18/2018")
    private Date end;

    // Colleague - course_sections.sec_ovr_prereg_start_date, course_sections.sec_ovr_prereg_end_date
    // Banner -
    // PeopleSoft - does not exist
    @ApiModelProperty(value = "Start date of the pre-registration period.", example="4/1/2018")
    private Date preRegistrationStart;

    @ApiModelProperty(value = "End date of the pre-registration period.", example="6/30/2018")
    private Date preRegistrationEnd;

    // Colleague - course_sections.sec_ovr_reg_start_date, course_sections.sec_ovr_reg_end_date
    // Banner - SORRTRM_START_DATE, SORRTRM_END_DATE
    // PeopleSoft - open_enrollment_date, last_date_to_enroll
    @ApiModelProperty(value = "Start date of the registration period.", example="7/1/2018")
    private Date registrationStart;
    @ApiModelProperty(value = "End date of the registration period.", example="8/23/2018")
    private Date registrationEnd;

    // Colleague - course_sections.sec_ovr_add_end_date
    // Banner -
    // PeopleSoft -
    @ApiModelProperty(value = "Last date to add the section.", example="9/6/2018")
    private Date addDeadline;

    // Colleague - course_sections.sec_ovr_drop_end_date
    // Banner -
    // PeopleSoft -
    @ApiModelProperty(value = "Last date to drop the section.", example="11/20/2018")
    private Date dropDeadline;

    // Colleague - course_sections.sec_ovr_drop_gr_reqd_date
    // Banner -
    // PeopleSoft -
    @ApiModelProperty(value = "Last date to drop the section without receiving a withdrawal grade.", example="11/1/2018")
    private Date withdrawalDeadline;

    // we may need to have this defined this in the UI
    // Colleague - does not exist
    // Banner -
    // PeopleSoft -
    @ApiModelProperty(value = "Last date to pay fees for the section.", example="9/6/2018")
    private Date feeDeadline;

    // used for 320 reporting
    // Colleague - course_sections_ls.sec_ovr_census_dates (pos = 1)
    // Banner -
    // PeopleSoft -
    @ApiModelProperty(value = "Census date associated with the section.", example="9/7/2018")
    private Date censusDate;

    // Used by Canvas LMS, section title which may be different from course title.
    @ApiModelProperty(value= "Title of section.", example="Film/Television Production Workshop")
    private String title;

    // Used by Canvas LMS, status indicator for sections.
    @ApiModelProperty(value="Status of section.", example="Active, Cancelled, Pending")
    private SectionStatus status;

    @ApiModelProperty(value = "Detailed information about the crosslisting")
    private CrosslistingDetail crosslistingDetail;

    @JsonProperty("crosslistingStatus")
    @ApiModelProperty(required = true, value = "Crosslisting status of the section. Calculated based on the data in crosslistingDetail.", example = "CrosslistedPrimary")
    public CrosslistingStatus getCrosslistingStatus() {
        if (crosslistingDetail == null)
            return CrosslistingStatus.NotCrosslisted;

        if (sisSectionId != null && sisSectionId.equals(crosslistingDetail.getPrimarySisSectionId()))
            return CrosslistingStatus.CrosslistedPrimary;

        return CrosslistingStatus.CrosslistedSecondary;
    }

    public Section() {
    }

    private Section(Builder builder) {
        setMisCode(builder.misCode);
        setSisSectionId(builder.sisSectionId);
        setSisTermId(builder.sisTermId);
        setSisCourseId(builder.sisCourseId);
        setInstructors(builder.instructors);
        setMaxEnrollments(builder.maxEnrollments);
        setMaxWaitlist(builder.maxWaitlist);
        setMinimumUnits(builder.minimumUnits);
        setMaximumUnits(builder.maximumUnits);
        setMeetingTimes(builder.meetingTimes);
        setWeeksOfInstruction(builder.weeksOfInstruction);
        setCampus(builder.campus);
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
        setTitle(builder.title);
        setStatus(builder.status);
        setCrosslistingDetail(builder.crosslistingDetail);
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisSectionId() {
        return sisSectionId;
    }

    public void setSisSectionId(String sisSectionId) {
        this.sisSectionId = sisSectionId;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public String getSisCourseId() {
        return sisCourseId;
    }

    public void setSisCourseId(String sisCourseId) {
        this.sisCourseId = sisCourseId;
    }

    public List<Instructor> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<Instructor> instructors) {
        this.instructors = instructors;
    }

    public Integer getMaxEnrollments() {
        return maxEnrollments;
    }

    public void setMaxEnrollments(Integer maxEnrollments) {
        this.maxEnrollments = maxEnrollments;
    }

    public Integer getMaxWaitlist() {
        return maxWaitlist;
    }

    public void setMaxWaitlist(Integer maxWaitlist) {
        this.maxWaitlist = maxWaitlist;
    }

    public Float getMinimumUnits() {
        return minimumUnits;
    }

    public void setMinimumUnits(Float minimumUnits) {
        this.minimumUnits = minimumUnits;
    }

    public Float getMaximumUnits() {
        return maximumUnits;
    }

    public void setMaximumUnits(Float maximumUnits) {
        this.maximumUnits = maximumUnits;
    }

    public List<MeetingTime> getMeetingTimes() {
        return meetingTimes;
    }

    public void setMeetingTimes(List<MeetingTime> meetingTimes) {
        this.meetingTimes = meetingTimes;
    }

    public Integer getWeeksOfInstruction() {
        return weeksOfInstruction;
    }

    public void setWeeksOfInstruction(Integer weeksOfInstruction) {
        this.weeksOfInstruction = weeksOfInstruction;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SectionStatus getStatus() {
        return status;
    }

    public void setStatus(SectionStatus status) {
        this.status = status;
    }

    public CrosslistingDetail getCrosslistingDetail() {
        return crosslistingDetail;
    }

    public void setCrosslistingDetail(CrosslistingDetail crosslistingDetail) {
        this.crosslistingDetail = crosslistingDetail;
    }

    @Override
    public String toString() {
        return "Section{" +
                "misCode='" + misCode + '\'' +
                ", sisSectionId='" + sisSectionId + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", sisCourseId='" + sisCourseId + '\'' +
                ", instructors=" + instructors +
                ", maxEnrollments=" + maxEnrollments +
                ", maxWaitlist=" + maxWaitlist +
                ", minimumUnits=" + minimumUnits +
                ", maximumUnits=" + maximumUnits +
                ", meetingTimes=" + meetingTimes +
                ", weeksOfInstruction=" + weeksOfInstruction +
                ", campus='" + campus + '\'' +
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
                ", title='" + title + '\'' +
                ", status=" + status +
                ", crosslistingDetail=" + crosslistingDetail +
                '}';
    }

    public static final class Builder {
        private String misCode;
        private String sisSectionId;
        private String sisTermId;
        private String sisCourseId;
        private List<Instructor> instructors;
        private Integer maxEnrollments;
        private Integer maxWaitlist;
        private Float minimumUnits;
        private Float maximumUnits;
        private List<MeetingTime> meetingTimes;
        private Integer weeksOfInstruction;
        private String campus;
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
        private String title;
        private SectionStatus status;
        private CrosslistingDetail crosslistingDetail;

        public Builder() {
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public Builder sisSectionId(String val) {
            sisSectionId = val;
            return this;
        }

        public Builder sisTermId(String val) {
            sisTermId = val;
            return this;
        }

        public Builder sisCourseId(String val) {
            sisCourseId = val;
            return this;
        }

        public Builder instructors(List<Instructor> val) {
            instructors = val;
            return this;
        }

        public Builder maxEnrollments(Integer val) {
            maxEnrollments = val;
            return this;
        }

        public Builder maxWaitlist(Integer val) {
            maxWaitlist = val;
            return this;
        }

        public Builder minimumUnits(Float val) {
            minimumUnits = val;
            return this;
        }

        public Builder maximumUnits(Float val) {
            maximumUnits = val;
            return this;
        }

        public Builder meetingTimes(List<MeetingTime> val) {
            meetingTimes = val;
            return this;
        }

        public Builder weeksOfInstruction(Integer val) {
            weeksOfInstruction = val;
            return this;
        }

        public Builder campus(String val) {
            campus = val;
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

        public Builder title(String val) {
            title = val;
            return this;
        }

        public Builder status(SectionStatus val) {
            status = val;
            return this;
        }

        public Builder crosslistingDetail(CrosslistingDetail val) {
            crosslistingDetail = val;
            return this;
        }

        public Section build() {
            return new Section(this);
        }
    }
}
