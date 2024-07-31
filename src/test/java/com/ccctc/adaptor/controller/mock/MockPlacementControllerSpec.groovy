package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.mock.PlacementDB
import com.ccctc.adaptor.model.placement.PlacementTransaction

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MockPlacementControllerSpec extends BaseMockController {

    PlacementDB mock
    MockPlacementController controller

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    static class PlacementDBExtended extends PlacementDB {
        PlacementDBExtended() throws Exception {
            super(null)
        }

        @Override
        List<PlacementDB.MockPlacementTransaction> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<PlacementDB.MockPlacementTransaction> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        PlacementDB.MockPlacementTransaction add(PlacementDB.MockPlacementTransaction record) {
            return super.add(record)
        }

        @Override
        void deleteAll(boolean cascade) {
            super.deleteAll(cascade)
        }
    }

    void setup() {
        mock = Mock(PlacementDBExtended)
        controller = new MockPlacementController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/placements")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/placements?sisPersonId=123")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/placements/1")

        then:
        1 * mock.get(*_)
        result.andExpect(status().isOk())
    }

    def "post"() {
        setup:
        def t = new PlacementDB.MockPlacementTransaction(null, new PlacementTransaction())

        when:
        def result = doPost("/mock/placements", t)

        then:
        1 * mock.add(_)
        result.andExpect(status().isCreated())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/placements/1")

        then:
        1 * mock.delete(*_)
        result.andExpect(status().isOk())
    }

    def "clear"() {
        when:
        def result = doPost("/mock/placements/clear", null)

        then:
        1 * mock.deleteAll(false)
        result.andExpect(status().isNoContent())
    }
}
