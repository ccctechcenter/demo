package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.Enrollment;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.model.mock.EnrollmentDB;
import com.ccctc.adaptor.model.mock.StudentDB;
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
        value = "Mock Enrollments",
        tags = "Mock Enrollments",
        description = "Operations related to Mock Enrollment data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockEnrollmentController {

    private final static Logger logger = LoggerFactory.getLogger(MockEnrollmentController.class);

    private EnrollmentDB enrollmentDB;

    @Autowired
    public MockEnrollmentController(EnrollmentDB enrollmentDB) {
        this.enrollmentDB = enrollmentDB;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get Enrollment records from the mock database with optional filtering",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/enrollments")
    public List<Enrollment> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? enrollmentDB.getAllSorted() : enrollmentDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by TERM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Enrollment records from the mock database for a Term",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/enrollments")
    public List<Enrollment> getByTerm(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                      @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId) {
        logger.debug("Mock: getByTerm {} {}", misCode, sisTermId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);

        return enrollmentDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by STUDENT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Enrollment records from the mock database for a Student in a Term",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/enrollments")
    public List<Enrollment> getByStudent(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                         @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: getByStudent {} {} {}", misCode, sisTermId, sisPersonId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisPersonId", sisPersonId);

        return enrollmentDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by SECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Enrollment records from the mock database for a Section",
            response = Enrollment.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/enrollments")
    public List<Enrollment> getBySection(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                         @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId) {
        logger.debug("Mock: getBySection {} {} {}", misCode, sisTermId, sisSectionId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisSectionId", sisSectionId);

        return enrollmentDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get an Enrollment record from the mock database",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/enrollments/{sisPersonId:.+}")
    public Enrollment get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                          @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                          @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                          @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: get {} {} {} {}", misCode, sisTermId, sisSectionId, sisPersonId);
        return enrollmentDB.get(misCode, sisPersonId, sisTermId, sisSectionId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add an Enrollment record to the mock database",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/enrollments")
    public Enrollment post(@ApiParam(value = "Enrollment to add", required = true) @RequestBody Enrollment enrollment) {
        logger.debug("Mock: add {}", enrollment);
        return enrollmentDB.add(enrollment);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update an Enrollment record in the mock database",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/enrollments/{sisPersonId:.+}")
    public Enrollment put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                          @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                          @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                          @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                          @ApiParam(value = "Enrollment to update", required = true) @RequestBody Enrollment enrollment) {
        logger.debug("Mock: put {} {} {} {} {}", misCode, sisTermId, sisSectionId, sisPersonId, enrollment);
        return enrollmentDB.update(misCode, sisPersonId, sisTermId, sisSectionId, enrollment);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch an Enrollment record in the mock database",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/enrollments/{sisPersonId:.+}")
    public Enrollment patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                            @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                            @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                            @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                            @ApiParam(value = "Enrollment to update", required = true) @RequestBody Map enrollment) {
        logger.debug("Mock: patch {} {} {} {} {}", misCode, sisTermId, sisSectionId, sisPersonId, enrollment);
        return enrollmentDB.patch(misCode, sisPersonId, sisTermId, sisSectionId, enrollment);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete an Enrollment record in the mock database",
            response = Enrollment.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/enrollments/{sisPersonId:.+}")
    public Enrollment delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                             @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                             @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                             @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                             @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                                 @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {} {}", misCode, sisTermId, sisSectionId, sisPersonId, cascade);
        return enrollmentDB.delete(misCode, sisPersonId, sisTermId, sisSectionId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Course data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/enrollments/reload")
    public void reload() {
        logger.debug("Mock: reload");
        enrollmentDB.loadData();
    }
}