package api.colleague.transcript.base

import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.util.ColleagueUtils
import api.colleague.util.DmiDataServiceCached
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.core.coremain.v1_14.SessionTypeType
import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.AcademicRecordType
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicRecordTypeImpl
import com.xap.ccctran.impl.AcademicRecordCCCExtensionsImpl
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.service.DmiDataService

import java.util.concurrent.CompletableFuture

@CompileStatic
abstract class AcademicRecordService {

    // DMI services
    TranscriptDmiServices transcriptDmiServices
    DmiEntityService dmiEntityService
    DmiDataService dmiDataService
    DmiDataServiceCached dmiDataServiceCached

    // transcript services
    AcademicAwardService academicAwardService
    GradeSchemeService gradeSchemeService
    AcademicSessionService academicSessionService
    AcademicSummaryService academicSummaryService

    // configuration
    List<String> gradeSchemes
    SessionTypeType defaultSessionType

    AcademicRecordService(AcademicRecordService a) {
        this(a.transcriptDmiServices, a.academicSummaryService, a.academicAwardService, a.gradeSchemeService, a.academicSessionService)
    }

    AcademicRecordService(TranscriptDmiServices transcriptDmiServices,
                          AcademicSummaryService academicSummaryService = null,
                          AcademicAwardService academicAwardService = null,
                          GradeSchemeService gradeSchemeService = null,
                          AcademicSessionService academicSessionService = null) {

        this.transcriptDmiServices = transcriptDmiServices

        List<String> gradeSchemeIds = ColleagueUtils.getColleaguePropertyAsList(
                transcriptDmiServices.environment, transcriptDmiServices.misCode, "transcript.grade.schemes")

        String defaultTermType = ColleagueUtils.getColleagueProperty(
                transcriptDmiServices.environment, transcriptDmiServices.misCode, "default.term.type")

        if (defaultTermType) {
            this.defaultSessionType = SessionTypeType.values().find { it.name() == defaultTermType.toUpperCase() }

            if (!this.defaultSessionType)
                throw new InternalServerException("Configuration value for default.term.type is invalid - it should be Quarter or Semester")
        }

        this.gradeSchemes = gradeSchemeIds


        if (academicSummaryService == null) academicSummaryService = new AcademicSummaryService() {}
        if (academicAwardService == null) academicAwardService = new AcademicAwardService(transcriptDmiServices) {}
        if (gradeSchemeService == null) gradeSchemeService = new GradeSchemeService(transcriptDmiServices) {}
        if (academicSessionService == null) academicSessionService =
                new AcademicSessionService(transcriptDmiServices, this.defaultSessionType, academicSummaryService) {}

        this.dmiDataService = transcriptDmiServices.dmiDataService
        this.dmiDataServiceCached = transcriptDmiServices.dmiDataServiceCached
        this.dmiEntityService = transcriptDmiServices.dmiEntityService

        this.academicAwardService = academicAwardService
        this.gradeSchemeService = gradeSchemeService
        this.academicSessionService = academicSessionService
        this.academicSummaryService = academicSummaryService

    }

    /**
     * Get Academic Record for a student
     */
    AcademicRecordType getAcademicRecord(String personId) {
        def academicRecord = new AcademicRecordTypeImpl()

        // read academic awards and courses asynchronously
        def async1 = getAcademicAwardsAsync(personId, academicRecord)
        def async2 = getAcademicSessionsAsync(personId, academicRecord)

        // add grade scheme entries
        getGradeScheme(academicRecord)

        // wait for async tasks to end
        async1.get()
        async2.get()

        // add academic summary. must be done after academic sessions have finished loading
        getAcademicSummary(academicRecord)

        return academicRecord
    }

    protected CompletableFuture getAcademicAwardsAsync(String personId, AcademicRecordType academicRecord) {
        return CompletableFuture.runAsync {
            def awards = academicAwardService.getAcademicAwards(personId)
            if (awards) academicRecord.academicAwards.addAll(awards)
        }
    }

    protected CompletableFuture getAcademicSessionsAsync(String personId, AcademicRecordType academicRecord) {
        return CompletableFuture.runAsync {
            def sessions = academicSessionService.getAcademicSessions(personId)
            if (sessions.academicSessions)
                academicRecord.academicSessions.addAll(sessions.academicSessions.values().sort { it.academicSessionDetail?.sessionBeginDate })

            if (sessions.otherCourses)
                academicRecord.courses.addAll(sessions.otherCourses)
        }
    }

    protected void getGradeScheme(AcademicRecordType academicRecord) {
        def gradeSchemeEntries = gradeSchemeService.getGradeScheme(gradeSchemes)

        if (gradeSchemeEntries) {
            academicRecord.userDefinedExtensions = new UserDefinedExtensionsTypeImpl()
            academicRecord.userDefinedExtensions.academicRecordCCCExtensions = new AcademicRecordCCCExtensionsImpl()
            academicRecord.userDefinedExtensions.academicRecordCCCExtensions.gradeSchemeEntries.addAll(gradeSchemeEntries)
        }
    }

    protected void getAcademicSummary(AcademicRecordType academicRecord) {
        academicRecord.academicSummaries << academicSummaryService.fromAcademicRecord(academicRecord)
    }
}
