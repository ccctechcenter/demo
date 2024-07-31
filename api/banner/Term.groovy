/**
 * Created by Rasul on 1/18/2016.
 */
package api.banner

import com.ccctc.adaptor.exception.EntityNotFoundException

import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType
import com.ccctc.adaptor.util.MisEnvironment

import groovy.sql.Sql
import org.springframework.core.env.Environment

class Term {
    Environment environment

    def get(misCode, sisTermId) {
        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        def response
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.term.getQuery")
            response = sql.firstRow(query, [sisTermId: sisTermId])
        } finally {
            sql.close()
        }
        if (!response) {
            throw new EntityNotFoundException("Search returned no results.")
        }
        return buildTerm(response, misCode)

    }

    def getAll(misCode) {
        // Use JDBC to make a package call or sql query
        Sql sql = BannerConnection.getSession(environment, misCode)
        def termList = []
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.term.list.getQuery")
            sql.eachRow(query) { record ->
                termList << buildTerm(record, misCode)
            }
        } finally {
            sql.close()
        }
        return termList
    }

    def buildTerm(termRecord, misCode) {
        def calendar = new GregorianCalendar()
        calendar.setTimeInMillis(termRecord.start.getTime())
        def builder = new com.ccctc.adaptor.model.Term.Builder()
        builder
                .misCode(misCode)
                .sisTermId(termRecord.sisTermId)
                .year(calendar.get(Calendar.YEAR))
                .session(getTermSession(termRecord.description))
                .type(getTermType(termRecord.type, misCode))
                .start(termRecord.start)
                .end(termRecord.end)
                .preRegistrationStart()
                .preRegistrationEnd()
                .registrationStart(termRecord.registrationStart)
                .registrationEnd(termRecord.registrationEnd)
                .addDeadline()
                .dropDeadline()
                .withdrawalDeadline()
                .feeDeadline()
                .censusDate()
                .description(termRecord.description)
        return builder.build()
    }

    def getTermType(String termType, String misCode) {
        switch (termType) {
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.term.type.Semester", it) }:
                return TermType.Semester
                break
            case { MisEnvironment.checkPropertyMatch(environment, misCode, "banner.term.type.Quarter", it) }:
                return TermType.Quarter
                break
            default:
                return null
        }
    }

    def getTermSession(String description) {
        if (description) {
            switch (description) {
                case { it.contains(TermSession.Fall.toString()) }:
                    return TermSession.Fall
                case { it.contains(TermSession.Spring.toString()) }:
                    return TermSession.Spring
                case { it.contains(TermSession.Summer.toString()) }:
                    return TermSession.Summer
                case { it.contains(TermSession.Winter.toString()) }:
                    return TermSession.Winter
                default:
                    return null
            }
        }
    }

}