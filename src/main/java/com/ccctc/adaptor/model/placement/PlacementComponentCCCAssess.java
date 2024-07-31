package com.ccctc.adaptor.model.placement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ApiModel
public class PlacementComponentCCCAssess extends PlacementComponent implements Serializable {

    @ApiModelProperty(value = "Unique identifier for the assessment session for the student's attempt")
    private String assessmentAttemptId;

    @ApiModelProperty(value = "Title of the assessment taken by the student")
    private String assessmentTitle;

    @ApiModelProperty(value = "Date/Time the student completed the assessment")
    private Date completeDate;

    public PlacementComponentCCCAssess() {
        super(PlacementComponentType.CCCAssess);
    }

    private PlacementComponentCCCAssess(Builder builder) {
        this();
        setCb21(builder.cb21);
        setCourseGroup(builder.courseGroup);
        setPlacementComponentDate(builder.placementComponentDate);
        setPlacementComponentId(builder.placementComponentId);
        setElaIndicator(builder.elaIndicator);
        setTrigger(builder.trigger);
        setCourses(builder.courses);
        setAssessmentAttemptId(builder.assessmentAttemptId);
        setAssessmentTitle(builder.assessmentTitle);
        setCompleteDate(builder.completeDate);
    }

    public String getAssessmentAttemptId() {
        return assessmentAttemptId;
    }

    public void setAssessmentAttemptId(String assessmentAttemptId) {
        this.assessmentAttemptId = assessmentAttemptId;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public void setAssessmentTitle(String assessmentTitle) {
        this.assessmentTitle = assessmentTitle;
    }

    public Date getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(Date completeDate) {
        this.completeDate = completeDate;
    }

    public static final class Builder {
        private Character cb21;
        private String courseGroup;
        private Date placementComponentDate;
        private String placementComponentId;
        private ElaIndicator elaIndicator;
        private String trigger;
        private List<PlacementCourse> courses;
        private PlacementComponentType type;
        private String assessmentAttemptId;
        private String assessmentTitle;
        private Date completeDate;

        public Builder() {
        }

        public Builder cb21(Character val) {
            cb21 = val;
            return this;
        }

        public Builder courseGroup(String val) {
            courseGroup = val;
            return this;
        }

        public Builder placementComponentDate(Date val) {
            placementComponentDate = val;
            return this;
        }

        public Builder placementComponentId(String val) {
            placementComponentId = val;
            return this;
        }

        public Builder elaIndicator(ElaIndicator val) {
            elaIndicator = val;
            return this;
        }

        public Builder trigger(String val) {
            trigger = val;
            return this;
        }

        public Builder courses(List<PlacementCourse> val) {
            courses = val;
            return this;
        }

        public Builder type(PlacementComponentType val) {
            type = val;
            return this;
        }

        public Builder assessmentAttemptId(String val) {
            assessmentAttemptId = val;
            return this;
        }

        public Builder assessmentTitle(String val) {
            assessmentTitle = val;
            return this;
        }

        public Builder completeDate(Date val) {
            completeDate = val;
            return this;
        }

        public PlacementComponentCCCAssess build() {
            return new PlacementComponentCCCAssess(this);
        }
    }

    @Override
    public String toString() {
        return "PlacementComponentCCCAssess{" +
                "assessmentAttemptId='" + assessmentAttemptId + '\'' +
                ", assessmentTitle='" + assessmentTitle + '\'' +
                ", completeDate=" + completeDate +
                ", cb21=" + cb21 +
                ", courseGroup='" + courseGroup + '\'' +
                ", placementComponentDate=" + placementComponentDate +
                ", placementComponentId='" + placementComponentId + '\'' +
                ", elaIndicator=" + elaIndicator +
                ", trigger='" + trigger + '\'' +
                ", courses=" + courses +
                '}';
    }
}
