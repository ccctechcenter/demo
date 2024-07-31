package api.peoplesoft

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.fraud.FraudReport as FraudReportModel
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * <h1>Fraud Report Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the FraudReport Peoplesoft sis functionality:</p>
 *     <ol>
 *         <li>Storing and populating Fraud Reports into the peoplesoft staging table</li>
 *         <li>Retrieving Fraud Reports by Id, and/or by appId/cccId, from the peoplesoft staging table</li>
 *         <li>Removing Fraud Reports by Id from the peoplesoft staging table</li>
 *     <ol>
 * </summary>
 *
 * @version CAv4.11.0
 *
 */
class FraudReport {

    protected final static Logger log = LoggerFactory.getLogger(FraudReport.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    FraudReport(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    FraudReport(Environment e) {
        this.environment = e
    }

    /**
     * Gets FraudReport Data from the sis staging table matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     * @returns A matching Fraud Report
     */
    FraudReportModel getById(String misCode, Long sisFraudReportId) {
        log.debug("getById: getting fraud data by id")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!sisFraudReportId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sis fraud report id cannot be null or blank")
        }

        log.debug("getById: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FRAUD_REPORT_PKG"
        String className = "CCTC_FraudReport"
        String methodName = "getReport"

        String[] args = []
        args += sisFraudReportId

        FraudReportModel result = new FraudReportModel()

        log.debug("getById: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getById: calling the remote peoplesoft method")
            String[] fraudReportData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(fraudReportData == null || fraudReportData.length <= 0) {
                throw new EntityNotFoundException("No Fraud Report found by given id")
            }

            List<FraudReportModel> results = ConvertStringArrayToFraudReports(fraudReportData)

            if(results == null || results.size() <= 0) {
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "could not parse string data to fraud report object")
            }
            result = results[0];
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getById: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("getById: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("getById: done")
        return result
    }

    /**
     * Removes FraudReport Data from the sis staging table matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     */
    void deleteById(String misCode, Long sisFraudReportId) {
        log.debug("deleteById: removing fraud data by id")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!sisFraudReportId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sis fraud report id cannot be null or blank")
        }

        log.debug("deleteById: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FRAUD_REPORT_PKG"
        String className = "CCTC_FraudReport"
        String methodName = "removeReport"

        String[] args = []
        args += sisFraudReportId


        log.debug("deleteById: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("deleteById: calling the remote peoplesoft method to remove record")

            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected == null || rowsAffected.length <= 0) {
                log.error("deleteById: invalid number of rows affected. empty array")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0],"1")) {
                log.error("deleteById: invalid number of rows affected [" + rowsAffected[0] + "]")
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
                        log.error("deleteById: removal failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("deleteById: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("deleteById: done")
    }

    void deleteFraudReport( FraudReportModel report ) {
        List<FraudReportModel> reports = getMatching(report.misCode, report.appId, report.cccId)
        if( !reports  || reports.size() <= 0) {
            throw new EntityNotFoundException("No Fraud Reports found by given params to rescind")
        }

        log.debug("deleteFraudReport: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, report.misCode)

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FRAUD_REPORT_PKG"
        String className = "CCTC_FraudReport"
        String methodName = "rescindReport"

        try {
            for (FraudReportModel r : reports) {
                String[] args = [ r.sisFraudReportId ]
                log.debug("deleteFraudReport: calling the remote peoplesoft method to remove record")

                String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (rowsAffected == null || rowsAffected.length <= 0) {
                    log.error("deleteFraudReport: invalid number of rows affected. empty array")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
                }
                if (!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                    log.error("deleteFraudReport: invalid number of rows affected [" + rowsAffected[0] + "]")
                    throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected [" + rowsAffected[0] + "]")
                }
            }
        }
        catch (psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if (peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if (msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("deleteFraudReport: removal failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("deleteFraudReport: disconnecting")
            if (peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }
    }


    /**
     * Retrieves FraudReport Data from the db matching on either appId or cccId or both
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param appId The Fraudulent AppId to match on
     * @param appId The Fraudulent cccId to match on
     * @returns A List of matching Fraud Reports
     */
    List<FraudReportModel> getMatching(String misCode, Long appId, String cccId) {
        log.debug("getMatching: getting matching fraud data by appId and/or cccId")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Mis Code cannot be null or blank")
        }
        if (!appId && !cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "either appId or cccId must be provided")
        }

        log.debug("getMatching: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_FRAUD_REPORT_PKG"
        String className = "CCTC_FraudReport"
        String methodName = "searchReports"

        String[] args = [
                PSParameter.ConvertLongToCleanString(appId),
                PSParameter.ConvertStringToCleanString(cccId),
                PSParameter.ConvertStringToCleanString(misCode)
        ]

        List<FraudReportModel> results = new ArrayList<FraudReportModel>()

        log.debug("getMatching: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("getMatching: calling the remote peoplesoft method")
            String[] fraudReportData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            results = ConvertStringArrayToFraudReports(fraudReportData)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("getMatching: search failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, "Peoplesoft Error- " + messageToThrow)
        }
        finally {
            log.debug("getMatching: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("getMatching: done")
        return results
    }

    /**
     * Inserts a FraudReport Data into the sis staging table
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param newModel The Fraud Report to insert into the sis staging table
     * @returns the sisFraudReportId of the newly inserted report
     */
    Long create(String misCode, FraudReportModel newModel) {
        log.debug("create: setting fraud report data")

        //****** Validate parameters ****
        if (newModel == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Fraud Report data cannot be null")
        }
        if (!newModel.appId || !newModel.cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Either Application Id or cccId must be provided")
        }
        if (StringUtils.isEmpty(newModel.fraudType)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "fraudType cannot be null")
        }

        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        log.debug("create: params ok; attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {

            //****** Build parameters to send to our custom Peopletools API ****
            String packageName
            String className
            String methodName

            String[] args = []
            if(newModel.sisFraudReportId) {
                packageName = "CCTC_FRAUD_REPORT_PKG"
                className = "CCTC_FraudReport"
                methodName = "getReport"
                args = [
                        PSParameter.ConvertLongToCleanString(newModel.sisFraudReportId)
                ]
                log.debug("create: calling the remote peoplesoft method to see if given reportId already exists")
                String[] existsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if(existsData.length > 0) {
                    throw new EntityConflictException("Fraud Report already exists for given id")
                }
            } else {
                packageName = "CCTC_FRAUD_REPORT_PKG"
                className = "CCTC_FraudReport"
                methodName = "searchReports"

                args = [
                        PSParameter.ConvertLongToCleanString(newModel.appId),
                        PSParameter.ConvertStringToCleanString(newModel.cccId),
                        PSParameter.ConvertStringToCleanString(newModel.misCode)
                ]
                log.debug("create: calling the remote peoplesoft method to see if given fraud report already exists")
                String[] existingData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                if (existingData.length > 0) {
                    throw new EntityConflictException("Fraud Report already exists for given cccId and appId")
                } else {
                    packageName = "CCTC_FRAUD_REPORT_PKG"
                    className = "CCTC_FraudReport"
                    methodName = "getMaxReportId"
                    args = [ ]
                    log.debug("create: calling the remote peoplesoft method to get max id")
                    String[] idData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                    if (idData.length <= 0 || !StringUtils.isNumeric(idData[0])) {
                        throw new InternalServerException(InternalServerException.Errors.sisQueryError, "could not get new id")
                    }
                    newModel.sisFraudReportId = (idData[0].toLong() + 3)
                }
            }

            packageName = "CCTC_FRAUD_REPORT_PKG"
            className = "CCTC_FraudReport"
            methodName = "setReport"

            args = this.ConvertModelToArgsArray(newModel)

            log.debug("create: calling the remote peoplesoft method to set the fraud report")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected.length != 1) {
                log.error("create: invalid length on fraud report create results")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "invalid number of rows affected. empty array")
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                log.error("create: invalid number of rows affected on fraud Report [" + rowsAffected[0] + "]")
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
                        log.error("create: setting the record failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("create: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("create: done")
        return newModel.sisFraudReportId
    }


    /**
     * Converts a string array into an FraudReport Object
     * @param results The string array to convert to an Fraud Report Data Object
     * @retuns a Fraud Report Data object with each field populated from the string array
     */
    protected List<FraudReportModel> ConvertStringArrayToFraudReports(String[] fraudReportsData) {

        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
        List<FraudReportModel> frList = []

        if (fraudReportsData && fraudReportsData.length > 0) {
            Integer i = 0
            while (i + 11 <= fraudReportsData.size()) {
                FraudReportModel data = new FraudReportModel()

                data.sisFraudReportId = fraudReportsData[i++].toLong()

                data.misCode = fraudReportsData[i++]

                data.appId = fraudReportsData[i++].toLong()

                data.cccId = fraudReportsData[i++]

                data.fraudType = fraudReportsData[i++]

                data.reportedByMisCode = fraudReportsData[i++]

                data.reportedBySource = fraudReportsData[i++]

                data.tstmpSubmit = (fraudReportsData[i] == null || fraudReportsData[i].trim().isEmpty() ? null : LocalDateTime.parse(fraudReportsData[i], dateTimeFormat))
                i++

                data.sisProcessedFlag = fraudReportsData[i++]

                data.tstmpSISProcessed = (fraudReportsData[i] == null || fraudReportsData[i].trim().isEmpty() ? null : LocalDateTime.parse(fraudReportsData[i], dateTimeFormat))
                i++
                data.sisProcessedNotes = fraudReportsData[i++]

                frList.push(data)
            }
        }
        return frList
    }

    protected String[] ConvertModelToArgsArray(FraudReportModel data) {
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
        String[] map = [
                PSParameter.ConvertLongToCleanString(data.sisFraudReportId),
                PSParameter.ConvertStringToCleanString(data.misCode),
                PSParameter.ConvertLongToCleanString(data.appId),
                PSParameter.ConvertStringToCleanString(data.cccId),
                PSParameter.ConvertStringToCleanString(data.fraudType),
                PSParameter.ConvertStringToCleanString(data.reportedByMisCode),
                PSParameter.ConvertStringToCleanString(data.reportedBySource),
                PSParameter.ConvertDateTimeToCleanString(data.tstmpSubmit, dateTimeFormat)
        ]

        return map
    }
}