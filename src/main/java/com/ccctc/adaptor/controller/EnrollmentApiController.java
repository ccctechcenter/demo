package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.Enrollment;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.PrerequisiteStatus;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Api(
        value="Enrollments",
        tags="Enrollments",
        description="Operations related to enrollments stored in an SIS",
        produces="application/json",
        position=1)
@RestController
@RequestMapping("/enrollments")
public class EnrollmentApiController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve all enrollments for a section by SIS Section ID and SIS Term ID",
            notes = "Possible error codes: sectionNotFound, termNotFound",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping(value = "/section/{sisSectionId:.+}")
    public List<Enrollment> getSection(@ApiParam(value = "SIS Section ID", example = "1234", required = true) @PathVariable("sisSectionId") String sisSectionId,
                                       @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                                       @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Enrollment", "getSection", new Object[]{misCode, sisTermId, sisSectionId});
    }

    @ApiOperation(
            value = "Retrieve all enrollments for a student, optionally filtering by SIS Term ID and/or SIS Section ID",
            notes = "Possible error codes: multipleResultsFound, sectionNotFound, studentNotFound, termNotFound",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping(value = "/student/{cccid}")
    public List<Enrollment> getStudent(@ApiParam(value = "CCC ID", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                                       @ApiParam(value = "SIS Term ID", example = "2017FA") @RequestParam(value = "sisTermId", required = false) String sisTermId,
                                       @ApiParam(value = "SIS Section ID", example = "1234") @RequestParam(value = "sisSectionId", required = false) String sisSectionId,
                                       @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Enrollment", "getStudent", new Object[]{misCode, sisTermId, cccid, sisSectionId});
    }

    @ApiOperation(
            value = "Enroll a student in a section",
            notes = "The body of this request must contain cccid, sisTermId and sisSectionId. Other fields in the body may or may not be supported at this time. Possible error codes: multipleResultsFound, studentNotFound, studentNotEnrollable, generalEnrollmentError, sisQueryError, sectionNotFound, termNotFound, alreadyEnrolled",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotEnrollable"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "generalEnrollmentError"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sisQueryError"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 409, response = ErrorResource.class, message = "alreadyEnrolled")
    })
    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public Enrollment post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                           @ApiParam(value = "Enrollment to create. Must contain cccid, sisTermId and sisSectionId.", required = true) @RequestBody Enrollment enrollment) {

        return groovyService.run(misCode, "Enrollment", "post", new Object[]{misCode, enrollment});
    }

    @ApiOperation(
            value = "Update an enrollment",
            notes = "Currently the only supported action is dropping an enrollment. To perform a drop, pass in an Enrollment object with enrollmentStatus=Dropped. Possible error codes: multipleResultsFound, studentNotFound, generalEnrollmentError, sectionNotFound, termNotFound",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "generalEnrollmentError"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound")
    })
    @PutMapping(value = "/{cccid}")
    public Enrollment put(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                          @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                          @ApiParam(value = "SIS Section ID", example = "1234", required = true) @RequestParam("sisSectionId") String sisSectionId,
                          @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                          @ApiParam(value = "The enrollment to be updated", required = true) @RequestBody Enrollment enrollment) {

        return groovyService.run(misCode, "Enrollment", "put", new Object[]{misCode, cccid, sisSectionId, sisTermId, enrollment});
    }

    @ApiOperation(
            value = "Retrieve a student's prerequisite status for a given course by start date of anticipated enrollment",
            notes = "Possible error codes: multipleResultsFound, studentNotFound, courseNotFound",
            response = PrerequisiteStatus.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @GetMapping(value = "/prerequisitestatus/{sisCourseId:.+}")
    public PrerequisiteStatus getPrereqStatus(@ApiParam(value = "SIS Course ID", example = "ENGL-100", required = true) @PathVariable("sisCourseId") String sisCourseId,
                                              @ApiParam(value = "Anticipated start date of enrollment", required = true) @RequestParam("start") Long start,
                                              @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                              @ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @RequestParam("cccid") String cccid) {

        Date startDate = new Date(start);
        return groovyService.run(misCode, "Enrollment", "getPrereqStatus", new Object[]{misCode, sisCourseId, startDate, cccid});
    }
}