package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.FinancialAidUnits;
import com.ccctc.adaptor.model.mock.FinancialAidUnitsDB;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(
        value = "Mock Financial Aid Units",
        tags = "Mock Financial Aid Units",
        description = "Operations related to Mock Financial Aid Units data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockFinancialAidUnitsController {

    private final static Logger logger = LoggerFactory.getLogger(MockFinancialAidUnitsController.class);

    private FinancialAidUnitsDB financialAidUnitsDB;

    @Autowired
    public MockFinancialAidUnitsController(FinancialAidUnitsDB financialAidUnitsDB) {
        this.financialAidUnitsDB = financialAidUnitsDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all FinancialAidUnits records from the mock database with optional filtering",
            response = FinancialAidUnits.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/faunits")
    public List<FinancialAidUnits> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? financialAidUnitsDB.getAllSorted() : financialAidUnitsDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by STUDENT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all FinancialAidUnits records from the mock database with optional filtering",
            response = FinancialAidUnits.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/faunits")
    public List<FinancialAidUnits> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                       @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: get {} {} {}", misCode, sisTermId, sisPersonId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisPersonId", sisPersonId);

        return financialAidUnitsDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a FinancialAidUnits record from the mock database",
            response = FinancialAidUnits.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/faunits/{sisPersonId:.+}")
    public FinancialAidUnits get(@ApiParam(value = "SIS Person ID", example = "1234567", required = true) @PathVariable("sisPersonId") String sisPersonId,
                                 @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                                 @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("misCode") String misCode,
                                 @ApiParam(value = "MIS Code of the Enrolled College", example = "002", required = true) @RequestParam("enrolledMisCode") String enrolledMisCode,
                                 @ApiParam(value = "C_ID of course", example = "ENGL 100", required = true) @RequestParam("c_id") String c_id) {
        logger.debug("Mock: get {} {} {} {} {}", sisPersonId, sisTermId, misCode, enrolledMisCode, c_id);
        return financialAidUnitsDB.get(misCode, sisTermId, sisPersonId, enrolledMisCode, c_id);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a FinancialAidUnits record to the mock database",
            response = FinancialAidUnits.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/faunits")
    public FinancialAidUnits post(@ApiParam(value = "FinancialAidUnits to add", required = true) @RequestBody FinancialAidUnits financialAidUnits) {
        logger.debug("Mock: add {}", financialAidUnits);
        return financialAidUnitsDB.add(financialAidUnits);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a FinancialAidUnits record in the mock database",
            response = FinancialAidUnits.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/faunits/{sisPersonId:.+}")
    public FinancialAidUnits put(@ApiParam(value = "SIS Person ID", example = "1234567", required = true) @PathVariable("sisPersonId") String sisPersonId,
                                 @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                                 @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("misCode") String misCode,
                                 @ApiParam(value = "MIS Code of the Enrolled College", example = "002", required = true) @RequestParam("enrolledMisCode") String enrolledMisCode,
                                 @ApiParam(value = "C_ID of course", example = "ENGL 100", required = true) @RequestParam("c_id") String c_id,
                                 @ApiParam(value = "BOGWaiver to update", required = true) @RequestBody FinancialAidUnits financialAidUnits) {
        logger.debug("Mock: put {} {} {} {} {} {}", sisPersonId, sisTermId, misCode, enrolledMisCode, c_id, financialAidUnits);
        return financialAidUnitsDB.update(misCode, sisTermId, sisPersonId, enrolledMisCode, c_id, financialAidUnits);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a FinancialAidUnits record in the mock database",
            response = FinancialAidUnits.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/faunits/{sisPersonId:.+}")
    public FinancialAidUnits patch(@ApiParam(value = "SIS Person ID", example = "1234567", required = true) @PathVariable("sisPersonId") String sisPersonId,
                                   @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                                   @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("misCode") String misCode,
                                   @ApiParam(value = "MIS Code of the Enrolled College", example = "002", required = true) @RequestParam("enrolledMisCode") String enrolledMisCode,
                                   @ApiParam(value = "C_ID of course", example = "ENGL 100", required = true) @RequestParam("c_id") String c_id,
                                   @ApiParam(value = "BOGWaiver to update", required = true) @RequestBody Map financialAidUnits) {
        logger.debug("Mock: patch {} {} {} {} {} {}", sisPersonId, sisTermId, misCode, enrolledMisCode, c_id, financialAidUnits);
        return financialAidUnitsDB.patch(misCode, sisTermId, sisPersonId, enrolledMisCode, c_id, financialAidUnits);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a FinancialAidUnits record in the mock database",
            response = FinancialAidUnits.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/faunits/{sisPersonId:.+}")
    public FinancialAidUnits delete(@ApiParam(value = "SIS Person ID", example = "1234567", required = true) @PathVariable("sisPersonId") String sisPersonId,
                                    @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                                    @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("misCode") String misCode,
                                    @ApiParam(value = "MIS Code of the Enrolled College", example = "002", required = true) @RequestParam("enrolledMisCode") String enrolledMisCode,
                                    @ApiParam(value = "C_ID of course", example = "ENGL 100", required = true) @RequestParam("c_id") String c_id,
                                    @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                                    @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {} {} {}", sisPersonId, sisTermId, misCode, enrolledMisCode, c_id, cascade);
        return financialAidUnitsDB.delete(misCode, sisTermId, sisPersonId, enrolledMisCode, c_id, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock FinancialAidUnits data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/faunits/reload")
    public void reload() {
        logger.debug("Mock: reload");
        financialAidUnitsDB.loadData();
    }
}