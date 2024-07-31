package api.peoplesoft

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.MisEnvironment
import com.ccctc.adaptor.model.Email
import com.ccctc.adaptor.model.EmailType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

/**
 * <h1>Person Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Retrieving multiple Person Details</li>
 *         <li>Getting a Persons Emails</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 3.3.0
 *
 */
class Person {

    protected final static Logger log = LoggerFactory.getLogger(Person.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Person(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Person(Environment e) {
        this.environment = e
    }

    /**
     * Gets the person details for the given sisPersonId
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal/College-specific person Id to limit results to. (required)
     * @return a Person record matching the PersonId provided if found. otherwise, null
     */
    com.ccctc.adaptor.model.Person get(String misCode, String sisPersonId) {
        String[] people = [
                sisPersonId
        ]
        List<com.ccctc.adaptor.model.Person> results = this.getPeople(misCode, people)
        if(!results || results.size() <= 0) {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "No results found")
        }
        return results[0]
    }

    /**
     * Gets the person details for the given sisPersonId, eppn, or cccid
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal/College-specific person Id to limit results to. (optional)
     * @param eppn The EduPerson Personnel Id to limit results to. (optional)
     * @param cccId The California Community College person Id to limit results to. (optional)
     * @Note: If sisPersonId is not provided, falls back to the cccid and converts it to sisPersonId.
     * if sisPersonId and cccId both not provided (or cccId fails to convert to sisPersonId), falls back to the eppn and converts it to sisPersonId.
     * if after conversion done, still no sisPersonId, errors out.
     * @return a Person record matching the search params provided if found
     */
    com.ccctc.adaptor.model.Person getStudentPerson(String misCode, String sisPersonId, String eppn, String cccid) {

        if (!sisPersonId && !cccid && !eppn) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "at least one id (person, ccc, or eppn) must be provided")
        }

        if(!sisPersonId) {
            if (cccid) {
                api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
                sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
            }
        }
        if(!sisPersonId) {
            if (eppn) {
                api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
                sisPersonId = identityApi.translateEPPNToSisPersonId(misCode, eppn)
            }
        }

        if(!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "No Person found")
        }

        String[] people = [
                sisPersonId
        ]
        List<com.ccctc.adaptor.model.Person> results = this.getPeople(misCode, people)
        if(!results || results.size() <= 0) {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "No results found")
        }
        return results[0]
    }

    /**
     * Gets a person detail record for each of the given sisPersonId, eppn, or cccid
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonIds The list of internal/College-specific person Id to limit results to. (optional)
     * @param cccIds The list of California Community College person Id to limit results to. (optional)
     * @Note: Converts any/all cccIds to sisPersonIds and then appends them (uniquely) to the provided sisPersonIds
     * @return A list of Person records matching the list of IDs provided if found
     */
    List<com.ccctc.adaptor.model.Person> getAll(String misCode, String[] sisPersonIds, String[] cccids) {

        List<String> allPeople = (sisPersonIds ?: []).toList()
        if (cccids && cccids.length > 0) {
            def i = 0
            api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)

            while (i < cccids.length) {
                String sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccids[i++])
                if(sisPersonId){
                    allPeople.add(sisPersonId)
                }
            }
        }
        String[] distinctPeeps = allPeople.unique().toArray()

        return this.getPeople(misCode, distinctPeeps)
    }

    /**
     * Gets the person details for the given list of sisPersonIds
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The list of internal/College-specific person Id to limit results to. (required)
     * @return A list of Person records matching the list of IDs provided if found
     */
    private List<com.ccctc.adaptor.model.Person> getPeople(String misCode, String[] sisPersonIds) {

        log.debug("getPeople: getting data for list of people")

        //****** Validate parameters ****
        if (!sisPersonIds || sisPersonIds.length <= 0) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "List of sisPersonId cannot be null or blank")
        }
        if(sisPersonIds.length > 999) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "List of sisPersonId cannot exceed 999")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("getPeople: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Person"
        String methodName = "getPeople"

        String[] args = [
                String.join(", ", sisPersonIds)
        ]

        String[] getPeopleResults = []

        log.debug("getPeople: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getPeople: calling the remote peoplesoft method")

            getPeopleResults = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getPeople: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("getPeople: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        List<com.ccctc.adaptor.model.Person> people = []
        if(getPeopleResults && getPeopleResults.length > 0) {
            log.debug("getPeople: converting data to people objects")

            String loginSuffix = MisEnvironment.getProperty(environment, misCode,"peoplesoft.person.loginSuffix")

            api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
            Integer i = 0
            while (i + 4 <= getPeopleResults.length) {
                String sisPersonId = getPeopleResults[i++]
                String firstName = getPeopleResults[i++]
                String lastName = getPeopleResults[i++]
                String loginId = getPeopleResults[i++]

                String cccId = identityApi.translateSisPersonIdToCCCId(misCode, sisPersonId)
                List<Email> emailList = this.getPersonEmails(misCode, sisPersonId)

                def builder = new com.ccctc.adaptor.model.Person.Builder()
                builder.misCode(misCode)
                        .sisPersonId(sisPersonId)
                        .firstName(firstName ?: null)
                        .lastName(lastName ?: null)
                        .emailAddresses(emailList)
                        .loginId(loginId ?: null)
                        .loginSuffix(loginSuffix)
                        .cccid(cccId ?: null)
                people += builder.build()
            }
        }

        return people
    }

    /**
     * Gets all the persons emails for the given sisPersonId
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal/College-specific person Id to limit results to. (required)
     * @return A String array containing all the emails associated with the given person
     */
    List<Email> getPersonEmails(String misCode, String sisPersonId) {

        log.debug("getPersonEmails: getting emails for person")

        //****** Validate parameters ****
        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisPersonId cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("getPersonEmails: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Person"
        String methodName = "getPersonEmails"

        String[] args = [
                PSParameter.ConvertStringToCleanString(sisPersonId)
        ]

        String[] emailResults = []

        log.debug("getPersonEmails: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getPersonEmails: calling the remote peoplesoft method")

            emailResults = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getPersonEmails: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("getPersonEmails: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("getPersonEmails: converting data to people objects")
        List<Email> emailList = []
        if(emailResults && emailResults.length > 0) {
            Integer i = 0
            while (i + 1 < emailResults.length) {
                def email_type = emailResults[i++]
                def emailAddr = emailResults[i++]
                def emailbuilder = new Email.Builder()
                        .type(this.parseEmailAddressType(misCode, email_type))
                        .emailAddress(emailAddr.toString())
                emailList += emailbuilder.build()
            }
        }

        return emailList
    }

    /**
     * Converts/parses the Peoplesoft Specific e_addr_type code into the corresponding EmailType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param e_addr_type The peoplesoft specific e_addr_type to convert (required)
     * @return a EmailType with a value associated with the given e_addr_type
     */
    protected EmailType parseEmailAddressType(String misCode, String e_addr_type) {
        EmailType result
        if (e_addr_type) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.person.mappings.emailType.e_addr_type." + e_addr_type)
            if (configMapping) {
                EmailType found = EmailType.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse e_addr_type [" + e_addr_type + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("e_addr_type [" + e_addr_type + "] mapping not found")
            }
        }
        return result
    }

}
