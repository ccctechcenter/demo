package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

@PrimaryKey("misCode")
public class College implements Serializable {

    @ApiModelProperty(required = true, value = "3 digit college MIS code", example = "001")
    private String misCode;

    @ApiModelProperty(required = true, value = "3 digit district MIS code", example = "000")
    private String districtMisCode;

    @ApiModelProperty(required = true, value = "Name of the college", example = "Test College")
    private String name;

    public String getMisCode() {
        return misCode;
    }

    public void setMisCode(String misCode) {
        this.misCode = misCode;
    }

    public String getDistrictMisCode() {
        return districtMisCode;
    }

    public void setDistrictMisCode(String districtMisCode) {
        this.districtMisCode = districtMisCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "College{" +
                "misCode='" + misCode + '\'' +
                ", districtMisCode='" + districtMisCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
