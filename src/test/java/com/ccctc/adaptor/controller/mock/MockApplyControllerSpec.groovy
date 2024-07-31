package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.mock.ApplyDB
import com.ccctc.adaptor.model.mock.CollegeDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockApplyControllerSpec extends BaseMockController {

    ApplyDB mock
    MockApplyController controller
    def data = [new Application(collegeId: "000", cccId: "ABC123")]

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class ApplyDBExtended extends ApplyDB {
        ApplyDBExtended() throws Exception {
            super(null)
        }

        @Override
        List<Application> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<Application> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        Application get(String misCode, long id) {
            return super.get(misCode, id)
        }

        @Override
        Application update(String misCode, long id, Application application) {
            return super.update(misCode, id, application)
        }

        @Override
        Application patch(String misCode, long id, Map application) {
            return super.patch(misCode, id, application)
        }

        @Override
        Application delete(String misCode, long id, boolean cascade) {
            return super.delete(misCode, id, cascade)
        }

        @Override
        Application add(Application record) {
            return super.add(record)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(ApplyDBExtended)
        controller = new MockApplyController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/apply")

        then:
        1 * mock.getAllSorted() >> data
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/apply?collegeId=000")

        then:
        1 * mock.findSorted(_) >> data
        result.andExpect(status().isOk())
    }


    def "get"() {
        when:
        def result = doGet("/mock/colleges/000/apply/1")

        then:
        1 * mock.get(_, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/apply", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000/apply/1", data[0])

        then:
        1 * mock.update(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000/apply/1", [:])

        then:
        1 * mock.patch(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000/apply/1")

        then:
        1 * mock.delete(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/apply/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }
}
