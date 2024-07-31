package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.CohortTypeEnum;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.model.StudentHomeCollege;
import com.ccctc.adaptor.model.students.StudentFieldSet;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(
        value = "Students",
        tags = "Students",
        description = "Operations related to Student data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/students")
public class StudentApiController {

    private static final String GROOVY_OBJECT_NAME = "Student";

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve List<Student> for student records in given terms.",
            notes = "If terms are not specified, information for the current term will be returned.",
            response = Student.class,
            responseContainer = "List"
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "")
    public List<Map> getAllByTerm(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                  @ApiParam(value = "SIS Term IDs", example = "2017FA,2016SP", required = false) @RequestParam(value = "sisTermId", required = false) List<String> sisTermIds,
                                  @ApiParam(value = "Populate only the fields included in this Set", example = "IDENTIFIERS", required = false, defaultValue = "ALL") @RequestParam(value = "fieldSet", required = false) StudentFieldSet fieldSet) {

        if(sisTermIds != null && sisTermIds.size() > 3) {
            String errMsg = "getAllByTerm: sisTermId list cannot have more than 3 terms";
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg);
        }
        return groovyService.run(misCode, GROOVY_OBJECT_NAME, "getAllByTerm", new Object[]{misCode, sisTermIds, fieldSet});
    }

    @ApiOperation(
            value = "Retrieve a student by CCC ID. If term is not specified, information for the current term will be returned.",
            notes = "Possible error codes: multipleResultsFound, termNotFound",
            response = Student.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{cccid}")
    public Student get(@ApiParam(value = "CCC ID of the Student", example = "ABC1234") @PathVariable("cccid") String cccid,
                       @ApiParam(value = "MIS Code of the College", example = "001") @RequestParam("mis") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @RequestParam(value = "sisTermId", required = false) String sisTermId) {

        return groovyService.run(misCode, GROOVY_OBJECT_NAME, "get", new Object[]{misCode, cccid, sisTermId});
    }

    @ApiOperation(
            value = "Retrieve home college MIS Code for a student by CCC ID",
            notes = "Possible error codes: multipleResultsFound",
            response = StudentHomeCollege.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/homecollege/{cccid}")
    public StudentHomeCollege getHomeCollege(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                                             @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, GROOVY_OBJECT_NAME, "getHomeCollege", new Object[]{misCode, cccid});
    }

    @ApiOperation(
            value = "Add a Student to a cohort for a specified term",
            notes = "Possible error codes: multipleResultsFound, studentNotFound and termNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @PostMapping(value = "/{cccid}/cohorts/{cohortName}")
    public void postCohort(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                           @ApiParam(value = "Cohort name", example = "COURSE_EXCHANGE", required = true) @PathVariable("cohortName") CohortTypeEnum cohortName,
                           @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                           @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId) {

        groovyService.run(misCode, GROOVY_OBJECT_NAME, "postCohort", new Object[]{cccid, cohortName, misCode, sisTermId});
    }

    @ApiOperation(
            value = "Remove a Student from a cohort for a specified term",
            notes = "Possible error codes: multipleResultsFound, studentNotFound and termNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{cccid}/cohorts/{cohortName}", method = RequestMethod.DELETE)
    public void deleteCohort(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                             @ApiParam(value = "Cohort name", example = "COURSE_EXCHANGE", required = true) @PathVariable("cohortName") CohortTypeEnum cohortName,
                             @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                             @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId) {

        groovyService.run(misCode, GROOVY_OBJECT_NAME, "deleteCohort", new Object[]{cccid, cohortName, misCode, sisTermId});
    }

    @ApiOperation(
            value = "Get list of CCC Ids of the student",
            notes = "Possible error codes: studentNotFound",
            response = String.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @GetMapping(value = "/{sisPersonId}/cccids")
    public List<String> getStudentCCCIds(@ApiParam(value = "sisPersonId of the Student", example = "1234") @PathVariable("sisPersonId") String sisPersonId,
                                               @ApiParam(value = "MIS Code of the College", example = "001") @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, GROOVY_OBJECT_NAME, "getStudentCCCIds", new Object[]{misCode, sisPersonId});
    }

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
    @PostMapping(value = "/{sisPersonId}/cccids/{cccid}")
    public List<String> postStudentCCCId(@ApiParam(value = "sisPersonId of the Student", example = "1234") @PathVariable("sisPersonId") String sisPersonId,
                                    @ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                                    @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

       return groovyService.run(misCode, GROOVY_OBJECT_NAME, "postStudentCCCId", new Object[]{misCode, sisPersonId, cccid});
    }

    @ApiOperation(
            value = "Update data for a student",
            notes = "Possible error codes: studentNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{cccid}", method = RequestMethod.PATCH)
    public void patchStudent(
            @ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccId,
            @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
            @ApiParam(value = "Term code", example = "202060") @RequestParam(value = "sisTermId", required = false) String sisTermId,
            @ApiParam(value = "Student updates", example = "{ ... }", required = true) @RequestBody Student updates) {
        groovyService.run(misCode, GROOVY_OBJECT_NAME, "patch", new Object[]{misCode, cccId, sisTermId, updates});
    }

}
