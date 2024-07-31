package com.ccctc.adaptor.controller.mock

import com.ccctc.adaptor.model.CrosslistingDetail
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.model.mock.SectionDB

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by zekeo on 7/21/2017.
 */
class MockSectionControllerSpec extends BaseMockController {

    SectionDB mock
    MockSectionController controller
    def data = [new Section(misCode: "000", sisTermId: "sisTermId", sisSectionId: "sisSectionId")]

    // extended class to expose some methods so they can be used w/ spock
    // (spock can't mock methods from BaseMockDataDB that aren't overloaded in a sub class)
    class SectionDBExtended extends SectionDB {
        SectionDBExtended() throws Exception {
            super(null, null, null, null)
        }

        @Override
        Section add(Section record) {
            return super.add(record)
        }

        @Override
        List<Section> getAllSorted() {
            return super.getAllSorted()
        }

        @Override
        List<Section> findSorted(Map<String, Object> searchMap) {
            return super.findSorted(searchMap)
        }

        @Override
        void loadData() {
            super.loadData()
        }
    }

    void setup() {
        mock = Mock(SectionDBExtended)
        controller = new MockSectionController(mock)
        setMockMvc(controller)
    }

    def "get all"() {
        when:
        def result = doGet("/mock/sections")

        then:
        1 * mock.getAllSorted()
        result.andExpect(status().isOk())

        when:
        result = doGet("/mock/sections?misCode=000")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get all by course"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/courses/COURSE/sections")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get all by term"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/sections")

        then:
        1 * mock.findSorted(_)
        result.andExpect(status().isOk())
    }

    def "get"() {
        when:
        def result = doGet("/mock/colleges/000/terms/TERM/sections/SECTION")

        then:
        1 * mock.get(_, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "post"() {
        when:
        def result = doPost("/mock/sections", data[0])

        then:
        1 * mock.add(_) >> data[0]
        result.andExpect(status().isCreated())
    }

    def "put"() {
        when:
        def result = doPut("/mock/colleges/000/terms/TERM/sections/SECTION", data[0])

        then:
        1 * mock.update(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "patch"() {
        when:
        def result = doPatch("/mock/colleges/000/terms/TERM/sections/SECTION", [:])

        then:
        1 * mock.patch(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "delete"() {
        when:
        def result = doDelete("/mock/colleges/000/terms/TERM/sections/SECTION")

        then:
        1 * mock.delete(_, _, _, _) >> data[0]
        result.andExpect(status().isOk())
    }

    def "copy"() {
        when:
        def result = doPost("/mock/colleges/000/terms/TERM/sections/SECTION/copy?newSisTermId=NEWTERM", null)

        then:
        1 * mock.copy(_, _, _, _)
        result.andExpect(status().isOk())
    }

    def "reload"() {
        when:
        def result = doPost("/mock/sections/reload", null)

        then:
        1 * mock.loadData()
        result.andExpect(status().isNoContent())
    }

    def "crosslist"() {
        when:
        def result = doPost("/mock/colleges/000/terms/TERM/sections/SECTION/crosslist?secondarySisSectionIds=OTHERSECTION", null)

        then:
        1 * mock.createCrosslisting(_, _, _, _) >> new CrosslistingDetail()
        result.andExpect(status().isOk())
    }

    def "crosslist - delete"() {
        when:
        def result = doDelete("/mock/colleges/000/terms/TERM/sections/SECTION/crosslist")

        then:
        1 * mock.deleteCrosslisting(_, _, _) >> new CrosslistingDetail()
        result.andExpect(status().isOk())
    }
}
