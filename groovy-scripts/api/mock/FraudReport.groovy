package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.FraudReportDB
import org.apache.commons.lang.StringUtils

/**
 * <h1>Fraud Report Mock Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the FraudReport In-Memory DB to mock fraudreport sis functionality:</p>
 *     <ol>
 *         <li>Storing and populating Fraud Reports into the in-memory db</li>
 *         <li>Retrieving Fraud Reports by Id, and/or by appId/cccId, from the in-memory db</li>
 *         <li>Removing Fraud Reports by Id from the in-memory db</li>
 *     <ol>
 * </summary>
 *
 * @version 4.4.0
 *
 */
class FraudReport {

    //****** Populated by injection in Groovy Service Implementation ****
    FraudReportDB fraudReportDB

    /**
     * Gets FraudReport Data from the in-memory db matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     * @returns A matching Fraud Report
     */
    com.ccctc.adaptor.model.fraud.FraudReport getById(String misCode, Long sisFraudReportId) {
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        return fraudReportDB.getById(misCode, sisFraudReportId)
    }

    /**
     * Removes FraudReport Data from the in-memory db matching strictly on the sisFraudReportId
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param sisFraudReportId The internal sis Fraud Report Id generated when originally populating the data into the in memory db
     */
    void deleteById(String misCode, Long sisFraudReportId) {
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        fraudReportDB.deleteById(misCode, sisFraudReportId)
    }

    /**
     * Removes FraudReport Data from the in-memory db matching strictly on the appId and reportedByMisCode fields
     * @param FraudReport The report to remove.
     */
    void deleteFraudReport( com.ccctc.adaptor.model.fraud.FraudReport report) {
        if (!report.misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (!report.appId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "appId cannot be null or blank")
        }
        if (!report.reportedByMisCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "reportedByMisCode cannot be null or blank")
        }
        Map<String,Object> deleteFilter = new HashMap<String, Object>();
        deleteFilter.put("misCode", report.misCode);
        deleteFilter.put("appId", report.appId);
        deleteFilter.put("reportedByMisCode", report.reportedByMisCode);
        fraudReportDB.deleteFraudReport(deleteFilter);
    }

    /**
     * Retrieves FraudReport Data from the in-memory db matching on either appId or cccId or both
     * @param misCode The College Code used for grabbing college specific settings from the environment.
     * @param appId The Fraudulent AppId to match on
     * @param appId The Fraudulent cccId to match on
     * @returns A List of matching Fraud Reports
     */
    List<com.ccctc.adaptor.model.fraud.FraudReport> getMatching(String misCode, Long appId, String cccId) {
        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }
        if (appId == 0l && StringUtils.isEmpty(cccId)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "Either AppId or cccId must be provided")
        }
        return fraudReportDB.getMatching(misCode, appId, cccId)
    }

    /**
     * Inserts a FraudReport Data into the in-memory db
     * @param misCode The College Code used for grabbing college specific settings from the environment. Not Used for Mock
     * @param newModel The Fraud Report to insert into the in memory db
     * @returns the sisFraudReportId of the newly inserted report
     */
    Long create(String misCode, com.ccctc.adaptor.model.fraud.FraudReport newModel) {
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "misCode cannot be null or blank")
        }

        if (!newModel) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "new Model cannot be null or blank")
        }

        if(!newModel.getSisFraudReportId() || newModel.getSisFraudReportId() == 0l) {
            Long newId = fraudReportDB.getMaxId() + 3;
            newModel.setSisFraudReportId(newId)
        }
        com.ccctc.adaptor.model.fraud.FraudReport result = fraudReportDB.add(newModel)
        return result.getSisFraudReportId()
    }
}