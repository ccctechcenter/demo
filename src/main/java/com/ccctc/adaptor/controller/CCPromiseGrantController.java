package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.CCPromiseGrant;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;

@Api(
        value = "California College Promise Grant Applications",
        tags = "California College Promise Grant Applications",
        description = "Operations related to California College Promise Grant Applications",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/ccpg")
public class CCPromiseGrantController {

    @Resource
    private GroovyService groovyService;


    @ApiOperation(
            value = "Get California College Promise Grant application from the SIS staging table",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{id}")
    public CCPromiseGrant get(@ApiParam(value = "Unique California College Promise Grant Application ID", example = "12345", required = true) @PathVariable("id") Long id,
                              @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "CCPromiseGrant", "get", new Object[]{misCode, id});
    }


    @ApiOperation(
            value = "Post California College Promise Grant application to the SIS staging table",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<CCPromiseGrant> post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                               @ApiParam(value = "California College Promise Grant application data") @RequestBody CCPromiseGrant ccPromiseGrant,
                                               UriComponentsBuilder uriComponentsBuilder) {

        if (ccPromiseGrant == null || ccPromiseGrant.getAppId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (CCPromiseGrant) or application ID missing in request");

        // for non-test colleges, ensure passed MIS code matches application's college id
        if (!misCode.equals("001") && !misCode.equals("002") && !misCode.equals(ccPromiseGrant.getCollegeId())) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "MIS Code of request URL does not match College ID in body of request");
        }
        if (ccPromiseGrant.getCccId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "CccId is missing in request");

        CCPromiseGrant result = groovyService.run(misCode, "CCPromiseGrant", "post", new Object[]{misCode, ccPromiseGrant});

        UriComponents uriComponents = uriComponentsBuilder
                .path("/ccpg/{id}")
                .queryParam("mis", misCode)
                .buildAndExpand(ccPromiseGrant.getAppId(), misCode);

        return ResponseEntity.created(uriComponents.toUri()).body(result);
    }
}