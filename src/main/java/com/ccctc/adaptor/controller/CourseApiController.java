package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.Course;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(
        value="Courses",
        tags="Courses",
        description="Operations related to Course data",
        produces="application/json",
        position=1)
@RestController
@RequestMapping(value = "/courses")
public class CourseApiController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve course by SIS Course Identifier which is the concatenation of the subject/department and course number with a dash in the middle, ie ENGL-100.",
            notes = "Possible error codes: multipleResultsFound, studentNotFound",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{sisCourseId:.+}")
    public Course get(@ApiParam(value = "SIS Course ID", example = "ENGL-100", required = true) @PathVariable("sisCourseId") String sisCourseId,
                      @ApiParam(value = "SIS Term ID", example = "2017FA") @RequestParam(value = "sisTermId", required = false) String sisTermId,
                      @ApiParam(value = "MIS Code of College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Course", "get", new Object[]{misCode, sisCourseId, sisTermId});
    }
}