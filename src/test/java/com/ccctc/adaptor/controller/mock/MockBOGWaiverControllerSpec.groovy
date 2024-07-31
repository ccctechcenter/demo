package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.BOGWaiver
import com.ccctc.adaptor.model.mock.BOGWaiverDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockBOGWaiverControllerSpec extends BaseMockController {

    BOGWaiverDB mock
    MockBOGWaiverController controller
    def data = [new BOGWaiver(misCode: "000", sisTermId: "sisTermId", sisPersonId: "sisPersonId")]

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class BOGWaiverDBExtended extends BOGWaiverDB {
        BOGWaiverDBExtended() throws Exception {
            super(null, null, null, null)
        }

        @Override
        List<BOGWaiver> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<BOGWaiver> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(BOGWaiverDBExtended)
        controller = new MockBOGWaiverController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/bogfw")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/bogfw?sisPersonId=sisPersonId")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/students/STUDENT/bogfw")

        then:
        1 * mock.get(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/bogfw", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000/terms/TERM/students/STUDENT/bogfw", data[0])

        then:
        1 * mock.update(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000/terms/TERM/students/STUDENT/bogfw", [:])

        then:
        1 * mock.patch(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000/terms/TERM/students/STUDENT/bogfw")

        then:
        1 * mock.delete(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/bogfw/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }
}
