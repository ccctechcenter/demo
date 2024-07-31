package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.Student
import com.ccctc.adaptor.model.mock.StudentDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockStudentControllerSpec extends BaseMockController {

    StudentDB mock
    MockStudentController controller
    def data = [new Student(misCode: "000", sisTermId: "sisTermId", sisPersonId: "sisPersonId")]
    def cccIds = ["cccId"]
    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class StudentDBExtended extends StudentDB {
        StudentDBExtended() throws Exception {
            super(null, null, null, null, null, null, null)
        }

        @Override
        List<Student> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<Student> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(StudentDBExtended)
        controller = new MockStudentController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/students")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/students?misCode=000")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get all by term"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/students")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/students/STUDENT")

        then:
        1 * mock.get(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/students", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000/terms/TERM/students/STUDENT", data[0])

        then:
        1 * mock.update(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000/terms/TERM/students/STUDENT", [:])

        then:
        1 * mock.patch(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000/terms/TERM/students/STUDENT")

        then:
        1 * mock.delete(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "copy"() {
        when:
        def result = doPost("/mock/colleges/000/terms/TERM/students/STUDENT/copy?newSisTermId=NEWTERM", null)

        then:
        1 * mock.copy(_, _, _, _)
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/students/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }

    def "get home college"() {
        when:
        def result = doGet("/mock/colleges/000/students/STUDENT/homecollege")

        then:
        1 * mock.getHomeCollege(_, _)
        result.andExpect(status().isOk())
    }

    def "update home college"() {
        when:
        def result = doPut("/mock/colleges/000/students/STUDENT/homecollege?newHomeCollege=001", null)

        then:
        1 * mock.updateHomeCollege(_, _, _)
        result.andExpect(status().isOk())
    }

    def "get student cccids"() {
        when:
        def result = doGet("/mock/students/sisPersonId/cccids?mis=000")

        then:
        1 * mock.getStudentCCCIds(_, _) >> cccIds
        result.andExpect(status().isOk())
    }
    def "get post student cccid"() {
        when:
        def result = doPost("/mock//students/sisPersonId/cccids/cccid?mis=000", null)

        then:
        1 * mock.postStudentCCCId(_, _, _) >> cccIds
        result.andExpect(status().isOk())
    }


}
