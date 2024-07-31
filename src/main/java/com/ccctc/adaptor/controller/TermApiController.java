package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Term;
import com.ccctc.adaptor.util.GroovyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(
        value="Terms",
        tags="Terms",
        description="Operations related to Term data",
        produces="application/json",
        position=1)
@RestController
@RequestMapping("/terms")
public class TermApiController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve a term by SIS Term ID",
            response = Term.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{sisTermId:.+}")
    public Term get(@ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @PathVariable("sisTermId") String sisTermId,
                    @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Term", "get", new Object[]{misCode, sisTermId});
    }

    @ApiOperation(
            value = "Retrieve all terms",
            response = Term.class
    )
    @GetMapping
    public List<Term> getAll(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Term", "getAll", new Object[]{misCode});
    }
}