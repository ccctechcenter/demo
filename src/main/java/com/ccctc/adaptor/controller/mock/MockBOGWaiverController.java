package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.BOGWaiver;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.mock.BOGWaiverDB;
import com.ccctc.adaptor.util.CoverageIgnore;
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
        value = "Mock BOG Fee Waivers",
        tags = "Mock BOG Fee Waivers",
        description = "Operations related to Mock BOG Fee Waiver data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockBOGWaiverController {

    private final static Logger logger = LoggerFactory.getLogger(MockBOGWaiverController.class);

    private BOGWaiverDB bogWaiverDB;

    @Autowired
    public MockBOGWaiverController(BOGWaiverDB bogWaiverDB) {
        this.bogWaiverDB = bogWaiverDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all BOGWaiver records from the mock with optional filtering",
            response = BOGWaiver.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/bogfw")
    public List<BOGWaiver> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? bogWaiverDB.getAllSorted() : bogWaiverDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a BOGWaiver record from the mock database",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/bogfw")
    public BOGWaiver get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: get {} {} {}", misCode, sisTermId, sisPersonId);
        return bogWaiverDB.get(misCode, sisTermId, sisPersonId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a BOGWaiver record to the mock database",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/bogfw")
    public BOGWaiver post(@ApiParam(value = "BOGWaiver to add", required = true) @RequestBody BOGWaiver bogWaiver) {
        logger.debug("Mock: add {}", bogWaiver);
        return bogWaiverDB.add(bogWaiver);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a BOGWaiver record in the mock database",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/bogfw")
    public BOGWaiver put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                         @ApiParam(value = "BOGWaiver to update", required = true) @RequestBody BOGWaiver bogWaiver) {
        logger.debug("Mock: put {} {} {} {}", misCode, sisTermId, sisPersonId, bogWaiver);
        return bogWaiverDB.update(misCode, sisTermId, sisPersonId, bogWaiver);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a BOGWaiver record in the mock database",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/bogfw")
    public BOGWaiver patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                           @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                           @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                           @ApiParam(value = "BOGWaiver to update", required = true) @RequestBody Map bogWaiver) {
        logger.debug("Mock: patch {} {} {} {}", misCode, sisTermId, sisPersonId, bogWaiver);
        return bogWaiverDB.patch(misCode, sisTermId, sisPersonId, bogWaiver);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a BOGWaiver record in the mock database",
            response = BOGWaiver.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "multipleResultsFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "studentNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/students/{sisPersonId:.+}/bogfw")
    public BOGWaiver delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                            @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                            @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                            @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                            @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {} ", misCode, sisTermId, sisPersonId, cascade);
        return bogWaiverDB.delete(misCode, sisTermId, sisPersonId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @CoverageIgnore
    @ApiOperation(
            value = "Reload/reset mock BOGWaiver data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/bogfw/reload")
    public void reload() {
        logger.debug("Mock: reload");
        bogWaiverDB.loadData();
    }
}