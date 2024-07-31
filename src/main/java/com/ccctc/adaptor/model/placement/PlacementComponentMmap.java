package com.ccctc.adaptor.model.placement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ApiModel
public class PlacementComponentMmap extends PlacementComponent implements Serializable {

    @ApiModelProperty(value = "Would indicate the source of the data. i.e Apply")
    private String dataSource;

    @ApiModelProperty(value = "Would indicate the type of the data. i.e. Self Reported")
    private DataSourceType dataSourceType;

    @ApiModelProperty(value = "Would indicate the date the data was collected or submitted")
    private Date dataSourceDate;

    @ApiModelProperty(value = "Would indicate the course results if applicable from execution of the mmap decision trees. i.e. Calculus-No, Pre-Calculus-Yes")
    private List<String> mmapQualifiedCourses;

    @ApiModelProperty(value = "CB21 code returned from the MMAP Decision Tree")
    private Character mmapCb21Code;

    public PlacementComponentMmap() {
        super(PlacementComponentType.Mmap);
    }

    private PlacementComponentMmap(Builder builder) {
        this();
        setCb21(builder.cb21);
        setCourseGroup(builder.courseGroup);
        setPlacementComponentDate(builder.placementComponentDate);
        setPlacementComponentId(builder.placementComponentId);
        setElaIndicator(builder.elaIndicator);
        setTrigger(builder.trigger);
        setCourses(builder.courses);
        setDataSource(builder.dataSource);
        setDataSourceType(builder.dataSourceType);
        setDataSourceDate(builder.dataSourceDate);
        setMmapQualifiedCourses(builder.mmapQualifiedCourses);
        setMmapCb21Code(builder.mmapCb21Code);
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public Date getDataSourceDate() {
        return dataSourceDate;
    }

    public void setDataSourceDate(Date dataSourceDate) {
        this.dataSourceDate = dataSourceDate;
    }

    public List<String> getMmapQualifiedCourses() {
        return mmapQualifiedCourses;
    }

    public void setMmapQualifiedCourses(List<String> mmapQualifiedCourses) {
        this.mmapQualifiedCourses = mmapQualifiedCourses;
    }

    public Character getMmapCb21Code() {
        return mmapCb21Code;
    }

    public void setMmapCb21Code(Character mmapCb21Code) {
        this.mmapCb21Code = mmapCb21Code;
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
        private String dataSource;
        private DataSourceType dataSourceType;
        private Date dataSourceDate;
        private List<String> mmapQualifiedCourses;
        private Character mmapCb21Code;

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

        public Builder dataSource(String val) {
            dataSource = val;
            return this;
        }

        public Builder dataSourceType(DataSourceType val) {
            dataSourceType = val;
            return this;
        }

        public Builder dataSourceDate(Date val) {
            dataSourceDate = val;
            return this;
        }

        public Builder mmapQualifiedCourses(List<String> val) {
            mmapQualifiedCourses = val;
            return this;
        }

        public Builder mmapCb21Code(Character val) {
            mmapCb21Code = val;
            return this;
        }

        public PlacementComponentMmap build() {
            return new PlacementComponentMmap(this);
        }
    }

    @Override
    public String toString() {
        return "PlacementComponentMmap{" +
                "dataSource='" + dataSource + '\'' +
                ", dataSourceType=" + dataSourceType +
                ", dataSourceDate=" + dataSourceDate +
                ", mmapQualifiedCourses=" + mmapQualifiedCourses +
                ", mmapCb21Code=" + mmapCb21Code +
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
