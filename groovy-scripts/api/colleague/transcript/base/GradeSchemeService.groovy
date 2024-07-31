package api.colleague.transcript.base

import api.colleague.transcript.model.GradesRecord
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.transcript.model.TranscriptException
import api.colleague.transcript.util.TranscriptUtils
import api.colleague.util.ColleagueUtils
import api.colleague.util.DmiDataServiceCached
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.xap.ccctran.AcademicRecordCCCExtensions
import com.xap.ccctran.impl.AcademicRecordCCCExtensionsImpl
import groovy.transform.CompileStatic

import java.time.LocalDate

@CompileStatic
abstract class GradeSchemeService {

    static final Date MIN_EFFECTIVE_START_DATE = ColleagueUtils.fromLocalDate(LocalDate.of(1900, 1, 2))
    static final Date MAX_EFFECTIVE_END_DATE = ColleagueUtils.fromLocalDate(LocalDate.of(9999, 12, 30))

    // DMI services
    TranscriptDmiServices transcriptDmiServices
    DmiEntityService dmiEntityService
    DmiDataServiceCached dmiDataServiceCached

    // entities for reading data
    Class<? extends ColleagueRecord> gradesEntity = GradesRecord.class

    GradeSchemeService(GradeSchemeService s) {
        this(s.transcriptDmiServices)
    }

    GradeSchemeService(TranscriptDmiServices transcriptDmiServices) {
        this.transcriptDmiServices = transcriptDmiServices
        this.dmiEntityService = transcriptDmiServices.dmiEntityService
        this.dmiDataServiceCached = transcriptDmiServices.dmiDataServiceCached
    }

    /**
     * Get Grade Scheme Entries
     *
     * @param gradeSchemeIds Grade scheme(s) to get grades for. If null / empty will return all grades.
     * @return Grade Scheme Entries
     */
    List<AcademicRecordCCCExtensions.GradeSchemeEntry> getGradeScheme(List<String> gradeSchemes) {
        def data = readGrades(gradeSchemes)
        def result = parseGrades(data)
        return result
    }

    protected List<ColleagueRecord> readGrades(List<String> gradeSchemes) {
        // if grade schemes are specified, filter on them
        String criteria = null
        if (gradeSchemes)
            criteria = "GRD.GRADE.SCHEME = " + gradeSchemes.collect { ColleagueUtils.quoteString(it) }.join(" ")

        return (List<ColleagueRecord>) dmiEntityService.readForEntity("GRADES", criteria, null, gradesEntity)
    }

    protected List<AcademicRecordCCCExtensions.GradeSchemeEntry> parseGrades(List<ColleagueRecord> data) {
        if (!data) return null

        if (!data[0] instanceof GradesRecord)
            throw new TranscriptException("Default implementation of parseGrades expecting GradesRecord, found " + data[0].class.name)

        def sx04 = dmiDataServiceCached.elfTranslationTable("CAST.SX04")?.asMap()

        Map<String, AcademicRecordCCCExtensions.GradeSchemeEntry> result = [:]

        // add grades to grade scheme extension
        for (def g : data) {
            def v = (GradesRecord) g

            def e = new AcademicRecordCCCExtensionsImpl.GradeSchemeEntryImpl()
            e.grade = v.grdGrade
            e.gradePoints = v.grdValue ?: new BigDecimal(0)
            e.description = v.grdLegend
            e.EDIGradeQualifier = v.gradeScaleCode
            e.attemptedUnitsCalc = v.grdAttCredFlag == "Y" ?
                    AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.COUNTED
                    : AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.NOT_COUNTED
            e.earnedUnitsCalc = v.grdCmplCredFlag == "Y" ?
                    AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.COUNTED
                    : AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.NOT_COUNTED
            e.gradePointsCalc = v.grdGpaCredFlag == "Y" ?
                    AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.COUNTED
                    : AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.NOT_COUNTED

            // these values are required by eTranscriptCA (or were at time of writing)
            // so set them to min/max allowed
            e.effectiveFromDate = MIN_EFFECTIVE_START_DATE
            e.effectiveToDate = MAX_EFFECTIVE_END_DATE

            // get grade code from SX04 and map to a grade scale code
            def sx04Grade = sx04?.get(v.grdGrade)?.newCode
            if (sx04Grade)
                e.EDIGradeQualifier = TranscriptUtils.getGradeCodeFromSX04(sx04Grade)

            result.put(e.grade, e)
        }

        // sort result by grade
        return result.values().sort { it.grade }
    }

}
