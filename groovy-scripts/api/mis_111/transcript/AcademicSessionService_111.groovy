package api.mis_111.transcript

import api.colleague.transcript.base.AcademicSessionService
import api.colleague.transcript.model.AcademicSessionsResults
import com.ccctc.core.coremain.v1_14.AcademicSessionDetailType
import com.ccctc.core.coremain.v1_14.CourseCreditBasisType
import com.ccctc.core.coremain.v1_14.SessionTypeType
import com.ccctc.sector.academicrecord.v1_9.CourseType
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

class AcademicSessionService_111 extends AcademicSessionService {

    /**
     * Here we use the copy constructor to create a class with the same attributes as the default one that was created,
     * then add in our customization
     */
    AcademicSessionService_111(AcademicSessionService s) {
        super(s)

        //
        // Customization: change the STUDENT.ACAD.CRED entity (for extra fields)
        //
        this.studentAcadCredEntity = StudentAcadCredRecord_111.class
    }

    /**
     * Here we override the readStudentAcadCred() method so that we can perform filtering on the source data
     */
    @Override
    protected <T extends ColleagueRecord> List<T> readStudentAcadCred(String personId, Class<T> type) {
        def results = super.readStudentAcadCred(personId, type)

        //
        // Customization: filter the results and remove records that we don't want in our result
        //
        List<T> filtered = []

        for (def r : results) {
            def stc = (StudentAcadCredRecord_111) r

            // remove non-term and credit types starting with "T"
            if (stc.stcTerm == null || stc.stcCredType == "TT" || stc.stcCredType == "TR")
                continue

            // remove "DR" and "X" grades
            if (stc.stcVerifiedGrade != null && (stc.stcVerifiedGrade.grdGrade == "DR" || stc.stcVerifiedGrade.grdGrade == "X"))
                continue

            // remove dropped courses without a grade
            if (stc.stcVerifiedGrade == null && stc.currentStatus != "A" && stc.currentStatus != "N")
                continue

            filtered << r
        }

        return filtered
    }

    /**
     * Here we override parseStudentAcadCred() so we can override / set values of certain fields
     */
    @Override
    protected CourseType parseStudentAcadCred(String personId, AcademicSessionsResults academicSessionsResults, ColleagueRecord data) {
        def course = super.parseStudentAcadCred(personId, academicSessionsResults, data)

        //
        // Customization: add courseCreditBasis and courseCreditUnits
        //
        def stc = (StudentAcadCredRecord_111) data

        // add course credit basis based on STC.CRED.TYPE
        course.courseCreditBasis = getCourseCreditBasis(stc.stcCredType)

        // determine whether these are Quarter or Semester units
        if (stc.stcTerm <= "1985XX" && stc.stcTerm != "1985FA")
            course.courseCreditUnits = "Quarter"
        else
            course.courseCreditUnits = "Semester"

        return course
    }


    /**
     * Here we override parseAcademicSessionDetails()
     */
    @Override
    protected AcademicSessionDetailType parseAcademicSessionDetails(AcademicSessionsResults academicSessionsResults, ColleagueRecord data) {
        def session = super.parseAcademicSessionDetails(academicSessionsResults, data)

        //
        // Customization: Derive session type. Butte uses the last two letters of the term ID to indicate the term:
        //
        //- FA = Fall
        //- SP = Spring
        //- SU = Summer
        //- IW or WI = Winter
        //
        // Note that prior to Fall 1985 Butte was on a quarter system. Starting with Fall 1985FA they are on a semester
        // system with the "winter" term being intersession.
        //

        if (data.recordId.length() == 6) {
            def lastTwo = data.recordId[4..5]

            if (data.recordId <= "1985XX" && data.recordId != "1985FA")
                session.sessionType = SessionTypeType.QUARTER
            else if (lastTwo == "IW" || lastTwo == "WI")
                session.sessionType = SessionTypeType.INTERSESSION
            else if (lastTwo == "SU")
                session.sessionType = SessionTypeType.SUMMER_SESSION
            else
                session.sessionType = SessionTypeType.SEMESTER
        }

        return session
    }



    /**
     * Butte College uses Credit Types for the following "credit basis":
     *
     * 1. Academic Renewal
     * 2. Credit By Exam
     * 3. High School Credit Only
     *
     * Additionally, the values for "C" and "D" indicate regular credit whereas "N" indicates non-credit.
     *
     * Butte also has a code "GA" - Grade Alleviation - its not clear what this should map to, but perhaps should
     * be looked into if this data gets mapped to eTranscriptCA or another source to ensure it is correct.
     */
    CourseCreditBasisType getCourseCreditBasis(String stcCredType) {
        switch (stcCredType) {
            case "AR":
                return CourseCreditBasisType.ACADEMIC_RENEWAL
            case "EX":
                return CourseCreditBasisType.CREDIT_BY_EXAM
            case "HS":
                return CourseCreditBasisType.HIGH_SCHOOL_CREDIT_ONLY
            case "C":
            case "D":
                return CourseCreditBasisType.REGULAR
        }

        return null
    }
}