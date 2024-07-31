package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.Course;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.mock.CourseDB;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(
        value = "Mock Courses",
        tags = "Mock Courses",
        description = "Operations related to Mock Course data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockCourseController {

    private final static Logger logger = LoggerFactory.getLogger(MockCourseController.class);

    private CourseDB courseDB;

    @Autowired
    public MockCourseController(CourseDB courseDB) {
        this.courseDB = courseDB;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Course records from the mock database with optional filtering",
            response = Course.class,
            responseContainer = "List"
    )
    @GetMapping("/courses")
    public List<Course> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? courseDB.getAllSorted() : courseDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by Term ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Course records from the mock database with optional filtering",
            response = Course.class,
            responseContainer = "List"
    )
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses")
    public List<Course> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                            @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId) {
        logger.debug("Mock: get {} {}", misCode, sisTermId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);

        return courseDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a course record to the mock database",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/courses")
    public Course post(@ApiParam(value = "Course to add", required = true) @RequestBody Course course) {
        logger.debug("Mock: add {}", course);
        return courseDB.add(course);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Course record from the mock database",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}")
    public Course get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                      @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                      @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId) {
        logger.debug("Mock: get {} {} {}", misCode, sisTermId, sisCourseId);
        return courseDB.get(misCode, sisTermId, sisCourseId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a course record in the mock database",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}")
    public Course put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                      @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                      @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId,
                      @ApiParam(value = "Course to update", required = true) @RequestBody Course course) {
        logger.debug("Mock: put {} {} {} {}", misCode, sisTermId, sisCourseId, course);
        return courseDB.update(misCode, sisTermId, sisCourseId, course);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a course record in the mock database",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}")
    public Course patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                        @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                        @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId,
                        @ApiParam(value = "Course to update", required = true) @RequestBody Map course) {
        logger.debug("Mock: patch {} {} {} {}", misCode, sisTermId, sisCourseId, course);
        return courseDB.patch(misCode, sisTermId, sisCourseId, course);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a course record in the mock database",
            response = Course.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}")
    public Course delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "SIS Course ID", example = "ENGL-100") @PathVariable("sisCourseId") String sisCourseId,
                         @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                         @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {}", misCode, sisTermId, sisCourseId, cascade);
        return courseDB.delete(misCode, sisTermId, sisCourseId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Course data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/courses/reload")
    public void reload() {
        logger.debug("Mock: reload");
        courseDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ COPY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Copy a Course to a new term"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @PostMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}/copy")
    public Course copy(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "SIS Course ID", example = "ENGL-100") @PathVariable("sisCourseId") String sisCourseId,
                       @ApiParam(value = "New SIS Term ID", example = "2018SP", required = true) @RequestParam("newSisTermId") String newSisTermId) {
        logger.debug("Mock: copy {} {} {} {}", misCode, sisTermId, sisCourseId, newSisTermId);
        return courseDB.copy(misCode, sisTermId, sisCourseId, newSisTermId);
    }
}