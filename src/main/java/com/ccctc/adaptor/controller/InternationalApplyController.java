package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.InternationalApplication;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;

@Api(
        value = "CCC International Applications",
        tags = "CCC International Applications",
        description = "Operations related to CCC International Applications",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/international-apply")
public class InternationalApplyController {

    @Resource
    private GroovyService groovyService;


    @ApiOperation(
            value = "Get CCC International Application data from the SIS staging table",
            response = InternationalApplication.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{id}")
    public InternationalApplication get(@ApiParam(value = "Unique CCC International Application ID", example = "12345", required = true) @PathVariable("id") Long id,
                           @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "InternationalApply", "get", new Object[]{misCode, id});
    }


    @ApiOperation(
            value = "Post CCC International Application data to the SIS staging table",
            response = InternationalApplication.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<InternationalApplication> post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                            @ApiParam(value = "CCC International Application data") @RequestBody InternationalApplication application,
                                            UriComponentsBuilder uriComponentsBuilder) {

        if (application == null || application.getAppId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (International Application) or International Application ID missing in request");

        // for non-test colleges, ensure passed MIS code matches application's college id
        
            if (( !misCode.equals("001") && !misCode.equals("002")) && (!misCode.equals(application.getCollegeId())))
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                        "MIS Code of request URL does not match College ID in body of request");
        if (application.getCccId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "CccId is missing in request");

        InternationalApplication result = groovyService.run(misCode, "InternationalApply", "post", new Object[]{misCode, application});

        UriComponents uriComponents = uriComponentsBuilder
                .path("/international-apply/{id}")
                .queryParam("mis", misCode)
                .buildAndExpand(application.getAppId(), misCode);

        return ResponseEntity.created(uriComponents.toUri()).body(result);
    }
}