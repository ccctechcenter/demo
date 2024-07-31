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
import java.util.Date;
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
public class Placement implements Serializable {

    @ApiModelProperty(value = "Statewide CB21 code character (Y,A-H)", example = "B")
    private Character cb21;

    @ApiModelProperty(value = "A number to represent the specific level within a cb21. Combined with a cb21 code, it uniquely identifies a level in the course sequence.")
    private String courseGroup;

    @ApiModelProperty(value = "Timestamp the placement was created")
    private Date placementDate;

    @ApiModelProperty(value = "Unique identifier for the placement")
    private String placementId;

    @ApiModelProperty(value = "English, ESL, NA")
    private ElaIndicator elaIndicator;

    @ApiModelProperty(value = "Indicates if the placement is assigned or not. An assigned placement is the active placement for the student", example = "true")
    private Boolean isAssigned;

    @ApiModelProperty(value = "When the placement was assigned")
    private Date assignedDate;

    @ApiModelProperty(value = "List of courses in the placement")
    private List<PlacementCourse> courses;

    @ApiModelProperty(value = "List of placement component IDs that were considered as part of this placement")
    private List<String> placementComponentIds;
}
