package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Course
import com.ccctc.adaptor.model.CrosslistingDetail
import com.ccctc.adaptor.model.CrosslistingStatus
import com.ccctc.adaptor.model.Section
import com.ccctc.adaptor.model.Term
import spock.lang.Specification

/**
 * Created by zekeo on 7/20/2017.
 */
class SectionDBSpec extends Specification  {

    List<Section> all
    Section first, second, third

    def misCode = "001"
    def sisTermId = "TERM"
    def sisCourseId = "ENGL-101"
    def sisSectionId1 = "1"
    def sisSectionId2 = "2"
    def sisSectionId3 = "3"

    def collegeDB = new CollegeDB()
    def termDB = new TermDB(collegeDB)
    def courseDB = new CourseDB(termDB)
    def sectionDB = new SectionDB(collegeDB, termDB, courseDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        termDB.init()
        courseDB.init()
        sectionDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        termDB.add(new Term(misCode: misCode, sisTermId: sisTermId, start: new Date(2000, 1, 1), end: new Date(2000, 6, 30)))
        courseDB.add(new Course(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, minimumUnits: 3.0))

        sectionDB.add(new Section(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId1, minimumUnits: 3.0))
        sectionDB.add(new Section(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId2, minimumUnits: 3.0))
        sectionDB.add(new Section(misCode: misCode, sisTermId: sisTermId, sisCourseId: sisCourseId, sisSectionId: sisSectionId3, minimumUnits: 3.0))

        all = sectionDB.getAll()

        first = all[0]
        second = all[1]
        third = all[2]
    }

    def "get"() {
        when:
        def r = sectionDB.get(first.misCode, first.sisTermId, first.sisSectionId)

        then:
        r.toString() == first.toString()
    }

    def "add"() {
        setup:
        def section = new Section()
        def result

        // missing mis code
        when:
        sectionDB.add(section)

        then:
        thrown InvalidRequestException

        // missing term
        when:
        section.misCode = first.misCode
        sectionDB.add(section)

        then:
        thrown InvalidRequestException

        // missing section
        when:
        section.sisTermId = first.sisTermId
        sectionDB.add(section)

        then:
        thrown InvalidRequestException

        // missing course
        when:
        section.sisSectionId = "test"
        sectionDB.add(section)

        then:
        thrown InvalidRequestException

        // success
        when:
        section.sisCourseId = first.sisCourseId
        result = sectionDB.add(section)

        then:
        sectionDB.getPrimaryKey(result) == sectionDB.getPrimaryKey(section)

        // success - clean up units (min to max)
        when:
        section = sectionDB.delete(section.misCode, section.sisTermId, section.sisSectionId, true)
        section.maximumUnits = null
        result = sectionDB.add(section)

        then:
        result.maximumUnits != null

        // success - clean up units (max to min)
        when:
        section = sectionDB.delete(section.misCode, section.sisTermId, section.sisSectionId, true)
        section.minimumUnits = null
        result = sectionDB.add(section)

        then:
        result.minimumUnits != null
    }

    def "update"() {
        setup:
        def copy = sectionDB.deepCopy(first)
        def result

        when:
        copy.sisSectionId = "nope"
        sectionDB.update(first.misCode, first.sisTermId, first.sisSectionId, copy)

        then:
        thrown InvalidRequestException

        when:
        copy.sisSectionId = first.sisSectionId
        result = sectionDB.update(first.misCode, first.sisTermId, first.sisSectionId, copy)

        then:
        result.toString() == copy.toString()
    }

    def "patch"() {
        when:
        def r = sectionDB.patch(first.misCode, first.sisTermId, first.sisSectionId, [title: "patched"])

        then:
        r.title == "patched"
    }

    def "delete"() {
        when:
        def result = sectionDB.delete(first.misCode, first.sisTermId, first.sisSectionId, true)

        then:
        result.toString() == first.toString()
    }

    def "copy"() {
        when:
        termDB.add(new Term(misCode: first.misCode, sisTermId: "sisTermId", start: new Date(), end: new Date()+1))
        courseDB.copy(first.misCode, first.sisTermId, first.sisCourseId, "sisTermId")
        def result = sectionDB.copy(first.misCode, first.sisTermId, first.sisSectionId, "sisTermId")

        then:
        result.sisTermId == "sisTermId"
    }

    def "validate"() {
        when:
        sectionDB.validate(first.misCode, first.sisTermId, "nope")

        then:
        thrown InvalidRequestException
    }

    def "createCrosslisting and deleteCrosslisting"() {
        setup:
        // find a term with more than one section that's not yet crosslisted
        def terms = termDB.getAll()
        def sections = [first, second, third]

        // invalid
        when:
        sectionDB.createCrosslisting(sections[0].misCode, sections[0].sisTermId, "nope", [])

        then:
        thrown InvalidRequestException

        // invalid - only one section
        when:
        sectionDB.createCrosslisting(sections[0].misCode, sections[0].sisTermId, sections[0].sisSectionId, [sections[0].sisSectionId])

        then:
        thrown InvalidRequestException

        // success
        when:
        def result = sectionDB.createCrosslisting(sections[0].misCode, sections[0].sisTermId, sections[0].sisSectionId, [sections[1].sisSectionId, sections[2].sisSectionId])
        def primary = sectionDB.get(sections[0].misCode, sections[0].sisTermId, sections[0].sisSectionId)
        def secondary = sectionDB.get(sections[1].misCode, sections[1].sisTermId, sections[1].sisSectionId)

        then:
        result.name != null
        result.sisTermId == sections[0].sisTermId
        result.primarySisSectionId == sections[0].sisSectionId
        result.sisSectionIds != null
        result.sisSectionIds.size() == 3
        primary.crosslistingStatus == CrosslistingStatus.CrosslistedPrimary
        secondary.crosslistingStatus == CrosslistingStatus.CrosslistedSecondary
        primary.crosslistingDetail == secondary.crosslistingDetail

        // already crosslisted
        when:
        sectionDB.createCrosslisting(sections[1].misCode, sections[1].sisTermId, sections[1].sisSectionId, [sections[2].sisSectionId])

        then:
        thrown InvalidRequestException

        // delete
        when:
        result = sectionDB.deleteCrosslisting(sections[0].misCode, sections[0].sisTermId, sections[0].sisSectionId)

        then:
        result != null

        // delete - doesnt exist
        when:
        sectionDB.deleteCrosslisting(sections[1].misCode, sections[1].sisTermId, sections[1].sisSectionId)

        then:
        thrown InvalidRequestException
    }

    def "CrosslistingDetail extra coverage"() {
        when:
        def a = new CrosslistingDetail.Builder().sisSectionIds([]).name("name").sisTermId("sisTermId").primarySisSectionId("primarySisSectionId").build()
        def s = new Section.Builder().crosslistingDetail(a).sisTermId("sisTermId").build()

        then:
        a.name == "name"
        a.sisTermId == "sisTermId"
        a.primarySisSectionId == "primarySisSectionId"
        s.crosslistingDetail == a
        s.crosslistingStatus == CrosslistingStatus.CrosslistedSecondary

        when:
        s.sisSectionId = "primarySisSectionId"

        then:
        s.crosslistingStatus == CrosslistingStatus.CrosslistedPrimary
    }

    def "cascadeDelete"() {
        when:
        courseDB.delete(first.misCode, first.sisTermId, first.sisCourseId, true)
        sectionDB.get(first.misCode, first.sisTermId, first.sisSectionId)

        then:
        thrown EntityNotFoundException
    }

    // for added coverage of sorting in base
    def "getAllSorted"() {
        when:
        def r = sectionDB.getAllSorted()

        then:
        r.size() == 3
        r[0].sisSectionId == sisSectionId1
        r[1].sisSectionId == sisSectionId2
        r[2].sisSectionId == sisSectionId3
    }
}
