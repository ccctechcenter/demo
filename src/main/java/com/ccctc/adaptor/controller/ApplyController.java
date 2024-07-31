package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.Application;
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
import java.time.LocalDate;
import java.time.ZoneOffset;

@Api(
        value = "CCC Apply Applications",
        tags = "CCC Apply Applications",
        description = "Operations related to CCC Apply Applications",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping(value = "/apply")
public class ApplyController {

    @Resource
    private GroovyService groovyService;


    @ApiOperation(
            value = "Get CCC Apply application data from the SIS staging table",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{id}")
    public Application get(@ApiParam(value = "Unique CCC Apply Application ID", example = "12345", required = true) @PathVariable("id") Long id,
                           @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Apply", "get", new Object[]{misCode, id});
    }


    @ApiOperation(
            value = "Post CCC Apply application data to the SIS staging table",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "invalidRequest")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<Application> post(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                            @ApiParam(value = "CCC Apply application data") @RequestBody Application application,
                                            UriComponentsBuilder uriComponentsBuilder) {

        if (application == null || application.getAppId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "Request body (Application) or application ID missing in request");

        // for non-test colleges, ensure passed MIS code matches application's college id
        if( !misCode.equals("001") && !misCode.equals("002")) {
            if (!misCode.equals(application.getCollegeId()))
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                        "MIS Code of request URL does not match College ID in body of request");
        }
        if (application.getCccId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                    "CccId is missing in request");
        if (application.getSupplementalQuestions() != null) {
            LocalDate suppDate1 = application.getSupplementalQuestions().getSuppDate01();
            LocalDate suppDate2 = application.getSupplementalQuestions().getSuppDate02();
            LocalDate suppDate3 = application.getSupplementalQuestions().getSuppDate03();
            LocalDate suppDate4 = application.getSupplementalQuestions().getSuppDate04();
            LocalDate suppDate5 = application.getSupplementalQuestions().getSuppDate05();

            String suppDate1Year = suppDate1 != null ? suppDate1.toString().substring(0, 4) : "";
            String suppDate2Year = suppDate2 != null ? suppDate2.toString().substring(0, 4) : "";
            String suppDate3Year = suppDate3 != null ? suppDate3.toString().substring(0, 4) : "";
            String suppDate4Year = suppDate4 != null ? suppDate4.toString().substring(0, 4) : "";
            String suppDate5Year = suppDate5 != null ? suppDate5.toString().substring(0, 4) : "";

            int currentYearLastTwo = Integer.valueOf(String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear()).substring(2, 4));
            int currentYearFirstTwo = Integer.valueOf(String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear()).substring(0, 2));
            int currentCentury = currentYearFirstTwo * 100;
            int lastCentury = (currentYearFirstTwo - 1) * 100;
            int addFifteenYearsToCurrent = currentYearLastTwo + 15;

            if (!suppDate1Year.isEmpty() && suppDate1Year.substring(0, 2).equals("00")) {
                if (Integer.valueOf(suppDate1Year.substring(2, 4)) >= 0 && Integer.valueOf(suppDate1Year.substring(2, 4)) <= addFifteenYearsToCurrent) {
                    application.getSupplementalQuestions().setSuppDate01(suppDate1.plusYears(currentCentury));
                } else {
                    application.getSupplementalQuestions().setSuppDate01(suppDate1.plusYears(lastCentury));
                }
            }
            if (!suppDate2Year.isEmpty() && suppDate2Year.substring(0, 2).equals("00")) {
                if (Integer.valueOf(suppDate2Year.substring(2, 4)) >= 0 && Integer.valueOf(suppDate2Year.substring(2, 4)) <= addFifteenYearsToCurrent) {
                    application.getSupplementalQuestions().setSuppDate02(suppDate2.plusYears(currentCentury));
                } else {
                    application.getSupplementalQuestions().setSuppDate02(suppDate2.plusYears(lastCentury));
                }
            }
            if (!suppDate3Year.isEmpty() && suppDate3Year.substring(0, 2).equals("00")) {
                if (Integer.valueOf(suppDate3Year.substring(2, 4)) >= 0 && Integer.valueOf(suppDate3Year.substring(2, 4)) <= addFifteenYearsToCurrent) {
                    application.getSupplementalQuestions().setSuppDate03(suppDate3.plusYears(currentCentury));
                } else {
                    application.getSupplementalQuestions().setSuppDate03(suppDate3.plusYears(lastCentury));
                }
            }
            if (!suppDate4Year.isEmpty() && suppDate4Year.substring(0, 2).equals("00")) {
                if (Integer.valueOf(suppDate4Year.substring(2, 4)) >= 0 && Integer.valueOf(suppDate4Year.substring(2, 4)) <= addFifteenYearsToCurrent) {
                    application.getSupplementalQuestions().setSuppDate04(suppDate4.plusYears(currentCentury));
                } else {
                    application.getSupplementalQuestions().setSuppDate04(suppDate4.plusYears(lastCentury));
                }
            }
            if (!suppDate5Year.isEmpty() && suppDate5Year.substring(0, 2).equals("00")) {
                if (Integer.valueOf(suppDate5Year.substring(2, 4)) >= 0 && Integer.valueOf(suppDate5Year.substring(2, 4)) <= addFifteenYearsToCurrent) {
                    application.getSupplementalQuestions().setSuppDate05(suppDate5.plusYears(currentCentury));
                } else {
                    application.getSupplementalQuestions().setSuppDate05(suppDate5.plusYears(lastCentury));
                }
            }
        }
        Application result = groovyService.run(misCode, "Apply", "post", new Object[]{misCode, application});

        UriComponents uriComponents = uriComponentsBuilder
                .path("/apply/{id}")
                .queryParam("mis", misCode)
                .buildAndExpand(application.getAppId(), misCode);

        return ResponseEntity.created(uriComponents.toUri()).body(result);
    }
}
