package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.CrosslistingDetail;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Section;
import com.ccctc.adaptor.model.mock.SectionDB;
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
        value = "Mock Sections",
        tags = "Mock Sections",
        description = "Operations related to Mock Section data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockSectionController {

    private final static Logger logger = LoggerFactory.getLogger(MockSectionController.class);

    private SectionDB sectionDB;

    @Autowired
    public MockSectionController(SectionDB sectionDB) {
        this.sectionDB = sectionDB;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Section records from the mock database with optional filtering",
            response = Section.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
    })
    @GetMapping("/sections")
    public List<Section> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {} ", map);

        return map.size() == 0 ? sectionDB.getAllSorted() : sectionDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by COURSE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Section records from the mock database for a Course",
            response = Section.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound"),
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/courses/{sisCourseId:.+}/sections")
    public List<Section> getByCourse(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                     @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                     @ApiParam(value = "SIS Course ID", example = "ENGL-1") @PathVariable("sisCourseId") String sisCourseId) {
        logger.debug("Mock: getByCourse {} {} {}", misCode, sisTermId, sisCourseId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisCourseId", sisCourseId);

        return sectionDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by TERM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Section records from the mock database for a Term",
            response = Section.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections")
    public List<Section> getByTerm(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                   @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId) {
        logger.debug("Mock: getByTerm {} {}", misCode, sisTermId);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);

        return sectionDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Section record from the mock database",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}")
    public Section get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId) {
        logger.debug("Mock: get {} {} {}", misCode, sisTermId, sisSectionId);
        return sectionDB.get(misCode, sisTermId, sisSectionId);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Section record to the mock database",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "courseNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sections")
    public Section post(@ApiParam(value = "Section to add", required = true) @RequestBody Section section) {
        logger.debug("Mock: add {}", section);
        return sectionDB.add(section);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Section record in the mock database",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PutMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}")
    public Section put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                       @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                       @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                       @ApiParam(value = "Section to update", required = true) @RequestBody Section section) {
        logger.debug("Mock: put {} {} {} {}", misCode, sisTermId, sisSectionId, section);
        return sectionDB.update(misCode, sisTermId, sisSectionId, section);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a Section record in the mock database",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @PatchMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}")
    public Section patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                         @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                         @ApiParam(value = "Section to update", required = true) @RequestBody Map section) {
        logger.debug("Mock: patch {} {} {} {}", misCode, sisTermId, sisSectionId, section);
        return sectionDB.patch(misCode, sisTermId, sisSectionId, section);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Section record in the mock database",
            response = Section.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}")
    public Section delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                          @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                          @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                          @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                          @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {} {}", misCode, sisTermId, sisSectionId, cascade);
        return sectionDB.delete(misCode, sisTermId, sisSectionId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Section data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/sections/reload")
    public void reload() {
        logger.debug("Mock: reload");
        sectionDB.loadData();
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ COPY ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Copy a Section to a new term"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound")
    })
    @PostMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/copy")
    public Section copy(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                        @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                        @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                        @ApiParam(value = "New SIS Term ID", example = "2018SP", required = true) @RequestParam("newSisTermId") String newSisTermId) {
        logger.debug("Mock: copy {} {} {} {}", misCode, sisTermId, sisSectionId, newSisTermId);
        return sectionDB.copy(misCode, sisTermId, sisSectionId, newSisTermId);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ CROSSLIST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Create a crosslisting making the given section the primary"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound")
    })
    @PostMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/crosslist")
    public CrosslistingDetail addCrosslisting(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                              @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                              @ApiParam(value = "Primary SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId,
                                              @ApiParam(value = "Section SIS Section IDs", example = "1234", required = true) @RequestParam("secondarySisSectionIds") List<String> secondarySisSectionIds) {
        logger.debug("Mock: addCrosslisting {} {} {} {}", misCode, sisTermId, sisSectionId, secondarySisSectionIds);
        return sectionDB.createCrosslisting(misCode, sisTermId, sisSectionId, secondarySisSectionIds);
    }

    @ApiOperation(
            value = "Delete a crosslisting. Will delete the entire crosslisting, not just the given section in the crosslisting."
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "termNotFound"),
            @ApiResponse(code = 400, response = ErrorResource.class, message = "sectionNotFound")
    })
    @DeleteMapping("/colleges/{misCode}/terms/{sisTermId:.+}/sections/{sisSectionId:.+}/crosslist")
    public CrosslistingDetail deleteCrosslisting(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                                                 @ApiParam(value = "SIS Term ID", example = "2017FA") @PathVariable("sisTermId") String sisTermId,
                                                 @ApiParam(value = "SIS Section ID", example = "1234") @PathVariable("sisSectionId") String sisSectionId) {
        logger.debug("Mock: deleteCrosslisting {} {} {}", misCode, sisTermId, sisSectionId);
        return sectionDB.deleteCrosslisting(misCode, sisTermId, sisSectionId);
    }
}