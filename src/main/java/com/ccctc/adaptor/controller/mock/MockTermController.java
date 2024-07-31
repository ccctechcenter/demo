package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Term;
import com.ccctc.adaptor.model.mock.TermDB;
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
        value = "Mock Terms",
        tags = "Mock Terms",
        description = "Operations related to Mock Term data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockTermController {

    private final static Logger logger = LoggerFactory.getLogger(MockTermController.class);

    private TermDB termDB;

    @Autowired
    public MockTermController(TermDB termDB) {
        this.termDB = termDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Term records from the mock database with optional filtering",
            response = Term.class,
            responseContainer = "List"
    )
    @GetMapping("/terms")
    public List<Term> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);

        return map.size() == 0 ? termDB.getAllSorted() : termDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Term record to the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/terms")
    public Term post(@ApiParam(value = "Term to add", required = true) @RequestBody Term term) {
        logger.debug("Mock: add {}", term);
        return termDB.add(term);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by College ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Term records from the mock database for a college",
            response = Term.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms")
    public List<Term> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode) {
        logger.debug("Mock: get {}", misCode);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);

        return termDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Term record from the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}")
    public Term get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                    @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId) {
        logger.debug("Mock: get {} {}", misCode, sisTermId);
        return termDB.get(misCode, sisTermId);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET CURRENT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get current Term record from the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/current")
    public Term getCurrent(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @PathVariable("misCode") String misCode) {
        logger.debug("Mock: get {} ", misCode);
        return termDB.getCurrentTerm(misCode);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Term record in the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}")
    public Term put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                    @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                    @ApiParam(value = "Term to update", required = true) @RequestBody Term term) {
        logger.debug("Mock: put {} {} {}", misCode, sisTermId, term);
        return termDB.update(misCode, sisTermId, term);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a Term record in the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}")
    public Term patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                      @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                      @ApiParam(value = "Term to update", required = true) @RequestBody Map term) {
        logger.debug("Mock: patch {} {} {}", misCode, sisTermId, term);
        return termDB.patch(misCode, sisTermId, term);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Term record in the mock database",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}")
    public Term delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                       @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {}", misCode, sisTermId, cascade);
        return termDB.delete(misCode, sisTermId, cascade);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Term data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/terms/reload")
    public void reload() {
        logger.debug("Mock: reload");
        termDB.loadData();
    }
}