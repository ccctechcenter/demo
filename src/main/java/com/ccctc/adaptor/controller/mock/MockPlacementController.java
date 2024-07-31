package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.mock.PlacementDB;
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
        value = "Mock Placements",
        tags = "Mock Placements",
        description = "Operations related to Mock Placement data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock/placements")
@Conditional(MockCondition.class)
public class MockPlacementController {

    private final static Logger logger = LoggerFactory.getLogger(MockPlacementController.class);

    private PlacementDB placementDB;

    @Autowired
    public MockPlacementController(PlacementDB placementDB) {
        this.placementDB = placementDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Placement Transaction records from the mock database with optional filtering",
            response = PlacementDB.MockPlacementTransaction.class,
            responseContainer = "List"
    )
    @GetMapping
    public List<PlacementDB.MockPlacementTransaction> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);
        return map.size() == 0 ? placementDB.getAllSorted() : placementDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Placement Transaction record from the mock database",
            response = PlacementDB.MockPlacementTransaction.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/{id}")
    public PlacementDB.MockPlacementTransaction get(@ApiParam(value = "Placement Transaction ID", example = "1") @PathVariable("id") int id) {
        logger.debug("Mock: get {}", id);
        return placementDB.get(id);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Placement Transaction record to the mock database",
            response = PlacementDB.MockPlacementTransaction.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "personNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PlacementDB.MockPlacementTransaction post(@ApiParam(value = "Placement transaction to add", required = true)
                                                     @RequestBody PlacementDB.MockPlacementTransaction placementTransaction) {
        logger.debug("Mock: add {}", placementTransaction);
        return placementDB.add(placementTransaction);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Placement Transaction record in the mock database",
            response = PlacementDB.MockPlacementTransaction.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @DeleteMapping("/{id}")
    public PlacementDB.MockPlacementTransaction delete(@ApiParam(value = "Placement Transaction ID", example = "1") @PathVariable("id") int id) {
        logger.debug("Mock: delete {}", id);
        return placementDB.delete(id, false);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Placement data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/reload")
    public void reload() {
        logger.debug("Mock: reload");
        placementDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ CLEAR ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Clear all Placement Transaction records from the mock database"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/clear")
    public void clear() {
        logger.debug("Mock: clear");
        placementDB.deleteAll(false);
    }
}