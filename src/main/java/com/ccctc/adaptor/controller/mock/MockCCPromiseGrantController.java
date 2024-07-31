package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.CCPromiseGrant;
import com.ccctc.adaptor.model.mock.CCPromiseGrantDB;
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

import java.util.List;
import java.util.Map;

@Api(
        value = "Mock CC Promise Grant Applications",
        tags = "Mock CC Promise Grant Applications",
        description = "Operations related to Mock CC Promise Grant Application data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockCCPromiseGrantController {

    private final static Logger logger = LoggerFactory.getLogger(MockCCPromiseGrantController.class);

    private CCPromiseGrantDB ccPromiseGrantDB;

    @Autowired
    public MockCCPromiseGrantController(CCPromiseGrantDB ccPromiseGrantDB) {
        this.ccPromiseGrantDB = ccPromiseGrantDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all CC Promise Grant records from the mock with optional filtering",
            response = CCPromiseGrant.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @GetMapping("/ccpg")
    public List<CCPromiseGrant> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? ccPromiseGrantDB.getAllSorted() : ccPromiseGrantDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a CC Promise Grant record from the mock database",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/ccpg/{id}")
    public CCPromiseGrant get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                              @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id) {
        logger.debug("Mock: get {} {}", misCode, id);
        return ccPromiseGrantDB.get(misCode, id);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a CC Promise Grant record to the mock database",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/ccpg")
    public CCPromiseGrant post(@ApiParam(value = "Application to add", required = true) @RequestBody CCPromiseGrant ccPromiseGrant) {
        logger.debug("Mock: add {}", ccPromiseGrant);
        return ccPromiseGrantDB.add(ccPromiseGrant);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a CC Promise Grant record in the mock database",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PutMapping("/colleges/{misCode}/ccpg/{id}")
    public CCPromiseGrant put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                              @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                              @ApiParam(value = "Application to update", required = true) @RequestBody CCPromiseGrant ccPromiseGrant) {
        logger.debug("Mock: put {} {}", misCode, id, ccPromiseGrant);
        return ccPromiseGrantDB.update(misCode, id, ccPromiseGrant);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a CC Promise Grant record in the mock database",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PatchMapping("/colleges/{misCode}/ccpg/{id}")
    public CCPromiseGrant patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                                @ApiParam(value = "Application to update", required = true) @RequestBody Map ccPromiseGrant) {
        logger.debug("Mock: patch {} {} {}", misCode, id, ccPromiseGrant);
        return ccPromiseGrantDB.patch(misCode, id, ccPromiseGrant);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete an CC Promise Grant record in the mock database",
            response = CCPromiseGrant.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @DeleteMapping("/colleges/{misCode}/ccpg/{id}")
    public CCPromiseGrant delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                 @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                                 @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                                 @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {}", misCode, id, cascade);
        return ccPromiseGrantDB.delete(misCode, id, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock CC Promise Grant data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/ccpg/reload")
    public void reload() {
        logger.debug("Mock: reload");
        ccPromiseGrantDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ CLEAR ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Clear all CC Promise Grant records from the mock database"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/ccpg/clear")
    public void clear() {
        logger.debug("Mock: clear");
        ccPromiseGrantDB.deleteAll(false);
    }
}
