package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

@ApiModel
public class CrosslistingDetail implements Serializable {
    @ApiModelProperty(required = true, value = "Name / identifier of the crosslisting that uniquely identifies it in the term")
    private String name;

    @ApiModelProperty(required = true, value = "Term of the crosslisting", example = "2018SP")
    private String sisTermId;

    @ApiModelProperty(required = true, value = "SIS Section ID of the primary section in the crosslisting", example = "1234")
    private String primarySisSectionId;

    @ApiModelProperty(required = true, value = "SIS Section IDs of all sections in the crosslisting")
    private List<String> sisSectionIds;

    public CrosslistingDetail() {
    }

    private CrosslistingDetail(Builder builder) {
        setName(builder.name);
        setSisTermId(builder.sisTermId);
        setPrimarySisSectionId(builder.primarySisSectionId);
        setSisSectionIds(builder.sisSectionIds);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSisTermId() {
        return sisTermId;
    }

    public void setSisTermId(String sisTermId) {
        this.sisTermId = sisTermId;
    }

    public String getPrimarySisSectionId() {
        return primarySisSectionId;
    }

    public void setPrimarySisSectionId(String primarySisSectionId) {
        this.primarySisSectionId = primarySisSectionId;
    }

    public List<String> getSisSectionIds() {
        return sisSectionIds;
    }

    public void setSisSectionIds(List<String> sisSectionIds) {
        this.sisSectionIds = sisSectionIds;
    }

    public static final class Builder {
        private String name;
        private String sisTermId;
        private String primarySisSectionId;
        private List<String> sisSectionIds;

        public Builder() {
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder sisTermId(String val) {
            sisTermId = val;
            return this;
        }

        public Builder primarySisSectionId(String val) {
            primarySisSectionId = val;
            return this;
        }

        public Builder sisSectionIds(List<String> val) {
            sisSectionIds = val;
            return this;
        }

        public CrosslistingDetail build() {
            return new CrosslistingDetail(this);
        }
    }

    @Override
    public String toString() {
        return "CrosslistingDetail{" +
                "name='" + name + '\'' +
                ", sisTermId='" + sisTermId + '\'' +
                ", primarySisSectionId='" + primarySisSectionId + '\'' +
                ", sisSectionIds=" + sisSectionIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrosslistingDetail that = (CrosslistingDetail) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (sisTermId != null ? !sisTermId.equals(that.sisTermId) : that.sisTermId != null) return false;
        if (primarySisSectionId != null ? !primarySisSectionId.equals(that.primarySisSectionId) : that.primarySisSectionId != null)
            return false;
        return sisSectionIds != null ? sisSectionIds.equals(that.sisSectionIds) : that.sisSectionIds == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (sisTermId != null ? sisTermId.hashCode() : 0);
        result = 31 * result + (primarySisSectionId != null ? primarySisSectionId.hashCode() : 0);
        result = 31 * result + (sisSectionIds != null ? sisSectionIds.hashCode() : 0);
        return result;
    }
}
