package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.placement.PlacementTransaction;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(
        value = "Placements",
        tags = "Placements",
        description = "Operations related to Placement data from CCC Assess",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/placements")
public class PlacementController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Post a placement transaction to the SIS",
            notes = "Possible error codes: multipleResultsFound, studentNotFound"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @PostMapping
    public void post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                     @ApiParam(value = "The placement transaction to be created", required = true) @RequestBody PlacementTransaction placementTransaction) {

        groovyService.run(misCode, "Placement", "post", new Object[]{misCode, placementTransaction});
    }
}
