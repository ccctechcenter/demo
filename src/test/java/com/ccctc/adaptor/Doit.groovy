package com.ccctc.adaptor

import com.ccctc.adaptor.model.mock.BOGWaiverDB
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.EnrollmentDB
import com.ccctc.adaptor.model.mock.FinancialAidUnitsDB
import com.ccctc.adaptor.model.mock.PersonDB
import com.ccctc.adaptor.model.mock.SectionDB
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.StudentPrereqDB
import com.ccctc.adaptor.model.mock.TermDB

/**
 * THIS IS NOT A UNIT TEST.
 *
 * This is used by the unit test GroovyServiceImplSpec to test out dynamic groovy scripts.
 */
class Doit {

    def environment
    // injected using GroovyServiceImpl on creation of class
    private CourseDB courseDB;
    private EnrollmentDB enrollmentDB;
    private PersonDB personDB;
    private SectionDB sectionDB;
    private StudentDB studentDB;
    private TermDB termDB;
    private FinancialAidUnitsDB financialAidUnitsDB;
    private BOGWaiverDB bogWaiverDB;
    private StudentPrereqDB studentPrereqDB;

    def run() {
        return "Stinky"
    }

    def returnSomething( arg ) {
        return arg
    }
}
