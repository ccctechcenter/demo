package api.colleague

import api.colleague.Term as GroovyTerm
import api.colleague.model.TermsLocationsRecord
import api.colleague.model.TermsRecord
import api.colleague.util.ColleagueUtils
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneOffset

class TermSpec extends Specification {

    def environment = Mock(Environment)
    def dmiService = Mock(DmiService)
    def dmiDataService = Mock(DmiDataService)
    def dmiEntityService = Mock(DmiEntityService)
    def groovyTerm = new GroovyTerm()
    def mockCache = Mock(Cache)
    def services = new ClassMap()

    def misCode = "000"

    def setup() {
        services.putAll([(DmiService.class): dmiService, (DmiDataService.class): dmiDataService,
                         (Cache.class): mockCache, (DmiEntityService.class): dmiEntityService])
        groovyTerm.colleagueInit(misCode, environment, services)
    }

    def "missing params"() {
        when:
        groovyTerm.get(null, null)

        then:
        thrown AssertionError

        when:
        groovyTerm.get("000", null)

        then:
        thrown AssertionError

        when:
        groovyTerm.getAll(null)

        then:
        thrown AssertionError
    }

    def "get - not found"() {
        when:
        groovyTerm.get("000", "sisTermId")

        then:
        1 * dmiEntityService.readForEntity(*_) >> []
        thrown EntityNotFoundException
    }

    def "getAll - no results"() {
        when:
        def r = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> []
        1 * dmiEntityService.readForEntity(*_) >> []
        r == []
    }


    def "get - no term/location data"() {
        setup:
        def d = LocalDate.now(ZoneOffset.UTC)
        def d2 = LocalDate.now(ZoneOffset.UTC).minusDays(1)

        def term1 = new TermsRecord(
                recordId: "term1",
                termDesc: "Description"
        )

        def term2 = new TermsRecord(
                recordId: "term2",
                termDesc: "Description",
                termCensusDates: [d],
                termDropGradeReqdDate: d2
        )

        when:
        def result1 = groovyTerm.get("000", "term1")
        def result2 = groovyTerm.get("000", "term2")

        then:
        1 * dmiEntityService.readForEntity(*_) >> [term1]
        1 * dmiEntityService.readForEntity(*_) >> [term2]
        result1.description == "Description"
        result1.withdrawalDeadline == null
        result1.censusDate == null
        result2.description == "Description"
        result2.censusDate == ColleagueUtils.fromLocalDate(d)
        result2.withdrawalDeadline == ColleagueUtils.fromLocalDate(d2.minusDays(1))
    }

    def "get - with term/location data"() {
        setup:
        def d = LocalDate.now(ZoneOffset.UTC)
        def d2 = LocalDate.now(ZoneOffset.UTC).minusDays(1)

        def term1 = new TermsRecord(
                recordId: "term1",
                termDesc: "Description",
                termCensusDates: [d],
                termDropGradeReqdDate: d2,
                termLocations: [new TermsLocationsRecord(recordId: "term1*1")]
        )

        def term2 = new TermsRecord(
                recordId: "term2",
                termDesc: "Description",
                termLocations: [new TermsLocationsRecord(
                        recordId: "term2*1",
                        tlocCensusDates: [d],
                        tlocDropGradeReqdDate: d2
                )]
        )

        when:
        // re-init to pick up pick up the environment property colleague.term.locations
        groovyTerm.colleagueInit(misCode, environment, services)
        def result1 = groovyTerm.get("000", "sisTermId")
        def result2 = groovyTerm.get("000", "sisTermId")

        then:
        1 * environment.getProperty("colleague.term.locations") >> "1"
        1 * dmiEntityService.readForEntity(*_) >> [term1]
        1 * dmiEntityService.readForEntity(*_) >> [term2]
        result1.description == "Description"
        result1.censusDate == null
        result1.withdrawalDeadline == null
        result2.description == "Description"
        result2.censusDate == ColleagueUtils.fromLocalDate(d)
        result2.withdrawalDeadline == ColleagueUtils.fromLocalDate(d2.minusDays(1))
    }

    def "getAll - no term/location data"() {
        setup:
        def term = new TermsRecord(recordId: "term1", termDesc: "Description")

        when:
        def result = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> [term.recordId]
        1 * dmiEntityService.readForEntity([term.recordId], TermsRecord.class) >> [term]
        result.size() == 1
        result[0].description == "Description"
    }

    def "getAll - with term/location data, add default term type"() {
        setup:
        def term = new TermsRecord(
                recordId: "term1",
                termDesc: "Description",
                termLocations: [
                        new TermsLocationsRecord(recordId: "term1*1", tlocRegStartDate: LocalDate.now(ZoneOffset.UTC)),
                        new TermsLocationsRecord(recordId: "term1*2")
                ]
        )

        when:
        // re-init to pick up mock environment properties
        groovyTerm.colleagueInit(misCode, environment, services)
        def result = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> [term.recordId]
        1 * dmiEntityService.readForEntity([term.recordId], TermsRecord.class) >> [term]
        1 * environment.getProperty("colleague.term.locations") >> "1,2"
        1 * environment.getProperty("colleague.default.term.type") >> "Semester"
        result.size() == 1
        result[0].description == "Description"
        result[0].registrationStart != null
        result[0].type == TermType.Semester
    }

    def "get - session"() {
        setup:
        def terms = [new TermsRecord(recordId: "term1", termSession: "FA"),
                     new TermsRecord(recordId: "term2", termSession: "SP"),
                     new TermsRecord(recordId: "term3", termSession: "SU"),
                     new TermsRecord(recordId: "term4", termSession: "WI"),
                     new TermsRecord(recordId: "term5", termSession: "IW")]

        when:
        def result = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> terms.collect { it.recordId }
        1 * dmiEntityService.readForEntity(*_) >> terms
        result.collect { it.session } == [
                TermSession.Fall, TermSession.Spring, TermSession.Summer,
                TermSession.Winter, TermSession.Winter]
    }

    def "get - session by sisTermId"() {
        setup:
        def terms = [new TermsRecord(recordId: "term1_FA"),
                     new TermsRecord(recordId: "term2_SP"),
                     new TermsRecord(recordId: "term3_SU"),
                     new TermsRecord(recordId: "term4_WI"),
                     new TermsRecord(recordId: "term5_IW")]

        when:
        def result = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> terms.collect { it.recordId }
        1 * dmiEntityService.readForEntity(*_) >> terms
        result.collect { it.session } == [
                TermSession.Fall, TermSession.Spring, TermSession.Summer,
                TermSession.Winter, TermSession.Winter]
    }

    def "get - session by description"() {
        setup:
        def terms = [new TermsRecord(recordId: "term1", termDesc: "Fall 2010"),
                     new TermsRecord(recordId: "term2", termDesc: "Spring 2010"),
                     new TermsRecord(recordId: "term3", termDesc: "Summer 2010"),
                     new TermsRecord(recordId: "term4", termDesc: "Winter 2010")]

        when:
        def result = groovyTerm.getAll("000")

        then:
        1 * dmiDataService.selectKeys("TERMS") >> terms.collect { it.recordId }
        1 * dmiEntityService.readForEntity(*_) >> terms
        result.collect { it.session } == [
                TermSession.Fall, TermSession.Spring, TermSession.Summer, TermSession.Winter]
    }

}