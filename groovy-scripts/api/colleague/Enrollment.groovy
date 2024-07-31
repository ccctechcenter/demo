package api.colleague

import api.colleague.util.ColleagueUtils
import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceAsync
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.Enrollment as CAEnrollment
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

import java.time.LocalDate
import java.util.concurrent.CompletableFuture

import static api.colleague.util.ColleagueUtils.fromLocalDate
import static api.colleague.util.ColleagueUtils.quoteString

class Enrollment {

    DmiDataService dmiDataService
    DmiDataServiceAsync dmiDataServiceAsync
    DataUtils dataUtils

    static secStudentsColumn = ["SEC.STUDENTS"]

    static secSynonymColumn = ["SEC.SYNONYM"]

    static crsCIdColumn = ["CRS.STANDARD.ARTICULATION.NO"]

    static studentAcadCredColumns = ["STC.PERSON.ID", "STC.TITLE", "STC.COURSE", "STC.STATUS", "STC.STATUS.DATE", "STC.CRED",
                                     "STC.VERIFIED.GRADE", "STC.VERIFIED.GRADE.DATE", "STC.COURSE.NAME", "STC.TERM",
                                     "STC.STUDENT.COURSE.SEC"]

    static studentCourseSecColumns = ["SCS.COURSE.SECTION", "SCS.PASS.AUDIT", "SCS.LAST.ATTEND.DATE"]

    static gradesColumns = ["GRD.GRADE"]

    static altIdColumns = ["PERSON.ALT.IDS", "PERSON.ALT.ID.TYPES"]


    /**
     * Initialize
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiDataService = services.get(DmiDataService.class)
        def dmiService = services.get(DmiService.class)
        def dmiCTXService = services.get(DmiCTXService.class)
        def cache = services.get(Cache.class)

        this.dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)
        this.dataUtils = new DataUtils(misCode, environment, dmiService, dmiCTXService, dmiDataService, cache)

        ColleagueUtils.keepAlive(dmiService)
    }


    /**
     * Get Enrollments by Section
     */
    List<CAEnrollment> getSection(String misCode, String sisTermId, String sisSectionId) {
        assert misCode != null
        assert sisTermId != null
        assert sisSectionId != null

        if (!dataUtils.validateTerm(sisTermId))
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")

        // get pointers to STUDENT.COURSE.SEC from COURSE.SECTIONS
        def query = "SEC.TERM = " + quoteString(sisTermId) + " AND SEC.SYNONYM = " + quoteString(sisSectionId)
        def sections = dmiDataService.batchSelect("ST", "COURSE.SECTIONS", secStudentsColumn, query)

        if (sections.size() > 1)
            throw new InternalServerException("Multiple sections found when expecting one")

        if (sections.size() == 0)
            throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")

        def scsIds = (String[]) sections[0].values["SEC.STUDENTS"]

        if (scsIds) {
            // get pointers to STUDENT.ACAD.CRED from STUDENT.COURSE.SEC
            def stcIds = dmiDataService.selectKeys("STUDENT.COURSE.SEC", "SAVING UNIQUE SCS.STUDENT.ACAD.CRED", scsIds.toList())

            if (stcIds) {
                def enrollments = parse(misCode, stcIds.toList())

                // bad pointers can lead to bad data, so ensure we only return records that match the sisTermId and sisSectionId
                return enrollments.findAll { it.sisTermId == sisTermId && it.sisSectionId == sisSectionId }
            }
        }

        return []
    }


    /**
     * Get Enrollments by Student
     */
    List<CAEnrollment> getStudent(String misCode, String sisTermId, String cccId, String sisSectionId) {
        assert misCode != null
        assert cccId != null

        if (sisTermId && !dataUtils.validateTerm(sisTermId))
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")
        if (sisTermId && sisSectionId && !dataUtils.validateSection(sisTermId, sisSectionId))
            throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")

        def ids = dataUtils.getColleagueIds(cccId)

        if (ids.size() == 0)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found")
        if (ids.size() > 1)
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple students found with CCC ID")

        def query = "STC.PERSON.ID = " + quoteString(ids.first())
        if (sisTermId) query += " AND STC.TERM = " + quoteString(sisTermId)

        // restrict to only enrollments with STUDENT.COURSE.SEC records
        query = query + " AND STC.STUDENT.COURSE.SEC NE ''"

        def stcIds = dmiDataService.selectKeys("STUDENT.ACAD.CRED", query)

        def enrollments = parse(misCode, stcIds.toList())

        if (sisSectionId)
            return enrollments.findAll { it.sisSectionId == sisSectionId }

        return enrollments
    }


    /**
     * Produce Enrollments from a list of STUDENT.ACAD.CRED record IDs
     */
    List<CAEnrollment> parse(String misCode, List<String> stcIds) {

        def result = []

        // read STUDENT.ACAD.CRED.STATUSES (valcode), GRADES in background. Both use caching.
        def fVc = dmiDataServiceAsync.valcodeAsyncCached("ST", "STUDENT.ACAD.CRED.STATUSES")
        def fGrades = dmiDataServiceAsync.batchSelectAsyncCached("ST", "GRADES", gradesColumns, null)

        // read STUDENT.ACAD.CRED
        def studentAcadCred = readStudentAcadCred(stcIds)

        // read COURSES in background for C-ID translation
        def courseIds = DataUtils.getPointers(studentAcadCred, "STC.COURSE")
        def fCIds = readCIdsAsync(courseIds)

        // read PERSON in background for CCC ID translation
        def personIds = DataUtils.getPointers(studentAcadCred, "STC.PERSON.ID")
        def fCccIds = readCccIdsAsync(personIds)

        // read STUDENT.COURSE.SEC
        def scsIds = DataUtils.getPointers(studentAcadCred, "STC.STUDENT.COURSE.SEC")
        def studentCourseSec = readStudentCourseSec(scsIds)

        // read COURSE.SECTIONS
        def secIds = DataUtils.getPointers(studentCourseSec.values(), "SCS.COURSE.SECTION")
        def secSynonyms = readCourseSections(secIds)

        // complete async reads
        def cccIds = fCccIds.get()
        def crsCIds = fCIds.get()
        def enrlStatuses = fVc.get()?.asMap()
        def grades = fGrades.get().collectEntries { [(it.key) : (String) it.values["GRD.GRADE"]]}

        for(def stc : studentAcadCred) {

            // STUDENT.COURSE.SEC values - pass, audit, last attend date
            def scsId = (String) stc.values["STC.STUDENT.COURSE.SEC"]
            def scsRecord = scsId ? studentCourseSec.get(scsId) : null
            def lastAttendDate = fromLocalDate ((LocalDate) scsRecord?.values?.get("SCS.LAST.ATTEND.DATE"))
            def scsPassAudit = (String) scsRecord?.values?.get("SCS.PASS.AUDIT")
            def audit = (scsPassAudit == "A")
            def passNoPass = (scsPassAudit == "P")

            // COURSE.SECTIONS values - sisSectionID (synonym)
            def secId = (String) scsRecord?.values?.get("SCS.COURSE.SECTION")
            def sisSectionId = secId ? secSynonyms[secId] : null

            // PERSON values - CCC ID
            def personId = (String) stc.values["STC.PERSON.ID"]
            def cccid = cccIds.get(personId)

            // COURSES values - C-ID
            def crsId = (String) stc.values["STC.COURSE"]
            def c_id = crsId ? crsCIds[crsId] : null


            // first STC.STATUS, STC.STATUS.DATE determines enrollment status and date
            def curStatus = ((String[]) stc.values["STC.STATUS"])?.first()
            def curStatusDate = ((LocalDate[]) stc.values["STC.STATUS.DATE"])?.first()
            def enrollmentStatusDate = fromLocalDate(curStatusDate)
            def curStatusAction1 = curStatus ? enrlStatuses?.get(curStatus)?.action1 : null

            // action code #1 of STUDENT.ACAD.CRED.STATUSES determines status. default to enrolled.
            def enrollmentStatus
            switch (curStatusAction1) {
                case ["3", "4", "5"]:
                    enrollmentStatus = EnrollmentStatus.Dropped
                    break
                case "6":
                    enrollmentStatus = EnrollmentStatus.Cancelled
                    break
                default:
                    enrollmentStatus = EnrollmentStatus.Enrolled
            }

            // use "verified" grade and grade date
            def gradeId = (String) stc.values["STC.VERIFIED.GRADE"]
            def grade = gradeId ? (String) grades[gradeId] : null
            def gradeDate = grade ? fromLocalDate((LocalDate) stc.values["STC.VERIFIED.GRADE.DATE"]) : null

            def enrl = new CAEnrollment.Builder()
                    .misCode(misCode)
                    .cccid(cccid)
                    .sisPersonId((String) stc.values["STC.PERSON.ID"])
                    .sisTermId((String) stc.values["STC.TERM"])
                    .sisSectionId(sisSectionId)
                    .enrollmentStatus(enrollmentStatus)
                    .enrollmentStatusDate(enrollmentStatusDate)
                    .units(((BigDecimal) stc.values["STC.CRED"])?.toFloat())
                    .passNoPass(passNoPass)
                    .audit(audit)
                    .grade(grade)
                    .gradeDate(gradeDate)
                    .lastDateAttended(lastAttendDate)
                    .sisCourseId((String) stc.values["STC.COURSE.NAME"])
                    .c_id(c_id)
                    .title((String) stc.values["STC.TITLE"])
                    .build()

            result << enrl
        }

        return result
    }

    /**
     * Read C-ID data asynchronously
     */
    CompletableFuture<Map<String, String>> readCIdsAsync(List<String> courseIds) {
        return CompletableFuture.supplyAsync {
            if (courseIds) {
                def c = dmiDataService.batchKeys("ST", "COURSES", crsCIdColumn, courseIds)
                return c.collectEntries { [(it.key) : (String) it.values["CRS.STANDARD.ARTICULATION.NO"]] }
            }

            return [:]
        }
    }

    /**
     * Read CCC ID data asynchronously
     */
    CompletableFuture<Map<String, String>> readCccIdsAsync(List<String> personIds) {
        return CompletableFuture.supplyAsync {
            if (personIds) {
                def p = dmiDataService.batchKeys("CORE", "PERSON", altIdColumns, personIds)
                return dataUtils.getCccIds(p)
            }

            return [:]
        }
    }


    /**
     * Read STUDENT.COURSE.SEC data, return as a Map
     */
    Map<String, ColleagueData> readStudentCourseSec(List<String> studentCourseSecIds) {
        if (studentCourseSecIds) {
            def s = dmiDataService.batchKeys("ST", "STUDENT.COURSE.SEC", studentCourseSecColumns, studentCourseSecIds)
            return s.collectEntries { [(it.key) : it] }
        }

        return [:]
    }


    /**
     * Read COURSE.SECTIONS data, return as a Map
     */
    Map<String, String> readCourseSections(List<String> sectionIds) {
        if (sectionIds) {
            def s = dmiDataService.batchKeys("ST", "COURSE.SECTIONS", secSynonymColumn, sectionIds)
            return s.collectEntries { [(it.key) : (String) it.values["SEC.SYNONYM"]]}
        }

        return [:]
    }


    /**
     * Read STUDENT.ACAD.CRED data
     */
    def readStudentAcadCred(List<String> studentAcadCredIds) {
        return dmiDataService.batchKeys("ST", "STUDENT.ACAD.CRED", studentAcadCredColumns, studentAcadCredIds)
    }


    /**
     * Get Prerequisite status for a potential enrollment
     */
    PrerequisiteStatus getPrereqStatus(String misCode, String sisCourseId, Date start, String cccId) {
        assert misCode != null
        assert sisCourseId != null
        assert cccId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Enroll a student in a course section
     */
    CAEnrollment post(String misCode, CAEnrollment enrollment) {
        assert misCode != null
        assert enrollment != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Drop a student from a course section. For testing purposes only, not used by Course Exchange.
     */
    CAEnrollment put(String misCode, String cccId, String sisSectionId, String sisTermId, CAEnrollment enrollment) {
        assert misCode != null
        assert cccId != null
        assert sisSectionId != null
        assert sisTermId != null
        assert enrollment != null

        throw new InternalServerException("Unsupported")
    }

}