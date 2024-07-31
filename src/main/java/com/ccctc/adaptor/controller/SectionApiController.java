package com.ccctc.adaptor.controller;

import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Section;
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
        value="Sections",
        tags="Sections",
        description="Operations related to Section data",
        produces="application/json",
        position=1)
@RestController
@RequestMapping("/sections")
public class SectionApiController {

    @Resource
    private GroovyService groovyService;

    @ApiOperation(
            value = "Retrieve a section by SIS Section ID and SIS Term ID",
            notes = "A section is uniquely identified by SIS Section ID and SIS Term ID",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping(value = "/{sisSectionId:.+}")
    public Section get(@ApiParam(value = "SIS Section ID", example = "1234", required = true) @PathVariable("sisSectionId") String sisSectionId,
                       @ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam("sisTermId") String sisTermId,
                       @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Section", "get", new Object[]{misCode, sisSectionId, sisTermId});
    }

    @ApiOperation(
            value = "Retrieve a list of sections by SIS Term ID, optionally filtering by SIS Course ID",
            response = Section.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping
    public List<Section> getAll(@ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam(value = "sisTermId") String sisTermId,
                                @ApiParam(value = "SIS Course ID", example = "ENGL-100") @RequestParam(value = "sisCourseId", required = false) String sisCourseId,
                                @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Section", "getAll", new Object[]{misCode, sisTermId, sisCourseId});
    }

    @ApiOperation(
            value = "Flex search sections in a term by a specific word or words. Words are case insensitive; various Section fields will be checked for matches with the word.",
            response = Section.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/search")
    public List<Section> flexSearch(@ApiParam(value = "SIS Term ID", example = "2017FA", required = true) @RequestParam(value = "sisTermId") String sisTermId,
                                    @ApiParam(value = "Flex search word(s)", example = "math", required = true) @RequestParam("words") String[] words,
                                    @ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode) {

        return groovyService.run(misCode, "Section", "search", new Object[]{misCode, sisTermId, words});
    }
}