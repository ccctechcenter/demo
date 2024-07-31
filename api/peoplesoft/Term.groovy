package api.peoplesoft

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.TermSession
import com.ccctc.adaptor.model.TermType
import com.ccctc.adaptor.util.MisEnvironment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.text.SimpleDateFormat

import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * <h1>Term Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Retrieving Term data from the built-in peoplesoft tables</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 3.8.0
 *
 */
class Term {

    protected final static Logger log = LoggerFactory.getLogger(Term.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Term(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Term(Environment e) {
        this.environment = e
    }

    /**
     * Gets a Single Term from the legit Peoplesoft table using
     * the CCTC_Term:getTerms peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisTermId The College-specific/internal unique identifier for the term to retrieve (optional).
     *        if blank, retrieves all available terms
     */
    com.ccctc.adaptor.model.Term get(String misCode, String sisTermId) {
        List<com.ccctc.adaptor.model.Term> results = this.getTerms(misCode, sisTermId)
        if (!results || results.size() <= 0) {
            String errMsg = "get: Term not found"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, errMsg)
        }
        return results[0]
    }

    /**
     * Gets all Terms from the legit Peoplesoft table using
     * the CCTC_Term:getTermsByDate peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param asOf the date used to get terms for; asOf must lie between start and end date for each term returned (required)
     */
    List<com.ccctc.adaptor.model.Term> getTermsByDate(String misCode, LocalDate asOf) {
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

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_TERMS_PKG"
        String className = "CCTC_Term"
        String methodName = "getTermsByDate"

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        String[] args = [
                PSParameter.ConvertDateToCleanString(asOf, dateFormat),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.term.last_day_to_enroll.time_period"))
        ]
        String[] termData = []

        log.debug("getTermsByDate: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getTermsByDate: calling the remote peoplesoft method")
            termData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getTermsByDate: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("getTermsByDate: disconnecting")
            if (peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        List<com.ccctc.adaptor.model.Term> retList = this.convertStringArrayToListOfTerms(misCode, termData)

        log.debug("getTermsByDate: done")
        return retList
    }

    /**
     * Gets all Terms from the legit Peoplesoft table using
     * the CCTC_Term:getTerms peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     */
    List<com.ccctc.adaptor.model.Term> getAll(String misCode) {
        return this.getTerms(misCode, "")
    }

    /**
     * Gets all matching Terms from the legit Peoplesoft table using
     * the CCTC_Term:getTerms peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment (required)
     * @param sisTermId The College-specific/internal unique identifier for the term to retrieve (optional).
     *        if blank, retrieves all available terms
     */
    protected List<com.ccctc.adaptor.model.Term> getTerms(String misCode, String sisTermId) {

        log.debug("getTerms: getting Term data")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "getTerms: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("getTerms: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_TERMS_PKG"
        String className = "CCTC_Term"
        String methodName = "getTerms"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career")),
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.term.last_day_to_enroll.time_period"))
        ]
        String[] termData = []

        log.debug("getTerms: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getTerms: calling the remote peoplesoft method")
            termData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getTerms: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("getTerms: disconnecting")
            if (peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        List<com.ccctc.adaptor.model.Term> retList = this.convertStringArrayToListOfTerms(misCode, termData)

        log.debug("getTerms: done")
        return retList
    }

    /**
     * Converts the string representation of, potentially multiple, Terms into a list of Rich objects
     * @param misCode The College Code used to set the misCode in each object (required)
     * @param termData String array (to convert) with each term represented as consecutive strings in array (required)
     */
    protected List<com.ccctc.adaptor.model.Term> convertStringArrayToListOfTerms(String misCode, String[] termData) {
        List<com.ccctc.adaptor.model.Term> retList = []

        if (termData != null && termData.length > 0) {
            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd")

            Integer i = 0
            while (i + 15 <= termData.size()) {
                String term = termData[i++]
                Integer acad_year = termData[i++].toInteger()
                String term_category = termData[i++]
                String term_begin_dt = termData[i++]
                String term_end_dt = termData[i++]
                String session_code = termData[i++]
                String pre_reg_start_dt = termData[i++]
                String pre_reg_end_dt = termData[i++]
                String enroll_open_dt = termData[i++]
                String enroll_end_dt = termData[i++]
                String census_dt = termData[i++]
                String lst_drop_dt_ret = termData[i++]
                String lst_wd_wo_pen_dt = termData[i++]
                String descr = termData[i++]
                String term_type = termData[i++]

                com.ccctc.adaptor.model.Term.Builder builder = new com.ccctc.adaptor.model.Term.Builder()
                builder.misCode(misCode)
                        .sisTermId(term)
                        .year(acad_year ?: null)
                        .session(this.convertTermCategoryToTermSession(misCode, term_category))
                        .type(this.parseTermType(misCode, term_type))
                        .start((term_begin_dt != "") ? df2.parse(term_begin_dt) : null)
                        .end((term_end_dt != "") ? df2.parse(term_end_dt) : null)
                        .preRegistrationStart((pre_reg_start_dt != "") ? df2.parse(pre_reg_start_dt) : null)
                        .preRegistrationEnd((pre_reg_end_dt != "") ? df2.parse(pre_reg_end_dt) : null)
                        .registrationStart((enroll_open_dt != "") ? df2.parse(enroll_open_dt) : null)
                        .registrationEnd((enroll_end_dt != "") ? df2.parse(enroll_end_dt) : null)
                        .addDeadline((enroll_end_dt != "") ? df2.parse(enroll_end_dt) : null)
                        .dropDeadline((lst_drop_dt_ret != "") ? df2.parse(lst_drop_dt_ret) : null)
                        .withdrawalDeadline((lst_wd_wo_pen_dt != "") ? df2.parse(lst_wd_wo_pen_dt) : null)
                        .feeDeadline()
                        .censusDate((census_dt != "") ? df2.parse(census_dt) : null)
                        .description(descr ?: null)
                retList += builder.build()
            }
        }

        return retList
    }

    /**
     * Converts Peoplesoft's Term Category into a Term Session
     * @param misCode The College Code used to set the misCode in each object (required)
     * @param term_category College-specific term_category to be translated into the CCCTC standard TermSession
     */
    protected TermSession convertTermCategoryToTermSession(String misCode, String term_category) {
        TermSession result = TermSession.Fall
        if (term_category) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.term.mappings.termSession.term_category." + term_category)
            if (configMapping) {
                TermSession found = TermSession.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("convertTermCategoryToTermSession: Could not parse term_category [" + term_category + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("convertTermCategoryToTermSession: term_category [" + term_category + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Parses the Peoplsoft's term type into a Term Type
     * @param misCode The College Code used to set the misCode in each object (required)
     * @param term_type String to parsed into the CCCTC standard TermType (required)
     */
    protected TermType parseTermType(String misCode, String term_type) {
        TermType result = TermType.Semester
        if (term_type) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.term.mappings.termType.term_type." + term_type)
            if (configMapping) {
                TermType found = TermType.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("parseTermType: Could not parse term_type [" + term_type + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("parseTermType: term_type [" + term_type + "] mapping not found")
            }
        }
        return result
    }
}

