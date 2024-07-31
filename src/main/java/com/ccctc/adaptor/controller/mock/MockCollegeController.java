package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.College;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.mock.CollegeDB;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(
        value = "Mock Colleges",
        tags = "Mock Colleges",
        description = "Operations related to Mock College data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock/colleges")
@Conditional(MockCondition.class)
public class MockCollegeController {

    private final static Logger logger = LoggerFactory.getLogger(MockCollegeController.class);

    private CollegeDB collegeDB;

    @Autowired
    public MockCollegeController(CollegeDB collegeDB) {
        this.collegeDB = collegeDB;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all College records from the mock database with optional filtering",
            response = College.class,
            responseContainer = "List"
    )
    @GetMapping
    public List<College> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get");

        return map.size() == 0 ? collegeDB.getAllSorted() : collegeDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a College record to the mock database",
            response = College.class
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public College post(@ApiParam(value = "College to add", required = true) @RequestBody College college) {
        logger.debug("Mock: add {}", college);
        return collegeDB.add(college);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a College record from the mock database",
            response = College.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/{misCode}")
    public College get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode) {
        logger.debug("Mock: get {}", misCode);
        return collegeDB.get(misCode);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a College record in the mock database",
            response = College.class
    )
    @PutMapping("/{misCode}")
    public College put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "College to update", required = true) @RequestBody College college) {
        logger.debug("Mock: put {} {}", misCode);
        return collegeDB.update(misCode, college);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a College record in the mock database",
            response = College.class
    )
    @PatchMapping("/{misCode}")
    public College patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "College to update", required = true) @RequestBody Map college) {
        logger.debug("Mock: patch {} {} ", misCode, college);
        return collegeDB.patch(misCode, college);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a College record in the mock database",
            response = College.class
    )
    @DeleteMapping("/{misCode}")
    public College delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                          @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                          @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} ", misCode, cascade);
        return collegeDB.delete(misCode, cascade);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock College data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/reload")
    public void reload() {
        logger.debug("Mock: reload");
        collegeDB.loadData();
    }
}