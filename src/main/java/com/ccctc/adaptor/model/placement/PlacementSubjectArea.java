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
import java.util.List;

@Deprecated
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder(builderClassName = "Builder")
@AllArgsConstructor
@NoArgsConstructor
@ApiModel
public class PlacementSubjectArea implements Serializable {

    @ApiModelProperty(value = "Competency map discipline chosen during authoring of the subject area. Current values include English, ESL, Math.", example = "English")
    private CompetencyMapDiscipline competencyMapDiscipline;

    @ApiModelProperty(value = "Unique identifier for a subject area")
    private Integer subjectAreaId;

    @ApiModelProperty(value = "Combined with subjectAreaId, provides a unique composite key for a published subject area")
    private Integer subjectAreaVersion;

    @ApiModelProperty(value = "Name of the subject area provided during authoring")
    private String title;

    @ApiModelProperty(value = "Value provided by subject area author. Designed to pass through to the SIS for internal identification of the subject area.")
    private String sisTestName;

    @ApiModelProperty(value = "Subject Area author has chosen to opt into use of multiple measures in general", example = "true")
    private Boolean optInMultipleMeasures;

    @ApiModelProperty(value = "Subject Area author has chosen to opt into using MMAP Placement Components specifically", example = "true")
    private Boolean optInMmap;

    @ApiModelProperty(value = "Subject Area author has chosen to opt into using Self Reported data for generation of MMAP Placement Components", example = "true")
    private Boolean optInSelfReported;

    @ApiModelProperty(value = "List of placements in this subject area")
    private List<Placement> placements;

    @ApiModelProperty(value = "List of placement components in this subject area")
    private List<PlacementComponent> placementComponents;
}
