package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.FinancialAidUnits;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(
        value = "Financial Aid Units",
        tags="Financial Aid Units",
        description = "Operations related to Financial Aid Units data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/faunits")
public class FinancialAidUnitsController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve list of Financial Aid Units records for a student and term",
            response = FinancialAidUnits.class,
            responseContainer = "List",
            notes = "Possible error codes: multipleResultsFound, studentNotFound, termNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping(value = "/{cccid}/{sisTermId:.+}")
    public List<FinancialAidUnits> get(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                                       @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @PathVariable("sisTermId") String sisTermId,
                                       @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "FinancialAidUnits", "getUnitsList", new Object[]{misCode, cccid, sisTermId});
    }

    @ApiOperation(
            value = "Add a Financial Aid Units record to a student in a term",
            response = FinancialAidUnits.class,
            notes = "Possible error codes: entityAlreadyExists, multipleResultsFound, noFinancialAidRecord, studentNotFound, termNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "noFinancialAidRecord"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FinancialAidUnits post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                  @ApiParam(value = "Financial Aid Units record to add", required = true) @RequestBody FinancialAidUnits financialAidUnits) {

        return groovyService.run(misCode, "FinancialAidUnits", "post", new Object[]{misCode, financialAidUnits});
    }

    @ApiOperation(
            value = "Remove a Financial Aid Units record from a student in a term",
            notes = "Possible error codes: multipleResultsFound, noResultsFound, studentNotFound and termNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{cccid}/{sisTermId:.+}/{cid}")
    public void delete(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                       @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "C-ID of the Course", example = "001", required = true) @PathVariable(value = "cid") String cid,
                       @ApiParam(value = "MIS Code of the College associated with the Course", example = "002", required = true) @RequestParam("enrolledMisCode") String enrolledMisCode,
                       @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        groovyService.run(misCode, "FinancialAidUnits", "delete", new Object[]{misCode, cccid, sisTermId, enrolledMisCode, cid});
    }
}