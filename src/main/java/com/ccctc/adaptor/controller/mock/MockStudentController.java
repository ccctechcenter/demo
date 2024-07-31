package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.model.StudentHomeCollege;
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
        value = "Mock Students",
        tags = "Mock Students",
        description = "Operations related to Mock Student data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockStudentController {

    private final static Logger logger = LoggerFactory.getLogger(MockStudentController.class);

    private StudentDB studentDB;

    @Autowired
    public MockStudentController(StudentDB studentDB) {
        this.studentDB = studentDB;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Student records from the mock database with optional filtering",
            response = Student.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/students")
    public List<Student> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? studentDB.getAllSorted() : studentDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by TERM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Student records from the mock database for a Term",
            response = Student.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students")
    public List<Student> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                             @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId) {
        logger.debug("Mock: get {} {}", misCode, sisTermId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);

        return studentDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Student record from the mock database",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}")
    public Student get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: get {} {} {}", misCode, sisTermId, sisPersonId);
        return studentDB.get(misCode, sisTermId, sisPersonId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Student record to the mock database",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/students")
    public Student post(@ApiParam(value = "Student to add", required = true) @RequestBody Student student) {
        logger.debug("Mock: add {}", student);
        return studentDB.add(student);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Student record in the mock database",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}")
    public Student put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                       @ApiParam(value = "Student to update", required = true) @RequestBody Student student) {
        logger.debug("Mock: put {} {} {} {}", misCode, sisTermId, sisPersonId, student);
        // these Key fields are not updatable; they need to match the path keys
        student.setMisCode(misCode);
        student.setSisPersonId(sisPersonId);
        student.setSisTermId(sisTermId);
        return studentDB.update(misCode, sisTermId, sisPersonId, student);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a Student record in the mock database",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}")
    public Student patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                         @ApiParam(value = "Student to update", required = true) @RequestBody Map student) {
        logger.debug("Mock: patch {} {} {} {}", misCode, sisTermId, sisPersonId, student);
        // these Key fields are not updatable
        student.remove("misCode");
        student.remove("sisPersonId");
        student.remove("sisTermId");
        return studentDB.patch(misCode, sisTermId, sisPersonId, student);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Student record in the mock database",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}")
    public Student delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                          @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                          @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                          @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                          @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {} ", misCode, sisTermId, sisPersonId, cascade);
        return studentDB.delete(misCode, sisTermId, sisPersonId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Student data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/students/reload")
    public void reload() {
        logger.debug("Mock: reload");
        studentDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ COPY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Copy a Student to a new term"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @PostMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/copy")
    public Student copy(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                        @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                        @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                        @ApiParam(value = "New SIS Term ID", example = "2018SP", required = true) @RequestParam("newSisTermId") String newSisTermId) {
        logger.debug("Mock: copy {} {} {} {}", misCode, sisTermId, sisPersonId, newSisTermId);
        return studentDB.copy(misCode, sisTermId, sisPersonId, newSisTermId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET Home College ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Student's home college record from the mock database",
            response = StudentHomeCollege.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @GetMapping("/colleges/{misCode}/students/{sisPersonId:.+}/homecollege")
    public StudentHomeCollege getHomeCollege(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                             @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: getHomeCollege {} {}", misCode, sisPersonId);
        return studentDB.getHomeCollege(misCode, sisPersonId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ UPDATE Home College ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Student's home college record in the mock database",
            response = StudentHomeCollege.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @PutMapping("/colleges/{misCode}/students/{sisPersonId:.+}/homecollege")
    public StudentHomeCollege updateHomeCollege(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                                @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                                                @ApiParam(value = "New Home College", example = "002", required = true) @RequestParam("newHomeCollege") String newHomeCollege) {
        logger.debug("Mock: updateHomeCollege {} {} {}", misCode, sisPersonId, newHomeCollege);
        return studentDB.updateHomeCollege(misCode, sisPersonId, newHomeCollege);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~GET CCC ID ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get list of CCC Ids of the student",
            notes = "Possible error codes: studentNotFound",
            response = String.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/students/{sisPersonId}/cccids")
    public List<String>  getStudentCCCIds(@ApiParam(value = "sisPersonId of the Student", example = "1234") @PathVariable("sisPersonId") String sisPersonId,
                                          @ApiParam(value = "MIS Code of the College", example = "001") @RequestParam("mis") String misCode) {
        logger.debug("Mock: getStudentCCCIds {} {}", sisPersonId, misCode);
        return studentDB.getStudentCCCIds( misCode,  sisPersonId);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~POST CCC ID ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @ApiOperation(
            value = "Add CCCID to the student",
            notes = "Possible error codes: studentNotFound and entityAlreadyExists",
            response = String.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 409, response = ErrorResource.class, message = "entityAlreadyExists")
    })
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping(value = "/students/{sisPersonId}/cccids/{cccid}")
    public List<String> postStudentCCCId(@ApiParam(value = "sisPersonId of the Student", example = "1234") @PathVariable("sisPersonId") String sisPersonId,
                                         @ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                                         @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {
        logger.debug("Mock: postStudentCCCId {} {} {} ", sisPersonId, cccid, misCode);
        return studentDB.postStudentCCCId( misCode, sisPersonId, cccid);
    }
}