package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.BOGWaiver;
import com.ccctc.adaptor.model.ErrorResource;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(
        value = "BOG Fee Waiver Eligibility",
        tags = "BOG Fee Waiver Eligibility",
        description = "Operations related to BOG Fee Waiver eligibility data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/bogfw")
public class BOGWaiverController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve BOG Fee Waiver information for a student and term",
            notes = "Possible error codes: multipleResultsFound, studentNotFound and termNotFound",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{cccid}/{sisTermId:.+}")
    public BOGWaiver get(@ApiParam(value = "CCC ID of the Student", example = "ABC1234", required = true) @PathVariable("cccid") String cccid,
                         @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "BOGWaiver", "get", new Object[]{misCode, cccid, sisTermId});
    }

    @ApiOperation(
            value = "Post BOG Fee Wavier information to a student in a term",
            notes = "Possible error codes: entityAlreadyExists, multipleResultsFound, studentNotFound and termNotFound",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BOGWaiver post(@ApiParam(value = "MIS Code of College", example = "001", required = true) @RequestParam("mis") String misCode,
                          @ApiParam(value = "BOG Fee Waiver information to be created", required = true) @RequestBody BOGWaiver bogWaiver) {

        return groovyService.run(misCode, "BOGWaiver", "post", new Object[]{misCode, bogWaiver});
    }
}