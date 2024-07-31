package com.ccctc.adaptor.model.placement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Deprecated
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder(builderClassName = "Builder")
@AllArgsConstructor
@NoArgsConstructor
@ApiModel
public class PlacementTransaction implements Serializable  {

    @ApiModelProperty(value = "3 digit college MIS code", example = "111")
    private String misCode;

    @ApiModelProperty(value = "CCC ID of the student", example = "ABC123")
    private String cccid;

    @ApiModelProperty(value = "Secondary identifier for a student based on how that student has accessed Assess. Optional.", example = "student@college.edu")
    private String eppn;

    @ApiModelProperty(value = "Subject area and associated placement information")
    private PlacementSubjectArea subjectArea;
}
