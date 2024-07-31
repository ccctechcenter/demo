package com.ccctc.adaptor.model;

import com.ccctc.adaptor.util.CoverageIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

@ApiModel
public class CourseExchangeEnrollment implements Serializable {
    @ApiModelProperty(value = "3 digit college MIS code", example = "111")
    private String misCode;
    @ApiModelProperty(value = "Course Exchange college where class is taught", example = "Test College")
    private String collegeName;
    @ApiModelProperty(value = "Units the student enrolled in.")
    private Float units;
    // Identifier for C-ID.net course
    @ApiModelProperty(value = "C-ID.net course identifier. This field is required for the Course Exchange project but may not be required for other projects.", example = "ACCT 110")
    private String c_id;
    @ApiModelProperty(value = "Section object at Course Exchange college where class taken.")
    private Section section;

    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    public CourseExchangeEnrollment() {}

    private CourseExchangeEnrollment(Builder builder) {
        setMisCode(builder.misCode);
        setCollegeName((builder.collegeName));
        setUnits(builder.units);
        setC_id(builder.c_id);
        setSection(builder.section);
    }

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public Float getUnits() {
        return units;
    }

    public void setUnits(Float units) {
        this.units = units;
    }

    public String getC_id() {
        return c_id;
    }

    public void setC_id(String c_id) {
        this.c_id = c_id;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    @CoverageIgnore
    @Override
    public String toString() {
        return "CourseExchangeEnrollment{" +
                "misCode='" + misCode + '\'' +
                ", collegeName='" + collegeName + '\'' +
                ", units=" + units +
                ", c_id='" + c_id + '\'' +
                ", section=" + section +
                '}';
    }

    public static final class Builder {
        private String misCode;
        private String collegeName;
        private Float units;
        private String c_id;
        private Section section;


        public Builder misCode(String misCode) {
            this.misCode = misCode;
            return this;
        }

        public Builder collegeName(String collegeName) {
            this.collegeName = collegeName;
            return this;
        }

        public Builder units(Float units) {
            this.units = units;
            return this;
        }

        public Builder c_id(String c_id) {
            this.c_id = c_id;
            return this;
        }

        public Builder section(Section section) {
            this.section = section;
            return this;
        }

        public CourseExchangeEnrollment build() {
            return new CourseExchangeEnrollment(this);
        }
    }
}
