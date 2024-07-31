package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.model.mock.StudentDB;
import com.ccctc.adaptor.model.mock.StudentPrereqDB;
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
        value = "Mock Student Pre-reqs",
        tags = "Mock Student Pre-reqs",
        description = "Operations related to Mock Student prerequisite data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockStudentPrereqController {

    private final static Logger logger = LoggerFactory.getLogger(MockStudentPrereqController.class);

    private StudentPrereqDB studentPrereqDB;

    @Autowired
    public MockStudentPrereqController(StudentPrereqDB studentPrereqDB) {
        this.studentPrereqDB = studentPrereqDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Student prerequisite records from the mock database with optional filtering",
            response = StudentPrereqDB.PrereqInfo.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @GetMapping("/prereqs")
    public List<StudentPrereqDB.PrereqInfo> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? studentPrereqDB.getAllSorted() : studentPrereqDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by STUDENT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get Student prerequisite records from the mock database for a specific student",
            response = StudentPrereqDB.PrereqInfo.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @GetMapping("/colleges/{misCode}/students/{sisPersonId:.+}/prereqs")
    public List<StudentPrereqDB.PrereqInfo> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                                @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {

        logger.debug("Mock: get {} {}", misCode, sisPersonId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisPersonId", sisPersonId);

        return studentPrereqDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Student prerequisite record from the mock database",
            response = StudentPrereqDB.PrereqInfo.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/students/{sisPersonId:.+}/prereqs/{sisCourseId:.+}")
    public StudentPrereqDB.PrereqInfo get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                          @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                                          @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId) {
        logger.debug("Mock: get {} {} {}", misCode, sisPersonId, sisCourseId);

        return studentPrereqDB.get(misCode, sisPersonId, sisCourseId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Student prerequisite record to the mock database",
            response = StudentPrereqDB.PrereqInfo.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/prereqs")
    public StudentPrereqDB.PrereqInfo post(@ApiParam(value = "Prerequisite to add", required = true) @RequestBody StudentPrereqDB.PrereqInfo prereqInfo) {
        logger.debug("Mock: add {}", prereqInfo);
        return studentPrereqDB.add(prereqInfo);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Student prerequisite record in the mock database",
            response = StudentPrereqDB.PrereqInfo.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @PutMapping("/colleges/{misCode}/students/{sisPersonId:.+}/prereqs/{sisCourseId:.+}")
    public StudentPrereqDB.PrereqInfo put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                          @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                                          @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId,
                                          @ApiParam(value = "Prerequisite to update", required = true) @RequestBody StudentPrereqDB.PrereqInfo prereqInfo) {
        logger.debug("Mock: put {} {} {} {}", misCode, sisPersonId, sisCourseId, prereqInfo);
        return studentPrereqDB.update(misCode, sisPersonId, sisCourseId, prereqInfo);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a Student prerequisite record in the mock database",
            response = StudentPrereqDB.PrereqInfo.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @PatchMapping("/colleges/{misCode}/students/{sisPersonId:.+}/prereqs/{sisCourseId:.+}")
    public StudentPrereqDB.PrereqInfo patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                            @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                                            @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId,
                                            @ApiParam(value = "Prerequisite to update", required = true) @RequestBody Map prereqInfo) {
        logger.debug("Mock: patch {} {} {} {}", misCode, sisPersonId, sisCourseId, prereqInfo);
        return studentPrereqDB.patch(misCode, sisPersonId, sisCourseId, prereqInfo);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Student prerequisite record in the mock database",
            response = StudentPrereqDB.PrereqInfo.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/students/{sisPersonId:.+}/prereqs/{sisCourseId:.+}")
    public StudentPrereqDB.PrereqInfo delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                             @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                                             @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId,
                                             @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                                             @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {}", misCode, sisPersonId, sisCourseId, cascade);
        return studentPrereqDB.delete(misCode, sisPersonId, sisCourseId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Student prerequisite data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/prereqs/reload")
    public void reload() {
        logger.debug("Mock: reload");
        studentPrereqDB.loadData();
    }
}