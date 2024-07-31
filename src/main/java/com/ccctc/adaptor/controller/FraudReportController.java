package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.fraud.FraudReport;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.Resource;

@Api(
        value = "Fraud Reports",
        tags = "Fraud",
        produces = "application/json"
 )
@RestController
@RequestMapping(value = "/fraudReport")
public class FraudReportController {

    @Resource
    private GroovyService groovyService;


    @ApiOperation(
            value = "Get FraudReport data by sisFraudReportId from the SIS staging table",
            response = FraudReport.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{sisFraudReportId}")
    public FraudReport getById(@ApiParam(value = "Unique internal fraud report id", example = "12345", required = true) @PathVariable("sisFraudReportId") Long sisFraudReportId,
                               @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {
        return groovyService.run(misCode, "FraudReport", "getById", new Object[]{misCode, sisFraudReportId});
    }

    @ApiOperation(
            value = "Delete FraudReport data by sisFraudReportId from the SIS staging table"
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{sisFraudReportId}")
    public void deleteById(@ApiParam(value = "Unique internal fraud report id", example = "12345", required = true) @PathVariable("sisFraudReportId") Long sisFraudReportId,
                           @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {
        groovyService.run(misCode, "FraudReport", "deleteById", new Object[]{misCode, sisFraudReportId});
    }
    @ApiOperation(
            value = "Delete FraudReport data by cccId and appId from the SIS staging table"
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{appId}/{reportedByMisCode}")
    public void deleteFraudReport(@ApiParam(value = "MIS Code of reporting college", example = "101", required = true) @PathVariable("reportedByMisCode") String reportedByMisCode,
                                   @ApiParam(value = "Application ID", example = "2131234", required = true) @PathVariable("appId") Long appId,
                                     @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {
        FraudReport report = new FraudReport();
        report.setAppId(appId);
        report.setReportedByMisCode(reportedByMisCode);
        report.setMisCode(misCode);
        groovyService.run(misCode, "FraudReport", "deleteFraudReport", new Object[]{report});
    }

    @ApiOperation(
            value = "Get List Of FraudReport data from the SIS staging table",
            response = List.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @GetMapping
    public List<FraudReport> getMatching(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                         @ApiParam(value = "appId of application", example = "001", required = false) @RequestParam(value = "appId", required = false) Long appId,
                                         @ApiParam(value = "cccid of Student", example = "001", required = false) @RequestParam(value = "cccId", required = false) String cccId) {
        if((appId == null || appId == 0) && StringUtils.isEmpty(cccId)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "at least appId or cccId must be provided when listing FraudReports");
        }
        return groovyService.run(misCode, "FraudReport", "getMatching", new Object[]{misCode, appId, cccId});
    }

    @ApiOperation(
            value = "Post FraudReport data to create the record in the SIS staging table",
            response = FraudReport.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<FraudReport> create(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                              @ApiParam(value = "FraudReport data") @RequestBody FraudReport fraudReport,
                                              UriComponentsBuilder uriComponentsBuilder) {

        if (fraudReport == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) missing");
        }

        if(StringUtils.isEmpty(fraudReport.getMisCode())){
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) is missing misCode");
        }

        if ((fraudReport.getAppId() == null || fraudReport.getAppId() == 0l) && fraudReport.getCccId() == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) is missing either an appId or cccId");
        }

        if(StringUtils.isEmpty(fraudReport.getReportedByMisCode())){
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) is missing reportedByMisCode");
        }

        if(StringUtils.isEmpty(fraudReport.getFraudType())){
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) is missing fraudType");
        }

        if(StringUtils.isEmpty(fraudReport.getReportedBySource())){
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (FraudReport) is missing reportedBySource");
        }

        if(fraudReport.getTstmpSubmit() == null){
            fraudReport.setTstmpSubmit(LocalDateTime.now());
        }


        Long sisFraudReportId = groovyService.run(misCode, "FraudReport", "create", new Object[]{misCode, fraudReport});
        fraudReport.setSisFraudReportId(sisFraudReportId);
        UriComponents uriComponents = uriComponentsBuilder
                .path("/fraudReport/{sisFraudReportId}")
                .queryParam("mis", misCode)
                .buildAndExpand(sisFraudReportId);

        return ResponseEntity.created(uriComponents.toUri()).body(fraudReport);
    }
}
