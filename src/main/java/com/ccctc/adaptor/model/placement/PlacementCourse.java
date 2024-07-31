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
public class PlacementCourse implements Serializable {

    @ApiModelProperty(value = "Subject for the course the student has placed into.", example = "ENGL")
    private String subject;

    @ApiModelProperty(value = "Number for the course the student has placed into.", example = "1")
    private String number;

    @ApiModelProperty(value = "Value provided by course author. Designed to pass through to the SIS for internal identification of a placement. For example, this code may be used to unlock courses for registration.")
    private String sisTestMappingLevel;
}
