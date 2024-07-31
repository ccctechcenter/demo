package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(
        value = "Persons",
        tags = "Persons",
        description = "Operations related to Person data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/persons")
public class PersonApiController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve person by SIS Person ID",
            notes = "Possible error codes: multipleResultsFound",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{sisPersonId:.+}")
    public Person get(@ApiParam(value = "SIS Person ID", example = "1234567", required = true) @PathVariable("sisPersonId") String sisPersonId,
                      @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Person", "get", new Object[]{misCode, sisPersonId});
    }

    @ApiOperation(
            value = "Retrieve person by SIS Person ID or eppn or CCC ID",
            notes = "Possible error codes: multipleResultsFound",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/student")
    public Person getStudentPerson(
            @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
            @ApiParam(value = "SIS Person ID", example = "1234567", required = false) @RequestParam(value = "sisPersonId", required = false) String sisPersonId,
            @ApiParam(value = "eppn of student", example = "test@test.edu", required = false) @RequestParam(value = "eppn", required = false) String eppn,
            @ApiParam(value = "cccid of Student", example = "001", required = false) @RequestParam(value = "cccid", required = false) String cccId) {

        if (StringUtils.isBlank(sisPersonId) && StringUtils.isBlank(cccId) && StringUtils.isBlank(eppn)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisPersonId or cccid or eppn is required");
        }

        return groovyService.run(misCode, "Person", "getStudentPerson", new Object[]{misCode, sisPersonId, eppn, cccId});

    }

    @ApiOperation(
            value = "Retrieve multiple person records by a list of SIS Person IDs or CCC IDs",
            response = Person.class,
            responseContainer = "List"
    )
    @GetMapping
    public List<Person> getAll(@ApiParam(value = "List of SIS Person IDs") @RequestParam(value = "sisPersonIds", required = false) String[] sisPersonIds,
                               @ApiParam(value = "List of CCC IDs") @RequestParam(value = "cccids", required = false) String[] cccids,
                               @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        if ((sisPersonIds == null || sisPersonIds.length == 0) && (cccids == null || cccids.length == 0)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisPersonIds or cccids is required");
        }

        return groovyService.run(misCode, "Person", "getAll", new Object[]{misCode, sisPersonIds, cccids});
    }
}