package com.ccctc.adaptor.model.placement;

import com.ccctc.adaptor.model.PrimaryKey;
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
import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder(builderClassName = "Builder")
@AllArgsConstructor
@NoArgsConstructor
@ApiModel
@PrimaryKey({"miscode", "californiaCommunityCollegeId", "statewideStudentId"})
/**
 * Extension of ERP model : https://ab705.calpassplus.org/docs/index.html
 */
public class StudentPlacementData implements Serializable {


    @ApiModelProperty(value = "College mis code of college student completed application.", example = "111")
    private String miscode;
    @ApiModelProperty(value = "CCCID of the student", example = "ABC123")
    private String californiaCommunityCollegeId;
    @ApiModelProperty(value = "ssid of student completing CCCApply application", example = "B123456")
    private String statewideStudentId;
    @ApiModelProperty(value = "Id of completed CCCApply application", example = "110153")
    private Long appId;


    // copied from https://ab705.calpassplus.org/swagger/v1/swagger.json
    @ApiModelProperty(value = "Source Of Placement Data\n" +
            "1 = California College Guidance Initiative (CCGI)\n" +
            "2 = Cal-PASS Plus (CPP)\n" +
            "3 = CCC Apply (APP)\n" +
            "4 = Combination of CPP and APP\n" +
            "5 = CDE/CalPADS\n" +
            "6 = Combination of CDE and APP\n" +
            "7 = Combination of CDE, CPP, and APP")
    private Integer dataSource;
    @ApiModelProperty(value = "English Support Recommendation\n" +
            "1 = Support Not Recommended\n" +
            "2 = Support Recommended\n" +
            "3 = Support Strongly Recommended")
    private Integer english;
    @ApiModelProperty(value = "SLAM Support Recommendation\n" +
            "1 = Support Not Recommended\n" +
            "2 = Support Recommended\n" +
            "3 = Support Strongly Recommended" )
    private Integer slam;
    @ApiModelProperty(value = "STEM Support Recommendation\n" +
            "1 = Support Not Recommended\n" +
            "2 = Support Recommended\n" +
            "3 = Support Strongly Recommended")
    private Integer stem;
    @ApiModelProperty(value = "Successfully Completed Algebra I")
    private Boolean isAlgI;
    @ApiModelProperty(value = "Successfully Completed Algebra II")
    private Boolean isAlgII;
    @ApiModelProperty(value = "Trigonometry Recommendation\n" +
            "True = Recommended\n" +
            "False = Not Recommended")
    private Boolean trigonometry;
    @ApiModelProperty(value = "PreCalculus Recommendation\n" +
            "True = Recommended\n" +
            "False = Not Recommended")
    private Boolean preCalculus;
    @ApiModelProperty(value = "Calculus Recommendation\n" +
            "True = Recommended\n" +
            "False = Not Recommended" )
    private Boolean calculus;

    @ApiModelProperty(value = "Completed Eleventh Grade")
    private Boolean completedEleventhGrade;
    @ApiModelProperty(value = "Cumulative Grade Point Average" )
    private Float cumulativeGradePointAverage;
    @ApiModelProperty(value = "Highest English Course Completed")
    private Integer englishCompletedCourseId;
    @ApiModelProperty(value = "Grade of Highest English Course Completed" )
    private String englishCompletedCourseGrade;
    @ApiModelProperty(value = "Highest Mathematics Course Completed" )
    private Integer mathematicsCompletedCourseId;
    @ApiModelProperty(value = "Grade of Highest Mathematics Course Completed" )
    private String mathematicsCompletedCourseGrade;
    @ApiModelProperty(value = "Highest Mathematics Passed" )
    private Integer mathematicsPassedCourseId;
    @ApiModelProperty(value = "Grade of Highest Mathematics Course Passed" )
    private String mathematicsPassedCourseGrade;
    @ApiModelProperty(value = "Placement Status" )
    private String placementStatus;

    @ApiModelProperty(value = "Date/Time Received from ERP" )
    private Long tstmpERPTransmit;
    @ApiModelProperty(value = "Date/Time Written to SIS" )
    private LocalDateTime tstmpSISTransmit;

    @ApiModelProperty(value = "Processing Status by SIS" )
    private String sisProcessedFlag;

    @ApiModelProperty(value = "Date/Time Processed by SIS" )
    private LocalDateTime tstmpSISProcessed;

    @ApiModelProperty(value = "SIS Notes when processing" )
    private String sisProcessedNotes;

    @ApiModelProperty(value = "Highest Grade Completed" )
    private String highestGradeCompleted;

}