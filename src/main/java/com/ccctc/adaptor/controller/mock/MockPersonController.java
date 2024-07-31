package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ErrorResource;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.mock.PersonDB;
import com.ccctc.adaptor.model.mock.StudentDB;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(
        value = "Mock Persons",
        tags = "Mock Persons",
        description = "Operations related to Mock Person data",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockPersonController {

    private final static Logger logger = LoggerFactory.getLogger(MockPersonController.class);

    private PersonDB personDB;
    private StudentDB studentDB;


    @Autowired
    public MockPersonController(PersonDB personDB, StudentDB studentDB) {
        this.personDB = personDB;
        this.studentDB = studentDB;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Person records from the mock database with optional filtering",
            response = Person.class,
            responseContainer = "List"
    )
    @GetMapping("/persons")
    public List<Person> get(@RequestParam Map<String, Object> map) {
        logger.debug("Mock: get {}", map);
        return map.size() == 0 ? personDB.getAllSorted() : personDB.findSorted(map);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ POST ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Add a Person record to the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/persons")
    public Person post(@ApiParam(value = "Person to add", required = true) @RequestBody Person person) {
        logger.debug("Mock: add {}", person);
        return personDB.add(person);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ALL by College ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get all Person records from the mock database for a college",
            response = Person.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound")
    })
    @GetMapping("/colleges/{misCode}/persons")
    public List<Person> get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode) {
        logger.debug("Mock: get {}", misCode);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);

        return personDB.findSorted(map);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Person record from the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/persons/{sisPersonId:.+}")
    public Person get(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                      @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId) {
        logger.debug("Mock: get {} {}", misCode, sisPersonId);
        return personDB.get(misCode, sisPersonId);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~ GET ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Get a Student Person record from the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
            @ApiResponse(code = 404, response = ErrorResource.class, message = "noResultsFound")
    })
    @GetMapping("/colleges/{misCode}/persons/student")
    public Person getStudentPerson(@ApiParam(value = "MIS Code of the College", example = "001", required = true) @RequestParam("mis") String misCode,
                                   @ApiParam(value = "SIS Person ID", example = "1234567", required = false) @RequestParam(value = "sisPersonId", required = false) String sisPersonId,
                                   @ApiParam(value = "eppn of student", example = "test@test.edu", required = false) @RequestParam(value = "eppn", required = false) String eppn,
                                   @ApiParam(value = "cccid of Student", example = "001", required = false) @RequestParam(value = "cccid", required = false) String cccId) {
        logger.debug("Mock: getStudentPerson {} {} {} {}", misCode, sisPersonId, eppn, cccId);

        if (sisPersonId == null && cccId == null && eppn == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisPersonId or cccid or eppn is required");
        }
        Person person = personDB.validate(misCode, sisPersonId, cccId, eppn);
        try {
            if (person == null) {
                throw new EntityNotFoundException("person record not found");
            }

            if (studentDB.validate(misCode, person.getSisPersonId()) != null) {
                return person;
            }
        } catch (InvalidRequestException e) {
            throw new EntityNotFoundException("person record found but is not a student");
        }
        return person;
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Update a Person record in the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PutMapping("/colleges/{misCode}/persons/{sisPersonId:.+}")
    public Person put(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                      @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                      @ApiParam(value = "Person to update", required = true) @RequestBody Person person) {
        logger.debug("Mock: put {} {} {}", misCode, sisPersonId, person);
        return personDB.update(misCode, sisPersonId, person);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ PATCH ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Patch a Person record in the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @PatchMapping("/colleges/{misCode}/persons/{sisPersonId:.+}")
    public Person patch(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                        @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                        @ApiParam(value = "Person to update", required = true) @RequestBody Map person) {
        logger.debug("Mock: patch {} {} {}", misCode, sisPersonId, person);
        return personDB.patch(misCode, sisPersonId, person);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ DELETE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Delete a Person record in the mock database",
            response = Person.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, response = ErrorResource.class, message = "misCodeNotFound"),
    })
    @DeleteMapping("/colleges/{misCode}/persons/{sisPersonId:.+}")
    public Person delete(@ApiParam(value = "MIS Code of the College", example = "001") @PathVariable("misCode") String misCode,
                         @ApiParam(value = "SIS Person ID", example = "1234567") @PathVariable("sisPersonId") String sisPersonId,
                         @ApiParam(value = "Cascade delete", example = "true", defaultValue = "false")
                         @RequestParam(value = "cascade", required = false, defaultValue = "false") Boolean cascade) {
        logger.debug("Mock: delete {} {} {}", misCode, sisPersonId, cascade);
        return personDB.delete(misCode, sisPersonId, cascade);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~ RELOAD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @ApiOperation(
            value = "Reload/reset mock Person data"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/persons/reload")
    public void reload() {
        logger.debug("Mock: reload");
        personDB.loadData();
    }
}