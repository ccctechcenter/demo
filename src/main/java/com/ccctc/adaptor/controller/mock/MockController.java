package com.ccctc.adaptor.controller.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.mock.*;
import com.ccctc.adaptor.util.CoverageIgnore;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Api(
        value = "Mock",
        tags = "Mock",
        description = "Operations related to Mock/Test data with a mock SIS",
        produces = "application/json",
        position = 1)
@RestController
@RequestMapping("/mock")
@Conditional(MockCondition.class)
public class MockController {

    private final static Logger logger = LoggerFactory.getLogger(MockController.class);

    private BOGWaiverDB bogWaiverDB;
    private CollegeDB collegeDB;
    private CourseDB courseDB;
    private EnrollmentDB enrollmentDB;
    private FinancialAidUnitsDB financialAidUnitsDB;
    private PersonDB personDB;
    private SectionDB sectionDB;
    private StudentDB studentDB;
    private StudentPrereqDB studentPrereqDB;
    private TermDB termDB;
    private PlacementDB placementDB;
    private ApplyDB applyDB;
    private StudentPlacementDB studentPlacementDB;

    @Autowired
    public MockController(BOGWaiverDB bogWaiverDB, CollegeDB collegeDB, CourseDB courseDB, EnrollmentDB enrollmentDB,
                          FinancialAidUnitsDB financialAidUnitsDB, PersonDB personDB, SectionDB sectionDB,
                          StudentDB studentDB, StudentPrereqDB studentPrereqDB, TermDB termDB, PlacementDB placementDB,
                          ApplyDB applyDB, StudentPlacementDB studentPlacementDB) {
        this.bogWaiverDB = bogWaiverDB;
        this.courseDB = courseDB;
        this.collegeDB = collegeDB;
        this.enrollmentDB = enrollmentDB;
        this.financialAidUnitsDB = financialAidUnitsDB;
        this.personDB = personDB;
        this.sectionDB = sectionDB;
        this.studentDB = studentDB;
        this.studentPrereqDB = studentPrereqDB;
        this.termDB = termDB;
        this.placementDB = placementDB;
        this.applyDB = applyDB;
        this.studentPlacementDB = studentPlacementDB;
    }

    @ApiOperation(
            value = "Loads/resets all mock data into in-memory store."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(value = "/loadData")
    public void loadData() {
        logger.debug("Mock: loadData");

        // load in correct order to respect referential integrity
        collegeDB.loadData();
        personDB.loadData();

        termDB.loadData();
        courseDB.loadData();
        sectionDB.loadData();

        studentDB.loadData();
        bogWaiverDB.loadData();
        financialAidUnitsDB.loadData();
        studentPrereqDB.loadData();

        enrollmentDB.loadData();

        placementDB.loadData();
        applyDB.loadData();
        studentPlacementDB.loadData();
    }
}
