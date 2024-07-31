package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by jrscanlon on 6/24/15.
 */
@ApiModel
@PrimaryKey({"misCode", "sisTermId", "sisCourseId"})
public class Course implements Serializable {

    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "001")
    private String misCode;

    @ApiModelProperty(required = true, value = "Identifier/code representing a course in the SIS. This should be something a school or district administrator will know.", example = "ENGL-100")
    private String sisCourseId;

    @ApiModelProperty(value = "Identifier/code representing a term in the SIS. This should be something a school or district administrator will know.", example = "2018SP")
    private String sisTermId;

    // Identifier for C-ID.net course
    @ApiModelProperty(value = "C-ID.net course identifier. This field is required for the Course Exchange project but may not be required for other projects.", example = "ENGL 100")
    private String c_id;

    @ApiModelProperty(value = "Community Colleges of California Chancellor's Office course control number (CB00) as defined in the MIS database: http://extranet.cccco.edu/Portals/1/TRIS/MIS/Left_Nav/DED/Data_Elements/CB/cb00.pdf")
    private String controlNumber;

    @ApiModelProperty(value = "Course subject abbreviation.", example = "ENGL")
    private String subject;

    @ApiModelProperty(value = "Course number.", example = "100")
    private String number;

    @ApiModelProperty(value = "Course title", example = "English 100 - Introduction to College English")
    private String title;

    @ApiModelProperty(value = "Course description", example = "")
    private String description;

    @ApiModelProperty(value = "Course outline", example = "")
    private String outline;

    @ApiModelProperty(value = "Prerequisites description")
    private String prerequisites;
    @ApiModelProperty(value = "Corequisites description")
    private String corequisites;

    @ApiModelProperty(value = "List of course prerequisites")
    private List<Course> prerequisiteList;
    @ApiModelProperty(value = "List of course corequisites")
    private List<Course> corequisiteList;

    @ApiModelProperty(value = "Minimum units. For use with courses that have variable units. Max and min will be the same otherwise.", example = "")
    private Float minimumUnits;
    @ApiModelProperty(value = "Maximum units. For use with courses that have variable units. Max and min will be the same otherwise.", example = "")
    private Float maximumUnits;

    @ApiModelProperty(value = "Is this course transferable to a 4 year university", example = "Csu", dataType = "TransferStatus")
    private TransferStatus transferStatus;
    @ApiModelProperty(value = "Is this course degree applicable.", example = "DegreeApplicable", dataType = "CreditStatus")
    private CreditStatus creditStatus;
    @ApiModelProperty(value = "Is this course graded", example = "Graded", dataType = "GradingMethod")
    private GradingMethod gradingMethod;

    @ApiModelProperty(value = "List of methods the instructor interacts with students.")
    private List<CourseContact> courseContacts;

    @ApiModelProperty(value = "Course fee")
    private Float fee;

    // The start date of the course availability for sections.
    @ApiModelProperty(value = "Date course is available for building sections.", example = "01/01/2018")
    private Date start;

    // The end date of the course availability for sections.
    @ApiModelProperty(value = "Date course is no longer available for building sections.", example = "01/01/2029")
    private Date end;

    // The status of a course.
    @ApiModelProperty(value = "The status of the course", example = "Active, Inactive")
    private CourseStatus status;

    public Course() {
    }

    private Course(Builder builder) {
        setMisCode(builder.misCode);
        setSisCourseId(builder.sisCourseId);
        setSisTermId(builder.sisTermId);
        setC_id(builder.c_id);
        setControlNumber(builder.controlNumber);
        setSubject(builder.subject);
        setNumber(builder.number);
        setTitle(builder.title);
        setDescription(builder.description);
        setOutline(builder.outline);
        setPrerequisites(builder.prerequisites);
        setCorequisites(builder.corequisites);
        setPrerequisiteList(builder.prerequisiteList);
        setCorequisiteList(builder.corequisiteList);
        setMinimumUnits(builder.minimumUnits);
        setMaximumUnits(builder.maximumUnits);
        setTransferStatus(builder.transferStatus);
        setCreditStatus(builder.creditStatus);
        setGradingMethod(builder.gradingMethod);
        setCourseContacts(builder.courseContacts);
        setFee(builder.fee);
        setStart(builder.start);
        setEnd(builder.end);
        setStatus(builder.status);
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getSisCourseId() {
        return sisCourseId;
    }

    public void setSisCourseId(String sisCourseId) {
        this.sisCourseId = sisCourseId;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public String getC_id() {
        return c_id;
    }

    public void setC_id(String c_id) {
        this.c_id = c_id;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(String controlNumber) {
        this.controlNumber = controlNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public String getCorequisites() {
        return corequisites;
    }

    public void setCorequisites(String corequisites) {
        this.corequisites = corequisites;
    }

    public List<Course> getPrerequisiteList() {
        return prerequisiteList;
    }

    public void setPrerequisiteList(List<Course> prerequisiteList) {
        this.prerequisiteList = prerequisiteList;
    }

    public List<Course> getCorequisiteList() {
        return corequisiteList;
    }

    public void setCorequisiteList(List<Course> corequisiteList) {
        this.corequisiteList = corequisiteList;
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

    public TransferStatus getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(TransferStatus transferStatus) {
        this.transferStatus = transferStatus;
    }

    public CreditStatus getCreditStatus() {
        return creditStatus;
    }

    public void setCreditStatus(CreditStatus creditStatus) {
        this.creditStatus = creditStatus;
    }

    public GradingMethod getGradingMethod() {
        return gradingMethod;
    }

    public void setGradingMethod(GradingMethod gradingMethod) {
        this.gradingMethod = gradingMethod;
    }

    public List<CourseContact> getCourseContacts() {
        return courseContacts;
    }

    public void setCourseContacts(List<CourseContact> courseContacts) {
        this.courseContacts = courseContacts;
    }

    public Float getFee() {
        return fee;
    }

    public void setFee(Float fee) {
        this.fee = fee;
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

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }


    public static final class Builder {
        private String misCode;
        private String sisCourseId;
        private String sisTermId;
        private String c_id;
        private String controlNumber;
        private String subject;
        private String number;
        private String title;
        private String description;
        private String outline;
        private String prerequisites;
        private String corequisites;
        private List<Course> prerequisiteList;
        private List<Course> corequisiteList;
        private Float minimumUnits;
        private Float maximumUnits;
        private TransferStatus transferStatus;
        private CreditStatus creditStatus;
        private GradingMethod gradingMethod;
        private List<CourseContact> courseContacts;
        private Float fee;
        private Date start;
        private Date end;
        private CourseStatus status;

        public Builder() {
        }

        public Builder misCode(String val) {
            misCode = val;
            return this;
        }

        public Builder sisCourseId(String val) {
            sisCourseId = val;
            return this;
        }

        public Builder sisTermId(String val) {
            sisTermId = val;
            return this;
        }

        public Builder c_id(String val) {
            c_id = val;
            return this;
        }

        public Builder controlNumber(String val) {
            controlNumber = val;
            return this;
        }

        public Builder subject(String val) {
            subject = val;
            return this;
        }

        public Builder number(String val) {
            number = val;
            return this;
        }

        public Builder title(String val) {
            title = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder outline(String val) {
            outline = val;
            return this;
        }

        public Builder prerequisites(String val) {
            prerequisites = val;
            return this;
        }

        public Builder corequisites(String val) {
            corequisites = val;
            return this;
        }

        public Builder prerequisiteList(List<Course> val) {
            prerequisiteList = val;
            return this;
        }

        public Builder corequisiteList(List<Course> val) {
            corequisiteList = val;
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

        public Builder transferStatus(TransferStatus val) {
            transferStatus = val;
            return this;
        }

        public Builder creditStatus(CreditStatus val) {
            creditStatus = val;
            return this;
        }

        public Builder gradingMethod(GradingMethod val) {
            gradingMethod = val;
            return this;
        }

        public Builder courseContacts(List<CourseContact> val) {
            courseContacts = val;
            return this;
        }

        public Builder fee(Float val) {
            fee = val;
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

        public Builder status(CourseStatus val) {
            status = val;
            return this;
        }

        public Course build() {
            return new Course(this);
        }
    }

    @Override
    public String toString() {
        return "Course{" +
                "misCode='" + misCode + '\'' +
                ", sisCourseId='" + sisCourseId + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", c_id='" + c_id + '\'' +
                ", controlNumber='" + controlNumber + '\'' +
                ", subject='" + subject + '\'' +
                ", number='" + number + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", outline='" + outline + '\'' +
                ", prerequisites='" + prerequisites + '\'' +
                ", corequisites='" + corequisites + '\'' +
                ", prerequisiteList=" + prerequisiteList +
                ", corequisiteList=" + corequisiteList +
                ", minimumUnits=" + minimumUnits +
                ", maximumUnits=" + maximumUnits +
                ", transferStatus=" + transferStatus +
                ", creditStatus=" + creditStatus +
                ", gradingMethod=" + gradingMethod +
                ", courseContacts=" + courseContacts +
                ", fee=" + fee +
                ", start=" + start +
                ", end=" + end +
                ", status=" + status +
                '}';
    }
}
