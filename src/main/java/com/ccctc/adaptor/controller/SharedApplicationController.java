package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.Application;
import com.ccctc.adaptor.model.apply.SharedApplication;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;

@Api(
        value = "CCC Apply Shared-Applications",
        tags = "CCC Apply Shared-Applications",
        produces = "application/json")
@RestController
@RequestMapping(value = "/shared-application")
public class SharedApplicationController {

    @Resource
    private GroovyService groovyService;


    @ApiOperation(
            value = "Get CCC Apply application data from the SIS shared-application staging table",
            response = SharedApplication.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{id}")
    public SharedApplication get(@ApiParam(value = "Unique CCC Apply Application ID", example = "12345", required = true) @PathVariable("id") Long id,
                           @ApiParam(value = "MIS Code of the Teaching College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "SharedApplication", "get", new Object[]{misCode, id});
    }


    @ApiOperation(
            value = "Post CCC Apply application data to the SIS shared-application staging table",
            response = void.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity post(@ApiParam(value = "MIS Code of the Teaching College", example = "001", required = true) @RequestParam("mis") String misCode,
                                            @ApiParam(value = "CCC Apply application data") @RequestBody Application application,
                                            UriComponentsBuilder uriComponentsBuilder) {

        if (application == null || application.getAppId() == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (Application) or application ID missing in request");
        }

        if (application.getCccId() == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "CccId is missing in request");
        }

        groovyService.run(misCode, "SharedApplication", "post", new Object[]{misCode, application});

        UriComponents uriComponents = uriComponentsBuilder
                .path("/shared-application/{id}")
                .queryParam("mis", misCode)
                .buildAndExpand(application.getAppId(), misCode);

        return ResponseEntity.created(uriComponents.toUri()).build();
    }
}
