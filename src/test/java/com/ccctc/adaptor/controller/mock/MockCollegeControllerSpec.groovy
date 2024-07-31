package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.mock.CollegeDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockCollegeControllerSpec extends BaseMockController {

    CollegeDB mock
    MockCollegeController controller
    def data = [new College(misCode: "000")]

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class CollegeDBExtended extends CollegeDB {
        CollegeDBExtended() throws Exception {
            super(null, null)
        }

        @Override
        College add(College record) {
            return super.add(record)
        }

        @Override
        List<College> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<College> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(CollegeDBExtended)
        controller = new MockCollegeController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/colleges")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/colleges?misCode=000")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/colleges/000")

        then:
        1 * mock.get(_) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/colleges", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000", data[0])

        then:
        1 * mock.update(_, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000", [:])

        then:
        1 * mock.patch(_, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000")

        then:
        1 * mock.delete(_, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/colleges/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }
}
