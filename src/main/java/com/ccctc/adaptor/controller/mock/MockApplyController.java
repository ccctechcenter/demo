package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.apply.Application;
import com.ccctc.adaptor.model.mock.ApplyDB;
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
        value = "Mock CCC Apply Applications",
        tags = "Mock CCC Apply Applications",
        description = "Operations related to Mock CCC Apply Application data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockApplyController {

    private final static Logger logger = LoggerFactory.getLogger(MockApplyController.class);

    private ApplyDB applyDB;

    @Autowired
    public MockApplyController(ApplyDB applyDB) {
        this.applyDB = applyDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all CCC Apply records from the mock with optional filtering",
            response = Application.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @GetMapping("/apply")
    public List<Application> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? applyDB.getAllSorted() : applyDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a CCC Apply record from the mock database",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/apply/{id}")
    public Application get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                           @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id) {
        logger.debug("Mock: get {} {}", misCode, id);
        return applyDB.get(misCode, id);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a CCC Apply record to the mock database",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/apply")
    public Application post(@ApiParam(value = "Application to add", required = true) @RequestBody Application application) {
        logger.debug("Mock: add {}", application);
        return applyDB.add(application);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a CCC Apply record in the mock database",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PutMapping("/colleges/{misCode}/apply/{id}")
    public Application put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                           @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                           @ApiParam(value = "Application to update", required = true) @RequestBody Application application) {
        logger.debug("Mock: put {} {}", misCode, id, application);
        return applyDB.update(misCode, id, application);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a CCC Apply record in the mock database",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PatchMapping("/colleges/{misCode}/apply/{id}")
    public Application patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                             @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                             @ApiParam(value = "Application to update", required = true) @RequestBody Map application) {
        logger.debug("Mock: patch {} {} {}", misCode, id, application);
        return applyDB.patch(misCode, id, application);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete an CCC Apply record in the mock database",
            response = Application.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @DeleteMapping("/colleges/{misCode}/apply/{id}")
    public Application delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                              @ApiParam(value = "Application ID", example = "12345") @PathVariable("id") long id,
                              @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                              @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {}", misCode, id, cascade);
        return applyDB.delete(misCode, id, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Apply data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/apply/reload")
    public void reload() {
        logger.debug("Mock: reload");
        applyDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ CLEAR ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Clear all CCC Apply Application records from the mock database"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/apply/clear")
    public void clear() {
        logger.debug("Mock: clear");
        applyDB.deleteAll(false);
    }
}
