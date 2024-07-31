package com.ccctc.adaptor.model.placement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Deprecated
@EqualsAndHashCode
@Getter
@Setter
@ApiModel(value = "PlacementComponent", subTypes = {PlacementComponentCCCAssess.class, PlacementComponentMmap.class}, discriminator = "type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlacementComponentCCCAssess.class, name = "CCCAssess"),
        @JsonSubTypes.Type(value = PlacementComponentMmap.class, name = "Mmap")
})
public class PlacementComponent implements Serializable {

    @ApiModelProperty(value = "Statewide CB21 code character (Y,A-H)", example = "B")
    protected Character cb21;

    @ApiModelProperty(value = "A number to represent the specific level within a cb21. Combined with a cb21 code, it uniquely identifies a level in the course sequence.")
    protected String courseGroup;

    @ApiModelProperty(value = "Timestamp the placement component was created")
    protected Date placementComponentDate;

    @ApiModelProperty(value = "Unique identifier for the placement component")
    protected String placementComponentId;

    @ApiModelProperty(value = "English, ESL, or NA")
    protected ElaIndicator elaIndicator;

    @ApiModelProperty(value = "The event that triggered the generation of the placement component")
    protected String trigger;

    @ApiModelProperty(value = "List of courses in the placement component")
    protected List<PlacementCourse> courses;

    @ApiModelProperty(required = true, value = "Type of placement component - specifies which PlacementComponent object this")
    private final PlacementComponentType type;

    PlacementComponent(PlacementComponentType type) {
        this.type = type;
    }
}
