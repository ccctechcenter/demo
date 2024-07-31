package api.colleague.base

import api.colleague.model.TermsLocationsRecord
import api.colleague.model.TermsRecord
import api.colleague.util.ColleagueUtils
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.Term as CATerm
import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static api.colleague.util.ColleagueUtils.fromLocalDate

/**
 * <h1>Term Colleague Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native colleague java API for:</p>
 *     <ol>
 *         <li>Retrieving a List of all Terms from the built-in colleague tables</li>
 *         <li>Retrieving a List of Terms for the given date from the built-in colleague tables</li>
 *     <ol>
 * </summary>
 *
 * @version 4.0.0
 *
 */
@CompileStatic
abstract class Term {

    protected final static Logger log = LoggerFactory.getLogger(Term.class)

    //
    // DMI services
    //
    DmiService dmiService
    DmiEntityService dmiEntityService
    DmiDataService dmiDataService


    //
    // configuration from properties files
    //
    String defaultTermType
    List<String> termLocationOverride



    /**
     * Initialize services, read properties
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiService = services.get(DmiService.class)
        this.dmiDataService = services.get(DmiDataService.class)
        this.dmiEntityService = services.get(DmiEntityService.class)

        // read configuration
        this.defaultTermType = ColleagueUtils.getColleagueProperty(environment, misCode, "default.term.type")
        this.termLocationOverride = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "term.locations")

        ColleagueUtils.keepAlive(dmiService)
    }


    /**
     * Gets all Terms from the TERMS table that overlap the asOf date
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param asOf the date used to get terms for; asOf must lie between start and end date for each term returned (required)
     */
    List<String> getTermsByDate(String misCode, LocalDate asOf) {
        log.debug("getTermsByDate: getting term by date")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "getTermsByDate: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!asOf) {
            String errMsg = "getTermsByDate: As Of date cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("getTermsByDate: params ok; building method params")

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        String asOfDate = asOf.format(dateFormat)
        def query = "TERM.START.DATE LE  " + ColleagueUtils.quoteString(asOfDate) + " AND TERM.END.DATE GE " + ColleagueUtils.quoteString(asOfDate)
        log.debug("getTermsByDate: retrieving data")
        try {
            return dmiDataService.selectKeys("TERMS", query) as List<String>
        }
        finally {
            log.debug("getTermsByDate: done")
        }
    }

    /**
     * Get one term
     */
    CATerm get(String misCode, String sisTermId) {

        assert misCode != null
        assert sisTermId != null

        def terms = getAll(misCode, [sisTermId])

        if (!terms)
            throw new EntityNotFoundException("Term not found")

        return terms[0]
    }

    /**
     * Get terms, optionally filtering on a list of sisTermIds
     */
    List<CATerm> getAll(String misCode, List<String> sisTermIds = null) {

        def colleagueData = read(misCode, sisTermIds)
        colleagueData = filter(misCode, sisTermIds, colleagueData)

        def terms = colleagueData.collect { parse(misCode, it) }

        // sort by start date
        terms.sort { it.start }

        return terms
    }

    protected List<TermsRecord> filter(String misCode, List<String> sisTermIds, List<TermsRecord> data) {
        // no default implementation
        return data
    }

    /**
     * Get a list of terms, or all if termIds is null/empty
     */
    protected List<TermsRecord> read(String misCode, List<String> termIds) {
        assert misCode != null

        if (!termIds)
            termIds = dmiDataService.selectKeys("TERMS").toList()

        return dmiEntityService.readForEntity(termIds, TermsRecord.class)
    }


    /**
     * Parse one term
     */
    protected CATerm parse(String misCode, TermsRecord termData) {
        //def start = termData.termStartDate
        def session = getSession(termData.recordId, termData.termSession, termData.termDesc)

        // get term type either from a closure or a default value in the config
        def termType = defaultTermType

        def builder = new CATerm.Builder()
                .misCode(misCode)
                .sisTermId(termData.recordId)
                .year(termData.termStartDate?.year)
                .session(session)
                .type(termType ? termType as TermType : null)
                .start(fromLocalDate(termData.termStartDate))
                .end(fromLocalDate(termData.termEndDate))
                .description(termData.termDesc)
                .feeDeadline(null) // not available

        // find term location override value, if applicable
        TermsLocationsRecord override = null
        if (termLocationOverride) {
            for (def tl : termLocationOverride) {
                override = termData.termLocations?.find { it.recordId == termData.recordId + "*" + tl }
                if (override) break
            }
        }

        if (!override) {
            builder
                    .preRegistrationStart(fromLocalDate(termData.termPreregStartDate))
                    .preRegistrationEnd(fromLocalDate(termData.termPreregEndDate))
                    .registrationStart(fromLocalDate(termData.termRegStartDate))
                    .registrationEnd(fromLocalDate(termData.termRegEndDate))
                    .addDeadline(fromLocalDate(termData.termAddEndDate))
                    .dropDeadline(fromLocalDate(termData.termDropEndDate))
                    .withdrawalDeadline(fromLocalDate(termData.termDropGradeReqdDate?.minusDays(1)))
                    .censusDate(fromLocalDate(termData.termCensusDates ? termData.termCensusDates[0] : null))
        } else {
            builder
                    .preRegistrationStart(fromLocalDate(override.tlocPreregStartDate))
                    .preRegistrationEnd(fromLocalDate(override.tlocPreregEndDate))
                    .registrationStart(fromLocalDate(override.tlocRegStartDate))
                    .registrationEnd(fromLocalDate(override.tlocRegEndDate))
                    .addDeadline(fromLocalDate(override.tlocAddEndDate))
                    .dropDeadline(fromLocalDate(override.tlocDropEndDate))
                    .withdrawalDeadline(fromLocalDate(override.tlocDropGradeReqdDate?.minusDays(1)))
                    .censusDate(fromLocalDate(override.tlocCensusDates ? override.tlocCensusDates[0] : null))
        }

        return builder.build()
    }


    /**
     * Determine session - there is no good translation available from MIS, so this tries several methods of determining
     * the session:
     *
     * 1. By the session associated with the term (TERM.SESSION)
     * 2. By the last two digits of the sisTermID (TERMS.ID)
     * 3. By the description of the term
     *
     * Methods 1 and 2 above look for codes of "FA", "SP", "SU" to represent Fall, Spring, Summer. For Winter, it
     * looks for a code of "IN", "IW" and "WI".
     *
     * Method 3 looks for the words Fall, Spring, Winter, Summer in the description or the words Spring Inter which
     * would also indicate winter (short for Spring Intersession).
     */
    protected TermSession getSession(String sisTermId, String termSession, String description) {
        //
        // so try several methods of determining the session based on the session
        // code

        switch (termSession) {
            case "FA":
                return TermSession.Fall
            case "SP":
                return TermSession.Spring
            case "SU":
                return TermSession.Summer
            case "IW":
            case "WI":
                return TermSession.Winter
        }

        // Try again by last two characters of term id
        if (sisTermId && sisTermId.length() >= 2) {
            switch (sisTermId[-2, -1]) {
                case "FA":
                    return TermSession.Fall
                case "SP":
                    return TermSession.Spring
                case "SU":
                    return TermSession.Summer
                case "IW":
                case "WI":
                    return TermSession.Winter
            }
        }

        // Finally, try by searching for words in the description
        if (description) {
            if (description.toUpperCase().contains("FALL"))
                return TermSession.Fall
            if (description.toUpperCase().contains("SPRING"))
                return TermSession.Spring
            if (description.toUpperCase().contains("SUMMER"))
                return TermSession.Summer
            if (description.toUpperCase().contains("WINTER"))
                return TermSession.Winter
        }

        return null
    }
}