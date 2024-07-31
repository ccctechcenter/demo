package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.model.mock.TermDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockTermControllerSpec extends BaseMockController {

    TermDB mock
    MockTermController controller
    def data = [new Term(misCode: "000", sisTermId: "sisTermId")]

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class TermDBExtended extends TermDB {
        TermDBExtended() throws Exception {
            super(null, null, null)
        }

        @Override
        Term add(Term record) {
            return super.add(record)
        }

        @Override
        List<Term> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<Term> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(TermDBExtended)
        controller = new MockTermController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/terms")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/terms?misCode=000")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get all by college"() {
        when:
        def result = doGet("/mock/colleges/000/terms")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM")

        then:
        1 * mock.get(_, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/terms", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000/terms/TERM", data[0])

        then:
        1 * mock.update(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000/terms/TERM", [:])

        then:
        1 * mock.patch(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000/terms/TERM")

        then:
        1 * mock.delete(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/terms/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }
}
