package api.peoplesoft

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

/**
 * <h1>Identity Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Translating a CCCId to a sisPersonId (emplid)</li>
 *         <li>Translating a EPPN to a sisPersonId (emplid)</li>
 *         <li>Translating a sisPersonId to a CCCId</li>
 *         <li>Assigning a CCCID to a sisPersonId</li>
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
class Identity {

    protected final static Logger log = LoggerFactory.getLogger(Identity.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Identity(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Identity(Environment e) {
        this.environment = e
    }

    /**
     * Gets the internal sis person identifier (emplid) based on the mapping in the legit Peoplesoft table
     * using the CCTC_Identities:getEmplIdFromExternalId peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College Student Id to translate. (required)
     * @return internal sis person identifier for the given cccId
     */
    String translateCCCIdToSisPersonId(String misCode, String cccId) {

        log.debug("translateCCCIdToSisPersonId: translating cccid")

        //****** Validate parameters ****
        if (!cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CCC Id cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("translateCCCIdToSisPersonId: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Identities"
        String methodName = "getEmplIdFromExternalId"

        String[] args = [
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.identity.cccId.external_system")),
                PSParameter.ConvertStringToCleanString(cccId)
        ]

        String sisPersonId = ""

        log.debug("translateCCCIdToSisPersonId: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("translateCCCIdToSisPersonId: calling the remote peoplesoft method")

            String[] resultData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(resultData == null || resultData.length <= 0) {
                sisPersonId = null
            }
            else {
                sisPersonId = resultData[0]
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("translateCCCIdToSisPersonId: translation failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.sisQueryError, messageToThrow)
        }
        finally {
            log.debug("translateCCCIdToSisPersonId: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("translateCCCIdToSisPersonId: done")
        return sisPersonId
    }

    /**
     * Gets the internal sis person identifier (emplid) based on the mapping in the legit Peoplesoft
     * table for given EPPN (EduPerson-Personnel Number)
     * using the CCTC_Identities:getEmplIdFromOprId peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param eppn The EPPN to translate. (required)
     * @return internal sis person identifier for the given EPPN
     */
    String translateEPPNToSisPersonId(String misCode, String eppn) {

        log.debug("translateEPPNToSisPersonId: translating eppn")

        //****** Validate parameters ****
        if (!eppn) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "EPPN cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("translateEPPNToSisPersonId: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Identities"
        String methodName = "getEmplIdFromOprId"

        String[] args = [
                PSParameter.ConvertStringToCleanString(eppn)
        ]

        String sisPersonId = ""

        log.debug("translateEPPNToSisPersonId: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("translateEPPNToSisPersonId: calling the remote peoplesoft method")

            String[] resultData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(resultData == null || resultData.length <= 0) {
                sisPersonId = null
            }
            else {
                sisPersonId = resultData[0]
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("translateEPPNToSisPersonId: translation failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.sisQueryError, messageToThrow)
        }
        finally {
            log.debug("translateEPPNToSisPersonId: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("translateEPPNToSisPersonId: done")
        return sisPersonId
    }

    /**
     * Gets the internal sis person identifier (emplid) based on the matching against in the legit Peoplesoft
     * table for given first name, last name and date of birth
     * using the CCTC_Identities:getEmplIdFromNameDob peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param firstName The first name to translate. (required)
     * @param lastName The last name to translate. (required)
     * @param dateOfBirth The date of birth to translate. (required)
     * @return internal sis person identifier
     */
    String translateNameDobToSisPersonId(String misCode, String firstName, String lastName, String dateOfBirth) {

        log.debug("translateNameDobToSisPersonId: translating name/dob")

        //****** Validate parameters ****
        if (!firstName) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "first name cannot be null or blank")
        }
        if (!lastName) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "last name cannot be null or blank")
        }
        if (!dateOfBirth) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "date of birth cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("translateNameDobToSisPersonId: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Identities"
        String methodName = "getEmplIdFromNameDob"

        String[] args = [
                PSParameter.ConvertStringToCleanString(firstName),
                PSParameter.ConvertStringToCleanString(lastName),
                PSParameter.ConvertStringToCleanString(dateOfBirth)
        ]

        String sisPersonId = ""

        log.debug("translateNameDobToSisPersonId: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("translateNameDobToSisPersonId: calling the remote peoplesoft method")

            String[] resultData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(resultData == null || resultData.length <= 0) {
                sisPersonId = null
            }
            else {
                sisPersonId = resultData[0]
            }
        }
        finally {
            log.debug("translateNameDobToSisPersonId: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("translateNameDobToSisPersonId: done")
        return sisPersonId
    }

    /**
     * Gets the CCCId  based on the mapping in the legit Peoplesoft
     * table for given internal sis person identifier (emplid)
     * using the CCTC_Identities:getExternalIdFromEmplId peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal sis person identifier to translate. (required)
     * @return cccId for the given sisPersonId
     */
    String translateSisPersonIdToCCCId(String misCode, String sisPersonId) {

        log.debug("translateSisPersonIdToCCCId: translating sisPersonId")

        //****** Validate parameters ****
        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sisPersonId cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("translateSisPersonIdToCCCId: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Identities"
        String methodName = "getExternalIdFromEmplId"

        String[] args = [
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.identity.cccId.external_system")),
                PSParameter.ConvertStringToCleanString(sisPersonId)
        ]

        String cccId = ""

        log.debug("translateSisPersonIdToCCCId: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("translateSisPersonIdToCCCId: calling the remote peoplesoft method")

            String[] resultData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(resultData == null || resultData.length <= 0) {
                cccId = null
            }
            else {
                cccId = resultData[0]
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("translateSisPersonIdToCCCId: translation failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.sisQueryError, messageToThrow)
        }
        finally {
            log.debug("translateSisPersonIdToCCCId: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("translateSisPersonIdToCCCId: done")
        return cccId
    }

    /**
     * Sets the CCCId mapping in the legit Peoplesoft
     * table for given internal sis person identifier (emplid)
     * using the CCTC_Identities:setExternalIdForEmplId peoplesoft method
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal sis person identifier to set mapping for. (required)
     * @param cccId The California Community College Id to set mapping for. (required)
     */
    void assignCCCIDToSisPersonId(String misCode, String sisPersonId, String cccId) {

        log.debug("assignCCCIDToSisPersonId: assigning cccId to sisPersonId")

        //****** Validate parameters ****
        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sisPersonId cannot be null or blank")
        }
        if (!cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "cccId cannot be null or blank")
        }
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (this.translateSisPersonIdToCCCId(misCode, sisPersonId) == cccId) {
            throw new EntityConflictException("CCCID is already assigned to the student")
        }

        log.debug("assignCCCIDToSisPersonId: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_IDENTITY_PKG"
        String className = "CCTC_Identities"
        String methodName = "setExternalIdForEmplId"

        String[] args = [
                PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.identity.cccId.external_system")),
                PSParameter.ConvertStringToCleanString(sisPersonId),
                PSParameter.ConvertStringToCleanString(cccId)
        ]

        log.debug("assignCCCIDToSisPersonId: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("assignCCCIDToSisPersonId: calling the remote peoplesoft method")

            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected == null || rowsAffected.length != 1) {
                log.error("assignCCCIDToSisPersonId: invalid number of rows affected. empty array")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1") && !StringUtils.equalsIgnoreCase(rowsAffected[0],"2")) {
                log.error("assignCCCIDToSisPersonId: invalid number of rows affected [" + rowsAffected[0] + "]")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("assignCCCIDToSisPersonId: assignment failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.sisQueryError, messageToThrow)
        }
        finally {
            log.debug("assignCCCIDToSisPersonId: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }
        log.debug("assignCCCIDToSisPersonId: done")
    }
}
