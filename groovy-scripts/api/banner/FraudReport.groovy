package api.banner

import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.springframework.core.env.Environment

import java.sql.Timestamp

/**
 * <h1>Fraud Report Banner Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the FraudReport Banner functionality:</p>
 *     <ol>
 *         <li>Storing and populating Fraud Reports into the Banner staging table</li>
 *         <li>Retrieving Fraud Reports by Id, and/or by appId/cccId, from the Banner staging table</li>
 *         <li>Removing Fraud Reports by Id from the Banner staging table</li>
 *     <ol>
 * </summary>
 *
 * @version CAv4.6.0
 *
 */
@Slf4j
class FraudReport {

    Environment environment
    static Map<String, MetaProperty> fraudReportProperties = com.ccctc.adaptor.model.fraud.FraudReport.metaClass.properties.collectEntries { [(it.name.toUpperCase()): it] }
    def getById( String misCode, Long sisFraudReportId ) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def fraudReportQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.fraud.fraudQuery")
            GroovyRowResult fraudReportRecord = sql.firstRow(fraudReportQuery, [misCode: misCode, sisFraudReportId: sisFraudReportId])

            if (!fraudReportRecord)
                throw new EntityNotFoundException("No Fraud Report found by given id")

            def returnFraudReport = new com.ccctc.adaptor.model.fraud.FraudReport()
            Utility.mapValues(fraudReportProperties, fraudReportRecord, returnFraudReport)

            return returnFraudReport
        } finally {
            sql.close()
        }
    }

    void deleteById( String misCode, Long sisFraudReportId ) {
        def report = getById(misCode, sisFraudReportId)
        if( !report )
            throw new EntityNotFoundException("No Fraud Report found by given id to delete")
        Sql sql = BannerConnection.getSession(environment, misCode)
        try {
            def fraudDeleteQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.fraud.deleteQuery")
            sql.call(fraudDeleteQuery, [misCode: misCode, sisFraudReportId: sisFraudReportId])
        } finally {
            sql.close()
        }
    }

    void deleteFraudReport( com.ccctc.adaptor.model.fraud.FraudReport report ) {
        def reports = getMatching(report.misCode, report.appId, report.cccId);
        if( !reports  || reports.size() < 1)
            throw new EntityNotFoundException("No Fraud Report found by given id to delete")
        Sql sql = BannerConnection.getSession(environment, report.misCode)
        try {
            def fraudDeleteQuery = MisEnvironment.getProperty(environment, report.misCode, "sqljdbc.fraud.deleteReportQuery")
            sql.execute(fraudDeleteQuery, [misCode: report.misCode, appId: report.appId, reportedByMisCode: report.reportedByMisCode])
        } finally {
            sql.close()
        }
    }

    ArrayList<com.ccctc.adaptor.model.fraud.FraudReport> getMatching(String misCode, Long appId, String cccId ) {
        Sql sql = BannerConnection.getSession(environment, misCode)
        def reports = []
        try {
            def fraudReportQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.fraud.matchingQuery")
            def rows = sql.rows(fraudReportQuery, [misCode: misCode, appId  : appId, cccId  : cccId])
            rows.each { GroovyRowResult row ->
                def returnFraudReport = new com.ccctc.adaptor.model.fraud.FraudReport()
                Utility.mapValues(fraudReportProperties, row, returnFraudReport)
                reports << returnFraudReport
            }
        } finally {
            sql.close()
        }
        return reports
    }

    def create(String misCode, com.ccctc.adaptor.model.fraud.FraudReport fraudReport) {
        ArrayList<com.ccctc.adaptor.model.fraud.FraudReport> existing = getMatching(misCode, fraudReport.appId, fraudReport.cccId)
        if(existing.size() > 0) {
            throw new EntityConflictException("Fraud Report already exists for given cccId and appId")
        }

        Sql sql = BannerConnection.getSession(environment, misCode)
        def reportId
        try {
            reportId = insertFraudReport(misCode, fraudReport, sql)
        } finally {
            sql.close()
        }
        return reportId
    }

    def insertFraudReport( String misCode, com.ccctc.adaptor.model.fraud.FraudReport fraudReport, Sql sql) {
        def retrieveReportIdQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.fraud.getReportIdQuery")
        def insertFraudReportQuery = MisEnvironment.getProperty(environment, misCode, "sqljdbc.fraud.insertQuery")
        def reportId
        try {
            def record = sql.firstRow(retrieveReportIdQuery)
            reportId = (Long)record?.reportId
            sql.execute(insertFraudReportQuery, [
                    misCode: misCode,
                    sisFraudReportId: reportId,
                    appId: fraudReport.appId,
                    cccId: fraudReport.cccId,
                    fraudType: fraudReport.fraudType,
                    reportedByMisCode: fraudReport.reportedByMisCode,
                    tstmpSubmit: fraudReport.tstmpSubmit != null ? Timestamp.valueOf(fraudReport.tstmpSubmit) : null,
                    reportSource: fraudReport.reportedBySource
            ])
        }
        catch (Exception e) {
            throw new InternalServerException("Server encountered an error: " + e.getMessage() + e.stackTrace)
        }
        return reportId
    }


}
