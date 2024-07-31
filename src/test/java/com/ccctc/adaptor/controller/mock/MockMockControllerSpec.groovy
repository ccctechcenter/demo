package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.mock.ApplyDB
import com.ccctc.adaptor.model.mock.BOGWaiverDB
import com.ccctc.adaptor.model.mock.CollegeDB
import com.ccctc.adaptor.model.mock.CourseDB
import com.ccctc.adaptor.model.mock.EnrollmentDB
import com.ccctc.adaptor.model.mock.FinancialAidUnitsDB
import com.ccctc.adaptor.model.mock.PersonDB
import com.ccctc.adaptor.model.mock.PlacementDB
import com.ccctc.adaptor.model.mock.SectionDB
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.StudentPlacementDB
import com.ccctc.adaptor.model.mock.StudentPrereqDB
import com.ccctc.adaptor.model.mock.TermDB
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MockMockControllerSpec extends Specification {

    def "loadData"() {
        def bog = new BOGWaiverDB(null, null, null, null) {
            @Override
            void loadData() {

            }
        }
        def col = new CollegeDB() {
            @Override
            void loadData() {

            }
        }
        def cou = new CourseDB(null) {
            @Override
            void loadData() {

            }
        }
        def enr = new EnrollmentDB(null, null, null, null, null, null) {
            @Override
            void loadData() {

            }
        }
        def fin = new FinancialAidUnitsDB(null, null, null, null) {
            @Override
            void loadData() {

            }
        }
        def per = new PersonDB(null) {
            @Override
            void loadData() {

            }
        }
        def sec = new SectionDB(null, null, null) {
            @Override
            void loadData() {

            }
        }
        def stu = new StudentDB(null, null, null) {
            @Override
            void loadData() {

            }
        }
        def pre = new StudentPrereqDB(null, null, null, null) {
            @Override
            void loadData() {

            }
        }
        def ter = new TermDB(null) {
            @Override
            void loadData() {

            }
        }
        def plc = new PlacementDB(null) {
            @Override
            void loadData() {

            }
        }
        def apy = new ApplyDB(null) {
            @Override
            void loadData() {

            }
        }
        def sp = new StudentPlacementDB(null) {
            @Override
            void loadData() {

            }
        }

        def a = new MockController(bog, col, cou, enr, fin, per, sec, stu, pre, ter, plc, apy, sp)

        def mockMvc = MockMvcBuilders.standaloneSetup(a).build()

        when:
        def result = mockMvc.perform(post("/mock/loadData"))

        then:
        result.andExpect(status().is(204))
    }
}
