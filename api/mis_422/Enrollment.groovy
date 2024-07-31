/**
 * Created by Rasul on 2/18/16.
 * This class is based on Banner Student 8.6.6
 * The post() method is heavily reliant on the Banner version and any other
 *    versions of Banner Student would need to be examined for differences in
 *    the registration creation procedure.
 * Package code is taken from the following packages:
 *    BWCKREGS: 8.5.3 - 8.7.1.1 C3SC
 *    BWCKCOMS: 8.5.3[C3SC 8.7]
 *    BWCKSAMS: 8.5.2.1 [C3SC 8.6.1.3]
 *    BWSKFREG: 8.5.1.2
 *    BWVKAUTH: 8.8 C3SC
 *    SV_AUTH_ADDCODES_BP: 8.7 C3SC
 */
package api.banner


import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.EnrollmentStatus
import com.ccctc.adaptor.model.PrerequisiteStatus
import com.ccctc.adaptor.model.PrerequisiteStatusEnum
import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import org.springframework.core.env.Environment

class Enrollment {
    Environment environment


    def getSection(String misCode, String sisTermId, String sisSectionId) {

        Sql sql = getJdbcConnection(misCode)
        def results
        List grades, enrollments
        try {
            query = MisEnvironment.getProperty(environment, misCode, "banner.section.getQuery")
            if (!sql.firstRow(query, [sisTermId: sisTermId, sisSectionId: sisSectionId]))
                throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.sectionGrades.getQuery")
            grades = sql.rows(query, [sisTermId: sisTermId, sisSectionId: sisSectionId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.section.getQuery")
            enrollments = sql.rows(query, [sisTermId: sisTermId, sisSectionId: sisSectionId])
            results = buildEnrollments(misCode, enrollments, grades, sisTermId, sql)
        } finally {
            sql.close()
        }
        return results
    }

    def getStudent(String misCode, String sisTermId, String cccid, String sisSectionId) {

        Sql sql = getJdbcConnection(misCode)

        def student, results
        List grades, enrollments
        try {
            def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
            student = sql.firstRow(query, [cccid: cccid])
            if (!student)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student cccId was not found")
            if (sisSectionId) {
                query = MisEnvironment.getProperty(environment, misCode, "banner.section.getQuery")
                if (!sql.firstRow(query, [sisTermId: sisTermId, sisSectionId: sisSectionId]))
                    throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound, "Section not found")
            }
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.grades.getQuery")
            grades = sql.rows(query, [pidm: student.pidm, sisTermId: sisTermId, sisSectionId: sisSectionId])
            query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.getQuery")
            enrollments = sql.rows(query, [pidm: student.pidm, sisTermId: sisTermId, sisSectionId: sisSectionId])
            results = buildEnrollments(misCode, enrollments, grades, sisTermId, sql)
        } finally {
            sql.close()
        }
        return results
    }

    def getCid(Sql sql, def enrollment, def sisTermId, String misCode) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.course.cid.getQuery")
        def cid = sql.firstRow(query, [sisTermId: sisTermId, subject: enrollment.subject, number: enrollment.number])
        return cid?.cid
    }

    def buildEnrollments(String misCode, List enrollments, List grades, def sisTermId, Sql sql) {

        def enrollmentList = []

        enrollments.each { enrollment ->
            def current = new com.ccctc.adaptor.model.Enrollment.Builder()
            def grade = grades.find {
                it.sisSectionId == enrollment.sisSectionId &&
                        it.sisTermId == enrollment.sisTermId &&
                        it.pidm == enrollment.pidm
            }
            current.cccid(enrollment.cccid)
                    .sisPersonId(enrollment.sisPersonId)
                    .sisTermId(enrollment.sisTermId)
                    .sisSectionId(enrollment.sisSectionId)
                    .enrollmentStatus(getEnrollmentStatus(enrollment.enrollmentStatus))
                    .enrollmentStatusDate(enrollment.enrollmentStatusDate)
                    .units(enrollment.units)
                    .passNoPass(getPassNoPass(enrollment.gradingMethod, misCode))
                    .audit(MisEnvironment.checkPropertyMatch(environment, misCode, "banner.enrollment.auditStatus", enrollment.auditStatus))
                    .grade(grade?.grade)
                    .gradeDate(grade?.gradeDate)
                    .c_id(getCid(sql, enrollment, sisTermId, misCode))
                    .sisCourseId(enrollment.sisCourseId)
                    .title(enrollment.title)
                    .lastDateAttended(enrollment.lastDateAttended)
            enrollmentList << current.build()
        }
        return enrollmentList
    }

    def getPassNoPass(def gradingMethod, String misCode) {
        if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOptional", gradingMethod)) {
            return true
        } else if (MisEnvironment.checkPropertyMatch(environment, misCode, "banner.course.gradingMethod.PassNoPassOnly", gradingMethod)) {
            return true
        } else {
            return false
        }
    }

    def post(String misCode, com.ccctc.adaptor.model.Enrollment enrollment) {
        Sql sql = getJdbcConnection(misCode)
        try {
            def rsts_code = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.enrollStatus")
            createEnrollment(misCode, enrollment, rsts_code, sql)
        } finally {
            sql.close()
        }
        return getStudent(misCode, enrollment.sisTermId, enrollment.cccid, enrollment.sisSectionId)[0]
    }

    def put(String misCode, String cccid, String sisSectionId, String sisTermId, com.ccctc.adaptor.model.Enrollment enrollment) {
        Sql sql = getJdbcConnection()
        try {
            def rsts_code = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.dropStatus")
            if (enrollment.sisSectionId != sisSectionId || enrollment.sisTermId != sisTermId || enrollment.cccid != cccid) {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "You may not update the sisSectionId, sisTermId, or cccid of an enrollment")
            }
            if (enrollment.enrollmentStatus == EnrollmentStatus.Dropped) {
                createEnrollment(misCode, enrollment, rsts_code, sql)
            } else {
                throw new InvalidRequestException(InvalidRequestException.Errors.generalEnrollmentError, "Invalid enrollment status provided on update, valid status is: Dropped")
            }
        } finally {
            sql.close()
        }
        return getStudent(misCode, enrollment.sisTermId, enrollment.cccid, enrollment.sisSectionId)[0]
    }

    def getPrereqStatus(String misCode, String sisCourseId, Date start, String cccId) {
        Sql sql = getJdbcConnection(misCode)
        def courseId = sisCourseId.split('-')
        if (courseId.size() < 2) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Invalid sisCourseId provided")
        }
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        def student = sql.firstRow(query, [cccid: cccId])
        if (!student)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student cccId was not found")
        query = MisEnvironment.getProperty(environment, misCode, "banner.term.byDate.getQuery")
        def term = sql.firstRow(query, [start: start?.toTimestamp()])
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.getQuery")
        def course = sql.firstRow(query, [sisTermId: term.sisTermId, subject: courseId[0], number: courseId[1]])
        if (!course) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound, "Course does not exist")
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.course.prerequisites.getQuery")
        def prerequisites = sql.rows(query, [sisTermId: term.sisTermId, subject: courseId[0], number: courseId[1]])
        if (!prerequisites || prerequisites.size() == 0) {
            def prereqStatus = new PrerequisiteStatus.Builder()
            prereqStatus.status(PrerequisiteStatusEnum.Complete)
            return prereqStatus.build()
        }
        def failedMessage = getFullPrerequisiteMessage(prerequisites)
        query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.grades.getQuery")
        def history = sql.rows(query, [pidm: student.pidm])
        query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.transfers.getQuery")
        def transfers = sql.rows(query, [pidm: student.pidm])
        def tests = []
        if (prerequisites.find { it.test }) {
            query = MisEnvironment.getProperty(environment, misCode, "banner.student.test.getQuery")
            tests = sql.rows(query, [pidm: student.pidm])
        }
        query = MisEnvironment.getProperty(environment, misCode, "banner.enrollment.inprogress.getQuery")
        def enrollments = sql.rows(query, [pidm: student.pidm, start: start?.toTimestamp()])
        def prereqStatus = new PrerequisiteStatus.Builder()
        def result = checkPrerequisites(prerequisites, tests, history, transfers, enrollments)
        prereqStatus.status(result.status)
        prereqStatus.message(result.message ?: result.status == PrerequisiteStatusEnum.Incomplete ? failedMessage : null)
        return prereqStatus.build()
    }

// Recursive method that short circuits when a prerequisite fails.
    def checkPrerequisites(List prerequisites, List tests, List courses, List transfers, List enrollments) {
        def status = [:]
        def connector
        def currentPrereq = prerequisites?.get(0)
        if (currentPrereq.leftParen) { //When we encounter a grouping, call managePrereqList to split the grouping
            // into a seperate call/result of checkPrerequisites.
            status = managePrereqList(prerequisites, tests, courses, transfers, enrollments)
        } else {  // This is a prerequisite to be checked, this returns null if there was no prerequisite on the row.
            status = checkPrerequisite(currentPrereq, tests, courses, transfers, enrollments)
            prerequisites.remove(currentPrereq)
            if (!status && prerequisites.size()) {
                //Sometimes a prerequisite row has no check and comes at the beginning
                // of the rowset.  Have to avoid returning a null status when there are
                // more prerequisites to check at this level.
                status = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
            }
        }
        for (def i = 0; i < prerequisites.size() && !connector; i++) {
            //Sometimes the connector for this set of prerequisites
            // is not on the next row when this is a grouping.
            // We loop until we find the connector for this result.
            connector = prerequisites.get(0).connector
        }
        if (!connector) {
            //If we looped through all the remaining rows and didn't find a connector, send the status back
            // to the caller method so the caller can determine if we short-circuit or continue.
            // This also ensures we exit when we run out of prerequisites.
            return status
        }
        if (connector == 'O') {
            if (status.status == PrerequisiteStatusEnum.Complete) { //Done checking this level of pre-requisites.
                return status
            } else if (status.status == PrerequisiteStatusEnum.Pending) {
                //Need to continue checking, but recall that we did find a
                // pending result.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                if (newStatus.status == PrerequisiteStatusEnum.Complete) //A better result was found.
                    return status
                else
                    return status  //Return pending as no complete result was found.
            } else { // Keep looking for a complete or pending status.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                return newStatus ?: status  //Return an incomplete status rather than a null if the next record
                // had no prerequisite to check.
            }
        } else if (connector == 'A') {
            if (status.status == PrerequisiteStatusEnum.Complete) //Still have met all prerequisites, continue checks.
                return checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
            else if (status.status == PrerequisiteStatusEnum.Pending) {
                //Ensure a pending status is not overwritten by a Complete.
                def newStatus = checkPrerequisites(prerequisites, tests, courses, transfers, enrollments)
                if (newStatus.status != PrerequisiteStatusEnum.Incomplete) //Since one check was pending as long as the following
                // checks are not incomplete, the status is pending.
                    return status
                else
                    return newStatus
            } else
                return status  //Short circuit, one of the prerequisites was not met.
        }
    }

// Helper method for checkPrerequisites to handle parenthesis in prerequisites.
    def managePrereqList(List prerequisites, List tests, List courses, List transfers, List enrollments) {
        def subList = []
        def currPreq = prerequisites.get(0)
        prerequisites.remove(currPreq)
        currPreq.leftParen = null
        subList << currPreq
        def leftCount = 1
        def count = prerequisites.size() //Set count here as we are removing from prerequisites below.
        for (def i = 1; i <= count; i++) {
            //Slice out the prerequisites in this grouping include sub groups in the slice.
            currPreq = prerequisites.get(0)
            if (currPreq.leftParen)
                leftCount++
            if (currPreq.rightParen)
                leftCount--
            subList << currPreq
            prerequisites.remove(currPreq)
            if (leftCount == 0) //Send the slice as a new check to checkPrerequisites.
                return checkPrerequisites(subList, tests, courses, transfers, enrollments)
        }
    }

// Method that does the actual checking of a individual prerequisite.
    def checkPrerequisite(Map prerequisite, List tests, List courses, List transfers, List enrollments) {
        if (prerequisite.test) {
            for (Map row : tests) {
                if (row.test == prerequisite.test && row.score >= prerequisite.testScore) {
                    return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            return [status: PrerequisiteStatusEnum.Incomplete]
        } else if (prerequisite.preSubject) {
            for (Map course : courses) { //Check grade records.
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    if (prerequisite.score) {
                        if (prerequisite.score <= course.score)
                            return [status: PrerequisiteStatusEnum.Complete]
                    } else
                        return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            for (Map course : transfers) { //Check transfer records.
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    if (prerequisite.score) {
                        if (prerequisite.score <= course.score)
                            return [status: PrerequisiteStatusEnum.Complete]
                    } else
                        return [status: PrerequisiteStatusEnum.Complete]
                }
            }
            for (Map course : enrollments) { //Check current ungrade rolled enrollments.
                if (prerequisite.preSubject == course.subject && prerequisite.preNumber == course.number &&
                        (prerequisite.level ? prerequisite.level == course.level : true)) {
                    return [status: PrerequisiteStatusEnum.Pending, message: getPreqPendingMessage(course)]
                }
            }
            return [status: PrerequisiteStatusEnum.Incomplete]
        }
        return null  //Some rows have no prerequisite defined on them so return null.
    }

    def getPreqPendingMessage(def prerequisite) {
        return "Prerequisite pending completion of: " + prerequisite.subject + "-" + prerequisite.number + ".  "
    }

    def getFullPrerequisiteMessage(List prerequisites) {
        def prerequisiteMessage = ''
        prerequisites.each { preq ->
            prerequisiteMessage += getConnector(preq.connector) + ' ' + (preq.leftParen ?: "") + (preq.preSubject ? (preq.preSubject + '-' + preq.preNumber + ' with ' + preq.preGradeMin + ' grade minimum') :
                    (preq.testDescription + ' with ' + preq.testScore + ' score minimum')) + (preq.rightParen ?: "") + '\\n'
        }
        return prerequisiteMessage
    }

    def getConnector(String connector) {
        switch (connector) {
            case 'O': return "Or"
            case 'A': return "And"
            default: return ''
        }
    }

    def getEnrollmentStatus(String enrollmentStatus) {
        switch (enrollmentStatus) {
            case 'R':
                return EnrollmentStatus.Enrolled
            case 'D':
            case 'W':
                return EnrollmentStatus.Dropped
            default:
                return null
        }

    }

    def createEnrollment(String misCode, com.ccctc.adaptor.model.Enrollment enrollment, def rsts_code, Sql sql) {
        def query = MisEnvironment.getProperty(environment, misCode, "banner.cccid.getQuery")
        def pidmRecord = sql.firstRow(query, [cccid: enrollment.cccid])
        if (!pidmRecord)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "No such student found with the cccId provided.")
        def pidm = pidmRecord.pidm
        StringBuilder plsql = new StringBuilder("""
   DECLARE

-- Our global variables
      term                 VARCHAR2(6);  -- Passed to p_altpin
      rsts                 STVRSTS.STVRSTS_CODE%TYPE;  -- Populates p_regs arrays
      pidm                 NUMBER;       -- Populates p_regs arrays
      crn                  VARCHAR2(5);  -- Populates p_regs arrays

-- Variables passed to P_Regs
      term_in              OWA_UTIL.ident_arr;
      rsts_in              OWA_UTIL.ident_arr;
      assoc_term_in        OWA_UTIL.ident_arr;
      crn_in               OWA_UTIL.ident_arr;
      start_date_in        OWA_UTIL.ident_arr;
      end_date_in          OWA_UTIL.ident_arr;
      subj                 OWA_UTIL.ident_arr;
      crse                 OWA_UTIL.ident_arr;
      sec                  OWA_UTIL.ident_arr;
      levl                 OWA_UTIL.ident_arr;
      cred                 OWA_UTIL.ident_arr;
      gmod                 OWA_UTIL.ident_arr;
      title                bwckcoms.varchar2_tabtype;
      mesg                 OWA_UTIL.ident_arr;
      reg_btn              OWA_UTIL.ident_arr;
      regs_row             NUMBER;
      add_row              NUMBER;
      wait_row             NUMBER;


--BWCKCOMS global variables
          sftregs_rec             sftregs%ROWTYPE;
          sgbstdn_rec             sgbstdn%ROWTYPE;
          sfbetrm_rec             sfbetrm%ROWTYPE;
          ssbsect_row             ssbsect%ROWTYPE;
          sfrbtch_row             sfrbtch%ROWTYPE;
          tbrcbrq_row             tbrcbrq%ROWTYPE;
          row_count               NUMBER;
          global_pidm             NUMBER;
          genpidm                 SPRIDEN.SPRIDEN_PIDM%TYPE;
          lcur_tab                sokccur.curriculum_savedtab;
          capp_tech_error         VARCHAR2 (4);

--BWCKREGS global variables
          sgbstdn_row             sgbstdn%ROWTYPE;
          sobterm_row             sobterm%ROWTYPE;
          sfbetrm_row             sfbetrm%ROWTYPE;
          sftregs_row             sftregs%ROWTYPE;
          stvrsts_row             stvrsts%ROWTYPE;
          scbcrse_row             scbcrse%ROWTYPE;
          advr_row                soklibs.advr_rec;
          sql_error               NUMBER                               := 0;
          regs_date               DATE                                 := SYSDATE;
          sgrclsr_clas_code       stvclas.stvclas_code%TYPE;
          clas_desc               stvclas.stvclas_desc%TYPE;
          astd_prevent_reg_ind    stvastd.stvastd_prevent_reg_ind%TYPE;
          cast_prevent_reg_ind    stvcast.stvcast_prevent_reg_ind%TYPE;
          ests_prev_reg           stvests.stvests_prev_reg%TYPE;
          stst_reg_ind            stvstst.stvstst_reg_ind%TYPE;
          mhrs_min_hrs            sfrmhrs.sfrmhrs_min_hrs%TYPE;
          mhrs_max_hrs            sfrmhrs.sfrmhrs_max_hrs%TYPE;
          minh_srce               sfbetrm.sfbetrm_minh_srce_cde%TYPE;
          maxh_srce               sfbetrm.sfbetrm_maxh_srce_cde%TYPE;
          ests_eff_crse_stat      stvests.stvests_eff_crse_stat%TYPE;
          hold_passwd             VARCHAR2 (3);
          appr_error              VARCHAR2 (1);
          sftregs_wait_over       VARCHAR2 (1);
          override                VARCHAR2 (1);
          old_sftregs_row         sftregs%ROWTYPE;
          old_stvrsts_row         stvrsts%ROWTYPE;
          atmp_hrs                VARCHAR2 (1);
          scrcrse_count           NUMBER                               := 0;
          ssbsect_count           NUMBER                               := 0;
          stud_level              sorlcur.sorlcur_levl_code%TYPE;


             CURSOR overridec (
                  pidm_in   NUMBER,
                  term      VARCHAR2,
                  crn       VARCHAR2,
                  subj      VARCHAR2,
                  crse      VARCHAR2,
                  seq       VARCHAR2
               )
               IS
                  SELECT *
                    FROM sfrrovr, sfrsrpo
                   WHERE sfrsrpo_term_code = term
                     AND sfrsrpo_pidm = pidm_in
                     AND sfrsrpo_subj_code = subj
                     AND sfrsrpo_crse_numb = crse
                     AND NVL (sfrsrpo_crn, crn) = crn
                     AND sfrrovr_term_code = sfrsrpo_term_code
                     AND sfrrovr_rovr_code = sfrsrpo_rovr_code;

-- BWCKSAMS global variables
         use_man_control         VARCHAR2 (1);
         internal_code           gtvsdax.gtvsdax_internal_code%TYPE;
         gtvsdax_group           gtvsdax.gtvsdax_internal_code_group%TYPE;

         CURSOR chk_sfrbtch (
            term      stvterm.stvterm_code%TYPE,
            pidm_in   spriden.spriden_pidm%TYPE
         )
         IS
            SELECT 'X'
              FROM sfrbtch
             WHERE sfrbtch_term_code = term
               AND sfrbtch_pidm = pidm_in;

-- BWSKFREG global variables
         stvterm_rec    stvterm%ROWTYPE;
         sorrtrm_rec    sorrtrm%ROWTYPE;

-- BWCKREGS.f_stuhld rewritten
         FUNCTION f_stuhld (
              sql_err        OUT   NUMBER,
              pidm_in              NUMBER DEFAULT NULL,
              regs_date_in         DATE DEFAULT NULL,
              hold_serv            CHAR DEFAULT NULL,
              hold_pass            CHAR DEFAULT NULL
           )
              RETURN BOOLEAN
           IS
              hold_ind   VARCHAR2 (1);

              CURSOR hold_ind_c (pidm_in      spriden.spriden_pidm%TYPE,
                                 regs_date_in DATE)
              IS
              SELECT DISTINCT 'Y'
                FROM stvhldd
               WHERE stvhldd_reg_hold_ind = 'Y'
                 AND stvhldd_code IN (SELECT sprhold_hldd_code
                                        FROM sprhold
                                       WHERE (
                                                    TRUNC (regs_date_in) >=
                                                             TRUNC (sprhold_from_date)
                                                AND TRUNC (regs_date_in) <
                                                               TRUNC (sprhold_to_date)
                                             )
                                         AND sprhold_pidm = pidm_in);

           BEGIN

--               IF PIDM_IN IS NOT NULL THEN
--                  GENPIDM := PIDM_IN;
--               END IF;
              IF regs_date_in IS NOT NULL
              THEN
                 regs_date := regs_date_in;
              END IF;

              IF hold_serv IS NOT NULL
              THEN
                 sobterm_row.sobterm_hold_severity := hold_serv;
              END IF;

              IF hold_pass IS NOT NULL
              THEN
                 sobterm_row.sobterm_hold := hold_pass;
              END IF;

              OPEN hold_ind_c (genpidm, regs_date);
              FETCH hold_ind_c INTO hold_ind;
              IF hold_ind_c%notfound
              THEN
                  CLOSE hold_ind_c;
                  RETURN FALSE;
              END IF;
              CLOSE hold_ind_c;

              IF sobterm_row.sobterm_hold_severity = 'F'
              THEN
                 IF    (hold_passwd IS NULL)
                    OR (hold_passwd <> NVL (sobterm_row.sobterm_hold, 'OVR'))
                 THEN
                    sql_err := -20504;
                    RETURN TRUE;
                 END IF;
              END IF;

              RETURN FALSE;
           END f_stuhld;

-- BWCKREGS.f_validenrl copy and paste.
           FUNCTION f_validenrl (
              sql_err   OUT      NUMBER,
              term_in   IN       sftregs.sftregs_term_code%TYPE,
              ests               sfbetrm.sfbetrm_ests_code%TYPE DEFAULT NULL
           )
              RETURN BOOLEAN
           IS
              ests_flag   VARCHAR2 (1);

              CURSOR ests_flag_c (term_in   sftregs.sftregs_term_code%TYPE,
                                  regs_date_in DATE)
              IS
              SELECT 'X'
                FROM sfbests
               WHERE TRUNC (regs_date_in) BETWEEN TRUNC (sfbests_start_date)
                         AND TRUNC (sfbests_end_date)
                 AND sfbests_ests_code = sfbetrm_row.sfbetrm_ests_code
                 AND sfbests_term_code = term_in;

           BEGIN

              IF ests IS NOT NULL
              THEN
                 sfbetrm_row.sfbetrm_ests_code := ests;
              END IF;
              row_count := 0;

             FOR stvests IN stkests.stvestsc (sfbetrm_row.sfbetrm_ests_code)
             LOOP
                 ests_eff_crse_stat := stvests.stvests_eff_crse_stat;
                 ests_prev_reg := stvests.stvests_prev_reg;
                 row_count := stkests.stvestsc%rowcount;
                 row_count := 1;
              END LOOP;
              IF row_count <> 1
              THEN
                 sql_err := -20503;
                 RETURN FALSE;
              END IF;

              regs_date := bwcklibs.f_getregdate;
              OPEN ests_flag_c(term_in, regs_date);
              FETCH ests_flag_c INTO ests_flag;
              IF ests_flag_c%notfound
              THEN
                  sql_err := -20503;
                  CLOSE ests_flag_c;
                  RETURN FALSE;
              END IF;
              CLOSE ests_flag_c;

              RETURN TRUE;
           EXCEPTION
              WHEN OTHERS
              THEN
                 sql_err := -20503;
                 RETURN FALSE;
           END f_validenrl;


-- BWCKREGS.p_regschk(sgbstdn) rewritten.
           PROCEDURE p_regschk (
              stdn_row        sgbstdn%ROWTYPE,
              multi_term_in   BOOLEAN DEFAULT FALSE
           )
           IS
              astd_code            stvastd.stvastd_code%TYPE;
              cast_code            stvcast.stvcast_code%TYPE;
              sobterm_rec          sobterm%ROWTYPE;
              atmp_hrs             VARCHAR2 (1);
              lv_levl_code         sorlcur.sorlcur_levl_code%TYPE;
              lv_admit_term        sorlcur.sorlcur_term_code_admit%TYPE;

           BEGIN

              sgbstdn_row := stdn_row;
              sql_error := 0;
              regs_date := bwcklibs.f_getregdate;
              term := bwcklibs.f_getterm;

        /*************************************************************/
        /* Determine the Class of the Student - for Fees calculation */
        /*************************************************************/

              OPEN soklibs.sobterm_webc (term);
              FETCH soklibs.sobterm_webc INTO sobterm_rec;

              IF soklibs.sobterm_webc%NOTFOUND
              THEN
                 atmp_hrs := NULL;
              ELSE
                 IF sobterm_rec.sobterm_incl_attmpt_hrs_ind = 'Y'
                 THEN
                    atmp_hrs := 'A';
                 ELSE
                    atmp_hrs := NULL;
                 END IF;
              END IF;

              CLOSE soklibs.sobterm_webc;
              sgrclsr_clas_code := NULL;
              clas_desc := NULL;
        --
              lv_levl_code := sokccur.f_curriculum_value(
                                        p_pidm=> genpidm,
                                        p_lmod_code => sb_curriculum_str.f_learner,
                                        p_term_code => term,
                                        p_key_seqno => 99,
                                        p_eff_term => term,
                                        p_order => 1,
                                        p_field => 'LEVL');
              soklibs.p_class_calc (
                 genpidm,
                 lv_levl_code,
                 term,
                 atmp_hrs,
                 sgrclsr_clas_code,
                 clas_desc
              );

        /*************************************************************/
        /* End of Class Determination                                */
        /*************************************************************/

              IF     f_stuhld (sql_error, global_pidm)
                 AND NOT multi_term_in
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

              row_count := 0;

              FOR stvstst IN stkstst.stvststc (sgbstdn_row.sgbstdn_stst_code)
              LOOP
                 stst_reg_ind := stvstst.stvstst_reg_ind;
                 row_count := stkstst.stvststc%rowcount;
              END LOOP;

              IF     row_count <> 1
                 AND NOT multi_term_in
              THEN
                 raise_application_error (-20526, bwcklibs.error_msg_table (-20526));
              END IF;

              FOR shrttrm IN shklibs.shrttrmc (genpidm, term)
              LOOP
                 astd_code := shrttrm.shrttrm_astd_code_end_of_term;
                 cast_code := shrttrm.shrttrm_cast_code;
              END LOOP;

              IF sgbstdn_row.sgbstdn_term_code_astd = term
              THEN
                 astd_code := sgbstdn_row.sgbstdn_astd_code;
              END IF;

              IF sgbstdn_row.sgbstdn_term_code_cast = term
              THEN
                 cast_code := sgbstdn_row.sgbstdn_cast_code;
              END IF;

              IF cast_code IS NOT NULL
              THEN
                 row_count := 0;

                 FOR stvcast IN stkcast.stvcastc (cast_code)
                 LOOP
                    cast_prevent_reg_ind := stvcast.stvcast_prevent_reg_ind;
                    row_count := stkcast.stvcastc%rowcount;
                 END LOOP;

                 IF     row_count <> 1
                    AND NOT multi_term_in
                 THEN
                    raise_application_error (-20603, bwcklibs.error_msg_table (-20603));
                 END IF;
              ELSIF astd_code IS NOT NULL
              THEN
                 row_count := 0;

                 FOR stvastd IN stkastd.stvastdc (astd_code)
                 LOOP
                    astd_prevent_reg_ind := stvastd.stvastd_prevent_reg_ind;
                    row_count := stkastd.stvastdc%rowcount;
                 END LOOP;

                 IF NOT multi_term_in
                 THEN
                    IF row_count <> 1
                    THEN
                       raise_application_error (-20527, bwcklibs.error_msg_table (-20527));
                    ELSIF astd_prevent_reg_ind = 'Y'
                    THEN
                       raise_application_error (-20529, bwcklibs.error_msg_table (-20529));
                    END IF;
                 END IF;
              END IF;

              sfksels.p_get_min_max_by_curric_stand (
                     p_term        => term,
                     p_pidm        => genpidm,
                     p_seq_no      => NULL,
                     p_astd_code   => astd_code,
                     p_cast_code   => cast_code,
                     p_min_hrs_out => mhrs_min_hrs,
                     p_max_hrs_out => mhrs_max_hrs,
                     p_min_srce_out => minh_srce,
                     p_max_srce_out => maxh_srce );

              lv_admit_term := sokccur.f_curriculum_value(
                                        p_pidm=>genpidm,
                                        p_lmod_code => sb_curriculum_str.f_learner,
                                        p_term_code => term,
                                        p_key_seqno => 99,
                                        p_eff_term => term,
                                        p_order => 1,
                                        p_field => 'TADMIT');

              IF bwckregs.f_readmit_required(term, genpidm,
                                    lv_admit_term,
                                    sobterm_row.sobterm_readm_req,
                                    multi_term_in)
              THEN
                 raise_application_error (-20505, bwcklibs.error_msg_table (-20505));
              END IF;

              sobterm_row.sobterm_fee_assessmnt_eff_date :=
                GREATEST (
                   TRUNC (regs_date),
                   NVL (
                      TRUNC (sobterm_row.sobterm_fee_assessmnt_eff_date),
                      TRUNC (regs_date)
                   )
                );

              FOR advr IN soklibs.advisorc (genpidm, term)
              LOOP
                 advr_row := advr;
              END LOOP;
           END p_regschk;

-- BWCKREGS.f_validrgre copy and paste
          FUNCTION f_validrgre (
                sql_err   OUT   NUMBER,
                rgre            stvrgre.stvrgre_code%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
             BEGIN
                IF rgre IS NOT NULL
                THEN
                   sfbetrm_row.sfbetrm_rgre_code := rgre;
                END IF;

                row_count := 0;

                FOR stvrgre IN stkrgre.stvrgrec (sfbetrm_row.sfbetrm_rgre_code)
                LOOP
                   row_count := stkrgre.stvrgrec%rowcount;
                END LOOP;

                IF row_count <> 1
                THEN
                   sql_err := -20531;
                   RETURN FALSE;
                END IF;

                RETURN TRUE;
             END f_validrgre;

-- BWCKREGS.p_regschk(sfbetrm) rewritten.
           PROCEDURE p_regschk (
              etrm_row        sfbetrm%ROWTYPE,
              add_ind         VARCHAR2 DEFAULT NULL,
              multi_term_in   BOOLEAN DEFAULT FALSE
           )
           IS
           BEGIN
              sfbetrm_row := etrm_row;
              regs_date := bwcklibs.f_getregdate;

              IF sfbetrm_row.sfbetrm_term_code IS NULL
              THEN
                 sfbetrm_row.sfbetrm_term_code := term;
              END IF;

              IF sfbetrm_row.sfbetrm_pidm IS NULL
              THEN
                 sfbetrm_row.sfbetrm_pidm := genpidm;
              END IF;

              IF sfbetrm_row.sfbetrm_ests_code IS NULL
              THEN
                 sfbetrm_row.sfbetrm_ests_code := 'EL';
                 sfbetrm_row.sfbetrm_ests_date := TRUNC (regs_date);
              END IF;

              IF sfbetrm_row.sfbetrm_mhrs_over IS NULL
              THEN
                 sfbetrm_row.sfbetrm_mhrs_over := mhrs_max_hrs;
                 sfbetrm_row.sfbetrm_maxh_srce_cde := maxh_srce;
              END IF;

              IF sfbetrm_row.sfbetrm_min_hrs IS NULL
              THEN
                 sfbetrm_row.sfbetrm_min_hrs := mhrs_min_hrs;
                 sfbetrm_row.sfbetrm_minh_srce_cde := minh_srce;
              END IF;

              IF sfbetrm_row.sfbetrm_ar_ind IS NULL
              THEN
                 sfbetrm_row.sfbetrm_ar_ind := 'N';
              END IF;

              IF sfbetrm_row.sfbetrm_add_date IS NULL
              THEN
                 sfbetrm_row.sfbetrm_add_date := SYSDATE;
              END IF;

              sfbetrm_row.sfbetrm_user := USER;
              sfbetrm_row.sfbetrm_activity_date := SYSDATE;

              IF     stst_reg_ind = 'N'
                 AND NOT multi_term_in
              THEN
                 raise_application_error (-20507, bwcklibs.error_msg_table (-20507));
              END IF;

              sql_error := 0;

-- Call local              IF     NOT bwckregs.f_validenrl (
              IF     NOT f_validenrl (
                        sql_error,
                        sfbetrm_row.sfbetrm_term_code
                     )
                 AND NOT multi_term_in
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;
              --      sfbetrm_row.sfbetrm_term_code||'.'||
              --      sfbetrm_row.sfbetrm_ests_code||'.'||
              --      regs_date||'.'

              IF sfbetrm_row.sfbetrm_ar_ind <> 'Y'
              THEN
                 IF     NOT bwckregs.f_validacpt (sql_error)
                    AND NOT multi_term_in
                 THEN
                    raise_application_error (
                       sql_error,
                       bwcklibs.error_msg_table (sql_error)
                    );
                 END IF;
              END IF;

              IF sfbetrm_row.sfbetrm_rgre_code IS NOT NULL
              THEN
                 IF     NOT f_validrgre (sql_error)
                    AND NOT multi_term_in
                 THEN
                    raise_application_error (
                       sql_error,
                       bwcklibs.error_msg_table (sql_error)
                    );
                 END IF;
              END IF;

              IF     ests_prev_reg = 'Y'
                 AND NOT multi_term_in
              THEN
                 raise_application_error (-20506, bwcklibs.error_msg_table (-20506));
              END IF;

              IF     astd_prevent_reg_ind = 'Y'
                 AND NOT multi_term_in
              THEN
                 raise_application_error (-20529, bwcklibs.error_msg_table (-20529));
              END IF;

              IF     cast_prevent_reg_ind = 'Y'
                 AND NOT multi_term_in
              THEN
                 raise_application_error (-20604, bwcklibs.error_msg_table (-20604));
              END IF;

              IF NVL (add_ind, 'N') = 'Y'
              THEN
                 bwcklibs.p_add_sfbetrm (sfbetrm_row);
              END IF;
           END p_regschk;

-- BWCKREGS.f_validlevl rewritten.
           FUNCTION f_validlevl (
              sql_err   OUT   NUMBER,
              term_in         sftregs.sftregs_term_code%TYPE,
              levl            sftregs.sftregs_levl_code%TYPE DEFAULT NULL
           )
              RETURN BOOLEAN
           IS
              genpidm   spriden.spriden_pidm%TYPE;
              CURSOR scrlevl_c(term_in   sftregs.sftregs_term_code%TYPE,
                                subj_code_in ssbsect.ssbsect_subj_code%TYPE,
                                crse_numb_in ssbsect.ssbsect_crse_numb%TYPE)
              IS
                    SELECT MIN (scrlevl_levl_code)
                      FROM scrlevl
                     WHERE scrlevl_subj_code = subj_code_in
                       AND scrlevl_crse_numb = crse_numb_in
                       AND scrlevl_eff_term =
                            (SELECT MAX (scrlevl_eff_term)
                               FROM scrlevl
                              WHERE scrlevl_subj_code = subj_code_in
                                AND scrlevl_crse_numb = crse_numb_in
                                AND scrlevl_eff_term <= term_in)
                    HAVING COUNT (*) = 1;

              CURSOR sovccur_levl_c(p_pidm  sorlcur.sorlcur_pidm%TYPE,
                                    p_term  sorlcur.sorlcur_term_code%TYPE)
              IS
                SELECT sovccur_levl_code
                  FROM sovccur
                 WHERE sovccur_pidm = p_pidm
                   AND sovccur_term_code <= p_term
                   AND sovccur_order > 0
                   AND sovccur_lmod_code = sb_curriculum_str.f_learner
              ORDER BY sovccur_priority_no;

           BEGIN
--              IF NOT twbkwbis.f_validuser (global_pidm)
--              THEN
--                 RETURN FALSE;
--              END IF;

--              IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--              THEN
--                 genpidm :=
--                   TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--              ELSE
                 genpidm := global_pidm;
--              END IF;
              IF levl IS NOT NULL
              THEN
                 sftregs_row.sftregs_levl_code := levl;
              END IF;

              IF sftregs_row.sftregs_levl_code IS NULL
              THEN
                 row_count := 0;
                 OPEN scrlevl_c(term_in,
                                ssbsect_row.ssbsect_subj_code,
                                ssbsect_row.ssbsect_crse_numb);
                 FETCH scrlevl_c INTO sftregs_row.sftregs_levl_code;
                 IF scrlevl_c%notfound
                 THEN
        --
        -- Loop through student's current/active curriculum in priority order.
        -- The first curriculum level that matches one of the valid course
        -- levels is the one that will be used for the registration.
        --
                   soklcur.p_create_sotvcur(p_pidm      => genpidm,
                                             p_term_code => term_in,
                                             p_lmod_code => sb_curriculum_str.f_learner);
                 OPEN sovccur_levl_c(genpidm,
                                        term_in);
                    LOOP
                      FETCH sovccur_levl_c INTO stud_level;
                      EXIT WHEN (sovccur_levl_c%NOTFOUND) OR (row_count > 0);
                      FOR scrlevl IN scklibs.scrlevlc (
                                                       ssbsect_row.ssbsect_subj_code,
                                                       ssbsect_row.ssbsect_crse_numb,
                                                       term_in,
                                                       stud_level
                                                      )
                        LOOP
                          row_count := scklibs.scrlevlc%rowcount;
                        END LOOP;
                        sftregs_row.sftregs_levl_code := stud_level;
                    END LOOP;
                    CLOSE sovccur_levl_c;

                    IF row_count = 0
                    THEN
                      sql_err := -20532;
                      RETURN FALSE;
                    END IF;
                 END IF;
                 CLOSE scrlevl_c;
              END IF;
              RETURN TRUE;
           END f_validlevl;

-- BWCKREGS.p_getoverride rewritten.
           PROCEDURE p_getoverride
           IS
              genpidm   spriden.spriden_pidm%TYPE;
           BEGIN
--              IF NOT twbkwbis.f_validuser (global_pidm)
--              THEN
--                 RETURN;
--              END IF;

--              IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--              THEN
--                 genpidm :=
--                   TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--              ELSE
                 genpidm := global_pidm;
--              END IF;

              FOR ssbsect IN ssklibs.ssbsectc (
                                sftregs_row.sftregs_crn,
                                sftregs_row.sftregs_term_code
                             )
              LOOP
                 FOR over_rec IN overridec (
                                    genpidm,
                                    sftregs_row.sftregs_term_code,
                                    sftregs_row.sftregs_crn,
                                    ssbsect.ssbsect_subj_code,
                                    ssbsect.ssbsect_crse_numb,
                                    ssbsect.ssbsect_seq_numb
                                 )
                 LOOP
                    IF over_rec.sfrrovr_dupl_over = 'Y'
                    THEN
                       sftregs_row.sftregs_dupl_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_link_over = 'Y'
                    THEN
                       sftregs_row.sftregs_link_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_corq_over = 'Y'
                    THEN
                       sftregs_row.sftregs_corq_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_preq_over = 'Y'
                    THEN
                       sftregs_row.sftregs_preq_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_time_over = 'Y'
                    THEN
                       sftregs_row.sftregs_time_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_levl_over = 'Y'
                    THEN
                       sftregs_row.sftregs_levl_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_coll_over = 'Y'
                    THEN
                       sftregs_row.sftregs_coll_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_degc_over = 'Y'
                    THEN
                       sftregs_row.sftregs_degc_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_prog_over = 'Y'
                    THEN
                       sftregs_row.sftregs_prog_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_majr_over = 'Y'
                    THEN
                       sftregs_row.sftregs_majr_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_clas_over = 'Y'
                    THEN
                       sftregs_row.sftregs_clas_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_appr_over = 'Y'
                    THEN
                       sftregs_row.sftregs_appr_over := 'Y';
                       appr_error := '';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_rept_over = 'Y'
                    THEN
                       sftregs_row.sftregs_rept_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_rpth_over = 'Y'
                    THEN
                       sftregs_row.sftregs_rpth_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_camp_over = 'Y'
                    THEN
                       sftregs_row.sftregs_camp_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_dept_over = 'Y'
                    THEN
                       sftregs_row.sftregs_dept_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_chrt_over = 'Y'
                    THEN
                       sftregs_row.sftregs_chrt_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;
                    IF over_rec.sfrrovr_mexc_over = 'Y'
                    THEN
                       sftregs_row.sftregs_mexc_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;
                    IF over_rec.sfrrovr_atts_over = 'Y'
                    THEN
                       sftregs_row.sftregs_atts_over := 'Y';
                       sftregs_row.sftregs_error_flag := 'O';
                    END IF;

                    IF over_rec.sfrrovr_capc_over = 'Y'
                    THEN
                       IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
                       THEN
                          sftregs_row.sftregs_capc_over := 'Y';
                       END IF;

                       IF stvrsts_row.stvrsts_wait_ind = 'Y'
                       THEN
                          sftregs_wait_over := 'Y';
                       END IF;
                    END IF;
                 END LOOP;
              END LOOP;
           END p_getoverride;

--BWCKREGS.f_validcrn rewritten
             FUNCTION f_validcrn (
                sql_err   OUT      NUMBER,
                term_in   IN       sftregs.sftregs_term_code%TYPE,
                crn                sftregs.sftregs_crn%TYPE DEFAULT NULL,
                subj               ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
                crse               ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
                seq                ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
                stcr_crn   sftregs.sftregs_crn%TYPE;

                CURSOR stcr_crn_c(term_in sftregs.sftregs_term_code%TYPE,
                                  crn_in  sftregs.sftregs_crn%TYPE)
                IS
                   SELECT MIN (ssbsect_crn)
                     FROM ssbsect
                    WHERE UPPER (ssbsect_subj_code) =
                                                   UPPER (ssbsect_row.ssbsect_subj_code)
                      AND ssbsect_crse_numb = ssbsect_row.ssbsect_crse_numb
                      AND ssbsect_seq_numb = ssbsect_row.ssbsect_seq_numb
                      AND ssbsect_crn LIKE NVL (crn_in, '%')
                      AND ssbsect_term_code = term_in;

             BEGIN
          --
          -- Voodoo
          -- ===========================================================
                IF crn IS NOT NULL
                THEN
                   sftregs_row.sftregs_crn := crn;
                END IF;

                IF subj IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_subj_code := subj;
                END IF;

                IF crse IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_crse_numb := crse;
                END IF;

                IF seq IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_seq_numb := seq;
                END IF;

          --
          --
          -- ===========================================================
                IF     ssbsect_row.ssbsect_subj_code IS NOT NULL
                   AND ssbsect_row.ssbsect_crse_numb IS NOT NULL
                   AND ssbsect_row.ssbsect_seq_numb IS NOT NULL
                THEN
          --
          -- Check section records for subject,course,section,crn,term.
          -- ===========================================================
                   OPEN stcr_crn_c(term_in, sftregs_row.sftregs_crn);
                   FETCH stcr_crn_c INTO stcr_crn;
                   CLOSE stcr_crn_c;

          --
          -- If section not found, decide which condition was the cause:
          -- 1. Invalid subject, course, section
          -- 2. Invalid subject, course, section, crn
          -- ...then set sql_err and return false.
          -- ===========================================================
                   IF stcr_crn IS NULL
                   THEN
                      IF sftregs_row.sftregs_crn IS NULL
                      THEN
                         sql_err := -20514;
                         RETURN FALSE;
                      ELSE
                         sql_err := -20515;
                         RETURN FALSE;
                      END IF;
                   ELSE
                      IF stcr_crn <> NVL (sftregs_row.sftregs_crn, stcr_crn)
                      THEN
                         sql_err := -20515;
                         RETURN FALSE;
                      ELSE
                         sftregs_row.sftregs_crn := stcr_crn;
                      END IF;
                   END IF;
                END IF;

          --
          -- Loop through section records.
          -- ===========================================================
                OPEN ssklibs.ssbsectc (sftregs_row.sftregs_crn, term_in);
                ssbsect_count := 0;

                LOOP
                   FETCH ssklibs.ssbsectc INTO ssbsect_row;
                   EXIT WHEN ssklibs.ssbsectc%NOTFOUND;
                   ssbsect_count := ssbsect_count + 1;
          --
          -- Loop through course records.
          -- ===========================================================
                   scrcrse_count := 0;
                   OPEN scklibs.scbcrsec (
                      ssbsect_row.ssbsect_subj_code,
                      ssbsect_row.ssbsect_crse_numb,
                      term_in
                   );

                   LOOP
                      FETCH scklibs.scbcrsec INTO scbcrse_row;
                      EXIT WHEN scklibs.scbcrsec%NOTFOUND;
                      scrcrse_count := scrcrse_count + 1;

                      IF scbcrse_row.scbcrse_repeat_limit IS NULL
                      THEN
                         scbcrse_row.scbcrse_repeat_limit := 99;
                      END IF;

                      IF scbcrse_row.scbcrse_max_rpt_units IS NULL
                      THEN
                         scbcrse_row.scbcrse_max_rpt_units := 9999.999;
                      END IF;
                   END LOOP;

                   row_count := scklibs.scbcrsec%rowcount;
                   CLOSE scklibs.scbcrsec;

          --
          -- If one, and only one, course record was not found, set
          -- sql_err and return false.
          -- ===========================================================
                   IF scrcrse_count <> 1
                   THEN
                      CLOSE ssklibs.ssbsectc;
                      sql_err := -20508;
                      RETURN FALSE;
                   END IF;

                   IF ssbsect_row.ssbsect_wait_capacity IS NULL
                   THEN
                      ssbsect_row.ssbsect_wait_capacity := 0;
                   END IF;
                END LOOP;

                row_count := ssklibs.ssbsectc%rowcount;
                CLOSE ssklibs.ssbsectc;

          --
          -- If one, and only one, section record was not found, set
          -- sql_err and return false.
          -- ===========================================================
                IF ssbsect_count <> 1
                THEN
                   sql_err := -20508;
                   RETURN FALSE;
                END IF;

                row_count := 0;

          --
          -- Check if the section allows web/vr registration.
          -- ===========================================================
-- Course exchange bypasses self-service available check
--                IF ssbsect_row.ssbsect_voice_avail = 'Y' OR
--                   NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N') = 'Y'
--                THEN
--                   NULL;
--                ELSE
--                   sql_err := -20544;
--                   RETURN FALSE;
--                END IF;

                RETURN TRUE;
             END f_validcrn;

-- BWCKREGS.p_defstcr copy and paste.
          PROCEDURE p_defstcr (add_ind CHAR DEFAULT NULL)
          IS
             ssts_code   stvssts.stvssts_code%TYPE;
             gmod_code   stvgmod.stvgmod_code%TYPE;
             l_found     BOOLEAN;

             CURSOR ssts_code_c (ssts_code_in ssbsect.ssbsect_ssts_code%TYPE)
             IS
                SELECT MIN (stvssts_code)
                  FROM stvssts
                 WHERE (    ssts_code_in IS NOT NULL
                        AND stvssts_code = ssts_code_in)
                   AND stvssts_reg_ind = 'N';
          BEGIN
             OPEN ssts_code_c (ssbsect_row.ssbsect_ssts_code);
             FETCH ssts_code_c INTO ssts_code;
             CLOSE ssts_code_c;

             IF ssts_code IS NOT NULL
             THEN
                raise_application_error (-20511, bwcklibs.error_msg_table (-20511));
             END IF;

             row_count := 0;
             regs_date := bwcklibs.f_getregdate;
             l_found := FALSE;

             IF ssbsect_row.ssbsect_ptrm_code IS NOT NULL
             THEN
                FOR sfrrsts IN sfkcurs.sfrrstsc (
                                  sftregs_row.sftregs_rsts_code,
                                  sftregs_row.sftregs_term_code,
                                  ssbsect_row.ssbsect_ptrm_code
                               )
                LOOP
                   IF     TRUNC (regs_date) >= TRUNC (sfrrsts.sfrrsts_start_date)
                      AND TRUNC (regs_date) <= TRUNC (sfrrsts.sfrrsts_end_date)
                   THEN
                       IF add_ind = 'Y' THEN
                          sftregs_row.sftregs_ptrm_code :=
                              ssbsect_row.ssbsect_ptrm_code;
                          sftregs_row.sftregs_gmod_code :=
                              ssbsect_row.ssbsect_gmod_code;
                          sftregs_row.sftregs_credit_hr :=
                              ssbsect_row.ssbsect_credit_hrs;
                          sftregs_row.sftregs_bill_hr :=
                              ssbsect_row.ssbsect_bill_hrs;
                          sftregs_row.sftregs_camp_code :=
                              ssbsect_row.ssbsect_camp_code;
                       END IF;
                      l_found := TRUE;
                   END IF;

                   row_count := sfkcurs.sfrrstsc%rowcount;
                END LOOP;

                IF row_count = 0
                THEN
                   raise_application_error (-20512, bwcklibs.error_msg_table (-20512));
                END IF;

                IF NOT l_found
                THEN
                   raise_application_error (-20513, bwcklibs.error_msg_table (-20513));
                END IF;
             ELSE
                FOR ssrrsts_rec IN sfkcurs.ssrrstsc (
                                      sftregs_row.sftregs_rsts_code,
                                      sftregs_row.sftregs_term_code,
                                      sftregs_row.sftregs_crn
                                   )
                LOOP
                   IF (TRUNC (regs_date) BETWEEN ssrrsts_rec.ssbsect_reg_from_date
                          AND ssrrsts_rec.ssbsect_reg_to_date
                )
                 --c3sc GR 26/02/2006  Added
                   OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                                   p_rsts_code =>sftregs_row.sftregs_rsts_code)
                          )
                --END c3sc
                   THEN
                       IF add_ind = 'Y' THEN
                          sftregs_row.sftregs_ptrm_code :=
                              ssbsect_row.ssbsect_ptrm_code;
                          sftregs_row.sftregs_gmod_code :=
                              ssbsect_row.ssbsect_gmod_code;
                          sftregs_row.sftregs_credit_hr :=
                              ssbsect_row.ssbsect_credit_hrs;
                          sftregs_row.sftregs_bill_hr :=
                              ssbsect_row.ssbsect_bill_hrs;
                          sftregs_row.sftregs_camp_code :=
                              ssbsect_row.ssbsect_camp_code;
                       END IF;
                      l_found := TRUE;
                   END IF;

                   row_count := sfkcurs.ssrrstsc%rowcount;
                END LOOP;

                IF row_count = 0
                THEN
                   raise_application_error (-20608, bwcklibs.error_msg_table (-20608));
                END IF;

                IF NOT l_found
                THEN
                   raise_application_error (-20609, bwcklibs.error_msg_table (-20609));
                END IF;
             END IF;

             IF     add_ind IS NOT NULL
                AND add_ind = 'Y'
             THEN
                IF NVL (sftregs_row.sftregs_credit_hr, 00.00) = 00.00
                THEN
                   sftregs_row.sftregs_credit_hr := scbcrse_row.scbcrse_credit_hr_low;
                END IF;

                IF NVL (sftregs_row.sftregs_bill_hr, 00.00) = 00.00
                THEN
                   sftregs_row.sftregs_bill_hr := scbcrse_row.scbcrse_bill_hr_low;
                END IF;

                IF ssbsect_row.ssbsect_gmod_code IS NOT NULL
                THEN
                   sftregs_row.sftregs_gmod_code := ssbsect_row.ssbsect_gmod_code;
                ELSE
                   FOR scrgmod IN scklibs.scrgmodc (
                                     ssbsect_row.ssbsect_subj_code,
                                     ssbsect_row.ssbsect_crse_numb,
                                     sftregs_row.sftregs_term_code
                                  )
                   LOOP
                      gmod_code := scrgmod.scrgmod_gmod_code;

                      IF scrgmod.scrgmod_default_ind = 'D'
                      THEN
                         sftregs_row.sftregs_gmod_code := scrgmod.scrgmod_gmod_code;
                         EXIT;
                      END IF;
                   END LOOP;

                   IF sftregs_row.sftregs_gmod_code IS NULL
                   THEN
                      sftregs_row.sftregs_gmod_code := gmod_code;
                   END IF;
                END IF;
             END IF;
          EXCEPTION
             WHEN bwcklibs.sect_prev_regs
             THEN
                raise_application_error (-20511, bwcklibs.error_msg_table (-20511));
             WHEN bwcklibs.ptrm_stat_undefined
             THEN
                raise_application_error (-20512, bwcklibs.error_msg_table (-20512));
             WHEN bwcklibs.crse_date_err
             THEN
                raise_application_error (-20513, bwcklibs.error_msg_table (-20513));
             WHEN bwcklibs.olr_stat_undefined
             THEN
                raise_application_error (-20608, bwcklibs.error_msg_table (-20608));
             WHEN bwcklibs.olr_crse_date_err
             THEN
                raise_application_error (-20609, bwcklibs.error_msg_table (-20609));
             WHEN OTHERS
             THEN
                raise_application_error (-20533, bwcklibs.error_msg_table (-20533));
          END p_defstcr;


-- BWCKREGS.f_validrsts copy and paste
             FUNCTION f_validrsts (
                sql_err   OUT   NUMBER,
                rsts            stvrsts.stvrsts_code%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
             BEGIN
                IF rsts IS NOT NULL
                THEN
                   sftregs_row.sftregs_rsts_code := rsts;
                END IF;

                row_count := 0;
                regs_date := bwcklibs.f_getregdate;

          -- Defect 10737
          -- Implicit cursor for STVRSTS
          -- ===============================================================
                FOR stvrsts IN stkrsts.stvrstsc (sftregs_row.sftregs_rsts_code)
                LOOP
                   IF    stvrsts.stvrsts_enterable_ind = 'Y'
                      OR (
                                stvrsts.stvrsts_enterable_ind = 'N'
                            AND stvrsts.stvrsts_code = sfbetrm_row.sfbetrm_ests_code
                         )
                   THEN
                      stvrsts_row := stvrsts;
                      row_count := stkrsts.stvrstsc%rowcount;
                   END IF;
                END LOOP;

          --

                /* local declaration of stvrstsc */
          /*
             DECLARE cursor stvrstsc is
               select *
               from stvrsts
               where stvrsts_code = sfrstcr_row.sfrstcr_rsts_code
               and stvrsts_enterable_ind = 'Y'
               or  (stvrsts_enterable_ind = 'N'
                 and stvrsts_code = sfbetrm_row.sfbetrm_ests_code);

             begin
                 open stvrstsc;
                 fetch stvrstsc into stvrsts_row;
                 if stvrstsc%notfound then
                   row_count := 0;
                 else
                   row_count := row_count + 1 ;
                 end if ;
              end ;
          */
                IF row_count = 0
                THEN
                   sql_err := -20516;
                   RETURN FALSE;
                END IF;

                IF     stvrsts_row.stvrsts_wait_ind = 'Y'
                   AND ssbsect_row.ssbsect_wait_capacity = '0'
                THEN
                   sql_err := -20517;
                   RETURN FALSE;
                END IF;

                sftregs_row.sftregs_rsts_date := regs_date;

                IF NVL (ssbsect_row.ssbsect_gradable_ind, 'N') <> 'N'
                THEN
                   sftregs_row.sftregs_grde_code := stvrsts_row.stvrsts_auto_grade;
                END IF;

                row_count := 0;

                IF sftregs_row.sftregs_ptrm_code IS NOT NULL
                THEN
                   FOR sfrrsts IN sfkcurs.sfrrstsc (
                                     sftregs_row.sftregs_rsts_code,
                                     sftregs_row.sftregs_term_code,
                                     sftregs_row.sftregs_ptrm_code
                                  )
                   LOOP
                      IF     TRUNC (sftregs_row.sftregs_rsts_date) >=
                                                              sfrrsts.sfrrsts_start_date
                         AND TRUNC (sftregs_row.sftregs_rsts_date) <=
                                                                sfrrsts.sfrrsts_end_date
                      THEN
                         row_count := sfkcurs.sfrrstsc%rowcount;
                      END IF;
                   END LOOP;

                   IF row_count = 0
                   THEN
                      IF sftregs_row.sftregs_rsts_code =
                                               SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
                      THEN
                         sql_err := -20518;
                      ELSE
                         sql_err := -20519;
                      END IF;

                      RETURN FALSE;
                   END IF;
                ELSE
                   FOR ssrrsts_rec IN sfkcurs.ssrrstsc (
                                         sftregs_row.sftregs_rsts_code,
                                         sftregs_row.sftregs_term_code,
                                         sftregs_row.sftregs_crn
                                      )
                   LOOP
                      IF ( TRUNC (sftregs_row.sftregs_rsts_date) BETWEEN
                         ssrrsts_rec.ssbsect_reg_from_date AND ssrrsts_rec.ssbsect_reg_to_date
                   )
                     --c3sc GRS 26/02/2006 Added(OL override)
                      OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                                   p_rsts_code =>sftregs_row.sftregs_rsts_code)
                          )
                     --END c3sc
                      THEN
                      /* store row_count only if either start_tdate is current or course isnt being added */
                        IF TRUNC (sftregs_row.sftregs_rsts_date) <=
                              ssrrsts_rec.ssbsect_learner_regstart_tdate
                           OR sftregs_row.sftregs_rsts_code <>
-- Use rsts value sent in                                  SUBSTR (f_stu_getwebregsrsts ('R'), 1, 2)
                                          rsts
                       --c3sc GRS 26/02/2006 Added(OL override)
                          OR ( sv_auth_addcodes_bp.F_process_with_auth(p_term_code=>sftregs_row.sftregs_term_code,
                                                                        p_rsts_code =>sftregs_row.sftregs_rsts_code)
                              )

                       --END c3sc
                        THEN
                           row_count := sfkcurs.ssrrstsc%rowcount;
                        END IF;
                      END IF;
                   END LOOP;

                   IF row_count = 0
                   THEN
                      IF sftregs_row.sftregs_rsts_code =
                                               SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
                      THEN
                         sql_err := -20606;
                      ELSE
                         sql_err := -20607;
                      END IF;

                      RETURN FALSE;
                   END IF;
                END IF;

                IF stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                THEN
                   IF NVL (sftregs_row.sftregs_credit_hr_hold, 0) = 0
                   THEN
                      sftregs_row.sftregs_credit_hr_hold :=
                                                          sftregs_row.sftregs_credit_hr;
                   END IF;

                   sftregs_row.sftregs_credit_hr := 00.00;
                   sftregs_row.sftregs_error_flag := 'D';
                   sftregs_row.sftregs_dupl_over := 'N';
                   sftregs_row.sftregs_link_over := 'N';
                   sftregs_row.sftregs_corq_over := 'N';
                   sftregs_row.sftregs_preq_over := 'N';
                   sftregs_row.sftregs_time_over := 'N';
                   sftregs_row.sftregs_capc_over := 'N';
                   sftregs_row.sftregs_levl_over := 'N';
                   sftregs_row.sftregs_coll_over := 'N';
                   sftregs_row.sftregs_degc_over := 'N';
                   sftregs_row.sftregs_prog_over := 'N';
                   sftregs_row.sftregs_majr_over := 'N';
                   sftregs_row.sftregs_clas_over := 'N';
                   sftregs_row.sftregs_appr_over := 'N';
                   sftregs_wait_over := 'N';
                   sftregs_row.sftregs_rept_over := 'N';
                   sftregs_row.sftregs_rpth_over := 'N';
                   sftregs_row.sftregs_camp_over := 'N';
                   sftregs_row.sftregs_dept_over := 'N';
                   sftregs_row.sftregs_chrt_over := 'N';
                   sftregs_row.sftregs_mexc_over := 'N';
                   sftregs_row.sftregs_atts_over := 'N';
                END IF;

                IF     stvrsts_row.stvrsts_wait_ind = 'Y'
                   AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                THEN
                   sftregs_row.sftregs_error_flag := 'L';
                END IF;

                IF stvrsts_row.stvrsts_incl_assess = 'N'
                THEN
                   IF NVL (sftregs_row.sftregs_bill_hr_hold, 0) = 0
                   THEN
                      sftregs_row.sftregs_bill_hr_hold := sftregs_row.sftregs_bill_hr;
                   END IF;

                   sftregs_row.sftregs_bill_hr := 00.00;
                ELSIF sftregs_row.sftregs_bill_hr IS NULL
                OR (old_stvrsts_row.stvrsts_incl_assess = 'N'
                    AND sftregs_row.sftregs_bill_hr = 0)
                THEN
                   sftregs_row.sftregs_bill_hr := sftregs_row.sftregs_bill_hr_hold;

                   IF NVL (sftregs_row.sftregs_bill_hr, 00.00) = 00.00
                   THEN
                      sftregs_row.sftregs_bill_hr :=
                        NVL (
                           ssbsect_row.ssbsect_bill_hrs,
                           scbcrse_row.scbcrse_bill_hr_low
                        );
                   END IF;
                END IF;

                IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
                THEN
                   IF    NVL (sftregs_row.sftregs_error_flag, '#') = 'D'
                      OR NVL (sftregs_row.sftregs_error_flag, '#') = 'L'
                   THEN
                      sftregs_row.sftregs_credit_hr := ssbsect_row.ssbsect_credit_hrs;
                      sftregs_row.sftregs_error_flag := '';
                   END IF;

                   IF    (
                                NVL (sftregs_row.sftregs_error_flag, '#') = 'F'
                            AND old_stvrsts_row.stvrsts_wait_ind = 'Y'
                         )
                      OR (
                                old_stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                            AND sftregs_row.sftregs_credit_hr = 0
                         )
                      OR sftregs_row.sftregs_credit_hr is NULL
                   THEN
                      sftregs_row.sftregs_credit_hr :=
                                                     sftregs_row.sftregs_credit_hr_hold;

                      IF NVL (sftregs_row.sftregs_credit_hr, 00.00) = 00.00
                      THEN
                         sftregs_row.sftregs_credit_hr :=
                                                      scbcrse_row.scbcrse_credit_hr_low;
                      END IF;
                   END IF;
                END IF;

                sftregs_row.sftregs_crse_title :=
                        NVL (ssbsect_row.ssbsect_crse_title, scbcrse_row.scbcrse_title);
                RETURN TRUE;
             END f_validrsts;


-- BWCKREGS.f_validgmod copy and paste.
             FUNCTION f_validgmod (
                sql_err     OUT   NUMBER,
                term_in           sftregs.sftregs_term_code%TYPE,
                gmod              sftregs.sftregs_gmod_code%TYPE DEFAULT NULL,
                subj              ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
                crse              ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
                sect_gmod         ssbsect.ssbsect_gmod_code%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
             BEGIN
                IF gmod IS NOT NULL
                THEN
                   sftregs_row.sftregs_gmod_code := gmod;
                END IF;

                IF subj IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_subj_code := subj;
                END IF;

                IF crse IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_crse_numb := crse;
                END IF;

                IF sect_gmod IS NOT NULL
                THEN
                   ssbsect_row.ssbsect_gmod_code := sect_gmod;
                END IF;

                row_count := 0;

                FOR scrgmod IN scklibs.scrgmodc (
                                  ssbsect_row.ssbsect_subj_code,
                                  ssbsect_row.ssbsect_crse_numb,
                                  term_in
                               )
                LOOP
                   IF sftregs_row.sftregs_gmod_code IS NULL
                   THEN
                      IF scrgmod.scrgmod_default_ind = 'D'
                      THEN
                         sftregs_row.sftregs_gmod_code := scrgmod.scrgmod_gmod_code;
                         row_count := scklibs.scrgmodc%rowcount;
                         EXIT;
                      END IF;
                   ELSE
                      IF     sftregs_row.sftregs_gmod_code = scrgmod.scrgmod_gmod_code
                         AND NVL (
                                ssbsect_row.ssbsect_gmod_code,
                                sftregs_row.sftregs_gmod_code
                             ) = sftregs_row.sftregs_gmod_code
                      THEN
                         row_count := scklibs.scrgmodc%rowcount;
                         EXIT;
                      END IF;
                   END IF;
                END LOOP;

                IF row_count = 0
                THEN
                   sql_err := -20536;
                   RETURN FALSE;
                END IF;

                RETURN TRUE;
             EXCEPTION
                WHEN OTHERS
                THEN
                   sql_err := -20536;
                   RETURN FALSE;
             END f_validgmod;

-- BWCKREGS.f_validcredhr copy and paste.
             FUNCTION f_validcredhr (
                sql_err   OUT   NUMBER,
                cred_hr         SFTREGS.SFTREGS_CREDIT_HR%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
             BEGIN
                IF cred_hr IS NOT NULL
                THEN
                   sftregs_row.sftregs_credit_hr := cred_hr;
                END IF;

                IF     sftregs_row.sftregs_credit_hr = 00.00
                   AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                THEN
                   RETURN TRUE;
                END IF;

                IF scbcrse_row.scbcrse_credit_hr_ind = 'TO'
                THEN
                   IF (
                            (
                                   sftregs_row.sftregs_credit_hr BETWEEN scbcrse_row.scbcrse_credit_hr_low
                                       AND scbcrse_row.scbcrse_credit_hr_high
                               AND ssbsect_row.ssbsect_credit_hrs IS NULL
                            )
                         OR ssbsect_row.ssbsect_credit_hrs =
                                                           sftregs_row.sftregs_credit_hr
                      )
                   THEN
                      RETURN TRUE;
                   END IF;
                ELSE
                   IF (
                            (
                                   sftregs_row.sftregs_credit_hr IS NOT NULL
                               AND (
                                         sftregs_row.sftregs_credit_hr =
                                                       scbcrse_row.scbcrse_credit_hr_low
                                      OR sftregs_row.sftregs_credit_hr =
                                                      scbcrse_row.scbcrse_credit_hr_high
                                   )
                               AND ssbsect_row.ssbsect_credit_hrs IS NULL
                            )
                         OR ssbsect_row.ssbsect_credit_hrs =
                                                           sftregs_row.sftregs_credit_hr
                      )
                   THEN
                      RETURN TRUE;
                   END IF;
                END IF;

                sql_err := -20537;
                RETURN FALSE;
             END f_validcredhr;
""")

        def plsql2 = """
-- BWCKREGS.f_validbillhr copy and paste
             FUNCTION f_validbillhr (
                sql_err   OUT   NUMBER,
                bill_hr         SFTREGS.SFTREGS_BILL_HR%TYPE DEFAULT NULL
             )
                RETURN BOOLEAN
             IS
             BEGIN
                IF bill_hr IS NOT NULL
                THEN
                   sftregs_row.sftregs_bill_hr := bill_hr;
                END IF;
                IF    (
                             sftregs_row.sftregs_bill_hr = 00.00
                         AND stvrsts_row.stvrsts_incl_sect_enrl = 'N'
                      )
                   OR (stvrsts_row.stvrsts_incl_assess = 'N')
                THEN
                   RETURN TRUE;
                END IF;

                IF scbcrse_row.scbcrse_bill_hr_ind = 'TO'
                THEN
                   IF (
                            (
                                   sftregs_row.sftregs_bill_hr BETWEEN scbcrse_row.scbcrse_bill_hr_low
                                       AND scbcrse_row.scbcrse_bill_hr_high
                               AND ssbsect_row.ssbsect_bill_hrs IS NULL
                            )
                         OR ssbsect_row.ssbsect_bill_hrs = sftregs_row.sftregs_bill_hr
                      )
                   THEN
                      RETURN TRUE;
                   END IF;
                ELSE
                   IF (
                            (
                                   sftregs_row.sftregs_bill_hr IS NOT NULL
                               AND (
                                         sftregs_row.sftregs_bill_hr =
                                                         scbcrse_row.scbcrse_bill_hr_low
                                      OR sftregs_row.sftregs_bill_hr =
                                                        scbcrse_row.scbcrse_bill_hr_high
                                   )
                               AND ssbsect_row.ssbsect_bill_hrs IS NULL
                            )
                         OR ssbsect_row.ssbsect_bill_hrs = sftregs_row.sftregs_bill_hr
                      )
                   THEN
                      RETURN TRUE;
                   END IF;
                END IF;

                sql_err := -20538;
                RETURN FALSE;
             END f_validbillhr;

-- BWCKREGS.f_validappr copy and paste.
           FUNCTION f_validappr (sql_err OUT NUMBER, appr_ind CHAR DEFAULT NULL)
              RETURN BOOLEAN
           IS
           BEGIN
              IF appr_ind IS NOT NULL
              THEN
                 sftregs_row.sftregs_appr_received_ind := appr_ind;
              END IF;

              IF NVL (sftregs_row.sftregs_appr_received_ind, 'N') NOT IN ('Y', 'N')
              THEN
                 sql_err := -20001;
                 RETURN FALSE;
              END IF;

              IF sftregs_row.sftregs_appr_received_ind = 'Y'
              THEN
                 IF appr_error = 'Y'
                 THEN
                    appr_error := '';
                    sftregs_row.sftregs_error_flag := '';
                 END IF;
              END IF;

              RETURN TRUE;
           END f_validappr;

-- BWCKREGS.p_regschk rewritten.
           PROCEDURE p_regschk (
              stcr_row   sftregs%ROWTYPE,
              subj       ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
              crse       ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
              seq        ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
              over       CHAR DEFAULT NULL,
              add_ind    CHAR DEFAULT NULL
           )
           IS
              stvrsts_row   stkrsts.stvrstsc%ROWTYPE;
           BEGIN
              sftregs_row := stcr_row;
              regs_date := bwcklibs.f_getregdate;

              ssbsect_row.ssbsect_subj_code := subj;
              ssbsect_row.ssbsect_crse_numb := crse;
              ssbsect_row.ssbsect_seq_numb := seq;

              sftregs_row.sftregs_sect_subj_code := ssbsect_row.ssbsect_subj_code;
              sftregs_row.sftregs_sect_crse_numb := ssbsect_row.ssbsect_crse_numb;
              sftregs_row.sftregs_sect_seq_numb := ssbsect_row.ssbsect_seq_numb;
              sftregs_row.sftregs_sect_schd_code := ssbsect_row.ssbsect_schd_code;

              IF over IS NOT NULL
              THEN
                 override := over;
              END IF;

              IF sftregs_row.sftregs_preq_over IS NULL
              THEN
                 sftregs_row.sftregs_preq_over := 'N';
              END IF;

              IF sftregs_row.sftregs_rept_over IS NULL
              THEN
                 sftregs_row.sftregs_rept_over := 'N';
              END IF;

              IF sftregs_row.sftregs_coll_over IS NULL
              THEN
                 sftregs_row.sftregs_coll_over := 'N';
              END IF;

              IF sftregs_row.sftregs_degc_over IS NULL
              THEN
                 sftregs_row.sftregs_degc_over := 'N';
              END IF;

              IF sftregs_row.sftregs_prog_over IS NULL
              THEN
                 sftregs_row.sftregs_prog_over := 'N';
              END IF;

              IF sftregs_row.sftregs_clas_over IS NULL
              THEN
                 sftregs_row.sftregs_clas_over := 'N';
              END IF;

              IF sftregs_row.sftregs_camp_over IS NULL
              THEN
                 sftregs_row.sftregs_camp_over := 'N';
              END IF;

              IF sftregs_row.sftregs_dept_over IS NULL
              THEN
                 sftregs_row.sftregs_dept_over := 'N';
              END IF;

              IF sftregs_row.sftregs_chrt_over IS NULL
              THEN
                 sftregs_row.sftregs_chrt_over := 'N';
              END IF;
              IF sftregs_row.sftregs_mexc_over IS NULL
              THEN
                 sftregs_row.sftregs_mexc_over := 'N';
              END IF;
              IF sftregs_row.sftregs_atts_over IS NULL
              THEN
                 sftregs_row.sftregs_atts_over := 'N';
              END IF;

              IF sftregs_row.sftregs_link_over IS NULL
              THEN
                 sftregs_row.sftregs_link_over := 'N';
              END IF;

              IF sftregs_row.sftregs_corq_over IS NULL
              THEN
                 sftregs_row.sftregs_corq_over := 'N';
              END IF;

              IF sftregs_row.sftregs_appr_over IS NULL
              THEN
                 sftregs_row.sftregs_appr_over := 'N';
              END IF;

              IF sftregs_row.sftregs_majr_over IS NULL
              THEN
                 sftregs_row.sftregs_majr_over := 'N';
              END IF;

              IF sftregs_row.sftregs_dupl_over IS NULL
              THEN
                 sftregs_row.sftregs_dupl_over := 'N';
              END IF;

              IF sftregs_row.sftregs_levl_over IS NULL
              THEN
                 sftregs_row.sftregs_levl_over := 'N';
              END IF;

              IF sftregs_row.sftregs_capc_over IS NULL
              THEN
                 sftregs_row.sftregs_capc_over := 'N';
              END IF;

              IF sftregs_row.sftregs_time_over IS NULL
              THEN
                 sftregs_row.sftregs_time_over := 'N';
              END IF;

              IF sftregs_row.sftregs_rpth_over IS NULL
              THEN
                 sftregs_row.sftregs_rpth_over := 'N';
              END IF;

              IF sftregs_row.sftregs_add_date IS NULL
              THEN
                 sftregs_row.sftregs_add_date := SYSDATE;
              END IF;

              sftregs_row.sftregs_user := USER;
              sftregs_row.sftregs_activity_date := SYSDATE;
              sql_error := 0;

-- Call local     IF NOT bwckregs.f_validcrn (sql_error, sftregs_row.sftregs_term_code)
              IF NOT f_validcrn (sql_error, sftregs_row.sftregs_term_code)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local      bwckregs.p_defstcr (add_ind);
              p_defstcr(add_ind);

-- Call local      IF NOT bwckregs.f_validlevl (sql_error, sftregs_row.sftregs_term_code)
              IF NOT f_validlevl (sql_error, sftregs_row.sftregs_term_code)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local     IF NOT bwckregs.f_validrsts (sql_error)
              IF NOT f_validrsts (sql_error)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local     IF NOT bwckregs.f_validgmod (sql_error, sftregs_row.sftregs_term_code)
              IF NOT f_validgmod(sql_error, sftregs_row.sftregs_term_code)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local              IF NOT bwckregs.f_validcredhr (sql_error)
              IF NOT f_validcredhr (sql_error)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local              IF NOT bwckregs.f_validbillhr (sql_error)
              IF NOT f_validbillhr(sql_error)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

-- Call local              IF NOT bwckregs.f_validappr (sql_error)
              IF NOT f_validappr(sql_error)
              THEN
                 raise_application_error (
                    sql_error,
                    bwcklibs.error_msg_table (sql_error)
                 );
              END IF;

              FOR stvrsts IN stkrsts.stvrstsc (sftregs_row.sftregs_rsts_code)
             LOOP
                 stvrsts_row := stvrsts;
              END LOOP;

              IF stvrsts_row.stvrsts_incl_sect_enrl = 'Y' OR
                  (stvrsts_row.stvrsts_wait_ind = 'Y' AND NVL(add_ind,'N') = 'Y')
              THEN
-- Call local        bwckregs.p_getoverride;
                   p_getoverride;
              END IF;
           END p_regschk;

-- BWCKREGS.f_getstuclas copy and paste
           FUNCTION f_getstuclas
              RETURN stvclas.stvclas_code%TYPE
           IS
           BEGIN
              RETURN sgrclsr_clas_code;
           END f_getstuclas;

-- BWCKCOMS.p_regs_etrm_chk rewrite.
          PROCEDURE p_regs_etrm_chk (
              pidm_in                      NUMBER,
              term_in                      stvterm.stvterm_code%TYPE,
              clas_code           IN OUT   SGRCLSR.SGRCLSR_CLAS_CODE%TYPE,
              multi_term_in                BOOLEAN DEFAULT FALSE,
              create_sfbetrm_in            BOOLEAN DEFAULT TRUE
           )
           IS



           BEGIN
        --
        -- Get the latest student record. Do some validation and
        -- retrieve more info based on the student record.
        -- ===================================================
              FOR sgbstdn IN sgklibs.sgbstdnc (pidm_in, term_in)
              LOOP
                 sgbstdn_rec := sgbstdn;
              END LOOP;
        --
        -- Get student's curriculum records
        --
              lcur_tab := sokccur.f_current_active_curriculum(
                                     p_pidm => pidm_in,
                                     p_lmod_code => sb_curriculum_str.f_learner,
                                     p_eff_term  => term_in);

              p_regschk (sgbstdn_rec, multi_term_in);
        --
        -- Get the class code from another package (bwckregs).
        -- ===================================================
-- Call local              clas_code := bwckregs.f_getstuclas;
              clas_code := f_getstuclas;
        --
        -- Get the latest registration record. Do some validation
        -- and retrieve more info based on the registration record.
        -- ===================================================
              row_count := 0;

              FOR sfbetrm IN sfkcurs.sfbetrmc (pidm_in, term_in)
              LOOP
                 sfbetrm_rec := sfbetrm;
                 row_count := sfkcurs.sfbetrmc%rowcount;
              END LOOP;

              IF     row_count = 0
                 AND create_sfbetrm_in
              THEN
                 /* in a multi term scenario, when no sfbetrm_rec is found, */
                 /* the record will be for a different term to the one we */
                 /* expect, so reset it */
                 sfbetrm_rec.sfbetrm_term_code := term_in;
                 p_regschk (sfbetrm_rec, 'Y', multi_term_in);
              ELSE
                 p_regschk (sfbetrm_rec, NULL, multi_term_in);
              END IF;
           END p_regs_etrm_chk;

-- BWCKCOMS.f_reg_access_still_good rewritten
              FUNCTION f_reg_access_still_good (
                  pidm_in        IN   SPRIDEN.SPRIDEN_PIDM%TYPE,
                  term_in        IN   STVTERM.STVTERM_CODE%TYPE,
                  call_path_in   IN   VARCHAR2,
                  proc_name_in   IN   VARCHAR2
               )
                  RETURN BOOLEAN
               IS
                  reg_sess_id    VARCHAR2 (7)    := '';
                  cookie_value   VARCHAR2 (2000) := '';
                  sessid         VARCHAR2 (7);
                  gtvsdax_row    gtvsdax%ROWTYPE;
                  reg_seqno      NUMBER;

                  CURSOR seqno_c ( pidm_in SPRIDEN.SPRIDEN_PIDM%TYPE,
                                   term_in STVTERM.STVTERM_CODE%TYPE)
                  IS
                     SELECT NVL (MAX (sfrstca_seq_number), 0)
                       FROM sfrstca
                      WHERE sfrstca_term_code = term_in
                        AND sfrstca_pidm (+) = pidm_in;
               BEGIN
                  IF NOT sfkfunc.f_reg_access_still_good (pidm_in, term_in, call_path_in)
                  THEN
--                     bwckfrmt.p_open_doc (proc_name_in, term_in);

--                     twbkwbis.p_dispinfo (proc_name_in, 'SESSIONEXP');
--                     twbkwbis.p_closedoc (curr_release);
                     RETURN FALSE;
                  END IF;

                  OPEN seqno_c(pidm_in, term_in);
                  FETCH seqno_c INTO reg_seqno;
                  CLOSE seqno_c;
                  sfkcurs.p_get_gtvsdax ('MAXREGNO', 'WEBREG', gtvsdax_row);

                  IF reg_seqno > NVL (gtvsdax_row.gtvsdax_external_code, 9999)
                  THEN
--                     bwckfrmt.p_open_doc (proc_name_in, term_in);
--                     twbkwbis.p_dispinfo (proc_name_in, 'MAXREGNO');
--                     twbkwbis.p_closedoc (curr_release);
                     RETURN FALSE;
                  END IF;

                  RETURN TRUE;
               END f_reg_access_still_good;


-- BWCKREGS.p_reset_dupl_waitlist_regs copy and paste
               PROCEDURE p_reset_dupl_waitlist_regs(
                            p_pidm IN sftregs.sftregs_pidm%TYPE,
                            p_term IN sftregs.sftregs_term_code%TYPE)
               IS

                  CURSOR dupl_err_crn_c (pidm sftregs.sftregs_pidm%TYPE,
                                         term sftregs.sftregs_term_code%TYPE)
                  IS
                  SELECT sftregs_crn crn, sftregs_sect_subj_code subj,
                      sftregs_sect_crse_numb crse,sftregs_sect_schd_code schd
                    FROM sftregs
                   WHERE sftregs_term_code = term
                     AND sftregs_pidm = pidm
                     AND sftregs_rec_stat = 'Q'
                     AND sftregs_error_flag = 'F'
                     AND sftregs_rmsg_cde = 'DUPL';

                  CURSOR dupl_crn_c (pidm sftregs.sftregs_pidm%TYPE,
                                     term sftregs.sftregs_term_code%TYPE,
                                     crn  sftregs.sftregs_crn%TYPE,
                                     subj sftregs.sftregs_sect_subj_code%TYPE,
                                     crse sftregs.sftregs_sect_crse_numb%TYPE,
                                     schd sftregs.sftregs_sect_schd_code%TYPE)
                  IS
                  SELECT sftregs_crn
                    FROM sftregs
                   WHERE sftregs_term_code = term
                     AND sftregs_pidm = pidm
                     AND sftregs_sect_subj_code = subj
                     AND sftregs_sect_crse_numb = crse
                     AND sftregs_sect_schd_code = schd
                     AND sftregs_crn <> crn
                     AND NVL(sftregs_rec_stat,'X') <> 'Q'
                     AND NVL(sftregs_error_flag,'N') NOT IN ('F', 'D', 'L')
                     AND NVL(sftregs_vr_status_type,'N') NOT IN ('D', 'W');

               BEGIN

               FOR dupl_err_crn_rec IN dupl_err_crn_c(p_pidm,p_term)
               LOOP
                  /* duplicate courses */
                  FOR dupl_crn_rec IN dupl_crn_c(p_pidm, p_term,
                                                 dupl_err_crn_rec.crn,
                                                 dupl_err_crn_rec.subj,
                                                 dupl_err_crn_rec.crse,
                                                 dupl_err_crn_rec.schd)
                  LOOP
                     sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                                    term_in=>p_term,
                                                    crn_in=>dupl_crn_rec.sftregs_crn,
                                                    type_in=>NULL);
                  END LOOP;
                  /* equivalent courses */
                  FOR dupl_equiv_crn IN sfkfunc.screqiv1_c(p_pidm, p_term,
                                                           dupl_err_crn_rec.crn,
                                                           dupl_err_crn_rec.subj,
                                                           dupl_err_crn_rec.crse,
                                                           dupl_err_crn_rec.schd)
                  LOOP
                     sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                                    term_in=>p_term,
                                                    crn_in=>dupl_equiv_crn.sftregs_crn,
                                                    type_in=>NULL);
                  END LOOP;
                  FOR dupl_equiv_crn IN sfkfunc.screqiv2_c(p_pidm, p_term,
                                                           dupl_err_crn_rec.crn,
                                                           dupl_err_crn_rec.subj,
                                                           dupl_err_crn_rec.crse,
                                                           dupl_err_crn_rec.schd)
                  LOOP
                     sfkmods.p_reset_sftregs_fields(pidm_in=>p_pidm,
                                                    term_in=>p_term,
                                                    crn_in=>dupl_equiv_crn.sftregs_crn,
                                                    type_in=>NULL);
                  END LOOP;
               END LOOP;

               END p_reset_dupl_waitlist_regs;

-- BWCKREGS.p_init_final_update_vars copy and paste.
          --
          -- This procedure initialises the variables required as parameters to
          -- the group edits and final update procedures.
          -- =======================================================================
          PROCEDURE p_init_final_update_vars (pidm_in IN sfrracl.sfrracl_pidm%TYPE,
                                              term_in IN sfrracl.sfrracl_term_code%TYPE)
          IS
             lv_levl_code         stvlevl.stvlevl_code%TYPE;
          BEGIN
          --
          -- student variables.
          -- =======================================================================
             FOR sgbstdn IN sgklibs.sgbstdnc (pidm_in, term_in)
             LOOP
                sgbstdn_row := sgbstdn;
             END LOOP;

          --
          -- term variables.
          -- =======================================================================
             OPEN soklibs.sobterm_webc (term_in);
             FETCH soklibs.sobterm_webc INTO sobterm_row;

          --
          -- student class.
          -- =======================================================================
             IF soklibs.sobterm_webc%NOTFOUND
             THEN
                atmp_hrs := NULL;
             ELSE
                IF sobterm_row.sobterm_incl_attmpt_hrs_ind = 'Y'
                THEN
                   atmp_hrs := 'A';
                ELSE
                   atmp_hrs := NULL;
                END IF;
             END IF;

             CLOSE soklibs.sobterm_webc;
             sgrclsr_clas_code := NULL;
             clas_desc := NULL;
          --
             lv_levl_code := sokccur.f_curriculum_value(
                                               p_pidm=>pidm_in,
                                               p_lmod_code => sb_curriculum_str.f_learner,
                                               p_term_code => term_in,
                                               p_key_seqno => 99,
                                               p_eff_term => term_in,
                                               p_order => 1,
                                               p_field => 'LEVL');
             soklibs.p_class_calc (
                pidm_in,
                lv_levl_code,
                term_in,
                atmp_hrs,
                sgrclsr_clas_code,
                clas_desc
             );

          --
          -- registration term variables.
          -- =======================================================================
             FOR sfbetrm IN sfkcurs.sfbetrmc (pidm_in, term_in)
             LOOP
                sfbetrm_row := sfbetrm;
             END LOOP;
          END p_init_final_update_vars;

-- BWCKREGS.p_allcrsechk rewritten.
          --
          -- P_ALLCRSECHK
          -- This procedure validates relationships between all
          -- courses that the student is registering for.
          -- ==================================================
          PROCEDURE p_allcrsechk (
             term_in                IN       sftregs.sftregs_term_code%TYPE,
             called_by_in           IN       VARCHAR2 DEFAULT 'ADD_DROP',
             capp_tech_error_out    OUT      VARCHAR2,
             drop_problems_in_out   IN OUT   sfkcurs.drop_problems_rec_tabtype,
             drop_failures_in_out   IN OUT   sfkcurs.drop_problems_rec_tabtype,
             multi_term_list_in     IN       OWA_UTIL.ident_arr
        )
          IS
             genpidm           spriden.spriden_pidm%TYPE;
             error_rec         sftregs%ROWTYPE;
             error_flag        sftregs.sftregs_error_flag%TYPE;
             tmst_flag         sftregs.sftregs_error_flag%TYPE;
             capp_tech_error   VARCHAR2 (4);
             minh_error        VARCHAR2 (1) := 'N';
             source_system     VARCHAR2 (2);
             may_drop_last     BOOLEAN;
             sftregs_row       sfkcurs.sftregsc%ROWTYPE;
             call_type         VARCHAR2(1);
-- Add OWAUTIL array for calling SFKEDIT
           multi_term_list_in_mod           OWA_UTIL.ident_arr;
           error_message                    VARCHAR2(200 CHAR);

        BEGIN
--           IF NOT twbkwbis.f_validuser (global_pidm)
--           THEN
--              RETURN;
--           END IF;

--           IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--           THEN
--              genpidm :=
--                  TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--           ELSE
              genpidm := global_pidm;
--           END IF;

           IF called_by_in = 'ADD_DROP'
           THEN
              source_system := 'WA';
              call_type := 'F';
           ELSE
              source_system := 'WC';
              call_type := 'S';
           END IF;

-- Call local          bwckregs.p_init_final_update_vars(genpidm, term_in);
          p_init_final_update_vars(genpidm, term_in);

--           DELETE
--             FROM twgrwprm
--            WHERE twgrwprm_pidm = genpidm
--              AND twgrwprm_param_name = 'ERROR_FLAG'
--              AND twgrwprm_param_value <> 'M';

        -- =======================================================
        -- This procedure Checks all courses in sftregs for the
        -- current pidm/term:
        -- Checks for duplicate courses.
        -- Checks for time conflicts.
        -- Checks for pre-requisites.
        -- Checks for co-requisites.
        -- Checks for links.
        -- Checks for max hours.
        -- Checks for mutual exsclusion.
        -- =======================================================

-- Setting sobterm_row to override preq's and coreq's and populate the OWA_UTIL.ident_arr
           sobterm_row.sobterm_corq_severity := 'N';
           sobterm_row.sobterm_preq_severity := 'N';
           multi_term_list_in_mod (1) := term_in;

           sfkedit.p_web_group_edits (
              genpidm,
              term_in,
              call_type,
              NVL (sobterm_row.sobterm_dupl_severity, 'N'),
              NVL (sobterm_row.sobterm_time_severity, 'N'),
              NVL (sobterm_row.sobterm_corq_severity, 'N'),
              NVL (sobterm_row.sobterm_link_severity, 'N'),
              NVL (sobterm_row.sobterm_preq_severity, 'N'),
              NVL (sobterm_row.sobterm_maxh_severity, 'N'),
              NVL (sobterm_row.sobterm_minh_severity, 'N'),
              NVL (sobterm_row.sobterm_mexc_severity, 'N'),
              sfbetrm_row.sfbetrm_mhrs_over,
              sfbetrm_row.sfbetrm_min_hrs,
              SYS_CONTEXT ('USERENV', 'SESSIONID'),
              source_system,
              error_flag,
              capp_tech_error,
              minh_error,
              multi_term_list_in_mod
           );

           IF capp_tech_error IS NOT NULL
           THEN
              capp_tech_error_out := capp_tech_error;
              RETURN;
           END IF;

           -- Store the fact that a minimum hours error occurred, and quit all processing.
-- Removing minh_error check          IF minh_error = 'Y'
-- No need when student takes 1 class only          THEN
--              twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
--              RETURN;
--           END IF;

           IF called_by_in = 'ADD_DROP'
           THEN
              IF error_flag = 'Y'
              THEN
                 /* update new courses to indicate they are in error */
                 /* don't alter the rec_stat from 'N' */
                 UPDATE sftregs
                    SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                        sftregs_remove_ind = 'Y',
                        sftregs_user = goksels.f_get_ssb_id_context,
                        sftregs_activity_date = SYSDATE
                  WHERE sftregs_term_code = term_in
                    AND sftregs_pidm = genpidm
                    AND sftregs_error_flag = 'F'
                    AND sftregs_rec_stat = 'N';
                 p_reset_dupl_waitlist_regs(genpidm, term_in);
              END IF;
              bwcklibs.p_build_drop_problems_list ( genpidm,
                                                    term_in,
                                                    drop_problems_in_out,
                                                    drop_failures_in_out
                                                    );

           ELSIF error_flag <> 'Y'          /* change class options - and no errors */
           THEN
-- Avoiding this from stopping exchange             may_drop_last := sfkdrop.f_drop_last ('W');
              may_drop_last := TRUE;
              sfkedit.p_update_regs (
                 genpidm,
                 term_in,
                 SYSDATE,
                 sgrclsr_clas_code,
                 sgbstdn_row.sgbstdn_styp_code,
                 NVL (sobterm_row.sobterm_capc_severity, 'N'),
                 sobterm_row.sobterm_tmst_calc_ind,
                 sfbetrm_row.sfbetrm_tmst_maint_ind,
                 sfbetrm_row.sfbetrm_tmst_code,
                 may_drop_last,
                 source_system,
                 error_rec,
                 error_flag,
                 tmst_flag
              );
           END IF;
        END p_allcrsechk;

-- BWCKCOMS.p_group_edits rewritten.
        --
        -- This procedure performs group edits for all selected terms
        -- =====================================================================
          PROCEDURE p_group_edits (
           term_in                  IN       OWA_UTIL.ident_arr,
           pidm_in                  IN       spriden.spriden_pidm%TYPE,
           etrm_done_in_out         IN OUT   BOOLEAN,
           capp_tech_error_in_out   IN OUT   VARCHAR2,
           drop_problems_in_out     IN OUT   sfkcurs.drop_problems_rec_tabtype,
           drop_failures_in_out     IN OUT   sfkcurs.drop_problems_rec_tabtype
          )
          IS
             multi_term   BOOLEAN := TRUE;
             clas_code             sgrclsr.sgrclsr_clas_code%TYPE;
          BEGIN
            multi_term := FALSE;


        --
        -- Do batch validation on all registration records.
        -- ===================================================

           FOR i IN 1 .. term_in.COUNT
           LOOP
              IF multi_term
              THEN
        --
        -- Get the latest student and registration records.
        -- ===================================================
-- Call local                bwckcoms.p_regs_etrm_chk (
                 p_regs_etrm_chk (
                    pidm_in,
                    term_in (i),
                    clas_code,
                    multi_term,
                    create_sfbetrm_in   => FALSE
                 );
              ELSIF NOT etrm_done_in_out
              THEN
-- Call local                bwckcoms.p_regs_etrm_chk (
                 p_regs_etrm_chk (
                    pidm_in,
                    term_in (i),
                    clas_code,
                    create_sfbetrm_in   => FALSE
                 );
                 etrm_done_in_out := TRUE;
              END IF;

              p_allcrsechk (
                 term_in (i),
                 'ADD_DROP',
                 capp_tech_error_in_out,
                 drop_problems_in_out,
                 drop_failures_in_out,
                 term_in
              );

              IF capp_tech_error_in_out IS NOT NULL
              THEN
                 RETURN;
              END IF;

              -- Check for minimum hours error
--              IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'M'
--              THEN
--                 sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
--                 sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
--              END IF;
           END LOOP;
        END p_group_edits;

-- BWCKREGS.p_regsfees rewritten.
             PROCEDURE p_regsfees
             IS
                fee_assess_eff_date   DATE;
                fee_assess_date       VARCHAR2 (50);
                return_status         NUMBER                             := 0;
                genpidm               spriden.spriden_pidm%TYPE;
--                sessionid             twgrwprm.twgrwprm_param_value%TYPE;
             BEGIN
--                IF NOT twbkwbis.f_validuser (global_pidm)
--                THEN
--                   RETURN;
--                END IF;

--                IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--                THEN
--                   genpidm :=
--                     TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--                ELSE
                   genpidm := global_pidm;
--                END IF;

--                sessionid := SYS_CONTEXT ('USERENV', 'SESSIONID');
                regs_date := bwcklibs.f_getregdate;
                term := bwcklibs.f_getterm;

                bwcklibs.p_getsobterm(term,sobterm_row);

                fee_assess_eff_date :=
                  NVL (
                     GREATEST (
                        TRUNC (sobterm_row.sobterm_fee_assessmnt_eff_date),
                        TRUNC (regs_date)
                     ),
                     TRUNC (regs_date)
                  );

                SFKFEES.p_processfeeassessment (
                   term,
                   genpidm,
                   fee_assess_eff_date,
                   TRUNC (SYSDATE),
                   'R',                -- rule entry type - 'R'egular Registration Rules
                   'Y',                              -- create TBRACCD records indicator
                   'BWCKREGS',                 -- source program invoking fee assessment
                   'Y',                                              -- commit indicator
                   fee_assess_date,
                   'N',
                   return_status
                );

             END p_regsfees;

-- BWCKSAMS.p_immediate_fee_assess rewritten.
           PROCEDURE p_immediate_fee_assess (
              term      IN   stvterm.stvterm_code%TYPE,
              pidm_in   IN   spriden.spriden_pidm%TYPE
           )
           IS
              chk_sfrbtch_rec   VARCHAR2 (1);
              sfrbtch_row       sfrbtch%ROWTYPE;
           BEGIN
              bwcklibs.p_getsobterm (term, sobterm_row);

              -- is online fee assessment enabled?
              IF sobterm_row.sobterm_fee_assess_vr = 'Y'
              AND sobterm_row.sobterm_fee_assessment = 'Y'
              THEN
                 -- have any assessable actions taken place
                 -- since last assessment?
                 OPEN chk_sfrbtch (term, pidm_in);
                 FETCH chk_sfrbtch INTO chk_sfrbtch_rec;

                 IF chk_sfrbtch%FOUND
                 THEN
-- Call local           bwckregs.p_regsfees;
                    p_regsfees;
                    --BOGW C3SC ADDED GRS 12/09/2008
                    IF sobterm_row.SOBTERM_EXEMP_ONLINE_IND = 'Y'
                     AND sobterm_row.SOBTERM_EXEMPT_VR_IND = 'Y'
                    THEN
                      sv_state_aid_appl_bp.p_applyexemptions(term_in=>term,
                                                            pidm_in=>pidm_in
                                                            );
                    END IF;
                   -- C3SC BOGW END
                    sfrbtch_row.sfrbtch_pidm := pidm_in;
                    sfrbtch_row.sfrbtch_term_code := term;
                    bwcklibs.p_del_sfrbtch (sfrbtch_row);
                 END IF;

                 CLOSE chk_sfrbtch;
              END IF;
        --
           END p_immediate_fee_assess;

-- BWCKSAMS.p_regsresult rewritten
        PROCEDURE p_regsresult (
           term_in                  IN       OWA_UTIL.ident_arr,
           errs_count               IN OUT   NUMBER,
           regs_count               IN OUT   NUMBER,
           wait_count               IN OUT   NUMBER,
           first_pass               IN       BOOLEAN DEFAULT FALSE,
           reg_access_allowed_out   OUT      BOOLEAN,
           capp_tech_error_in_out   IN OUT   VARCHAR2,
           drop_result_label_in     IN       twgrinfo.twgrinfo_label%TYPE DEFAULT NULL,
           drop_problems_in         IN       sfkcurs.drop_problems_rec_tabtype,
           drop_failures_in         IN       sfkcurs.drop_problems_rec_tabtype
        )
        IS
           tot_credit_hr               sftregs.sftregs_credit_hr%TYPE         := 0;
           tot_bill_hr                 sftregs.sftregs_bill_hr%TYPE           := 0;
           tot_ceu                     sftregs.sftregs_credit_hr%TYPE         := 0;
           ssbxlst_count               NUMBER                                 := 0;
           gmod_code                   stvgmod.stvgmod_code%TYPE;
           gmod_desc                   stvgmod.stvgmod_desc%TYPE;
--           ssbxlst_row                 ssbxlst%ROWTYPE;
           wait_crn                    OWA_UTIL.ident_arr;
           sdax_rsts_code              VARCHAR2 (2);
           i                           INTEGER;
           j                           INTEGER;
           k                           INTEGER;
           next_rec                    BOOLEAN;
           sftregs_rec                 sftregs%ROWTYPE;
           sgbstdn_rec                 sgbstdn%ROWTYPE;
           sfbetrm_rec                 sfbetrm%ROWTYPE;
           genpidm                     spriden.spriden_pidm%TYPE;
           call_path                   VARCHAR2 (1);
           term                        stvterm.stvterm_code%TYPE              := NULL;
           multi_term                  BOOLEAN                                := TRUE;
           crse_title                  ssrsyln.ssrsyln_long_course_title%TYPE;
           change_class_options_proc   VARCHAR2 (100);
           msg                         VARCHAR2 (200);
--           wmnu_rec                    twgbwmnu%ROWTYPE;
           heading_displayed           BOOLEAN                                := FALSE;
           row_count_out               NUMBER                                 := 0;
           drops_table_open            BOOLEAN                                := FALSE;
           last_class_error            BOOLEAN := FALSE;
           update_error                BOOLEAN := FALSE;
           crndirect                   VARCHAR2(1) :=
                                          NVL(SUBSTR(bwcklibs.f_getgtvsdaxrule('CRNDIRECT','WEBREG'),1,1),'N');
           update_wait_count           NUMBER  := 0;
           waitlist_option             BOOLEAN := FALSE;
           minh_error                  BOOLEAN := FALSE;
           minh_restriction            BOOLEAN := FALSE;

        -- DegreeWorks: Variables for retrieving PreReq_Chk_Method
           sect_cv                     SYS_REFCURSOR;
--           sect_rec                    sb_section.section_rec;
--           error_msg                   SFTDWER.SFTDWER_ERROR_MESSAGE%TYPE;
        -- Cursor to get data from DegreeWorks Temporary Registration Error Message Table
/*           CURSOR csr_sftdwer(c_pidm IN SFTDWER.SFTDWER_PIDM%TYPE,
                              c_term IN SFTDWER.SFTDWER_TERM_CODE%TYPE,
                              c_crn  IN SFTDWER.SFTDWER_CRN%TYPE)
           IS
           SELECT sftdwer_error_message
           FROM   sftdwer
           WHERE  sftdwer_pidm = c_pidm
           AND    sftdwer_term_code = c_term
           AND    sftdwer_crn = c_crn
           ORDER BY sftdwer_seq_no;
*/
        -- SP changes
           lv_sgrstsp_eff_term        stvterm.stvterm_code%TYPE              := NULL;
           CURSOR get_sgrstsp_eff_term_c (p_pidm SPRIDEN.SPRIDEN_PIDM%TYPE,
                                      p_term STVTERM.STVTERM_CODE%TYPE,
                                      p_sp_seqno SGRSTSP.SGRSTSP_KEY_SEQNO%TYPE)
               IS
           SELECT SGRSTSP_TERM_CODE_EFF
             FROM SGRSTSP
            WHERE SGRSTSP_PIDM = p_pidm
              AND SGRSTSP_KEY_SEQNO = p_sp_seqno
              AND SGRSTSP_TERM_CODE_EFF  = (SELECT MAX(SGRSTSP_TERM_CODE_EFF )
                                              FROM SGRSTSP
                                             WHERE SGRSTSP_PIDM = p_pidm
                                               AND SGRSTSP_KEY_SEQNO = p_sp_seqno
                                               AND SGRSTSP_TERM_CODE_EFF <= p_term);
        BEGIN
            --
            -- Validate the user.
            -- ==================================================
--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

            --
            -- Check if procedure is being run from facweb or stuweb.
            -- If stuweb, then use the pidm of the user. Else, use
            -- the pidm that corresponds to the student that was
            -- specified by the user.
            -- ==================================================
--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--                  call_path := 'F';
--                  change_class_options_proc := 'bwlkfrad.P_FacChangeCrseOpt';
--                  OPEN twbklibs.getmenuc ('bwlkfrad.P_FacAddDropCrse');
--               ELSE
                  call_path := 'S';
                  genpidm := global_pidm;
                  change_class_options_proc := 'bwskfreg.P_ChangeCrseOpt';
--                  OPEN twbklibs.getmenuc ('bwskfreg.P_AddDropCrse');
--               END IF;

--               FETCH twbklibs.getmenuc INTO wmnu_rec;
--               CLOSE twbklibs.getmenuc;
               term := term_in (term_in.COUNT);

               IF term_in.COUNT = 1
               THEN
                  multi_term := FALSE;
               END IF;

            --
            -- Check to see if user tried to reduce registered
            -- hours below a defined minimum.
            -- ==================================================
/* Not doing minimum hours checking               IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'M'
               THEN
                   minh_error := TRUE;
                   DELETE
                       FROM twgrwprm
                       WHERE twgrwprm_pidm = genpidm
                       AND twgrwprm_param_name = 'ERROR_FLAG';
                   COMMIT;
               END IF;
*/
            --
            -- Check to see if user tried to drop last class and
            -- it is not allowed.
            -- ==================================================
 /* Bypass drop last class check
               IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'L'
               THEN
                   last_class_error := TRUE;
                   DELETE
                       FROM twgrwprm
                       WHERE twgrwprm_pidm = genpidm
                       AND twgrwprm_param_name = 'ERROR_FLAG';
                   COMMIT;
               END IF;
*/
            --
            -- Check to see if user tried to update registration
            -- and sfrstcr rows were not found to match sftregs.
            -- ==================================================
/* We are not supporting update at this time, look at if we must change reg status later.
               IF NVL (twbkwbis.f_getparam (genpidm, 'ERROR_FLAG'), 'N') = 'D'
               THEN
                   update_error := TRUE;
                   DELETE
                       FROM twgrwprm
                       WHERE twgrwprm_pidm = genpidm
                       AND twgrwprm_param_name = 'ERROR_FLAG';
                   COMMIT;
               END IF;
*/            --
            -- If this procedure was called from the menu, ie. no work
            -- yet done to sftregs, remove any remnant sftregs records,
            -- and copy all sfrstcr rows into sftregs.
            -- Also,
            -- This stores the access_id we create in SFRRACL.
            -- This will let other sessions that are already in a
            -- registration page know that we have taken 'possession'
            -- of sftregs, and if they try to update, they will be
            -- expelled from their registration editing session.
            -- ==================================================
               reg_access_allowed_out := TRUE;

               IF (first_pass OR last_class_error OR update_error or minh_error)
                 AND capp_tech_error_in_out IS NULL
               THEN
                  FOR i IN 1 .. term_in.COUNT
                  LOOP
                     IF NOT bwckregs.f_registration_access (
                           genpidm,
                           term_in (i),
                           call_path || global_pidm
                        )
                     THEN
                        capp_tech_error_in_out :=
                            sfkfunc.f_get_capp_tech_error(genpidm, term_in(i));
                        IF capp_tech_error_in_out IS NOT NULL
                        THEN
                            EXIT;
                        END IF;
                        reg_access_allowed_out := FALSE;
                        RETURN;
                     END IF;
                  END LOOP;
               END IF;
               IF capp_tech_error_in_out IS NOT NULL
               THEN
                  IF capp_tech_error_in_out = 'PIPE'
                  THEN
                     msg := 'PIPE_ERROR';
                  ELSE
                     msg := 'CAPP_ERROR';
                  END IF;

            --
            -- Return to the menu.
            -- =================================================
/*                 twbkwbis.p_genmenu (
                     SUBSTR (
                        wmnu_rec.twgbwmnu_back_url,
                        INSTR (wmnu_rec.twgbwmnu_back_url, '/', -1) + 1
                     ),
                     msg,
                     message_type   => 'ERROR'
                  );
*/
                  raise_application_error(-20001, 'Banner CAPP error has occurred');
                  RETURN;
               END IF;
-- Omitting all to the very end of the procedure!

           FOR i IN 1 .. term_in.COUNT
           LOOP
        --
        -- delete rows that have been flagged as in error, or waitlist
        -- eligible, or the user has requested to be dropped
        -- ============================================================
              sfkmods.p_delete_sftregs_removed (genpidm, term_in (i));
        --
        -- perform fee assessment, if applicable
        -- =================================================
              p_immediate_fee_assess (term_in (i), genpidm);

           END LOOP;
        END p_regsresult;


--BWCKREGS.p_addcrse
         PROCEDURE p_addcrse (
            stcr_row        sftregs%ROWTYPE,
            subj            ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
            crse            ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
            seq             ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
            start_date_in   VARCHAR2 DEFAULT NULL,
            end_date_in     VARCHAR2 DEFAULT NULL
         )
         IS
-- Use global            sgbstdn_rec         sgbstdn%ROWTYPE;
            stcr_err_ind        sftregs.sftregs_error_flag%TYPE;
            appr_err            sftregs.sftregs_error_flag%TYPE;
            local_ssbsect_row   ssbsect%ROWTYPE;

            CURSOR sobptrmc (term VARCHAR2, ptrm VARCHAR2)
            IS
               SELECT *
                 FROM sobptrm
                WHERE sobptrm_term_code = term
                  AND sobptrm_ptrm_code = ptrm;

            CURSOR stvssts_c(ssts_code_in ssbsect.ssbsect_ssts_code%type)
            IS
               SELECT 'STAT',
                      sb_registration_msg.f_get_message('STAT', 1),
                      'F'
                 FROM stvssts
                WHERE stvssts_code = ssts_code_in
                  AND stvssts_reg_ind = 'N';

            local_sobptrm_row   sobptrmc%ROWTYPE;
         BEGIN
      --      ssbsect_row.ssbsect_subj_code := subj;
      --      ssbsect_row.ssbsect_crse_numb := crse;
      --      ssbsect_row.ssbsect_seq_numb := seq;
            row_count := 0;
-- Use global             sgbstdn_rec := bwckcoms.sgbstdn_rec;

-- Call local            bwckregs.p_init_final_update_vars(stcr_row.sftregs_pidm,
            p_init_final_update_vars(stcr_row.sftregs_pidm,
                                              stcr_row.sftregs_term_code);

            IF stcr_row.sftregs_crn IS NOT NULL
            THEN
               FOR stcr_rec IN sfkcurs.sftregsc (
                                  stcr_row.sftregs_pidm,
                                  stcr_row.sftregs_term_code
                               )
               LOOP
                  IF stcr_rec.sftregs_crn = stcr_row.sftregs_crn
                  THEN
                     row_count := sfkcurs.sftregsc%rowcount;
                     EXIT;
                  END IF;
               END LOOP;

               IF row_count <> 0
               THEN
                  raise_application_error (-20525, bwcklibs.error_msg_table (-20525));
               END IF;
            END IF;

            old_stvrsts_row.stvrsts_incl_sect_enrl := 'N';
            old_stvrsts_row.stvrsts_wait_ind := 'N';
            old_sftregs_row.sftregs_credit_hr := 00.00;
            p_regschk (stcr_row, subj, crse, seq, NULL, 'Y');

            sftregs_row.sftregs_vr_status_type :=
                sfkfunc.f_get_rsts_type(sftregs_row.sftregs_rsts_code);

      --
      -- determine the sftregs start and end dates,
      -- if OLR, they will have been entered by the user and passed in,
      -- if trad course, they will be found in ssbsect, or sobptrm.
      -----------------------------------------------------------------
            OPEN ssklibs.ssbsectc (stcr_row.sftregs_crn, stcr_row.sftregs_term_code);
            FETCH ssklibs.ssbsectc INTO local_ssbsect_row;
            CLOSE ssklibs.ssbsectc;
            sftregs_row.sftregs_start_date :=
              NVL (
                 TO_DATE (start_date_in, twbklibs.date_input_fmt),
                 local_ssbsect_row.ssbsect_ptrm_start_date
              );
            sftregs_row.sftregs_completion_date :=
              NVL (
                 TO_DATE (end_date_in, twbklibs.date_input_fmt),
                 local_ssbsect_row.ssbsect_ptrm_end_date
              );

            IF sftregs_row.sftregs_start_date IS NULL
            THEN
               OPEN sobptrmc (
                  stcr_row.sftregs_term_code,
                  stcr_row.sftregs_ptrm_code
               );
               FETCH sobptrmc INTO local_sobptrm_row;
               CLOSE sobptrmc;
               sftregs_row.sftregs_start_date :=
                                               local_sobptrm_row.sobptrm_start_date;
               sftregs_row.sftregs_completion_date :=
                                                 local_sobptrm_row.sobptrm_end_date;
            END IF;

      -----------------------------------------------------------------

            sftregs_row.sftregs_number_of_units :=
                                          local_ssbsect_row.ssbsect_number_of_units;
            sftregs_row.sftregs_dunt_code := local_ssbsect_row.ssbsect_dunt_code;

            OPEN stvssts_c(ssbsect_row.ssbsect_ssts_code);
            FETCH stvssts_c INTO
                sftregs_row.sftregs_rmsg_cde,
                sftregs_row.sftregs_message,
                sftregs_row.sftregs_error_flag;
            CLOSE stvssts_c;

            IF (
                      sftregs_row.sftregs_error_flag = 'F'
                  AND sftregs_row.sftregs_rmsg_cde = 'STAT'
               )
            THEN
               stcr_err_ind := 'Y';
            ELSE
      -- =======================================================
      -- This procedure performs edits based on a single course
      -- Checks approval code restrictions.
      -- Checks level restrictions.
      -- Checks college restrictions.
      -- Checks degree restrictions.
      -- Checks program restrictions.
      -- Checks major restrictions.
      -- Checks campus restrictions.
      -- Checks class restrictions.
      -- Checks repeat restrictions
      -- Checks capacity.
      -- =======================================================
               sfkedit.p_pre_edit (
                  sftregs_row,
                  stcr_err_ind,
                  appr_err,
                  old_stvrsts_row.stvrsts_incl_sect_enrl,
                  old_stvrsts_row.stvrsts_wait_ind,
                  stvrsts_row.stvrsts_incl_sect_enrl,
                  stvrsts_row.stvrsts_wait_ind,
                  sobterm_row.sobterm_appr_severity,
                  sobterm_row.sobterm_levl_severity,
                  sobterm_row.sobterm_coll_severity,
                  sobterm_row.sobterm_degree_severity,
                  sobterm_row.sobterm_program_severity,
                  sobterm_row.sobterm_majr_severity,
                  sobterm_row.sobterm_camp_severity,
                  sobterm_row.sobterm_clas_severity,
                  sobterm_row.sobterm_capc_severity,
                  sobterm_row.sobterm_rept_severity,
                  sobterm_row.sobterm_rpth_severity,
                  sobterm_row.sobterm_dept_severity,
                  sobterm_row.sobterm_atts_severity,
                  sobterm_row.sobterm_chrt_severity,
                  sgrclsr_clas_code,
                  scbcrse_row.scbcrse_max_rpt_units,
                  scbcrse_row.scbcrse_repeat_limit,
                  ssbsect_row.ssbsect_sapr_code,
                  ssbsect_row.ssbsect_reserved_ind,
                  ssbsect_row.ssbsect_seats_avail,
                  ssbsect_row.ssbsect_wait_count,
                  ssbsect_row.ssbsect_wait_capacity,
                  ssbsect_row.ssbsect_wait_avail,
                  'WA'
               );
            END IF;
      -- C3SC, GR 19/02/06, Added
           -- bypass AUTH REQD errors if override flag is O

          sv_auth_addcodes_bp.P_rest_message(p_error_flag_in=>sftregs_row.sftregs_error_flag,
                                             p_rmsg_cde_in_out=>sftregs_row.sftregs_rmsg_cde,  --C3SC GRS 04/12/2009 ADD
                                             p_error_mesg_in_out=>sftregs_row.sftregs_message
                                             );
          -- End C3SC

        IF ssbsect_row.ssbsect_tuiw_ind = 'Y'
            THEN
               sftregs_row.sftregs_waiv_hr := '0';
            ELSE
               sftregs_row.sftregs_waiv_hr := sftregs_row.sftregs_bill_hr;
            END IF;

            sftregs_row.sftregs_activity_date := SYSDATE;

            IF stcr_err_ind = 'Y'
            THEN
               sftregs_row.sftregs_rsts_code :=
                                          SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2);
               sftregs_row.sftregs_vr_status_type :=
                   sfkfunc.f_get_rsts_type(sftregs_row.sftregs_rsts_code);
               sftregs_row.sftregs_remove_ind := 'Y';
               sftregs_row.sftregs_rec_stat := 'N';
            END IF;

            bwcklibs.p_add_sftregs (sftregs_row);
         END p_addcrse;

-- BWCKREGS.p_getsection copy and paste.
        -- ================================================================
        -- This procedure gets section information for a given crn.
        -- ================================================================
           PROCEDURE p_getsection (
              term_in         sftregs.sftregs_term_code%TYPE DEFAULT NULL,
              crn             sftregs.sftregs_crn%TYPE DEFAULT NULL,
              subj      OUT   ssbsect.ssbsect_subj_code%TYPE,
              crse      OUT   ssbsect.ssbsect_crse_numb%TYPE,
              seq       OUT   ssbsect.ssbsect_seq_numb%TYPE
           )
           IS
           BEGIN
        --
        -- Open the section cursor; get the section row;
        -- set the output parameters.
        -- ======================================================
              OPEN ssklibs.ssbsectc (crn, term_in);
              FETCH ssklibs.ssbsectc INTO ssbsect_row;

              IF ssklibs.ssbsectc%FOUND
              THEN
                 subj := ssbsect_row.ssbsect_subj_code;
                 crse := ssbsect_row.ssbsect_crse_numb;
                 seq := ssbsect_row.ssbsect_seq_numb;
              ELSE
                 subj := NULL;
                 crse := NULL;
                 seq := NULL;
              END IF;

              CLOSE ssklibs.ssbsectc;
           END p_getsection;
"""
        def plsql3 = """
-- BWCKSAMS.F_RegsStu rewritten
           FUNCTION F_RegsStu (
              pidm_in     IN   spriden.spriden_pidm%TYPE,
              term        IN   stvterm.stvterm_code%TYPE DEFAULT NULL,
              proc_name   IN   VARCHAR2 DEFAULT NULL
           )
              RETURN BOOLEAN
           IS
              sql_err                     NUMBER;
              gen_proc_name               VARCHAR2 (80);
              genpidm                     spriden.spriden_pidm%TYPE;
              restrict_time_ticket        VARCHAR2 (1);
              restrict_time_ticket_code   gtvsdax.gtvsdax_internal_code%TYPE
                                                                        := 'WEBRESTTKT';
              web_vr_ind                  VARCHAR2 (1)                       := 'W';
           BEGIN
        --
        -- Establish the student pidm.
        -- ==================================================
--              IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
--              THEN
--                 genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (pidm_in, 'STUPIDM'), '999999999');
--              ELSE
                 genpidm := pidm_in;
--              END IF;

        --
        --
        -- ==================================================
              bwcklibs.p_initvalue (pidm_in, term, NULL, SYSDATE, NULL, NULL);

        --
        -- Check if person is deceased.
        -- ==================================================
              IF bwckregs.f_deceasedperscheck (sql_err, genpidm)
              THEN
                 RAISE bwcklibs.deceased_person_err;
              END IF;

        --
        -- Do either registration management control checking
        -- or time ticket checking, based on a gtvsdax flag.
        -- ==================================================
              internal_code := 'WEBMANCONT';
              gtvsdax_group := 'WEBREG';
              OPEN sfkcurs.get_gtvsdaxc (internal_code, gtvsdax_group);
              FETCH sfkcurs.get_gtvsdaxc INTO use_man_control;
              CLOSE sfkcurs.get_gtvsdaxc;
              OPEN sfkcurs.get_gtvsdaxc (restrict_time_ticket_code, gtvsdax_group);
              FETCH sfkcurs.get_gtvsdaxc INTO restrict_time_ticket;
              CLOSE sfkcurs.get_gtvsdaxc;

              IF NOT sfkrctl.f_check_reg_appointment (
                    genpidm,
                    term,
                    NVL (use_man_control, 'N'),
                    NVL (restrict_time_ticket, 'N'),
                    web_vr_ind
                 )
              THEN
                 IF NVL (use_man_control, 'N') <> 'Y'
                 THEN
                    RAISE bwcklibs.time_ticket_err;
                 ELSE
                    RAISE bwcklibs.mgmt_control_err;
                 END IF;
              END IF;

        --
        -- Get the student information record.
        -- ==================================================
              FOR sgbstdn IN sgklibs.sgbstdnc (genpidm, term)
              LOOP
                 sgbstdn_rec := sgbstdn;
              END LOOP;

        --
        -- Check the student record.
        -- ==================================================
-- Call local             bwckregs.p_regschk (sgbstdn_rec);
              p_regschk(sgbstdn_rec);
              row_count := 0;

        --
        -- Get the registration header record.
        -- ==================================================
              FOR sfbetrm IN sfkcurs.sfbetrmc (genpidm, term)
              LOOP
                 sfbetrm_rec := sfbetrm;
                 row_count := sfkcurs.sfbetrmc%rowcount;
              END LOOP;

        --
        -- Check the registration header record if it exists.
        -- If not it will be created elsewhere when required.
        -- ==================================================
              IF row_count <> 0
              THEN
-- Call local                 bwckregs.p_regschk (sfbetrm_rec);
                p_regschk(sfbetrm_rec);
              END IF;

        --
        --
        -- ==================================================
--              sfkvars.regs_allowed := f_validregdate (term);
                sfkvars.regs_allowed := BWCKSAMS.f_validregdate (term);
              RETURN TRUE;
           EXCEPTION
              WHEN bwcklibs.time_ticket_err
              THEN
-- Raise error and comment out the web page generation.
--                 IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
--                 THEN
        /*
         * FACWEB-specific code
         */
--                    IF NOT bwlkilib.f_add_drp (term)
--                    THEN
--                       NULL;
--                    END IF;
--                 ELSE
        /*
         * STUWEB-specific code
         */

--                    IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
--                    THEN
--                       NULL;
--                    END IF;
--                 END IF;                      /* END stu/fac specific code sections */
/*
                 bwckfrmt.p_open_doc (proc_name, term);
                 --
                 row_count := 0;

                 FOR time_rec IN sfkcurs.timetksc (genpidm, term)
                 LOOP
                    row_count := sfkcurs.timetksc%rowcount;

                    IF sfkcurs.timetksc%rowcount > 0
                    THEN
                       IF sfkcurs.timetksc%rowcount = 1
                       THEN
                          twbkwbis.p_dispinfo (proc_name, 'TTTIMES');
                          twbkfrmt.p_tableopen (
                             'DATADISPLAY',
                             cattributes   => 'SUMMARY="' ||
                                                 g\$_nls.get ('BWCKSAM1-0000',
                                                    'SQL',
                                                    'This layout table is used to present time ticketing error related information') ||
                                                 '."'
                          );
                          twbkfrmt.p_tablerowopen;
                          twbkfrmt.p_tabledataheader (
                             g\$_nls.get ('BWCKSAM1-0001', 'SQL', 'From')
                          );
                          twbkfrmt.p_tabledataheader (
                             g\$_nls.get ('BWCKSAM1-0002', 'SQL', 'Begin Time')
                          );
                          twbkfrmt.p_tabledataheader (
                             g\$_nls.get ('BWCKSAM1-0003', 'SQL', 'To')
                          );
                          twbkfrmt.p_tabledataheader (
                             g\$_nls.get ('BWCKSAM1-0004', 'SQL', 'End Time')
                          );
                          twbkfrmt.p_tablerowclose;
                       END IF;

                       twbkfrmt.p_tablerowopen;
                       twbkfrmt.p_tabledata (
                          TO_CHAR (
                             time_rec.rec_sfrwctl_begin_date,
                             twbklibs.date_display_fmt
                          )
                       );
                       twbkfrmt.p_tabledata (
                          TO_CHAR (
                             TO_DATE (time_rec.rec_sfrwctl_hour_begin, 'HH24MI'),
                             twbklibs.twgbwrul_rec.twgbwrul_time_fmt
                          )
                       );
                       twbkfrmt.p_tabledata (
                          TO_CHAR (
                             time_rec.rec_sfrwctl_end_date,
                             twbklibs.date_display_fmt
                          )
                       );
                       twbkfrmt.p_tabledata (
                          TO_CHAR (
                             TO_DATE (time_rec.rec_sfrwctl_hour_end, 'HH24MI'),
                             twbklibs.twgbwrul_rec.twgbwrul_time_fmt
                          )
                       );
                       twbkfrmt.p_tablerowclose;
                    END IF;
                 END LOOP;

                 IF row_count > 0
                 THEN
                    twbkfrmt.p_tableclose;
                 ELSE
                    twbkwbis.p_dispinfo (proc_name, 'TTERR');
                 END IF;

                 twbkwbis.p_closedoc (curr_release);
*/
                 raise_application_error(-20001, 'You are not allowed to register during this time.');
                 RETURN FALSE;
              WHEN bwcklibs.mgmt_control_err
              THEN
/* Comment out web page generation.
                 IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
                 THEN
                    -- FACWEB-specific code
                    IF NOT bwlkilib.f_add_drp (term)
                    THEN
                       NULL;
                    END IF;
                 ELSE
                    -- STUWEB-specific code
                    IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
                    THEN
                       NULL;
                    END IF;
                 END IF;                      -- END stu/fac specific code sections

                 bwckfrmt.p_open_doc (proc_name, term);
                 twbkwbis.p_dispinfo (proc_name, 'MCERR');
                 twbkwbis.p_closedoc (curr_release);
*/
                 raise_application_error(-20001, 'You are not allowed to register during this time.');
                 RETURN FALSE;
              WHEN bwcklibs.deceased_person_err
              THEN
/* Comment out web page generation.
                 IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
                 THEN

         -- FACWEB-specific code

                    IF NOT bwlkilib.f_add_drp (term)
                    THEN
                       NULL;
                    END IF;
                 ELSE

         -- STUWEB-specific code

                    IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
                    THEN
                       NULL;
                    END IF;
                 END IF;                      -- END stu/fac specific code sections

                 bwckfrmt.p_open_doc (proc_name, term);
                 twbkfrmt.p_printmessage (
                    g\$_nls.get ('BWCKSAM1-0005',
                       'SQL',
                       'Please Contact the Registration Administrator; You May Not Register'),
                    'ERROR'
                 );
                 twbkwbis.p_closedoc (curr_release);
*/
                 raise_application_error(-20001, 'Student account has been marked as deceased.');
                 RETURN FALSE;
              WHEN OTHERS
              THEN
/*                 IF NVL (twbkwbis.f_getparam (pidm_in, 'STUFAC_IND'), 'STU') = 'FAC'
                 THEN

         -- FACWEB-specific code

                    IF NOT bwlkilib.f_add_drp (term)
                    THEN
                       NULL;
                    END IF;
                 ELSE

         -- STUWEB-specific code

                    IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
                    THEN
                       NULL;
                    END IF;
                 END IF;                      -- END stu/fac specific code sections

                 bwckfrmt.p_open_doc (proc_name, term);
                 IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
                     twbkfrmt.p_printmessage (
                    SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),
                    'ERROR'
                 );
                 ELSE
                     twbkfrmt.p_printmessage (
                        G\$_NLS.Get ('BWCKSAM1-0006','SQL','Error occurred while checking if the current student is eligible to register for the current term.'),
                        'ERROR'
                     );
                 END IF;
                          twbkwbis.p_closedoc (curr_release);
*/
                 raise;
                 RETURN FALSE;
           END F_RegsStu;

-- BWSKFREG.p_adddropcrse rewritten
           PROCEDURE p_adddropcrse (term_in IN stvterm.stvterm_code%TYPE DEFAULT NULL)
           IS
              errs_count           NUMBER;
              regs_count           NUMBER;
              wait_count           NUMBER;
              term                 OWA_UTIL.ident_arr;
              reg_access_allowed   BOOLEAN            := TRUE;
              drop_problems        sfkcurs.drop_problems_rec_tabtype;
              drop_failures        sfkcurs.drop_problems_rec_tabtype;
              capp_tech_error      VARCHAR2 (4) := NULL;
              minh_admin_error     VARCHAR2 (1) := 'N';

              --  Study path Changes
              lv_st_path_req  VARCHAR2(1);
              p_sp_dummy             OWA_UTIL.ident_arr;
              title          bwckcoms.varchar2_tabtype;
              sobctrl_row            sobctrl%ROWTYPE;
              CURSOR sobctrl_c IS
              select * from sobctrl;

           BEGIN
        --
        -- Validate the current user.
        -- ====================================================
--              IF NOT twbkwbis.f_validuser (global_pidm)
--              THEN
--                 RETURN;
--              END IF;

              p_sp_dummy(1):= 'dummy';
              title(1) := 'dummy';
        --
        --
        -- ====================================================
--              twbkwbis.p_setparam (global_pidm, 'STUFAC_IND', 'STU');

        --
        -- Use the term that was passed in and update the parameter
        -- table (gorwprm) for the current user. Or use the existing
        -- term in the parameter table.
        -- ====================================================
              IF term_in IS NOT NULL
              THEN
--                 twbkwbis.p_setparam (global_pidm, 'TERM', term_in);
                 term (1) := term_in;
              ELSE
                 term (1) := twbkwbis.f_getparam (global_pidm, 'TERM');
              END IF;

        --
        -- Validate the term. If not valid, prompt for a new one.
        -- ====================================================
--              IF NOT bwskflib.f_validterm (term (1), stvterm_rec, sorrtrm_rec)
--              THEN
--                 bwskflib.p_seldefterm (term (1), 'bwskfreg.P_AddDropCrse');
--                 RETURN;
--              END IF;

-- Call local             IF NOT bwcksams.F_RegsStu (
                IF NOT F_RegsStu (
                    global_pidm,
                    term (1),
                    'bwskfreg.P_AddDropCrse'
                 )
              THEN
                 RETURN;
              END IF;

        --
        -- Check if the current user is eligible to register for
        -- the current term.
        -- ====================================================

             IF NOT sfkvars.regs_allowed
              THEN
--                 bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
--                 twbkfrmt.p_printmessage (
--                    g\$_nls.get ('BWSKFRE1-0013',
--                      'SQL',
--                       'Registration is not allowed at this time'),
--                    'ERROR'
--                 );
--                 twbkwbis.p_closedoc (curr_release);
                 raise_application_error(-20001, 'Registration is not allowed at this time');
                 RETURN;
              END IF;

        --
        -- Check if registration is allowed at the current time.
        -- ====================================================
              IF NOT sfkvars.add_allowed
              THEN
/*                 SELECT NVL (COUNT (*), 0)
                   INTO row_count
                   FROM sfrstcr
                  WHERE sfrstcr_pidm = global_pidm
                    AND sfrstcr_term_code = term (1);

                 IF row_count = 0
                 THEN
                    bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
                    twbkfrmt.p_printmessage (
                       g\$_nls.get ('BWSKFRE1-0014',
                          'SQL',
                          'Registration is not allowed at this time'),
                       'ERROR'
                    );
                    twbkwbis.p_closedoc (curr_release);
                    RETURN;
                 END IF;
*/
                raise_application_error(-20001, 'Registration is not allowed at this time');
              END IF;

        --  Study Path Changes
        --  Redirect to studypaths select page if studypaths are enabled
          OPEN sobctrl_c;
          FETCH sobctrl_c into sobctrl_row;
          CLOSE sobctrl_c;

          lv_st_path_req := bwckcoms.F_StudyPathReq(term (1));
          IF sobctrl_row.SOBCTRL_STUDY_PATH_IND = 'Y' THEN
/*            IF (twbkwbis.f_getparam (global_pidm, 'G_FROM_SP') IS NULL) THEN
              bwckcoms.P_DeleteStudyPath ;

                  bwckcoms.P_SelStudyPath(term_in,
                                    'bwskfreg.p_adddropcrse',
                                    lv_st_path_req,
                                    NULL,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    p_sp_dummy,
                                    title,
                                    p_sp_dummy,
                                    NULL,
                                    NULL,
                                    NULL);

              RETURN;
            ELSE
              twbkmods.p_delete_twgrwprm_pidm_name (global_pidm, 'G_FROM_SP');
            END IF;
*/
            raise_application_error(-20001, 'Course Exchange does not support Banner systems with study path registration enabled');
          END IF;

        --  Study Path Changes

        --
        -- Display the current registration information.
        -- ====================================================
              reg_access_allowed := TRUE;
-- Call local             bwcksams.P_RegsResult (
              P_RegsResult (
                 term,
                 errs_count,
                 regs_count,
                 wait_count,
                 TRUE,
                 reg_access_allowed,
                 capp_tech_error,
                 NULL,
                 drop_problems,
                 drop_failures
              );

              IF capp_tech_error IS NOT NULL
              THEN
                 RETURN;
              END IF;

              IF reg_access_allowed = FALSE
              THEN
/*                 minh_admin_error := sfkfunc.f_get_minh_admin_error(global_pidm, term(1));
                 IF minh_admin_error = 'Y' THEN
                    bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
                    twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'SEE_ADMIN');
                    twbkwbis.p_closedoc (curr_release);
                    RETURN;
                 ELSE
                    bwckfrmt.p_open_doc ('bwskfreg.P_AddDropCrse', term (1));
                    twbkwbis.p_dispinfo ('bwskfreg.P_AddDropCrse', 'SESSIONBLOCKED');
                    twbkwbis.p_closedoc (curr_release);
*/
                    raise_application_error(-20001, 'Your session is being blocked by an administrative session');
                    RETURN;
--                 END IF;
              END IF;
              RETURN;
/* End of procedure all commented below.
              IF    errs_count > 0
                 OR wait_count > 0
              THEN
                 twbkfrmt.p_tableclose;
                 HTP.br;
              END IF;

        --
        -- Display the add table.
        -- ====================================================
              bwckcoms.p_add_drop_crn1;
        --
        -- Keep track of counts. Setup buttons.
        -- ====================================================
              bwckcoms.p_add_drop_crn2 (10, regs_count, wait_count);
              HTP.formclose;
*/
              -- FHDA Mod XLI 11/16/2014
              -- Change link destination to the TouchNet landing page
              -- Add a button for students to sign up for a payment plan

                     /* --- FHDA Mod 09/26/2012
                      --- FHDA Mod 11/02/2014 -- Revised 'Pay Now' to 'Pay Now or Payment Plan'
                      htp.p(' <div class="footerlinksdiv">');
                      \\*      twbkfrmt.P_PRINTTEXT (HTF.ANCHOR (
                                           CURL          => twbkwbis.f_cgibin ||'byskoacc.P_ViewAcctTerm',
                                           CTEXT         => '[Pay Now or Payment Plan]',
                                           cattributes   => 'style="font-size: 150%;"'
                                        )
                                  );*\\
                    htp.p('<button onclick="window.open(''https://myportal.fhda.edu/cp/ip/login?sys=tnproxy&url=https%3A%2F%2Fbanapps.fhda.edu%2Ftnproxy%2Flogin'')" style="font-size:12px;" type="button"> Pay Now or Payment Plan </button>');
                      TWBKFRMT.P_PRINTTEXT(' </div>');
                    --- End of FHDA Mod*/
/*
                htp.p(' <div class="footerlinksdiv">');
                htp.p('<button onclick="window.open(''https://myportal.fhda.edu/cp/ip/login?sys=tnproxy&url=https%3A%2F%2Fbanapps.fhda.edu%2Ftnproxy%2Flogin'')"
                       style="font-size:12px;" type="button"> Pay Now </button>');


                If  nvl(f_get_sdax('PPLAN_DISP', 'REGISTRATION'), 'N') = 'Y' then

                 htp.p('<button onclick="window.open(''https://myportal.fhda.edu/cp/ip/login?sys=tnproxy&url=https%3A%2F%2Fbanapps.fhda.edu%2Ftnproxy%2Flogin'')"
                               style="font-size:12px;" type="button"> Sign Up for a Payment Plan </button>');


                End if;
                TWBKFRMT.P_PRINTTEXT(' </div>');
                --- FHDA Mod End


              twbkwbis.p_closedoc (curr_release);
*/
           END p_adddropcrse;

-- BWSKFREG.p_altpin rewritten
           PROCEDURE p_altpin (term_in IN stvterm.stvterm_code%TYPE DEFAULT NULL)
           IS
              pinlen          NUMBER;
              last_access     DATE;
              msg             VARCHAR2 (2000);
              ask_alt_pin     VARCHAR2 (1);
              internal_code   gtvsdax.gtvsdax_internal_code%TYPE;
              gtvsdax_group   gtvsdax.gtvsdax_internal_code_group%TYPE;

              --FHDA Mod 7/9/2013
              --Variable to hold user designated survey to be used for pre-reg polling
              lv_survey       gubsrvy.gubsrvy_name%type;
              --FHDA Mod end

           BEGIN
        --
        -- Validate the current user.
        -- ====================================================
--              IF NOT twbkwbis.f_validuser (global_pidm)
--              THEN
--                 RETURN;
--              END IF;



        --
        -- Check for Valid Term
        -- =====================================================
              IF term_in IS NOT NULL
              THEN
                 IF bwskflib.f_validterm (term_in, stvterm_rec, sorrtrm_rec)
                 THEN
                   twbkwbis.p_setparam (global_pidm, 'TERM', term_in);
-- Set the term to term_in.
                   term := term_in;
                 END IF;

--                 term := term_in;
              -- Luminus must always ask for term to be selected
--              ELSIF twbkwbis.cp_integrated_mode
--              THEN
--                 bwskflib.p_seldefterm (term, 'bwskfreg.P_AltPin');
--                 RETURN;
              -- end luminus handler
--              ELSE
--                 term := twbkwbis.f_getparam (global_pidm, 'TERM');
-- Error out if no term was set.
              ELSE
                raise_application_error(-20001, 'sisTermId must be provided in enrollment request');
              END IF;
--                 IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
--                 THEN
--                    bwskflib.p_seldefterm (term, 'bwskfreg.P_AltPin');
--                    RETURN;
--                 END IF;
--              END IF;
/* FHDA specific - Not prompting for survey questions for OEI
        -- FHDA mod 6/27/2013
                 -- Check if student is required to fill out a survey prior to registration.
                 -- Sicne FHDA does not use alt pin as an institution, this location is ideal
                 -- to check for this
                 -- retrieve pre-preg survey
                 lv_survey := baninst1.f_checksdaxrule('SURVEY', 'WEBREG');
*/
--                 if bwgksrvy.f_survey_exists(global_pidm, lv_survey/*'XIAOBIN_SURVEY'*/) = 'Y' then
                          -- set parameters for returning to registration page
--                          twbkwbis.p_setparam(global_pidm, 's_origin', 'bwskfreg.P_AltPin');
--                          twbkwbis.p_redirecturl(twbkwbis.F_CgiBin||'bwgksrvy.P_ShowQuestions?srvy_name='||lv_survey/*'XIAOBIN_SURVEY'*/||'&next_disp=1');
--                 end if;
        -- FHDA mod end
        --
        -- Check if Alternate PIN is used by institution. If not,
        -- go directly to add/drop page. If so, continue...
        -- =====================================================
/*
              internal_code := 'WEBALTPINA';
              gtvsdax_group := 'WEBREG';
              OPEN sfkcurs.get_gtvsdaxc (internal_code, gtvsdax_group);
              FETCH sfkcurs.get_gtvsdaxc INTO ask_alt_pin;

              IF sfkcurs.get_gtvsdaxc%NOTFOUND
              THEN
                 CLOSE sfkcurs.get_gtvsdaxc;
-- Call local                bwskfreg.p_adddropcrse;
*/
                 p_adddropcrse(term);
                 RETURN;
/*  Commented out from here on
               END IF;

              CLOSE sfkcurs.get_gtvsdaxc;
        --
        -- Check if an Alternate PIN exists for the student/term.
        -- If not, then go directly to add/drop page. If so, continue...
        -- =====================================================
              OPEN sfkcurs.getaltpinc (global_pidm, term);
              FETCH sfkcurs.getaltpinc INTO sprapin_rec;

              IF sfkcurs.getaltpinc%NOTFOUND
              THEN
                 CLOSE sfkcurs.getaltpinc;
                 bwskfreg.p_adddropcrse;
                 RETURN;
              END IF;

              CLOSE sfkcurs.getaltpinc;
        --
        -- Check if alt pin has been entered already. If so,
        -- validate it and go directly to add/drop page. If
        -- not, continue...
        -- =====================================================
              apin := twbkwbis.f_getparam (global_pidm, 'AP ' || term);
              term := twbkwbis.f_getparam (global_pidm, 'TERM');

              IF apin IS NOT NULL
              THEN
                 OPEN sfkcurs.getaltpinc (global_pidm, term, apin);
                 FETCH sfkcurs.getaltpinc INTO sprapin_rec;

                 IF sfkcurs.getaltpinc%FOUND
                 THEN
                    CLOSE sfkcurs.getaltpinc;
                    bwskfreg.p_adddropcrse;
                    RETURN;
                 END IF;

                 CLOSE sfkcurs.getaltpinc;
              END IF;

        --
        -- Generate the Alternate PIN form. When user clicks on
        -- submit, run the p_checkaltpin procedure.
        -- =====================================================
              pinlen := goklibs.f_pinlength;
              bwckfrmt.p_open_doc ('bwskfreg.P_AltPin', term);
              twbkwbis.p_dispinfo ('bwskfreg.P_AltPin');

              IF msg IS NOT NULL
              THEN
                 twbkfrmt.p_printtext (msg);
              END IF;

              HTP.formopen (
                 twbkwbis.f_cgibin || 'bwskfreg.P_CheckAltPin',
                 cattributes   => 'onSubmit="return checkSubmit()"'
              );
              twbkfrmt.p_tableopen (
                 'DATAENTRY',
                 cattributes   => 'SUMMARY="' ||
                                     g\$_nls.get ('BWSKFRE1-0000',
                                        'SQL',
                                        'This table allows the user to enter their alternate PIN for registration processing.') ||
                                     '"'
              );
              twbkfrmt.p_tablerowopen;
              twbkfrmt.p_tabledatalabel (
                 twbkfrmt.f_formlabel (
                    g\$_nls.get ('BWSKFRE1-0001', 'SQL', 'Alternate PIN') || ':',
                    idname   => 'apin_id'
                 )
              );
              twbkfrmt.p_tabledata (
                 HTF.formpassword (
                    'pin',
                    pinlen,
                    pinlen,
                    cattributes   => 'ID="apin_id"'
                 )
              );
              twbkfrmt.p_tablerowclose;
              twbkfrmt.p_tableclose;
              HTP.br;
              HTP.formsubmit (NULL, g\$_nls.get ('BWSKFRE1-0002', 'SQL', 'Submit'));
              ----
              HTP.formclose;



              twbkwbis.p_closedoc (curr_release);
*/
           END p_altpin;


-- BWCKCOMS.F_SUBG_CRSE_SELECTED copy and paste
            function f_subg_crse_selected(f_pidm number, f_term varchar2, f_crn  varchar2 ) return boolean is

              lv_subj  varchar2(4);
              lv_crse  varchar2(10);
              lv_sec   varchar2(4);

              cursor crse_selected is
                select 1 from (
                 SELECT kn.shrtckn_pidm,
                        kn.shrtckn_term_code,
                        kn.shrtckn_crn,
                        kn.shrtckn_repeat_course_ind,
                        kg.shrtckg_grde_code_final
                  from  shrtckn kn, shrtckg kg
                  where kn.shrtckn_pidm = f_pidm --441987
                  and   kn.shrtckn_term_code  < f_term --'&cterm'
                  --and   kn.shrtckn_repeat_course_ind ='I'
                  --- possible removal needed
                  and   kn.shrtckn_subj_code = lv_subj--'&csubj'
                  AND   kn.shrtckn_crse_numb = lv_crse--'&ccrse'
                  AND   kn.shrtckn_pidm = kg.shrtckg_pidm
                  AND   kn.shrtckn_term_code = kg.shrtckg_term_code
                  AND   kg.shrtckg_tckn_seq_no =(
                    SELECT n.shrtckn_seq_no
                    FROM shrtckn n
                    WHERE n.shrtckn_pidm = kn.shrtckn_pidm
                    AND   n.shrtckn_term_code = kn.shrtckn_term_code
                    AND   n.shrtckn_crn = kn.shrtckn_crn )
                  AND   kg.shrtckg_seq_no =(
                    SELECT MAX(g.shrtckg_seq_no) FROM shrtckg g
                      WHERE g.shrtckg_pidm = kg.shrtckg_pidm
                      AND   g.shrtckg_term_code = kg.shrtckg_term_code
                      AND   g.shrtckg_tckn_seq_no = kg.shrtckg_tckn_seq_no)
                  and  kg.shrtckg_grde_code_final in ('D', 'D+', 'D-', 'NP', 'F', 'W', 'I')
                  -- possible removable needed
                  and baninst1.swkfrep.f_chk_ctg_repeat(kn.shrtckn_subj_code, kn.shrtckn_crse_numb, kn.shrtckn_term_code )=1)
                  having count(*) >=2 ;


                 lv_ind number default 0;

            begin

                -- get subject code and course number of a crn + term combination
--                bwckregs.p_getsection (f_term, f_crn, lv_subj, lv_crse, lv_sec);
                p_getsection (f_term, f_crn, lv_subj, lv_crse, lv_sec);

                open crse_selected;
                fetch crse_selected into lv_ind;
                close crse_selected;

                if lv_ind  = 1 then
                 return true;
                else
                 return false;
                end if;

                --------------- test-------------------------
                -- return true;
                --------------- end of test -----------------

            end;
"""

        def plsql4 = """
-- BWCKCOMS.p_adddrop2 rewritten
           PROCEDURE p_adddrop2 (
              term_in           IN   OWA_UTIL.ident_arr,
              err_term          IN   OWA_UTIL.ident_arr,
              err_crn           IN   OWA_UTIL.ident_arr,
              err_subj          IN   OWA_UTIL.ident_arr,
              err_crse          IN   OWA_UTIL.ident_arr,
              err_sec           IN   OWA_UTIL.ident_arr,
              err_code          IN   OWA_UTIL.ident_arr,
              err_levl          IN   OWA_UTIL.ident_arr,
              err_cred          IN   OWA_UTIL.ident_arr,
              err_gmod          IN   OWA_UTIL.ident_arr,
              capp_tech_error_in_out   IN OUT  VARCHAR2,
              drop_result_label_in IN twgrinfo.twgrinfo_label%TYPE DEFAULT NULL,
              drop_problems_in     IN sfkcurs.drop_problems_rec_tabtype,
              drop_failures_in     IN sfkcurs.drop_problems_rec_tabtype
              )
           IS
              i                    INTEGER                         := 2;
              errs_count           NUMBER;
              regs_count           NUMBER;
              wait_count           NUMBER;
              err_levl_desc        STVLEVL.STVLEVL_DESC%TYPE;
              err_crhrs            SSBSECT.SSBSECT_CREDIT_HRS%TYPE;
              err_gmod_desc        STVGMOD.STVGMOD_DESC%TYPE;
              reg_access_allowed   BOOLEAN;
              term                 stvterm.stvterm_code%TYPE       := NULL;
              multi_term           BOOLEAN                         := TRUE;
              lv_sp_name           VARCHAR2(4000);
              lv_sgrstsp_eff_term  stvterm.stvterm_code%TYPE       := NULL;

-- Our custom error message string
              error_message_sftregs   VARCHAR2(4000) := '';

           BEGIN
--Our custom code to retrieve a descriptive error message
            DECLARE
            -- Our variables
                      current_error_message   SFTREGS.SFTREGS_MESSAGE%TYPE;

                      CURSOR regs_error_retrieval(genpidm NUMBER, term VARCHAR2) IS
                      select distinct sftregs_message from sftregs
                       where sftregs_pidm = genpidm and sftregs_term_code = term and (sftregs_error_flag IS NOT NULL OR sftregs_message IS NOT NULL);
            BEGIN
                  OPEN regs_error_retrieval(genpidm,term_in(1));
                  FETCH regs_error_retrieval INTO current_error_message;
                  IF regs_error_retrieval%NOTFOUND THEN
                      error_message_sftregs := NULL;
                  END IF;
                  WHILE regs_error_retrieval%FOUND LOOP
                      error_message_sftregs := error_message_sftregs || current_error_message;
                      FETCH regs_error_retrieval INTO current_error_message;
                  END LOOP;
                  CLOSE regs_error_retrieval;
            END;

        --
        -- Display the current registration information.
        -- ===================================================
--              bwcksams.p_regsresult (
              p_regsresult (
                 term_in,
                 errs_count,
                 regs_count,
                 wait_count,
                 NULL,
                 reg_access_allowed,
                 capp_tech_error_in_out,
                 drop_result_label_in,
                 drop_problems_in,
                 drop_failures_in
              );

              IF error_message_sftregs IS NOT NULL THEN
                  raise_application_error(-20001, 'Registration failed: ' || error_message_sftregs);
              END IF;

           END p_adddrop2;

--BWCKREGS.p_final_updates rewritten
            PROCEDURE p_final_updates (
               term_in                IN       OWA_UTIL.ident_arr,
               err_term               IN       OWA_UTIL.ident_arr,
               err_crn                IN       OWA_UTIL.ident_arr,
               err_subj               IN       OWA_UTIL.ident_arr,
               err_crse               IN       OWA_UTIL.ident_arr,
               err_sec                IN       OWA_UTIL.ident_arr,
               err_code               IN       OWA_UTIL.ident_arr,
               err_levl               IN       OWA_UTIL.ident_arr,
               err_cred               IN       OWA_UTIL.ident_arr,
               err_gmod               IN       OWA_UTIL.ident_arr,
               drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
               drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
               drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype
            )
            IS
               CURSOR sftregs_errors_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in
                     AND sftregs_error_flag = 'F';

               CURSOR sftregs_test_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in;

               tmst_flag          sftregs.sftregs_error_flag%TYPE;
               group_error_flag   VARCHAR2 (1)                    := 'N';
               capp_tech_error    VARCHAR2 (4)                    := NULL;
               minh_error         VARCHAR2 (4)                    := 'N';
               genpidm            spriden.spriden_pidm%TYPE;
               error_flag         sftregs.sftregs_error_flag%TYPE := 'N';
               error_rec          sftregs%ROWTYPE;
               source_system      VARCHAR2 (2);
               clas_code          sgrclsr.sgrclsr_clas_code%TYPE;
               may_drop_last      BOOLEAN                         := TRUE;
               drop_problems      sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
               drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;


            BEGIN

--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--               ELSE
                  genpidm := global_pidm;
--               END IF;

-- Not stopping on last class drop
--             may_drop_last := sfkdrop.f_drop_last ('W');

               <<term_loop>>
               FOR i IN 1 .. term_in.COUNT
               LOOP

                  /* does term exist in sfbetrm? */
                  IF sb_enrollment.f_exists( p_pidm => genpidm,
                                             p_term_code => term_in (i)) = 'Y'
                  THEN

                     p_init_final_update_vars (genpidm, term_in (i));

                     IF drop_problems IS NOT NULL OR
                         drop_failures IS NOT NULL
                     THEN

                         /* re-run group edits to find any conflicts with drops */
                         /* that have been reset */
                         group_error_flag := NULL;
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                         sfkedit.p_web_group_edits (
                            genpidm,
                            term_in (i),
                            'S',
                            NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                            NVL (sobterm_row.sobterm_time_severity, 'N'),
                            NVL (sobterm_row.sobterm_corq_severity, 'N'),
                            NVL (sobterm_row.sobterm_link_severity, 'N'),
                            NVL (sobterm_row.sobterm_preq_severity, 'N'),
                            NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                            NVL (sobterm_row.sobterm_minh_severity, 'N'),
                            NVL (sobterm_row.sobterm_mexc_severity, 'N'),
                            sfbetrm_row.sfbetrm_mhrs_over,
                            sfbetrm_row.sfbetrm_min_hrs,
                            SYS_CONTEXT ('USERENV', 'SESSIONID'),
                            'WA',
                            group_error_flag,
                            capp_tech_error,
                            minh_error,
                            term_in
                         );

                         IF capp_tech_error IS NOT NULL
                         THEN
                            ROLLBACK;
                            EXIT term_loop;
                         END IF;
/* Not stopping on min hours exception.
                         -- Store the fact that a minimum hours error occurred, and quit all processing.
                         IF minh_error = 'Y'
                         THEN
                            twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                            sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                            sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
                         END IF;
*/
                         IF group_error_flag = 'Y'
                         THEN
                            /* update new courses to indicate they are in error */
                            /* don't alter the rec_stat from 'N' */
                            UPDATE sftregs
                               SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                   sftregs_remove_ind = 'Y',
                                   sftregs_user = goksels.f_get_ssb_id_context,
                                   sftregs_activity_date = SYSDATE
                             WHERE sftregs_term_code = term_in (i)
                               AND sftregs_pidm = genpidm
                               AND sftregs_error_flag = 'F'
                               AND sftregs_rec_stat = 'N';
                         END IF;

                         bwcklibs.p_build_drop_problems_list ( genpidm,
                                                               term_in (i),
                                                               drop_problems,
                                                               drop_failures
                                                               );

                     END IF;

                     error_flag := 'Y';

                     <<update_loop>>
                     WHILE nvl(error_flag,'N') <> 'N'
                     LOOP
               --
               -- Transfers sftregs changes into sfrstcr.
               -- =======================================================================

               IF NVL (sftregs_row.sftregs_vr_status_type,'X') IN ('D', 'W') THEN
                       sobterm_row.sobterm_capc_severity := 'N';
               END IF;

                        sfkedit.p_update_regs (
                           genpidm,
                           term_in (i),
                           SYSDATE,
                           sgrclsr_clas_code,
                           sgbstdn_row.sgbstdn_styp_code,
                           NVL (sobterm_row.sobterm_capc_severity, 'N'),
                           sobterm_row.sobterm_tmst_calc_ind,
                           sfbetrm_row.sfbetrm_tmst_maint_ind,
                           sfbetrm_row.sfbetrm_tmst_code,
                           may_drop_last,
                           'WA',
                           error_rec,
                           error_flag,
                           tmst_flag
                        );

                        IF error_flag = 'D'
                        THEN
                           /* sfrstcr row missing - system problem */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'D');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'L'
                        THEN
                           /* last class drop attempted */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'L');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'Y'
                        THEN
                           /* capacity error */

                           /* update new courses to indicate they are in error */
                           UPDATE sftregs
                              SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                  sftregs_remove_ind = 'Y',
                                  sftregs_user = goksels.f_get_ssb_id_context,
                                  sftregs_activity_date = SYSDATE
                            WHERE sftregs_term_code = term_in (i)
                              AND sftregs_pidm = genpidm
                              AND sftregs_error_flag = 'F'
                              AND sftregs_rec_stat = 'N';

                           group_error_flag := NULL;

                           /* re-run group edits on remaining classes */
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                           sfkedit.p_web_group_edits (
                              genpidm,
                              term_in (i),
                              'S',
                              NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                              NVL (sobterm_row.sobterm_time_severity, 'N'),
                              NVL (sobterm_row.sobterm_corq_severity, 'N'),
                              NVL (sobterm_row.sobterm_link_severity, 'N'),
                              NVL (sobterm_row.sobterm_preq_severity, 'N'),
                              NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                              NVL (sobterm_row.sobterm_minh_severity, 'N'),
                              NVL (sobterm_row.sobterm_mexc_severity, 'N'),
                              sfbetrm_row.sfbetrm_mhrs_over,
                              sfbetrm_row.sfbetrm_min_hrs,
                              SYS_CONTEXT ('USERENV', 'SESSIONID'),
                              'WA',
                              group_error_flag,
                              capp_tech_error,
                              minh_error,
                              term_in
                           );

                           IF capp_tech_error IS NOT NULL
                           THEN
                              ROLLBACK;
                              EXIT term_loop;
                           END IF;
/* Not stopping on minimum hours check.
                           IF minh_error = 'Y'
                           THEN
                              twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                              sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                              sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
                           END IF;
*/
                           /* group edits found an error, reset all changes  */
                           /* to classes associated with those now in error */
                           IF group_error_flag = 'Y'
                           THEN
                              FOR error_rec IN sftregs_errors_c (genpidm, term_in (i))
                              LOOP
                                 bwcklibs.p_handle_sftregs_error (error_rec);
                              END LOOP;
                           END IF;
                        END IF;
                     END LOOP;

                  END IF; /* term exists */

               END LOOP term_loop;

            -- Redisplay the add/drop page.
            -- =======================================================================

-- Call local               bwckcoms.p_adddrop2 (
               p_adddrop2 (
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  capp_tech_error,
                  drop_result_label_in,
                  drop_problems,
                  drop_failures
               );

               RETURN;

            END p_final_updates;

--SV_AUTH_CODES_BP.P_call_auth_page rewritten.
               PROCEDURE P_call_auth_page(   term_in                IN       OWA_UTIL.ident_arr,
                                            err_term               IN       OWA_UTIL.ident_arr,
                                            pidm                   IN       SPRIDEN.SPRIDEN_PIDM%TYPE,
                                            err_crn                IN       OWA_UTIL.ident_arr,
                                            err_subj               IN       OWA_UTIL.ident_arr,
                                            err_crse               IN       OWA_UTIL.ident_arr,
                                            err_sec                IN       OWA_UTIL.ident_arr,
                                            err_code               IN       OWA_UTIL.ident_arr,
                                            err_levl               IN       OWA_UTIL.ident_arr,
                                            err_cred               IN       OWA_UTIL.ident_arr,
                                            err_gmod               IN       OWA_UTIL.ident_arr,
                                            drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
                                            drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
                                            drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype,
                                            p_called_page_out            OUT      VARCHAR2,
                                            add_row                 NUMBER,
                                            orig_crn           IN    OWA_UTIL.ident_arr,
                                            orig_rsts          IN    OWA_UTIL.ident_arr
                                            )
              IS

               lv_call_authPage   VARCHAR2(1):='N';
               auth_code         OWA_UTIL.ident_arr;
               auth_msg          OWA_UTIL.ident_arr;
               num_attempts_in   OWA_UTIL.ident_arr;
               capp_tech_error    VARCHAR2 (4)                    := NULL;
             drop_problems        sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
               drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;

              BEGIN

               FOR i IN 1 .. term_in.COUNT
               LOOP
                lv_call_authPage:='N';

                 IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(i))
                 AND sv_auth_addcodes_bp.F_call_auth_page(term_in(i),pidm)
                 THEN
                   lv_call_authPage:='Y';
                  END IF;
                END LOOP;
/* Avoid display                p_called_page_out:=lv_call_authPage;
               IF  lv_call_authPage='Y' THEN
                auth_code(1):=null;
                auth_msg(1):= null;
                num_attempts_in(1):=null;

                bwvkauth.P_DispAutCode (term_in  ,
                                        err_term ,
                                        err_crn  ,
                                        err_subj ,
                                        err_crse ,
                                        err_sec  ,
                                        err_code ,
                                        err_levl ,
                                        err_cred ,
                                        err_gmod ,
                                        capp_tech_error,
                                        drop_result_label_in  ,
                                        drop_problems_in         ,
                                        drop_failures_in         ,
                                        auth_code             ,
                                        auth_msg              ,
                                        num_attempts_in     ,
                                        add_row,
                                        orig_crn ,
                                        orig_rsts
                                       );
                  --RETURN;
               END IF;
*/
            END P_call_auth_page;

--SV_AUTH_CODES_BP.p_local_final_updates
            PROCEDURE p_local_final_updates(
               term_in                IN       OWA_UTIL.ident_arr,
               err_term               IN       OWA_UTIL.ident_arr,
               err_crn                IN       OWA_UTIL.ident_arr,
               err_subj               IN       OWA_UTIL.ident_arr,
               err_crse               IN       OWA_UTIL.ident_arr,
               err_sec                IN       OWA_UTIL.ident_arr,
               err_code               IN       OWA_UTIL.ident_arr,
               err_levl               IN       OWA_UTIL.ident_arr,
               err_cred               IN       OWA_UTIL.ident_arr,
               err_gmod               IN       OWA_UTIL.ident_arr,
               drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
               drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
               drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype,
               add_row                         NUMBER,
               orig_crn           IN    OWA_UTIL.ident_arr,
               orig_rsts          IN    OWA_UTIL.ident_arr
               )
            IS
               CURSOR sftregs_errors_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in
                     AND sftregs_error_flag = 'F';

               CURSOR sftregs_test_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in;

               tmst_flag          sftregs.sftregs_error_flag%TYPE;
               group_error_flag   VARCHAR2 (1)                    := 'N';
               capp_tech_error    VARCHAR2 (4)                    := NULL;
               genpidm            spriden.spriden_pidm%TYPE;
               error_flag         sftregs.sftregs_error_flag%TYPE := 'N';
               error_rec          sftregs%ROWTYPE;
               source_system      VARCHAR2 (2);
               clas_code          sgrclsr.sgrclsr_clas_code%TYPE;
               may_drop_last      BOOLEAN                         := TRUE;
               drop_problems      sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
               drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;
               lv_call_authPage   VARCHAR2(1):='N';
               minh_error         VARCHAR2 (4)                    := 'N';

            BEGIN

--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--               ELSE
                  genpidm := global_pidm;
--               END IF;

-- Bypassing drop last class check for OEI.
--                 may_drop_last := sfkdrop.f_drop_last ('W');

             --C3SC CJL 24/02/2006 Added
             -- moved this code above sfkedit.p_update, so that we only perform final updates
             -- if no auth error are found
                    P_call_auth_page(term_in    => term_in,
                                                    err_term   =>err_term,
                                                    pidm       =>genpidm,
                                                    err_crn    =>err_crn,
                                                    err_subj   =>err_subj,
                                                    err_crse   =>err_crse,
                                                    err_sec    =>err_sec,
                                                    err_code   =>err_code,
                                                    err_levl   =>err_levl,
                                                    err_cred   =>err_cred,
                                                    err_gmod   =>err_gmod,
                                                    drop_result_label_in   =>drop_result_label_in,
                                                    drop_problems_in       =>drop_problems_in,
                                                    drop_failures_in       =>drop_failures_in,
                                                    p_called_page_out=> lv_call_authPage,
                                                    add_row=>add_row,
                                                    orig_crn=>orig_crn,
                                                    orig_rsts=>orig_rsts
                                                      );

               IF  lv_call_authPage = 'Y' THEN
                 raise_application_error(-20001, 'There was a problem processing the registration, please see an administrator.');
               END IF;


            --END C3SC

              <<term_loop>>
               FOR i IN 1 .. term_in.COUNT
               LOOP
                /* does term exist in sfbetrm? */
                  IF sb_enrollment.f_exists( p_pidm => genpidm,
                                             p_term_code => term_in (i)) = 'Y'
                  THEN
                     sv_auth_addcodes_bp.p_local_init_final_update_vars (genpidm, term_in (i));

                     IF drop_problems IS NOT NULL OR
                         drop_failures IS NOT NULL
                     THEN


                         /* re-run group edits to find any conflicts with drops */
                         /* that have been reset */
                         group_error_flag := NULL;
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                         sfkedit.p_web_group_edits (
                            genpidm,
                            term_in (i),
                            'S',
                            NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                            NVL (sobterm_row.sobterm_time_severity, 'N'),
                            NVL (sobterm_row.sobterm_corq_severity, 'N'),
                            NVL (sobterm_row.sobterm_link_severity, 'N'),
                            NVL (sobterm_row.sobterm_preq_severity, 'N'),
                            NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                            NVL (sobterm_row.sobterm_minh_severity, 'N'),
                            NVL (sobterm_row.sobterm_mexc_severity, 'N'),     -- C3SC VK 09/14/2009 ADD
                            sfbetrm_row.sfbetrm_mhrs_over,
                            sfbetrm_row.sfbetrm_min_hrs,
                            SYS_CONTEXT ('USERENV', 'SESSIONID'),
                            'WA',
                            group_error_flag,
                            capp_tech_error,
                            minh_error,
                            term_in
                         );

                       IF capp_tech_error IS NOT NULL
                         THEN
                            ROLLBACK;
                            EXIT term_loop;
                         END IF;

                         -- Store the fact that a minimum hours error occurred, and quit all processing.
/* Not stopping on minimum hours check
                         IF minh_error = 'Y'
                         THEN
                            twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                            sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                            sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
                         END IF;
*/
                         IF group_error_flag = 'Y'
                         THEN
                            /* update new courses to indicate they are in error */
                            /* don't alter the rec_stat from 'N' */

                            UPDATE sftregs
                               SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                   sftregs_remove_ind = 'Y'
                             WHERE sftregs_term_code = term_in (i)
                               AND sftregs_pidm = genpidm
                               AND sftregs_error_flag = 'F'
                               AND sftregs_rec_stat = 'N';
                         END IF;



                         bwcklibs.p_build_drop_problems_list ( genpidm,
                                                               term_in (i),
                                                               drop_problems,
                                                               drop_failures
                                                               );

                      END IF;

                     error_flag := 'Y';

                     <<update_loop>>
                     WHILE nvl(error_flag,'N') <> 'N'
                     LOOP
               --
               -- Transfers sftregs changes into sfrstcr.
               -- =======================================================================

                        sfkedit.p_update_regs (
                           genpidm,
                           term_in (i),
                           SYSDATE,
                           sgrclsr_clas_code,
                           sgbstdn_row.sgbstdn_styp_code,
                           NVL (sobterm_row.sobterm_capc_severity, 'N'),
                           sobterm_row.sobterm_tmst_calc_ind,
                           sfbetrm_row.sfbetrm_tmst_maint_ind,
                           sfbetrm_row.sfbetrm_tmst_code,
                           may_drop_last,
                           'WA',
                           error_rec,
                           error_flag,
                           tmst_flag
                        );

                        IF error_flag = 'D'
                        THEN
                           /* sfrstcr row missing - system problem */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'D');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'L'
                        THEN
                           /* last class drop attempted */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'L');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'Y'
                        THEN
                         /* capacity error */
                           /* update new courses to indicate they are in error */
                           UPDATE sftregs
                              SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                  sftregs_remove_ind = 'Y'
                            WHERE sftregs_term_code = term_in (i)
                              AND sftregs_pidm = genpidm
                              AND sftregs_error_flag = 'F'
                              AND sftregs_rec_stat = 'N';

                           group_error_flag := NULL;
                           /* re-run group edits on remaining classes */
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                        sfkedit.p_web_group_edits (
                            genpidm,
                            term_in (i),
                            'S',
                            NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                            NVL (sobterm_row.sobterm_time_severity, 'N'),
                            NVL (sobterm_row.sobterm_corq_severity, 'N'),
                            NVL (sobterm_row.sobterm_link_severity, 'N'),
                            NVL (sobterm_row.sobterm_preq_severity, 'N'),
                            NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                            NVL (sobterm_row.sobterm_minh_severity, 'N'),
                            NVL (sobterm_row.sobterm_mexc_severity, 'N'),  -- C3SC VK 09/14/2009 ADD
                            sfbetrm_row.sfbetrm_mhrs_over,
                            sfbetrm_row.sfbetrm_min_hrs,
                            SYS_CONTEXT ('USERENV', 'SESSIONID'),
                            'WA',
                            group_error_flag,
                            capp_tech_error,
                            minh_error,
                            term_in
                         );


                           IF capp_tech_error IS NOT NULL
                           THEN
                              ROLLBACK;
                              EXIT term_loop;
                           END IF;
/* Not doing minimum hours check
                           IF minh_error = 'Y'
                           THEN
                              twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                              sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                              sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
                           END IF;
*/
                           /* group edits found an error, reset all changes  */
                           /* to classes associated with those now in error */
                           IF group_error_flag = 'Y'
                           THEN
                           FOR error_rec IN sftregs_errors_c (genpidm, term_in (i))
                              LOOP
                                 bwcklibs.p_handle_sftregs_error (error_rec);
                              END LOOP;
                           END IF;
                        END IF;
                     END LOOP;
                  END IF; /* term exists */
               END LOOP term_loop;

            -- Redisplay the add/drop page.
            -- =======================================================================
--               bwckcoms.p_adddrop2 (
               p_adddrop2 (
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  capp_tech_error,
                  drop_result_label_in,
                  drop_problems,
                  drop_failures
               );

               RETURN;

            END p_local_final_updates;

--bwvkauth.p_final_upadates2 rewritten
            PROCEDURE p_final_upadates2(
               term_in                IN       OWA_UTIL.ident_arr,
               err_term               IN       OWA_UTIL.ident_arr,
               err_crn                IN       OWA_UTIL.ident_arr,
               err_subj               IN       OWA_UTIL.ident_arr,
               err_crse               IN       OWA_UTIL.ident_arr,
               err_sec                IN       OWA_UTIL.ident_arr,
               err_code               IN       OWA_UTIL.ident_arr,
               err_levl               IN       OWA_UTIL.ident_arr,
               err_cred               IN       OWA_UTIL.ident_arr,
               err_gmod               IN       OWA_UTIL.ident_arr,
               drop_result_label_in   IN       twgrinfo.twgrinfo_label%TYPE,
               drop_problems_in       IN       sfkcurs.drop_problems_rec_tabtype,
               drop_failures_in       IN       sfkcurs.drop_problems_rec_tabtype,
               add_row                         NUMBER
               )
            IS
               CURSOR sftregs_errors_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in
                     AND sftregs_error_flag = 'F';

               CURSOR sftregs_test_c (
                  pidm_in   sftregs.sftregs_pidm%TYPE,
                  term_in   sftregs.sftregs_term_code%TYPE
               )
               IS
                  SELECT *
                    FROM sftregs
                   WHERE sftregs_pidm = pidm_in
                     AND sftregs_term_code = term_in;

               tmst_flag          sftregs.sftregs_error_flag%TYPE;
               group_error_flag   VARCHAR2 (1)                    := 'N';
               capp_tech_error    VARCHAR2 (4)                    := NULL;
               genpidm            spriden.spriden_pidm%TYPE;
               error_flag         sftregs.sftregs_error_flag%TYPE := 'N';
               error_rec          sftregs%ROWTYPE;
               source_system      VARCHAR2 (2);
               clas_code          sgrclsr.sgrclsr_clas_code%TYPE;
               may_drop_last      BOOLEAN                         := TRUE;
               drop_problems      sfkcurs.drop_problems_rec_tabtype := drop_problems_in;
               drop_failures      sfkcurs.drop_problems_rec_tabtype := drop_failures_in;
               lv_call_authPage   VARCHAR2(1):='N';
               minh_error        VARCHAR2 (1) := 'N';

            BEGIN

--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--               ELSE
                  genpidm := global_pidm;
--               END IF;
-- Bypassing drop last class check for OEI
--               may_drop_last := sfkdrop.f_drop_last ('W');


              <<term_loop>>
               FOR i IN 1 .. term_in.COUNT
               LOOP
                /* does term exist in sfbetrm? */
                  IF sb_enrollment.f_exists( p_pidm => genpidm,
                                             p_term_code => term_in (i)) = 'Y'
                  THEN
                     bwvkauth.p_init_final_update_vars2 (genpidm, term_in (i));

                  -- descomentar temporalmente para prueba
                  /*   IF drop_problems IS NOT NULL OR
                         drop_failures IS NOT NULL
                     THEN
                         /* re-run group edits to find any conflicts with drops */
                         /* that have been reset */
               /*          group_error_flag := NULL;
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                          sfkedit.p_web_group_edits (
                            genpidm,
                            term_in (i),
                            'S',
                            NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                            NVL (sobterm_row.sobterm_time_severity, 'N'),
                            NVL (sobterm_row.sobterm_corq_severity, 'N'),
                            NVL (sobterm_row.sobterm_link_severity, 'N'),
                            NVL (sobterm_row.sobterm_preq_severity, 'N'),
                            NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                            NVL (sobterm_row.sobterm_minh_severity, 'N'),
                            NVL (sobterm_row.sobterm_mexc_severity, 'N'),     -- C3SC VK 09/14/2009 ADD
                            sfbetrm_row.sfbetrm_mhrs_over,
                            sfbetrm_row.sfbetrm_min_hrs,
                            SYS_CONTEXT ('USERENV', 'SESSIONID'),
                            'WA',
                            group_error_flag,
                            capp_tech_error,
                            minh_error,
                            term_in
                         );

                       IF capp_tech_error IS NOT NULL
                         THEN
                            ROLLBACK;
                            EXIT term_loop;
                         END IF;

                         -- Store the fact that a minimum hours error occurred, and quit all processing.

--                         IF minh_error = 'Y'
--                         THEN
--                            twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
--                            sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
--                            sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
--                         END IF;


                         IF group_error_flag = 'Y'
                         THEN
                            /* update new courses to indicate they are in error */
                            /* don't alter the rec_stat from 'N' */

            /*                UPDATE sftregs
                               SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                   sftregs_remove_ind = 'Y'
                             WHERE sftregs_term_code = term_in (i)
                               AND sftregs_pidm = genpidm
                               AND sftregs_error_flag = 'F'
                               AND sftregs_rec_stat = 'N';
                         END IF;

                         bwcklibs.p_build_drop_problems_list ( genpidm,
                                                               term_in (i),
                                                               drop_problems,
                                                               drop_failures
                                                               );
                      END IF;
              */
                     error_flag := 'Y';

                     <<update_loop>>
                     WHILE nvl(error_flag,'N') <> 'N'
                     LOOP


               --
               -- Transfers sftregs changes into sfrstcr.
               -- =======================================================================
                 sfkedit.p_update_regs (
                           genpidm,
                           term_in (i),
                           SYSDATE,
                           sgrclsr_clas_code,
                           sgbstdn_row.sgbstdn_styp_code,
                           NVL (sobterm_row.sobterm_capc_severity, 'N'),
                           sobterm_row.sobterm_tmst_calc_ind,
                           sfbetrm_row.sfbetrm_tmst_maint_ind,
                           sfbetrm_row.sfbetrm_tmst_code,
                           may_drop_last,
                           'WA',
                           error_rec,
                           error_flag,
                           tmst_flag
                        );


                        IF error_flag = 'D'
                        THEN
                           /* sfrstcr row missing - system problem */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'D');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'L'
                        THEN
                           /* last class drop attempted */
                           twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'L');
                           ROLLBACK;
                           EXIT term_loop;
                        ELSIF error_flag = 'Y'
                        THEN
                         /* capacity error */
                           /* update new courses to indicate they are in error */
                           UPDATE sftregs
                              SET sftregs_rsts_code = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2),
                                  sftregs_remove_ind = 'Y'
                            WHERE sftregs_term_code = term_in (i)
                              AND sftregs_pidm = genpidm
                              AND sftregs_error_flag = 'F'
                              AND sftregs_rec_stat = 'N';

                           group_error_flag := NULL;
                           /* re-run group edits on remaining classes */
-- Set corequisite and prerequisite checking to non-severe
                         sobterm_row.sobterm_corq_severity := 'N';
                         sobterm_row.sobterm_preq_severity := 'N';
                           sfkedit.p_web_group_edits (
                              genpidm,
                              term_in (i),
                              'S',
                              NVL (sobterm_row.sobterm_dupl_severity, 'N'),
                              NVL (sobterm_row.sobterm_time_severity, 'N'),
                              NVL (sobterm_row.sobterm_corq_severity, 'N'),
                              NVL (sobterm_row.sobterm_link_severity, 'N'),
                              NVL (sobterm_row.sobterm_preq_severity, 'N'),
                              NVL (sobterm_row.sobterm_maxh_severity, 'N'),
                              NVL (sobterm_row.sobterm_minh_severity, 'N'),
                              NVL (sobterm_row.sobterm_mexc_severity, 'N'),     -- C3SC VK 09/14/2009 ADD
                              sfbetrm_row.sfbetrm_mhrs_over,
                              sfbetrm_row.sfbetrm_min_hrs,
                              SYS_CONTEXT ('USERENV', 'SESSIONID'),
                              'WA',
                              group_error_flag,
                              capp_tech_error,
                              minh_error,
                              term_in
                           );

                           IF capp_tech_error IS NOT NULL
                           THEN
                              ROLLBACK;
                              EXIT term_loop;
                           END IF;
/* Not checking minimum hours error
                           IF minh_error = 'Y'
                           THEN
                              twbkwbis.p_setparam (genpidm, 'ERROR_FLAG', 'M');
                              sfkmods.p_delete_sftregs_by_pidm_term(genpidm, term_in (i));
                              sfkmods.p_insert_sftregs_from_stcr(genpidm, term_in (i),SYSDATE);
                           END IF;
*/

                           /* group edits found an error, reset all changes  */
                           /* to classes associated with those now in error */
                           IF group_error_flag = 'Y'
                           THEN
                           FOR error_rec IN sftregs_errors_c (genpidm, term_in (i))
                              LOOP
                                 bwcklibs.p_handle_sftregs_error (error_rec);
                              END LOOP;
                           END IF;
                        END IF;
                     END LOOP;
                  END IF; /* term exists */
               END LOOP term_loop;

            -- Redisplay the add/drop page.
            -- =======================================================================
--               bwckcoms.p_adddrop2 (
               p_adddrop2 (
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  capp_tech_error,
                  drop_result_label_in,
                  drop_problems,
                  drop_failures
               );

               RETURN;

            END  p_final_upadates2;

--SV_AUTH_CODES_BP.p_local_problems
            PROCEDURE p_local_problems (
               term_in            IN   OWA_UTIL.ident_arr,
               err_term           IN   OWA_UTIL.ident_arr,
               err_crn            IN   OWA_UTIL.ident_arr,
               err_subj           IN   OWA_UTIL.ident_arr,
               err_crse           IN   OWA_UTIL.ident_arr,
               err_sec            IN   OWA_UTIL.ident_arr,
               err_code           IN   OWA_UTIL.ident_arr,
               err_levl           IN   OWA_UTIL.ident_arr,
               err_cred           IN   OWA_UTIL.ident_arr,
               err_gmod           IN   OWA_UTIL.ident_arr,
               drop_problems_in   IN   sfkcurs.drop_problems_rec_tabtype,
               drop_failures_in   IN   sfkcurs.drop_problems_rec_tabtype,
               add_row                 NUMBER,
               call_procedure     IN   VARCHAR2,
               orig_crn           IN    OWA_UTIL.ident_arr,
               orig_rsts          IN    OWA_UTIL.ident_arr
            )
            IS
               unique_drop        BOOLEAN      := FALSE;
               autodrop_setting   VARCHAR2 (1);
               drop_msg           VARCHAR2 (15) := NULL;
            BEGIN

            --
            -- Initialize genpidm based on faculty/student.
            -- =================================================

--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--               ELSE
                  genpidm := global_pidm;
--               END IF;

               IF drop_problems_in.EXISTS (1)
               THEN
-- Set autodrop to C for exchange                  autodrop_setting := sfkfunc.f_autodrop_setting;
                  autodrop_setting := 'C';
            --
            -- Autodrop rule set to 'Y' - problems list related drops are
            -- automatically dropped.
            -- ======================================================================
                  IF autodrop_setting = 'Y'
                  THEN
                     bwcklibs.p_confirm_drops (genpidm, term_in, drop_problems_in);
                     drop_msg := 'DROPCONFIRMED';
            --
            -- Autodrop rule set to 'C' - The user can confirm/reject problems list
            -- related drops.
            -- ======================================================================
                  ELSIF autodrop_setting = 'C'
                  THEN
                     bwckcoms.p_disp_confirm_drops (
                        term_in,
                        err_term,
                        err_crn,
                        err_subj,
                        err_crse,
                        err_sec,
                        err_code,
                        err_levl,
                        err_cred,
                        err_gmod,
                        drop_problems_in,
                        drop_failures_in
                     );
                     RETURN;
            --
            -- Autodrop rule set to 'N' - problems list related drops cannot be
            -- dropped.
            -- ======================================================================
                  ELSE
                     bwcklibs.p_reset_drops (genpidm, drop_problems_in);
                     drop_msg := 'DROPPROHIBITED';
                  END IF;
               END IF;


            --
            -- Failures list (problems that can't be given a drop code) courses
            -- changes are rejected.
            -- ======================================================================
               --C3SC GR 06/21/2006  ADDED
               IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
                AND call_procedure <> 'bwvkauth.P_ProcAuthCode'
               THEN
                 sv_auth_addcodes_bp.P_local_reset_sftregs_fields(genpidm, drop_failures_in);
               ELSE
                 bwcklibs.p_reset_failures (genpidm, drop_failures_in);

               END IF;
              --END C3SC


            --
            -- Transfer all changes in SFTREGS to SFRSTCR.
            -- =====================================================================
               --C3SC GR 06/21/2006 ADDED
              IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
                 AND call_procedure <> 'bwvkauth.P_ProcAuthCode'
               THEN
-- Call local                sv_auth_addcodes_bp.p_local_final_updates(
                p_local_final_updates(
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  drop_msg,
                  drop_problems_in,
                  drop_failures_in,
                  add_row,
                  orig_crn   ,
                  orig_rsts
                  );

               ELSIF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
                 AND call_procedure = 'bwvkauth.P_ProcAuthCode' THEN


--                  bwvkauth.p_final_upadates2(
                  p_final_upadates2(
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  drop_msg,
                  drop_problems_in,
                  drop_failures_in,
                  add_row
                  );


               ELSE


--                  bwckregs.p_final_updates (
                 p_final_updates (
                    term_in,
                    err_term,
                    err_crn,
                    err_subj,
                    err_crse,
                    err_sec,
                    err_code,
                    err_levl,
                    err_cred,
                    err_gmod,
                    drop_msg,
                    drop_problems_in,
                    drop_failures_in
                 );


               END IF;
               RETURN;
            END p_local_problems;

--BWCKCOMS.p_problems rewritten
            PROCEDURE p_problems (
               term_in            IN   OWA_UTIL.ident_arr,
               err_term           IN   OWA_UTIL.ident_arr,
               err_crn            IN   OWA_UTIL.ident_arr,
               err_subj           IN   OWA_UTIL.ident_arr,
               err_crse           IN   OWA_UTIL.ident_arr,
               err_sec            IN   OWA_UTIL.ident_arr,
               err_code           IN   OWA_UTIL.ident_arr,
               err_levl           IN   OWA_UTIL.ident_arr,
               err_cred           IN   OWA_UTIL.ident_arr,
               err_gmod           IN   OWA_UTIL.ident_arr,
               drop_problems_in   IN   sfkcurs.drop_problems_rec_tabtype,
               drop_failures_in   IN   sfkcurs.drop_problems_rec_tabtype
            )
            IS
               unique_drop        BOOLEAN      := FALSE;
               autodrop_setting   VARCHAR2 (1);
               drop_msg           VARCHAR2 (15) := NULL;
            BEGIN

            --
            -- Initialize genpidm based on faculty/student.
            -- =================================================

--               IF NOT twbkwbis.f_validuser (global_pidm)
--               THEN
--                  RETURN;
--               END IF;

--               IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--               THEN
--                  genpidm :=
--                      TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--               ELSE
                  genpidm := global_pidm;
--               END IF;
               IF drop_problems_in.EXISTS (1)
               THEN
-- Set autodrop to C for exchange                  autodrop_setting := sfkfunc.f_autodrop_setting;
                  autodrop_setting := 'C';

            --
            -- Autodrop rule set to 'Y' - problems list related drops are
            -- automatically dropped.
            -- ======================================================================
                  IF autodrop_setting = 'Y'
                  THEN
                     bwcklibs.p_confirm_drops (genpidm, term_in, drop_problems_in);
                     drop_msg := 'DROPCONFIRMED';
            --
            -- Autodrop rule set to 'C' - The user can confirm/reject problems list
            -- related drops.
            -- ======================================================================
                  ELSIF autodrop_setting = 'C'
                  THEN
                     bwckcoms.p_disp_confirm_drops (
                        term_in,
                        err_term,
                        err_crn,
                        err_subj,
                        err_crse,
                        err_sec,
                        err_code,
                        err_levl,
                        err_cred,
                        err_gmod,
                        drop_problems_in,
                        drop_failures_in
                     );
                     RETURN;
            --
            -- Autodrop rule set to 'N' - problems list related drops cannot be
            -- dropped.
            -- ======================================================================
                  ELSE
                     bwcklibs.p_reset_drops (genpidm, drop_problems_in);
                     drop_msg := 'DROPPROHIBITED';
                  END IF;
               END IF;
            --
            -- Failures list (problems that can't be given a drop code) courses
            -- changes are rejected.
            -- ======================================================================
               bwcklibs.p_reset_failures (genpidm, drop_failures_in);
            --
            -- Transfer all changes in SFTREGS to SFRSTCR.
            -- =====================================================================
-- Call local               bwckregs.p_final_updates (
               p_final_updates (
                  term_in,
                  err_term,
                  err_crn,
                  err_subj,
                  err_crse,
                  err_sec,
                  err_code,
                  err_levl,
                  err_cred,
                  err_gmod,
                  drop_msg,
                  drop_problems_in,
                  drop_failures_in
               );
               RETURN;
            END p_problems;

""";

        def plsql5 = """
--BWCKREGS.p_dropcrse rewritten
               PROCEDURE p_dropcrse (
                  term           stvterm.stvterm_code%TYPE,
                  crn            sftregs.sftregs_crn%TYPE,
                  rsts           sftregs.sftregs_rsts_code%TYPE,
                  reserved_key   sftregs.sftregs_reserved_key%TYPE,
                  rec_stat       sftregs.sftregs_rec_stat%TYPE,
                  subj           ssbsect.ssbsect_subj_code%TYPE DEFAULT NULL,
                  crse           ssbsect.ssbsect_crse_numb%TYPE DEFAULT NULL,
                  seq            ssbsect.ssbsect_seq_numb%TYPE DEFAULT NULL,
                  del_ind        VARCHAR2 DEFAULT NULL
               )
               IS
                  genpidm            spriden.spriden_pidm%TYPE;
                  loop_sftregs_row   sftregs%ROWTYPE;
                  discount_exist           varchar2(200);  -- FR 8.4.0.2
                  lv_function varchar2(300);               -- FR 8.4.0.2
               BEGIN
--                  IF NOT twbkwbis.f_validuser (global_pidm)
--                  THEN
--                     RETURN;
--                  END IF;

--                  IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--                  THEN
--                     genpidm :=
--                       TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--                  ELSE
                     genpidm := global_pidm;
--                  END IF;
            --
            --  FR 8.4.0.2
            --
             IF genutil.product_installed('SF') = 'Y' THEN
                  lv_function := 'begin :1 := sffkutil.f_check_discount(:2,:3,:4); end;';
                  EXECUTE IMMEDIATE lv_function
                  USING   OUT discount_exist,
                          IN  genpidm,crn,term;
                  IF discount_exist = 'Y' THEN
                      raise_application_error(-20610, bwcklibs.error_msg_table(-20610));
                  END IF;
                END IF;

            --
            --
                  ssbsect_row.ssbsect_subj_code := subj;
                  ssbsect_row.ssbsect_crse_numb := crse;
                  ssbsect_row.ssbsect_seq_numb := seq;
                  row_count := 0;
                  sql_error := 0;
                  regs_date := bwcklibs.f_getregdate;

-- Call local                 IF NOT bwckregs.f_validcrn (sql_error, term, crn => crn)
                  IF NOT f_validcrn (sql_error, term, crn => crn)
                  THEN
                     raise_application_error (
                        sql_error,
                        bwcklibs.error_msg_table (sql_error)
                     );
                  END IF;

                  loop_sftregs_row := NULL;
                  OPEN sfkcurs.sftregsc (genpidm, term);

                  LOOP
                     FETCH sfkcurs.sftregsc INTO loop_sftregs_row;
                     EXIT WHEN sfkcurs.sftregsc%NOTFOUND;
                     IF sftregs_row.sftregs_crn = loop_sftregs_row.sftregs_crn
                     THEN
                        row_count := row_count + 1;
                        old_sftregs_row := loop_sftregs_row;
                     END IF;
                  END LOOP;

                  CLOSE sfkcurs.sftregsc;

                  -- IF all conditions true, display can't drop last class message
                     IF row_count = 0
                     THEN
                        raise_application_error (-20539, bwcklibs.error_msg_table (-20539));
                     END IF;

                     OPEN stkrsts.stvrstsc (old_sftregs_row.sftregs_rsts_code);
                     FETCH stkrsts.stvrstsc INTO stvrsts_row;

                     IF stkrsts.stvrstsc%NOTFOUND
                     THEN
                        NULL;
                     ELSE
                        old_stvrsts_row.stvrsts_wait_ind := stvrsts_row.stvrsts_wait_ind;
                        old_stvrsts_row.stvrsts_incl_sect_enrl :=
                                                       stvrsts_row.stvrsts_incl_sect_enrl;
                     END IF;

                     CLOSE stkrsts.stvrstsc;
                     sftregs_row.sftregs_rsts_code := rsts;

                     FOR stvrsts IN stkrsts.stvrstsc (rsts)
                     LOOP
                        stvrsts_row := stvrsts;
                     END LOOP;

                     IF del_ind IS NULL
                     THEN
-- Call local                        IF NOT bwckregs.f_validrsts (sql_error)
                        IF NOT f_validrsts (sql_error)
                        THEN
                           raise_application_error (
                              sql_error,
                              bwcklibs.error_msg_table (sql_error)
                           );
                        END IF;
                     END IF;

                     IF    stvrsts_row.stvrsts_incl_sect_enrl = 'Y'
                        OR stvrsts_row.stvrsts_incl_assess = 'Y'
                     THEN
                        raise_application_error (-20540, bwcklibs.error_msg_table (-20540));
                     END IF;

                     IF sftregs_row.sftregs_grde_date IS NOT NULL
                     THEN
                        raise_application_error (-20541, bwcklibs.error_msg_table (-20541));
                     END IF;
                     bwcklibs.p_del_sftregs (genpidm, term, crn, rsts, rec_stat);
               END p_dropcrse;

--BWCKCOMS.P_Regs rewritten
           PROCEDURE p_regs (
              term_in         IN   OWA_UTIL.ident_arr,
              rsts_in         IN   OWA_UTIL.ident_arr,
              assoc_term_in   IN   OWA_UTIL.ident_arr,
              crn_in          IN   OWA_UTIL.ident_arr,
              start_date_in   IN   OWA_UTIL.ident_arr,
              end_date_in     IN   OWA_UTIL.ident_arr,
              subj            IN   OWA_UTIL.ident_arr,
              crse            IN   OWA_UTIL.ident_arr,
              sec             IN   OWA_UTIL.ident_arr,
              levl            IN   OWA_UTIL.ident_arr,
              cred            IN   OWA_UTIL.ident_arr,
              gmod            IN   OWA_UTIL.ident_arr,
              title           IN   bwckcoms.varchar2_tabtype,
              mesg            IN   OWA_UTIL.ident_arr,
              reg_btn         IN   OWA_UTIL.ident_arr,
              regs_row             NUMBER,
              add_row              NUMBER,
              wait_row             NUMBER
           )
           IS
              i                     INTEGER                        := 2;
              k                     INTEGER                        := 1;
              err_term              OWA_UTIL.ident_arr;
              err_crn               OWA_UTIL.ident_arr;
              err_subj              OWA_UTIL.ident_arr;
              err_crse              OWA_UTIL.ident_arr;
              err_sec               OWA_UTIL.ident_arr;
              err_code              OWA_UTIL.ident_arr;
              err_levl              OWA_UTIL.ident_arr;
              err_cred              OWA_UTIL.ident_arr;
              err_gmod              OWA_UTIL.ident_arr;
              clas_code             SGRCLSR.SGRCLSR_CLAS_CODE%TYPE;
              stvterm_rec           stvterm%ROWTYPE;
              sorrtrm_rec           sorrtrm%ROWTYPE;
              stufac_ind            VARCHAR2 (1);
              term                  stvterm.stvterm_code%TYPE      := NULL;
              multi_term            BOOLEAN                        := TRUE;
              olr_course_selected   BOOLEAN                        := FALSE;
              capp_tech_error       VARCHAR2 (4);
              minh_admin_error      VARCHAR2 (1);
              assoc_term            OWA_UTIL.ident_arr             := assoc_term_in;
              crn                   OWA_UTIL.ident_arr;
              crn_index             NUMBER;
              crn_in_index          NUMBER;
              start_date            OWA_UTIL.ident_arr             := start_date_in;
              end_date              OWA_UTIL.ident_arr             := end_date_in;
              etrm_done             BOOLEAN                        := FALSE;
              drop_problems         sfkcurs.drop_problems_rec_tabtype;
              drop_failures         sfkcurs.drop_problems_rec_tabtype;
              local_capp_tech_error VARCHAR2 (30);
              called_by_proc_name   VARCHAR2 (100);
              web_rsts_checkc_flag  VARCHAR2(1);
              lv_literal            VARCHAR2(30);
              --- FHDA Mod Begin  08/16/2012
              --- Use these variables to record courses that already have two previous sub grades
              subg_term              OWA_UTIL.ident_arr;
              subg_crn               OWA_UTIL.ident_arr;
              subg_subj              OWA_UTIL.ident_arr;
              subg_crse              OWA_UTIL.ident_arr;
              subg_sec               OWA_UTIL.ident_arr;
              subg_course_selected   boolean                        := false;
              f                      integer                        := 1;
              user_ack_parm          constant varchar2(10)          := 'prev2subg';
              adding_class           boolean                        := false;
              --- FHDA Mod End

              CURSOR web_rsts_checkc(rsts_in VARCHAR2)
                 IS
                 SELECT 'Y'
                   FROM stvrsts
                  WHERE stvrsts_code = rsts_in;
-- Enable non-web enabled reg codes
--                    AND stvrsts_web_ind = 'Y';

           BEGIN
--              IF NOT twbkwbis.f_validuser (global_pidm)
--              THEN
--                 RETURN;
--              END IF;

              term := term_in (term_in.COUNT);

              IF term_in.COUNT = 1
              THEN
                 multi_term := FALSE;
              END IF;

              --
              -- Remove any null leading crns from the to-add list.
              -- ==================================================
              crn_index := 1;

              FOR crn_in_index IN 1..crn_in.COUNT
              LOOP
                 IF crn_in(crn_in_index) IS NOT NULL
                 THEN
                    crn(crn_index) := crn_in(crn_in_index);
                    crn_index := crn_index + 1;
                 END IF;
               END LOOP;
              --

--              IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC'
--              THEN
--                 genpidm :=
--                   TO_NUMBER (twbkwbis.f_getparam (global_pidm, 'STUPIDM'), '999999999');
--                 stufac_ind := 'F';
--                 called_by_proc_name := 'bwlkfrad.P_FacAddDropCrse';

--                 FOR i IN 1 .. term_in.COUNT
--                 LOOP
--                    IF NOT bwckcoms.f_reg_access_still_good (
--                          genpidm,
--                          term_in (i),
--                          stufac_ind || global_pidm,
--                          called_by_proc_name
--                       )
--                    THEN
--                       RETURN;
--                    END IF;
--                 END LOOP;
--              ELSE
                 genpidm := global_pidm;
                 stufac_ind := 'S';
                 called_by_proc_name := 'bwskfreg.P_AddDropCrse';

                 FOR i IN 1 .. term_in.COUNT
                 LOOP
-- Call local                   IF NOT bwckcoms.f_reg_access_still_good (
                      IF NOT f_reg_access_still_good (
                          genpidm,
                          term_in (i),
                          stufac_ind || global_pidm,
                          called_by_proc_name
                       )
                    THEN
--                       RETURN;
-- Raise an error rather than a blank page.
                      raise_application_error(-20001, 'An administrator is modifying your records.');
                    END IF;
                 END LOOP;
--              END IF;

              FOR i IN 2 .. rsts_in.COUNT
                 LOOP
                    IF rsts_in(i) IS NOT NULL
                    THEN
                       OPEN web_rsts_checkc(rsts_in(i));
                       FETCH web_rsts_checkc INTO web_rsts_checkc_flag;

                       IF web_rsts_checkc%NOTFOUND
                       THEN
                          CLOSE web_rsts_checkc;
--                          twbkwbis.p_dispinfo (called_by_proc_name, 'BADRSTS');
-- Raise an error for bad rsts code.
                          raise_application_error(-20001,'Invalid RSTS code in custom adaptor config');
                          RETURN;
                       END IF;

                       CLOSE web_rsts_checkc;
                    END IF;
                 END LOOP;

              IF NOT multi_term
              THEN
                 -- This added for security reasons, in order to prevent students
                 -- saving the add/droppage while registration is open, and
                 -- re-using the saved page after it has closed
                 IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
                 THEN
--                    twbkwbis.p_dispinfo ('bwskflib.P_SelDefTerm', 'BADTERM');
                    raise_application_error(-20001, 'Term selected for registration is not open at this time');
                    RETURN;
                 END IF;
              END IF;

        --
        -- If the Select Study Path button was pressed...
        -- ===================================================
/* Not supporting studypath registration at this time.
              lv_literal := g\$_nls.get ('BWCKCOM1-0025',
                               'SQL',
                               'Select Study Path');
              IF LTRIM (RTRIM (reg_btn (2))) = lv_literal
              THEN
                bwckcoms.P_DeleteStudyPath ;
                IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
                    bwckcoms.P_SelStudyPath(NULL,
                                     'bwlkfrad.P_FacAddDropCrseCRN',
                                    NULL,
                                    NULL,
                                    term_in,
                                    assoc_term_in,
                                    crn,
                                    rsts_in,
                                    crn_in,
                                    start_date_in,
                                    end_date_in,
                                    subj,
                                    crse,
                                    sec,
                                    levl,
                                    cred,
                                    gmod,
                                    title,
                                    mesg,
                                    regs_row,
                                    add_row,
                                    wait_row);

                ELSE
                   bwckcoms.P_SelStudyPath(NULL,
                                    'bwskfreg.p_adddropcrseCRN',
                                    NULL,
                                    NULL,
                                    term_in,
                                    assoc_term_in,
                                    crn,
                                    rsts_in,
                                    crn_in,
                                    start_date_in,
                                    end_date_in,
                                    subj,
                                    crse,
                                    sec,
                                    levl,
                                    cred,
                                    gmod,
                                    title,
                                    mesg,
                                    regs_row,
                                    add_row,
                                    wait_row);


                END IF;
                RETURN;
              END IF;
*/

/* Not necessary for UI only
        -- If the class search button was pressed...
        -- ===================================================
              lv_literal := g\$_nls.get ('BWCKCOM1-0026',
                               'SQL',
                               'Class Search');
              IF LTRIM (RTRIM (reg_btn (2))) = lv_literal
              THEN

                 bwckgens.P_RegsCrseSearch (
                    term_in,
                    rsts_in,
                    assoc_term_in,
                    crn,
                    start_date_in,
                    end_date_in,
                    subj,
                    crse,
                    sec,
                    levl,
                    cred,
                    gmod,
                    title,
                    mesg,
                    regs_row,
                    add_row,
                    wait_row
                 );
                 RETURN;
              END IF;
*/
        --
        -- If the Register, or Submit button was pressed.
        -- ===================================================
        --
        -- Setup globals.
        -- ===================================================
              bwcklibs.p_initvalue (global_pidm, term, '', SYSDATE, '', '');

        --
        -- Check if student is allowed to register - single term
        -- only, as multi term is handled in the search results
        -- page.
        -- ===================================================
              IF NOT multi_term
              THEN
-- Call local                 IF NOT bwcksams.f_regsstu (
                 IF NOT f_regsstu (
                          genpidm,
                          term,
                          called_by_proc_name
                       )
                 THEN
                    RETURN;
                 END IF;
              END IF;

        --
        -- Check whether OLR courses have been chosen, and if so,
        -- confirm their start/end dates
        -- ======================================================

              FOR i IN 2 .. crn.COUNT
              LOOP
                 IF crn (i) IS NULL
                 THEN
                    EXIT;
                 END IF;

                 IF     NOT multi_term
                    AND (   assoc_term (i) IS NULL
                         OR assoc_term (i) = 'dummy')
                 THEN
                    assoc_term (i) := term;
                 END IF;

                 IF NOT start_date.EXISTS (i)
                 THEN
                    start_date (i) := NULL;
                    end_date (i) := NULL;
                 END IF;

                 IF NOT olr_course_selected
                 THEN
                    IF sfkolrl.f_open_learning_course (assoc_term (i), crn (i))
                    THEN
                       IF start_date (i) IS NULL
                       THEN
                          olr_course_selected := TRUE;
                       END IF;
                    END IF;
                 END IF;
              END LOOP;

              IF olr_course_selected
              THEN
/* OLR course was selected and no start/end date exists in OEI registration.
                 bwckcoms.p_disp_start_date_confirm (
                    term_in,
                    rsts_in,
                    assoc_term,
                    crn,
                    start_date,
                    end_date,
                    subj,
                    crse,
                    sec,
                    levl,
                    cred,
                    gmod,
                    title,
                    mesg,
                    reg_btn,
                    regs_row,
                    add_row,
                    wait_row,
                    NULL,
                    NULL
                 );
*/
                 raise_application_error(-20001, 'Open learning course was selected and OEI does not support start and end dates');
                 RETURN;
              END IF;
        --
        --
        -- FHDA Mod Begin  08/07/2012
        -- Check if one of the courses has 2 previous substandard grades. If true, call p_disp_warning_confirm, which in turn
        -- calls p_proc_warning_confirm. TWGRWRPM table is used to keep track of whether a student has acknowledged and proceeded.
        -- TWGRWPRM record is deleted right away so that it does not impact the next transaction. Use err_term, err_crn, err_subj,
        -- err_crse (OWA_UTIL.ident_arr) to record CRNs that have prior two substandard grades.
        -- The parameter name used in TWGRWPRM table is prev2subg, a Y for this param indicates a course with two prior sub grades
        --

        -- Look for TWGRWRPM entry confirmed = Y, once found, don't call p_disp_warning_confirm, just delete the TWGRWRPM entry and proceed with
        -- normal flow. If not found, redirect to p_disp_warning_confirm, where user can choose to proceed or go back, which is recorded in
        -- TWGRWRPM by procedure p_proc_warning_confirm, which in turn calls p_regs.

        -- need to find out what the user action is: add or drop?
        /*
             FOR i IN 2 .. crn.COUNT LOOP

               htp.p(assoc_term (i)||'------'|| crn (i) ||' -- '||(rsts_in (i)));

            END LOOP;

            for iter in reg_btn.first .. reg_btn.last loop

              htp.p(reg_btn(iter));
            end loop;

           htp.p(regs_row);
           htp.p(add_row);
           htp.p(wait_row);
        */
        -- Initialize arrays

        -- ===================================================
              subg_term (1) := 'dummy';
              subg_crn (1) :=  'dummy';
              subg_subj (1) := 'dummy';
              subg_crse (1) := 'dummy';
              subg_sec (1) :=  'dummy';

        -- Check whether a course already has been taken with two
        -- previous sub grades, and if so,
        -- record it in the subg_xxx array
        -- ======================================================

        -- If user already confirmed the warning, do not need to check courses

           if twbkwbis.f_getparam(genpidm, user_ack_parm) is null then


              FOR i IN 2 .. crn.COUNT LOOP
                 IF crn (i) IS NULL THEN
                    EXIT;
                 END IF;

                 IF     NOT multi_term
                    AND (   assoc_term (i) IS NULL
                         OR assoc_term (i) = 'dummy')  THEN
                    assoc_term (i) := term;
                 end if;

                 -- check if user is doing an add or drop
                 if rsts_in (i) = 'RW' then

                   adding_class := true;

                 end if;


                 -- check if a class has two prior sub standardard grades
                 if f_subg_crse_selected(genpidm, assoc_term (i), crn (i) ) then
                   --htp.p('inside the true condition ++++'||genpidm ||' '||assoc_term (i)|| '  '|| '  '||crn (i));
                    subg_course_selected := true;
                    -- record the courses in violation
                       f := f + 1;

-- Call local                       bwckregs.p_getsection (assoc_term (i), crn (i), subg_subj (f), subg_crse (f), subg_sec (f));
                       p_getsection (assoc_term (i), crn (i), subg_subj (f), subg_crse (f), subg_sec (f));
                       subg_term (f) := assoc_term (i);
                       subg_crn (f)  := crn (i);

                 END IF;
              END LOOP;


              if subg_course_selected  and adding_class then
/* Not displaying web message.
                 bwckcoms.p_disp_warning_confirm (
                    term_in,
                    rsts_in,
                    assoc_term,
                    crn,
                    start_date,
                    end_date,
                    subj,
                    crse,
                    sec,
                    levl,
                    cred,
                    gmod,
                    title,
                    mesg,
                    reg_btn,
                    regs_row,
                    add_row,
                    wait_row,
                    NULL,
                    null,
                    subg_term,
                    subg_crn,
                    subg_subj,
                    subg_crse,
                    subg_sec
                 );
*/
                 raise_application_error(-20001, 'Student has taken the course two times with sub grade results must see teaching college administrative staff');
                 RETURN;
              END IF;

           END IF; -- end of checking twgrwprm entry
/*
           --- delete the twgrwprm entry for this transaction to
                 --- clear the way for the next transaction
                 begin
                    delete from twgrwprm m
                    where m.twgrwprm_pidm = genpidm
                    and m.twgrwprm_param_name= user_ack_parm;

                    commit;
                 end;
*/           ----
        -- End of FHDA mod
        --
        -- Initialize arrays.
        -- ===================================================
              err_term (1) := 'dummy';
              err_crn (1) := 'dummy';
              err_code (1) := 'dummy';
              err_subj (1) := 'dummy';
              err_crse (1) := 'dummy';
              err_sec (1) := 'dummy';
              err_levl (1) := 'dummy';
              err_cred (1) := 'dummy';
              err_gmod (1) := 'dummy';

        --
        -- Check for admin errors that may have been introduced
        -- during this add/drop session.
        -- ====================================================
              FOR i IN 1 .. term_in.COUNT
              LOOP

                /* Reset any linked courses that should be dropped. */
                /* This is so that they will be re-flagged as requiring drop confirmation. */
                /* This handles the scenario where a user presses the back button from */
                /* the drop confirmation page. */
                 UPDATE sftregs a
                 SET a.sftregs_error_flag = a.sftregs_hold_error_flag,
                     a.sftregs_rmsg_cde = a.sftregs_hold_rmsg_cde,
                     a.sftregs_message = a.sftregs_hold_message,
                     a.sftregs_rsts_code = a.sftregs_hold_rsts_code,
                     a.sftregs_rsts_date = a.sftregs_hold_rsts_date,
                     a.sftregs_vr_status_type = a.sftregs_hold_rsts_type,
                     a.sftregs_grde_code = a.sftregs_hold_grde_code,
                     a.sftregs_user = goksels.f_get_ssb_id_context,
                     a.sftregs_activity_date = SYSDATE
                 WHERE a.sftregs_term_code = term_in (i)
                 AND a.sftregs_pidm = genpidm
                 AND a.sftregs_error_link IS NOT NULL
                 AND a.sftregs_error_link = ( SELECT b.sftregs_error_link
                                            FROM sftregs b
                                            WHERE b.sftregs_term_code = a.sftregs_term_code
                                            AND b.sftregs_pidm = a.sftregs_pidm
                                            AND b.sftregs_crn <> a.sftregs_crn
                                            AND b.sftregs_error_flag = 'F'
                                            AND ROWNUM = 1);

                 UPDATE sftregs
                 SET sftregs_error_flag = sftregs_hold_error_flag,
                     sftregs_rmsg_cde = sftregs_hold_rmsg_cde,
                     sftregs_message = sftregs_hold_message,
                     sftregs_rsts_code = sftregs_hold_rsts_code,
                     sftregs_rsts_date = sftregs_hold_rsts_date,
                     sftregs_vr_status_type = sftregs_hold_rsts_type,
                     sftregs_grde_code = sftregs_hold_grde_code,
                     sftregs_user = goksels.f_get_ssb_id_context,
                     sftregs_activity_date = SYSDATE
                 WHERE sftregs_term_code = term_in (i)
                 AND sftregs_pidm = genpidm
                 AND sftregs_error_flag = 'F';

        -- C3SC GRS 04/09/2008 Modify
              -- Defect 1-3JBTM3  Move delete from sftregs to fix back button issue
              -- because it was causing dissapearing issues when a course was already registered.
                delete from sftregs
                where sftregs_pidm = genpidm
                  and sftregs_term_code = term_in (i)
                  and sftregs_error_flag = 'F'
                  and sftregs_message like G\$_NLS.FormatMsg ('x','SQL','%AUTH REQD%');
             -- C3SC END


                 sfkmods.p_admin_msgs (
                       genpidm,
                       term_in (i),
                       stufac_ind || global_pidm
                    );

/* Not checking minimum hours error.
                 minh_admin_error := sfkfunc.f_get_minh_admin_error(genpidm, term_in(i));
                 IF minh_admin_error = 'Y' THEN
                    bwckfrmt.p_open_doc (called_by_proc_name, term, NULL,
                                         multi_term, term_in(1));
                    twbkwbis.p_dispinfo (called_by_proc_name, 'SEE_ADMIN');
                    twbkwbis.p_closedoc (curr_release);
                    RETURN;
                 END IF;
*/
                 local_capp_tech_error :=
                   sfkfunc.f_get_capp_tech_error (genpidm, term_in(i));
                 IF local_capp_tech_error IS NOT NULL
                 THEN
--                    bwckcoms.p_adddrop2 ( term_in,
                             p_adddrop2 ( term_in,
                                          err_term,
                                          err_crn,
                                          err_subj,
                                          err_crse,
                                          err_sec,
                                          err_code,
                                          err_levl,
                                          err_cred,
                                          err_gmod,
                                          local_capp_tech_error,
                                          NULL,
                                          drop_problems,
                                          drop_failures );
                    raise_application_error(-20001, 'Banner CAPP errors exist');
                    RETURN;
                 END IF;

                 IF NOT bwckregs.f_finalize_admindrops (genpidm, term_in(i),
                                                        stufac_ind || global_pidm)
                 THEN
--                     bwckfrmt.p_open_doc (called_by_proc_name, term, NULL,
--                                          multi_term, term_in(1));
--                     twbkwbis.p_dispinfo (called_by_proc_name, 'SESSIONBLOCKED');
--                     twbkwbis.p_closedoc (curr_release);

                     RETURN;
                 END IF;

              END LOOP;

              i := 2;

        --
        -- Loop through the registration records on the page.
        -- ===================================================
              WHILE i <= regs_row + 1
              LOOP
                 BEGIN
                    IF LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
                    THEN
                       sftregs_rec.sftregs_crn := crn (i);
                       sftregs_rec.sftregs_pidm := genpidm;
                       sftregs_rec.sftregs_term_code := assoc_term (i);
                       sftregs_rec.sftregs_rsts_code := rsts_in (i);

                       BEGIN
                          IF multi_term
                          THEN
        --
        -- Get the latest student and registration records.
        -- ===================================================
-- Call local                            bwckcoms.p_regs_etrm_chk (
                             p_regs_etrm_chk (
                                genpidm,
                                assoc_term (i),
                                clas_code,
                                multi_term
                             );
                          ELSIF NOT etrm_done
                          THEN
-- Call local                            bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                             p_regs_etrm_chk (genpidm, term, clas_code);
                             etrm_done := TRUE;
                          END IF;

        --
        -- Get the section information.
        -- ===================================================
-- Call local                         bwckregs.p_getsection (
                          p_getsection (
                             sftregs_rec.sftregs_term_code,
                             sftregs_rec.sftregs_crn,
                             sftregs_rec.sftregs_sect_subj_code,
                             sftregs_rec.sftregs_sect_crse_numb,
                             sftregs_rec.sftregs_sect_seq_numb
                          );

        --
        -- If the action corresponds to gtvsdax web drop code,
        -- then call the procedure to drop a course.
        -- ===================================================
                          IF rsts_in (i) = SUBSTR (f_stu_getwebregsrsts ('D'), 1, 2)
                          THEN

-- Call local                            bwckregs.p_dropcrse (
                             p_dropcrse(
                                sftregs_rec.sftregs_term_code,
                                sftregs_rec.sftregs_crn,
                                sftregs_rec.sftregs_rsts_code,
                                sftregs_rec.sftregs_reserved_key,
                                rec_stat   => 'D',
                                del_ind    => 'Y'
                             );
        --
        -- If the second character of the action is not D, then
        -- call the procedure to update a course.
        -- ===================================================
                          ELSE
-- Not supporting updates at this time, pull in if required in the future.
--                             bwckregs.p_updcrse (sftregs_rec);
                             raise_application_error(-20001, 'Updates are not supported.');
                          END IF;

        --
        -- Create a batch fee assessment record.
        -- ===================================================
                          sfrbtch_row.sfrbtch_term_code := assoc_term (i);
                          sfrbtch_row.sfrbtch_pidm := genpidm;
                          sfrbtch_row.sfrbtch_clas_code := clas_code;
                          sfrbtch_row.sfrbtch_activity_date := SYSDATE;
                          bwcklibs.p_add_sfrbtch (sfrbtch_row);
                          tbrcbrq_row.tbrcbrq_term_code := assoc_term (i);
                          tbrcbrq_row.tbrcbrq_pidm := genpidm;
                          tbrcbrq_row.tbrcbrq_activity_date := SYSDATE;
                          bwcklibs.p_add_tbrcbrq (tbrcbrq_row);
                       EXCEPTION
                          WHEN OTHERS
                          THEN
                             IF SQLCODE = gb_event.APP_ERROR THEN
                                twbkfrmt.p_storeapimessages(SQLERRM);
                                RAISE;
                             END IF;
                             k := k + 1;
                             err_term (k) := assoc_term (i);
                             err_crn (k)  := crn (i);
                             err_subj (k) := sftregs_rec.sftregs_sect_subj_code;
                             err_crse (k) := sftregs_rec.sftregs_sect_crse_numb;
                             err_sec (k)  := sftregs_rec.sftregs_sect_seq_numb;
                             err_code (k) := SQLCODE;
                             err_levl (k) := lcur_tab(1).r_levl_code;
                             err_cred (k) := TO_CHAR (sftregs_rec.sftregs_credit_hr);
                             err_gmod (k) := sftregs_rec.sftregs_gmod_code;
                          END;
                    END IF;

                    i := i + 1;
                 EXCEPTION
                    WHEN OTHERS
                    THEN
                       IF SQLCODE = gb_event.APP_ERROR THEN
                          twbkfrmt.p_storeapimessages(SQLERRM);
                          RAISE;
                       END IF;
                       i := i + 1;
                 END;
              END LOOP;

        --
        -- Loop through the add table on the page.
        -- ===================================================

              WHILE i <= regs_row + wait_row + add_row + 1
              LOOP
                 DECLARE
                 BEGIN
                    IF     crn (i) IS NOT NULL
                       AND LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
                    THEN
                       sftregs_rec.sftregs_crn := crn (i);
                       sftregs_rec.sftregs_rsts_code := rsts_in (i);
                       sftregs_rec.sftregs_rsts_date := SYSDATE;
                       sftregs_rec.sftregs_pidm := genpidm;
                       sftregs_rec.sftregs_term_code := assoc_term (i);

                       BEGIN
                          IF multi_term
                          THEN
        --
        -- Get the latest student and registration records.
        -- ===================================================
-- Call local                             bwckcoms.p_regs_etrm_chk (
                             p_regs_etrm_chk (
                                genpidm,
                                assoc_term (i),
                                clas_code,
                                multi_term
                             );
                          ELSIF NOT etrm_done
                          THEN
-- Call local                             bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                             p_regs_etrm_chk (genpidm, term, clas_code);
                             etrm_done := TRUE;
                          END IF;
        -- Do check for study path status also. Create sfrensp if it doesnt exist for the studypath
/* Not supporting study paths at this time.
                        IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
                          IF NOT multi_term THEN

                            bwckcoms.p_regs_ensp_chk(
                                        genpidm,
                                        SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1),
                                        twbkwbis.f_getparam (global_pidm, 'TERM')
                                                   );
                          ELSE
                            IF twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)) <> 'NULL' THEN
                              bwckcoms.p_regs_ensp_chk(
                                          genpidm,
                                          twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)),
                                          assoc_term (i)
                                                     );
                            END IF;
                          END IF;

                        END IF;
*/

        --
        --
        -- Get the section information.
        -- ===================================================
-- Call local                          bwckregs.p_getsection (
                          p_getsection (
                             sftregs_rec.sftregs_term_code,
                             sftregs_rec.sftregs_crn,
                             sftregs_rec.sftregs_sect_subj_code,
                             sftregs_rec.sftregs_sect_crse_numb,
                             sftregs_rec.sftregs_sect_seq_numb
                          );
        --
        -- Get the studyPath information
        --====================================================
/* Not supporting study paths at this time.
                        IF bwckcoms.F_StudyPathEnabled = 'Y' THEN
                          IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') = 'FAC' THEN
                             genpidm := TO_NUMBER(twbkwbis.F_GetParam (global_pidm, 'STUPIDM'), 99999999);
                          ELSE
                             genpidm := global_pidm;
                          END IF;
                          IF NOT multi_term THEN
                            sftregs_rec.sftregs_stsp_key_sequence :=
                                           SUBSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),1, INSTR(twbkwbis.f_getparam (global_pidm, 'G_SPATH'),'|')-1);
                          ELSE
                            IF twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i)) <> 'NULL' THEN
                              sftregs_rec.sftregs_stsp_key_sequence :=twbkwbis.f_getparam (global_pidm, 'G_SP'||assoc_term (i));
                            ELSE
                              sftregs_rec.sftregs_stsp_key_sequence :=NULL;
                            END IF;
                          END IF;
                        END IF;
*/

        -- Call the procedure to add a course.
        -- ===================================================
-- Call local                          bwckregs.p_addcrse (
                          p_addcrse (
                             sftregs_rec,
                             sftregs_rec.sftregs_sect_subj_code,
                             sftregs_rec.sftregs_sect_crse_numb,
                             sftregs_rec.sftregs_sect_seq_numb,
                             start_date (i),
                             end_date (i)
                          );
        --
        -- Create a batch fee assessment record.
        -- ===================================================
                          sfrbtch_row.sfrbtch_term_code := assoc_term (i);
                          sfrbtch_row.sfrbtch_pidm := genpidm;
                          sfrbtch_row.sfrbtch_clas_code := clas_code;
                          sfrbtch_row.sfrbtch_activity_date := SYSDATE;
                          bwcklibs.p_add_sfrbtch (sfrbtch_row);
                          tbrcbrq_row.tbrcbrq_term_code := assoc_term (i);
                          tbrcbrq_row.tbrcbrq_pidm := genpidm;
                          tbrcbrq_row.tbrcbrq_activity_date := SYSDATE;
                          bwcklibs.p_add_tbrcbrq (tbrcbrq_row);
                       EXCEPTION
                          WHEN OTHERS
                          THEN
                             IF SQLCODE = gb_event.APP_ERROR THEN
                                twbkfrmt.p_storeapimessages(SQLERRM);
                                RAISE;
                             END IF;
                             k := k + 1;
                             err_term (k) := assoc_term (i);
                             err_crn (k)  := crn (i);
                             err_subj (k) := sftregs_rec.sftregs_sect_subj_code;
                             err_crse (k) := sftregs_rec.sftregs_sect_crse_numb;
                             err_sec (k)  := sftregs_rec.sftregs_sect_seq_numb;
                             err_code (k) := SQLCODE;
                             err_levl (k) := lcur_tab(1).r_levl_code;
                             err_cred (k) := TO_CHAR (sftregs_rec.sftregs_credit_hr);
                             err_gmod (k) := sftregs_rec.sftregs_gmod_code;
--Raise the exception instead of logging it.
                             RAISE;
                       END;
/* We only support when sisSectionId is provided, not subject and course.
                    ELSE
                       BEGIN
        --
        -- ?????
        -- ===================================================
                          IF     subj (i) IS NOT NULL
                             AND crse (i) IS NOT NULL
                             AND sec (i) IS NOT NULL
                             AND LTRIM (RTRIM (rsts_in (i))) IS NOT NULL
                          THEN
                             BEGIN
                                sftregs_rec.sftregs_rsts_code := rsts_in (i);
                                sftregs_rec.sftregs_rsts_date := SYSDATE;
                                sftregs_rec.sftregs_pidm := genpidm;
                                sftregs_rec.sftregs_term_code := assoc_term (i);

                                IF multi_term
                                THEN
        --
        -- Get the latest student and registration records.
        -- ===================================================
                                   bwckcoms.p_regs_etrm_chk (
                                      genpidm,
                                      assoc_term (i),
                                      clas_code,
                                      multi_term
                                   );
                                ELSIF NOT etrm_done
                                THEN
                                   bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                                   etrm_done := TRUE;
                                END IF;

                                bwckregs.p_addcrse (
                                   sftregs_rec,
                                   subj (i),
                                   crse (i),
                                   sec (i),
                                   start_date (i),
                                   end_date (i)
                                );
        --                        COMMIT;
        --                        commit_flag := 'Y';
                             EXCEPTION
                                WHEN OTHERS
                                THEN
                                   IF SQLCODE = gb_event.APP_ERROR THEN
                                      twbkfrmt.p_storeapimessages(SQLERRM);
                                      RAISE;
                                   END IF;
                                   k := k + 1;
                                   err_term (k) := assoc_term (i);
                                   err_crn (k) := crn (i);
                                   err_subj (k) := subj (i);
                                   err_crse (k) := crse (i);
                                   err_sec (k) := sec (i);
                                   err_code (k) := SQLCODE;
                                   err_levl (k) := lcur_tab(1).r_levl_code;
                                   err_cred (k) :=
                                              TO_CHAR (sftregs_rec.sftregs_credit_hr);
                                   err_gmod (k) := sftregs_rec.sftregs_gmod_code;
        --                           ROLLBACK;
        --                           commit_flag := 'N';
                             END;
                          END IF;
                       EXCEPTION
                          WHEN OTHERS
                          THEN
                             IF SQLCODE = gb_event.APP_ERROR THEN
                                twbkfrmt.p_storeapimessages(SQLERRM);
                                RAISE;
                             END IF;
                             NULL;
                       END;
*/
                    END IF;
                    i := i + 1;
                 EXCEPTION
                    WHEN NO_DATA_FOUND
                    THEN
/* We must have the sisSectionId we won't have a subject/course/sequence to look for.
                       BEGIN
                          IF     subj (i) IS NOT NULL
                             AND crse (i) IS NOT NULL
                             AND sec (i) IS NOT NULL
                          THEN
                             BEGIN
                                sftregs_rec.sftregs_rsts_code := rsts_in (i);
                                sftregs_rec.sftregs_rsts_date := SYSDATE;
                                sftregs_rec.sftregs_pidm := genpidm;
                                sftregs_rec.sftregs_term_code := assoc_term (i);

                                IF multi_term
                                THEN
        --
        -- Get the latest student and registration records.
        -- ===================================================
                                   bwckcoms.p_regs_etrm_chk (
                                      genpidm,
                                      assoc_term (i),
                                      clas_code,
                                      multi_term
                                   );
                                ELSIF NOT etrm_done
                                THEN
                                   bwckcoms.p_regs_etrm_chk (genpidm, term, clas_code);
                                   etrm_done := TRUE;
                                END IF;

                                bwckregs.p_addcrse (
                                   sftregs_rec,
                                   subj (i),
                                   crse (i),
                                   sec (i),
                                   start_date (i),
                                   end_date (i)
                                );
        --                        COMMIT;
        --                        commit_flag := 'Y';
                             EXCEPTION
                                WHEN OTHERS
                                THEN
                                   IF SQLCODE = gb_event.APP_ERROR THEN
                                      twbkfrmt.p_storeapimessages(SQLERRM);
                                      RAISE;
                                   END IF;
                                   k := k + 1;
                                   err_term (k) := assoc_term (i);
                                   err_crn (k) := crn (i);
                                   err_subj (k) := subj (i);
                                   err_crse (k) := crse (i);
                                   err_sec (k) := sec (i);
                                   err_code (k) := SQLCODE;
                                   err_levl (k) := lcur_tab(1).r_levl_code;
                                   err_cred (k) :=
                                              TO_CHAR (sftregs_rec.sftregs_credit_hr);
                                   err_gmod (k) := sftregs_rec.sftregs_gmod_code;
        --                           ROLLBACK;
        --                           commit_flag := 'N';
                             END;
                          END IF;
                       EXCEPTION
                          WHEN OTHERS
                          THEN
                             IF SQLCODE = gb_event.APP_ERROR THEN
                                twbkfrmt.p_storeapimessages(SQLERRM);
                                RAISE;
                             END IF;
                             NULL;
                       END;

                       i := i + 1;
*/
                       raise_application_error(-20001, 'Must provide a valid existing sisSectionId');
                 END;
              END LOOP;

        --
        -- Do batch validation on all registration records.
        -- ===================================================
-- Call local          bwckcoms.p_group_edits( term_in, genpidm, etrm_done,
           p_group_edits( term_in, genpidm, etrm_done,
                                   capp_tech_error,
                                   drop_problems, drop_failures);

           If capp_tech_error is null
           then
              --C3SC  grs ADD
             IF sv_auth_addcodes_bp.f_is_auth_enabled(term_in(1))
             THEN
-- Call local              sv_auth_addcodes_bp.p_local_problems( term_in,
               p_local_problems( term_in,
                                    err_term, err_crn, err_subj,
                                    err_crse, err_sec, err_code,
                                    err_levl, err_cred, err_gmod,
                                    drop_problems, drop_failures,add_row,
                                    'bwckcoms.p_regs',
                                    crn ,
                                    rsts_in);
                  return;
              ELSE
-- Call local                bwckcoms.p_problems ( term_in,
                p_problems ( term_in,
                                    err_term, err_crn, err_subj,
                                    err_crse, err_sec, err_code,
                                    err_levl, err_cred, err_gmod,
                                    drop_problems, drop_failures);
           return;
             end if;
              --END C3SC
           end if;


        --
        -- Redisplay the add/drop page (after a capp error)
        -- ===================================================
--           bwckcoms.p_adddrop2 (
           p_adddrop2 (
              term_in,
              err_term,
              err_crn,
              err_subj,
              err_crse,
              err_sec,
              err_code,
              err_levl,
              err_cred,
              err_gmod,
              capp_tech_error,
              NULL,
              drop_problems,
              drop_failures
           );

/* Let exceptions bubble up.
        EXCEPTION


              WHEN OTHERS
              THEN
                 IF NVL (twbkwbis.f_getparam (global_pidm, 'STUFAC_IND'), 'STU') =
                                                                                 'FAC'
                 THEN

         -- FACWEB-specific code

                    IF NOT bwlkilib.f_add_drp (term)
                    THEN
                       NULL;
                    END IF;

                    bwckfrmt.p_open_doc (
                       called_by_proc_name,
                       term,
                       NULL,
                       multi_term,
                       term_in (1)
                    );
                 ELSE

         -- STUWEB-specific code

                    IF NOT bwskflib.f_validterm (term, stvterm_rec, sorrtrm_rec)
                    THEN
                       NULL;
                    END IF;

                    bwckfrmt.p_open_doc (
                       called_by_proc_name,
                       term,
                       NULL,
                       multi_term,
                       term_in (1)
                    );
                 END IF;                       -- END stu/fac specific code sections


                 IF SQLCODE = gb_event.APP_ERROR THEN
                   twbkfrmt.p_storeapimessages(SQLERRM);
                 ELSE
                    IF SQLCODE  <= -20000 AND SQLCODE >= -20999 THEN
                       twbkfrmt.p_printmessage (
                        SUBSTR (SQLERRM, INSTR (SQLERRM, ':') + 1),
                        'ERROR'
                       );

                      raise_application_error(-20001, SQLERRM);
                    ELSE
                      twbkfrmt.p_printmessage (
                         G\$_NLS.Get ('BWCKCOM1-0027','SQL','Error occurred while processing registration changes.'),
                         'ERROR'
                       );

                      raise_application_error(-20001, SQLERRM);
                    END IF;
                 END IF;
*/
           END p_regs;

-- Registration entry point.
    BEGIN
        term := ?;
        pidm := ?;
        rsts := ?;
        crn := ?;
        global_pidm := pidm;
        genpidm := pidm;
--Call p_altpin to setup registration session.
        p_altpin(term);

--Setup and call p_regs to push new registration in.
    IF SUBSTR(f_stu_getwebregsrsts ('D'),1,2) = rsts THEN
        regs_row := 1;
        add_row := 0;
    ELSE
        regs_row := 0;
        add_row := 1;
    END IF;
        rsts_in(1) := 'dummy';
        term_in(1) := term;
        assoc_term_in(1) := 'dummy';
        crn_in(1) := 'dummy';
        start_date_in(1) := 'dummy';
        end_date_in(1) := 'dummy';
        subj(1) := 'dummy';
        crse(1) := 'dummy';
        sec(1) := 'dummy';
        levl(1) := 'dummy';
        cred(1) := 'dummy';
        gmod(1) := 'dummy';
        title(1) := 'dummy';
        mesg(1) := 'dummy';
        reg_btn(1) := 'dummy';
        rsts_in(2) := rsts;
        assoc_term_in(2) := term;
        crn_in(2) := crn;
        start_date_in(2) := NULL;
        end_date_in(2) := NULL;
        subj(2) := NULL;
        crse(2) := NULL;
        sec(2) := NULL;
        levl(2) := NULL;
        cred(2) := NULL;
        gmod(2) := NULL;
        title(2) := NULL;
        mesg(2) := NULL;
        reg_btn(2) := NULL;
        wait_row := 0;

        p_regs(term_in,
              rsts_in,
              assoc_term_in,
              crn_in,
              start_date_in,
              end_date_in,
              subj,
              crse,
              sec,
              levl,
              cred,
              gmod,
              title,
              mesg,
              reg_btn,
              regs_row,
              add_row,
              wait_row);

    END;
        """
        plsql.append(plsql2).append(plsql3).append(plsql4).append(plsql5)
        try {
            sql.call(plsql.toString(), [enrollment.sisTermId, pidm, rsts_code, enrollment.sisSectionId])
        } catch (Exception e) {
            def message = e.getMessage().substring(e.getMessage().indexOf(':') + 2, e.getMessage().indexOf('\n'))
            def code = message.equals("DUPLICATE <ACRONYM title = \"Course Reference Number\">CRN</ACRONYM>") ?
                    InvalidRequestException.Errors.alreadyEnrolled : InvalidRequestException.Errors.generalEnrollmentError
            throw new InvalidRequestException(code, message)
        }
    }

    def getJdbcConnection(String misCode) {
        // Use JDBC to make a package call or sql query
        def user = (MisEnvironment.getProperty(environment, misCode, "sisuser"))
        def password = (MisEnvironment.getProperty(environment, misCode, "sispassword"))

        Sql sql = Sql.newInstance(
                MisEnvironment.getProperty(environment, misCode, "jdbc.url"),
                user,
                password,
                MisEnvironment.getProperty(environment, misCode, "jdbc.driver")
        )
        sql
    }
}

