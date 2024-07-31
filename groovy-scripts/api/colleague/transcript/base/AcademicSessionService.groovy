package api.colleague.transcript.base

import api.colleague.transcript.util.TranscriptUtils
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import api.colleague.transcript.model.AcademicSessionsResults
import api.colleague.transcript.model.StudentAcadCredRecord
import api.colleague.transcript.model.StudentTermsRecord
import api.colleague.transcript.model.TermsRecord
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.transcript.model.TranscriptException
import api.colleague.util.ColleagueUtils
import api.colleague.util.DmiDataServiceCached
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.core.coremain.v1_14.AcademicSessionDetailType
import com.ccctc.core.coremain.v1_14.CourseAcademicGradeStatusCodeType
import com.ccctc.core.coremain.v1_14.CourseGPAApplicabilityCodeType
import com.ccctc.core.coremain.v1_14.CourseRepeatCodeType
import com.ccctc.core.coremain.v1_14.SessionTypeType
import com.ccctc.core.coremain.v1_14.impl.AcademicSessionDetailTypeImpl
import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.AcademicSessionType
import com.ccctc.sector.academicrecord.v1_9.CourseType
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicSessionTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.CourseTypeImpl
import com.xap.ccctran.CourseCCCExtensions
import com.xap.ccctran.impl.CourseCCCExtensionsImpl
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.util.StringUtils

import java.util.concurrent.CompletableFuture

@CompileStatic
abstract class AcademicSessionService {

    // DMI Services
    final TranscriptDmiServices transcriptDmiServices
    final DmiEntityService dmiEntityService
    final DmiDataService dmiDataService
    final DmiDataServiceCached dmiDataServiceCached

    // Data services
    AcademicSummaryService academicSummaryService

    // Configuration
    SessionTypeType defaultSessionType

    // entity classes for reading data
    Class<? extends ColleagueRecord> studentAcadCredEntity = StudentAcadCredRecord.class
    Class<? extends ColleagueRecord> termsEntity = TermsRecord.class
    Class<? extends ColleagueRecord> studentTermsEntity = StudentTermsRecord.class

    // valcodes / elf translations (loaded asynchronously)
    CompletableFuture<Map<String, Valcode.Entry>> fStudentAcadCredStatuses
    CompletableFuture<Map<String, ElfTranslateTable.Entry>> fCastSX04

    AcademicSessionService(AcademicSessionService s) {
        this(s.transcriptDmiServices, s.defaultSessionType, s.academicSummaryService)
    }

    AcademicSessionService(TranscriptDmiServices transcriptDmiServices, SessionTypeType defaultSessionType,
                           AcademicSummaryService academicSummaryService = null) {

        this.transcriptDmiServices = transcriptDmiServices
        this.defaultSessionType = defaultSessionType

        if (academicSummaryService == null) academicSummaryService = new AcademicSummaryService() {}

        this.academicSummaryService = academicSummaryService

        this.dmiDataService = transcriptDmiServices.dmiDataService
        this.dmiEntityService = transcriptDmiServices.dmiEntityService
        this.dmiDataServiceCached = transcriptDmiServices.dmiDataServiceCached

        // load STUDENT.ACAD.CRED.STATUSES asynchronously
        this.fStudentAcadCredStatuses = CompletableFuture.supplyAsync {
            def data = dmiDataServiceCached.valcode("ST", "STUDENT.ACAD.CRED.STATUSES")
            if (data != null) return data.asMap()
            return new HashMap<String, Valcode.Entry>()
        }

        // load SX04 asynchronously
        this.fCastSX04 = CompletableFuture.supplyAsync {
            def data = dmiDataServiceCached.elfTranslationTable("CAST.SX04")
            if (data != null) return data.asMap()
            return new HashMap<String, ElfTranslateTable.Entry>()
        }
    }

    AcademicSessionsResults getAcademicSessions(String personId) {

        def result = new AcademicSessionsResults()

        // get courses asynchronously
        def asyncJob = getCoursesAsync(personId, result)
        getStudentTerms(personId, result)
        asyncJob.get()
        getAcademicSessionDetails(result)

        // calculate summaries for each session
        for (def a : result.academicSessions.values()) {
            a.academicSummaries << academicSummaryService.fromAcademicSession(a)
        }

        return result
    }

    protected AcademicSessionType getOrCreateAcademicSession(String key, Map<String, AcademicSessionType> sessions) {
        def session = sessions.get(key)
        if (session == null) {
            session = new AcademicSessionTypeImpl()
            sessions.put(key, session)
        }

        return session
    }


    //
    //
    // STUDENT.ACAD.CRED
    //
    //

    protected CompletableFuture getCoursesAsync(String personId, AcademicSessionsResults academicSessionsResults) {
        return CompletableFuture.runAsync {
            def data = readStudentAcadCred(personId, studentAcadCredEntity)
            data.each { parseStudentAcadCred(personId, academicSessionsResults, it) }
        }
    }


    protected <T extends ColleagueRecord> List<T> readStudentAcadCred(String personId, Class<T> type) {
        return dmiEntityService.readForEntity("PERSON.ST", "@ID = "
                + ColleagueUtils.quoteString(personId) + " SAVING PST.STUDENT.ACAD.CRED", null, type)
    }


    protected CourseType parseStudentAcadCred(String personId, AcademicSessionsResults academicSessionsResults, ColleagueRecord data) {
        if (!data instanceof StudentAcadCredRecord)
            throw new TranscriptException("Default implementation of parseStudentAcadCred expecting StudentAcadCredRecord, found " + data.class.name)

        def stc = (StudentAcadCredRecord) data
        def course = new CourseTypeImpl()
        def studentAcadCredStatuses = fStudentAcadCredStatuses.get()
        def castSX04 = fCastSX04.get()

        course.courseCreditUnits = defaultSessionType?.value()

        // units / gpa
        course.courseCreditValue = stc.stcCred
        course.courseCreditEarned = stc.stcAltcumContribCmplCred
        if (stc.stcAltcumContribGpaCred != null) {
            course.courseQualityPointsEarned = stc.stcAltcumContribGradePts
            if (stc.stcAltcumContribGradePts > 0)
                course.courseGPAApplicabilityCode = CourseGPAApplicabilityCodeType.APPLICABLE
            else
                course.courseGPAApplicabilityCode = CourseGPAApplicabilityCodeType.NOT_APPLICABLE
        }

        // course name is delimited with a dash in Colleague ie ENGL-100
        String[] courseNameSplit = stc.stcCourseName?.split("-")
        course.courseSubjectAbbreviation = courseNameSplit ? courseNameSplit[0] : null
        course.courseNumber = courseNameSplit && courseNameSplit.length > 1 ? courseNameSplit[1] : null

        course.courseSectionNumber = stc.stcSectionNo
        course.agencyCourseID = stc.stcCourse?.crsStandardArticulationNo

        course.courseTitle = stc.stcTitle
        course.courseBeginDate = ColleagueUtils.fromLocalDate(stc.stcStartDate)
        course.courseEndDate = ColleagueUtils.fromLocalDate(stc.stcEndDate)

        if (stc.stcPrintedComments) {
            course.noteMessages.addAll(StringUtils.split(stc.stcPrintedComments, StringUtils.VM))
        }

        // add and drop date
        if (stc.stcStatusesAssoc) {
            def action1 = studentAcadCredStatuses?.get(stc.stcStatusesAssoc[0].stcStatus)?.action1
            def date1 = ColleagueUtils.fromLocalDate(stc.stcStatusesAssoc[0].stcStatusDate)

            // Action code #1:
            // 1 and 2 mean the student is enrolled / did not drop
            // 3 and 4 mean the student dropped
            if (action1 == "1" || action1 == "2") {
                course.courseAddDate = date1
            } else if (action1 == "3" || action1 == "4") {
                course.courseDropDate = date1

                // if current status is dropped, search rest of statuses for an add date
                for (int x = 1; x < stc.stcStatusesAssoc.size(); x++) {
                    def action = studentAcadCredStatuses?.get(stc.stcStatusesAssoc[x].stcStatus)?.action1
                    def date = ColleagueUtils.fromLocalDate(stc.stcStatusesAssoc[x].stcStatusDate)

                    if (action == "1" || action == "2") {
                        course.courseAddDate = date
                        break
                    }
                }
            }
        }


        // grade information
        if (stc.stcVerifiedGrade != null && stc.stcVerifiedGrade.grdGrade != null) {
            course.courseAcademicGrade = stc.stcVerifiedGrade.grdGrade

            // get grade scale code from SX04 translation
            def sx04Grade = castSX04.get(stc.stcVerifiedGrade.grdGrade)?.newCode
            course.courseAcademicGradeScaleCode = TranscriptUtils.getGradeCodeFromSX04(sx04Grade)
        }

        // audit, pass/no pass
        if (stc.stcStudentCourseSec?.scsPassAudit == "A")
            course.courseAcademicGradeStatusCode = CourseAcademicGradeStatusCodeType.AUDITED_COURSE
        else if (stc.stcStudentCourseSec?.scsPassAudit == "P") {
            // if the course was taken for pass/no pass check STC.CMPL.CRED to see if the student passed or failed
            if (stc.stcCred != null && stc.stcCred > 0 && stc.stcCmplCred != null) {
                if (stc.stcCmplCred == 0)
                    course.courseAcademicGradeStatusCode = CourseAcademicGradeStatusCodeType.PASS_FAIL_FAIL
                else
                    course.courseAcademicGradeStatusCode = CourseAcademicGradeStatusCodeType.PASS_FAIL_PASS
            }
        }

        // repeat / bracketing
        if (stc.stcReplCode)
            course.courseRepeatCode = CourseRepeatCodeType.REPEAT_NOT_COUNTED

        // user defined extensions for CCC
        if (stc.stcCred != null && stc.stcCred > 0 && stc.stcAltcumContribGpaCred == 0) {
            course.userDefinedExtensions = new UserDefinedExtensionsTypeImpl(courseCCCExtensions: new CourseCCCExtensionsImpl())
            course.userDefinedExtensions.courseCCCExtensions.attemptedUnitsCalc = CourseCCCExtensions.AttemptedUnitsCalcType.BRACKETED_NOT_COUNTED
        }

        // cip code
        def cip = stc.stcCourse?.crsCip?.replaceAll("[^0-9]", "")
        if (cip != null && cip.length() == 6)
            course.courseCIPCode = cip[0..1] + "." + cip[2..5]


        // finally, add the course to the result
        if (stc.stcTerm == null) {
            academicSessionsResults.otherCourses << course
        } else {
            def session = getOrCreateAcademicSession(stc.stcTerm, academicSessionsResults.academicSessions)
            session.getCourses() << course
        }

        return course
    }


    //
    //
    // STUDENT.TERMS (for term comments)
    //
    //

    protected void getStudentTerms(String personId, AcademicSessionsResults academicSessionsResults) {
        def data = readStudentTerms(personId, studentTermsEntity)
        data.each { parseStudentTerms(personId, academicSessionsResults, it) }
    }


    protected <T extends ColleagueRecord> List<T> readStudentTerms(String personId, Class<T> type) {
        def acadLevels = dmiDataService.selectKeys("STUDENTS", "@ID = " + ColleagueUtils.quoteString(personId) + " SAVING STU.ACAD.LEVELS")
        def termIds = dmiDataService.selectKeys("STUDENTS", "@ID = " + ColleagueUtils.quoteString(personId) + " SAVING STU.TERMS")

        if (acadLevels && termIds) {
            List<String> keys = []

            // put together all possible keys in the form STUDENT*TERM*LEVEL
            // we could do this with a wildcard select (WITH @ID = 'personId...') but that is MUCH more inefficient
            acadLevels.each { a -> termIds.each { t -> keys << personId + "*" + t + "*" + a } }

            return dmiEntityService.readForEntity("STUDENT.TERMS", "STTR.PRINTED.COMMENTS NE ''", keys, type)
        }

        return []
    }

    protected AcademicSessionType parseStudentTerms(String personId, AcademicSessionsResults academicSessionsResults, ColleagueRecord data) {
        if (!data instanceof StudentTermsRecord)
            throw new TranscriptException("Default implementation of parseStudentTerms expecting StudentTermsRecord, found " + data.class.name)

        def sttr = (StudentTermsRecord) data

        if (sttr.sttrPrintedComments) {
            def comment = sttr.sttrPrintedComments.replace(StringUtils.VM, ' ' as char)
            def termId = ColleagueUtils.getMVKeyValue(data.recordId, 2)
            def session = getOrCreateAcademicSession(termId, academicSessionsResults.academicSessions)
            session.noteMessages << comment

            return session
        }

        return null
    }


    //
    //
    // TERMS
    //
    //


    protected void getAcademicSessionDetails(AcademicSessionsResults academicSessionsResults) {
        def ids = academicSessionsResults.academicSessions.keySet()

        if (ids) {
            def data = readAcademicSessionDetails(ids, termsEntity)
            data.each { parseAcademicSessionDetails( academicSessionsResults, it) }
        }
    }

    protected <T extends ColleagueRecord> List<T> readAcademicSessionDetails(Collection<String> ids, Class<T> type) {
        return dmiEntityService.readForEntity(ids, type)
    }

    protected AcademicSessionDetailType parseAcademicSessionDetails(AcademicSessionsResults academicSessionsResults, ColleagueRecord data) {
        if (!data instanceof TermsRecord)
            throw new TranscriptException("Default implementation of parseAcademicSessionDetails expecting TermsRecord, found " + data.class.name)

        def term = (TermsRecord) data

        def session = academicSessionsResults.academicSessions.get(term.recordId)

        if (session) {
            def detail = new AcademicSessionDetailTypeImpl()

            def year = term.termStartDate?.year

            detail.sessionDesignator = ColleagueUtils.fromLocalDate(term.termStartDate)
            detail.sessionName = term.termDesc
            detail.sessionType = defaultSessionType
            detail.sessionSchoolYear = year ? (year.toString() + "-" + (year+1).toString().substring(2,4)) : null
            detail.sessionBeginDate = ColleagueUtils.fromLocalDate(term.termStartDate)
            detail.sessionEndDate = ColleagueUtils.fromLocalDate(term.termEndDate)

            session.academicSessionDetail = detail
        }
    }

}
