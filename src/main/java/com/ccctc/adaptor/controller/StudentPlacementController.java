package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.placement.StudentPlacementData;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Optional;
import java.time.ZoneOffset;

@Api(
        value = "Student Placements",
        tags = "Student Placements",
        description = "Operations related to Student Placement data from Education Results Partners (ERP)",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/student-placements")
@Slf4j
public class StudentPlacementController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Get student placement data from the SIS staging table",
            response = StudentPlacementData.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{cccId}")
    public StudentPlacementData get(
            @ApiParam(value = "CCCID for student", example = "12345", required = true)
            @PathVariable("cccId") String cccId,

            @ApiParam(value = "MIS Code of the College", example = "001", required = true)
            @RequestParam("mis") String misCode,

            @ApiParam(value = "Statewide Student ID (optional)", example = "B123456", required = false)
            @RequestParam("ssId") Optional<String> ssId )
    {
        String theSSID = ssId.orElse( null);
        return groovyService.run(misCode, "StudentPlacement", "get", new Object[]{misCode, cccId, theSSID });
    }

    @ApiOperation(
            value = "Post a student placement to the SIS"
    )
    @ApiResponses({
    })
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping
    public StudentPlacementData post(
            @ApiParam(value = "MIS Code of the College", example = "001", required = true)
            @RequestParam("mis") String misCode,

            @ApiParam(value = "CCCID for student", example = "001", required = true)
            @RequestParam("cccId") String cccId,

            @ApiParam(value = "Statewide Student ID (optional)", example = "B123456", required = false)
            @RequestParam("ssId") Optional<String> ssId,

            @ApiParam(value = "Student Placement Data", required = true, example = "{\n" +
                    "  \"dataSource\": 3,\n" +
                    "  \"english\": 1,\n" +
                    "  \"slam\": 1,\n" +
                    "  \"stem\": 3,\n" +
                    "  \"isAlgI\": false,\n" +
                    "  \"isAlgII\": false,\n" +
                    "  \"trigonometry\": false,\n" +
                    "  \"preCalculus\": false,\n" +
                    "  \"calculus\": false,\n" +
                    "  \"completedEleventhGrade\": null,\n" +
                    "  \"cumulativeGradePointAverage\": 3.2,\n" +
                    "  \"englishCompletedCourseId\": 2,\n" +
                    "  \"englishCompletedCourseGrade\": \"A\",\n" +
                    "  \"mathematicsCompletedCourseId\": 1,\n" +
                    "  \"mathematicsCompletedCourseGrade\": \"A\",\n" +
                    "  \"mathematicsPassedCourseId\": null,\n" +
                    "  \"mathematicsPassedCourseGrade\": null\n" +
                    "  \"placementStatus\": \"COMPLETE_PLACEMENT\", \n" +
                    "  \"tstmpERPTransmit\": 1553023308853 \n" +
                    "}")
            @RequestBody StudentPlacementData placementData) {


        placementData.setMiscode(misCode);
        placementData.setCaliforniaCommunityCollegeId(cccId);
        if( ssId.isPresent() ) {
            placementData.setStatewideStudentId(ssId.get());
        }

        placementData.setTstmpSISTransmit(LocalDateTime.now(ZoneOffset.UTC));

        groovyService.run(misCode, "StudentPlacement", "post", new Object[]{misCode, placementData});

        // TODO: update swagger docs and response codes as appropriate
        return placementData;
    }
}