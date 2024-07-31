package api.colleague

import api.colleague.collection.TupleIterable
import api.colleague.util.CastMisParmsUtil
import api.colleague.util.ColleagueUtils
import api.colleague.util.DmiDataServiceAsync
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Course as CACourse
import com.ccctc.adaptor.model.CourseContact
import com.ccctc.adaptor.model.CourseStatus
import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.InstructionalMethod
import com.ccctc.adaptor.model.TransferStatus
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.model.Valcode
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.util.StringUtils
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

import java.time.LocalDate

import static api.colleague.util.ColleagueUtils.fromLocalDate
import static api.colleague.util.ColleagueUtils.quoteString

class Course {

    Cache cache
    DmiDataService dmiDataService
    DmiDataServiceAsync dmiDataServiceAsync

    static courseColumns = ["CRS.NAME", "CRS.DESC", "CRS.SHORT.TITLE", "CRS.SUBJECT", "CRS.NO", "CRS.MIN.CRED",
                            "CRS.MAX.CRED", "CRS.START.DATE", "CRS.END.DATE", "CRS.FEE", "CRS.STANDARD.ARTICULATION.NO",
                            "CRS.RPT.UNIQUE.ID", "CRS.STATUS", "CRS.CRED.TYPE", "CRS.TRANSFER.STATUS",
                            "CRS.ONLY.PASS.NOPASS.FLAG", "CRS.ALLOW.PASS.NOPASS.FLAG", "CRS.REQS", "CRS.INSTR.METHODS",
                            "CRS.CONTACT.MEASURES", "CRS.CONTACT.HOURS", "CRS.NO.WEEKS", "CRS.OTHER.REG.BILLING.RATES",
                            "CRS.TERMS.OFFERED", "CRS.LOCATIONS"]

    static termColumns = ["TERM.START.DATE", "TERM.END.DATE"]

    static acadReqmtsColumns = ["ACR.PRINTED.SPEC", "ACR.REQS.TIMING"]

    static regBillingRatesColumns = ["RGBR.AMT.CALC.TYPE", "RGBR.CHARGE.AMT"]

    /**
     * Initialize
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.dmiDataService = services.get(DmiDataService.class)
        this.cache = services.get(Cache.class)
        def dmiService = services.get(DmiService.class)

        this.dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)

        ColleagueUtils.keepAlive(dmiService)
    }

    /**
     * Get a single course. If term is not passed in, the current version of the course will be returned.
     *
     * Implementation notes:
     * 1. A course is matched to a term either by it having sections in that term (CRS.TERMS.OFFERED)
     *    or by having a start date before the end date of the term.
     * 2. If multiple matches are found, active courses are given precedence. The second level of precedence is
     *    the course with the latest start date.
     * 3. In a multi-college district, courses are distinguished by location (CRS.LOCATIONS), comparing that with
     *    the MIS setup in CAST.MIS.PARMS (on the UI form CMRP). This matches a location to an INSTITUTIONS record.
     *    The INST.OTHER.ID value of the INSTITUTIONS record will contain the MIS Code.
     * 4. Some values rely on MIS Reporting translations, specifically transfer status (CB05), credit status (CB04)
     *
     * For more information on MIS reporting for Colleague see the "Using CA State Reporting - MIS Reports" document
     * from Ellucian, which details where values are stored for MIS reporting. The logic in this program expects that
     * the Colleague school has been setup properly for MIS reporting.
     *
     *
     * @param misCode     MIS Code
     * @param sisCourseId SIS Course ID
     * @param sisTermId   SIS Term ID (optional)
     * @return Course
     */
    CACourse get(String misCode, String sisCourseId, String sisTermId) {
        assert misCode != null
        assert sisCourseId != null

        List<ColleagueData> coursesData = null
        ColleagueData termData = null
        List<ColleagueData> acadReqmtsData = null
        List<ColleagueData> regBillingRatesData = null
        Valcode courseStatuses = null
        ElfTranslateTable cb04Translate = null
        ElfTranslateTable cb05Translate = null
        ElfTranslateTable xf01Translate = null
        CastMisParmsUtil castMisParmsUtil = null

        // get data in parallel:
        // 1. COURSES data
        // 2. TERMS data (if sisTermId supplied)
        // 3. COURSE.STATUSES valcode (use caching)
        // 4. CAST.CB04, CAST.CB05 and CAST.XF01 ELF translations (use caching)
        // 5. CAST.MIS.PARMS (use caching)
        // 6. ACAD.REQMTS data for the courses (for prerequisites and co-requisites)
        // 7. REG.BILLING.RATES data for the courses (for fees)
        def fCourses = dmiDataServiceAsync.batchSelectAsync("ST", "COURSES", courseColumns, "CRS.NAME = " + quoteString(sisCourseId))
        def fTerm = (sisTermId) ? dmiDataServiceAsync.singleKeyAsync("ST", "TERMS", termColumns, sisTermId) : null
        def fCourseStatuses = dmiDataServiceAsync.valcodeAsyncCached("ST", "COURSE.STATUSES")
        def fElfTranslations = dmiDataServiceAsync.elfTranslationTablesAsyncCached(["CAST.CB04", "CAST.CB05", "CAST.XF01"])
        def fCastMisParms = CastMisParmsUtil.fromAsync(dmiDataService, cache, false)

        coursesData = fCourses.get()

        if (coursesData == null)
            throw new EntityNotFoundException("Course not found")

        // child records of COURSES
        def acadReqmtsIds = [], regBillingRatesIds = []
        coursesData.each {
            if (it.values["CRS.REQS"]) acadReqmtsIds.addAll((String[]) it.values["CRS.REQS"])
            if (it.values["CRS.OTHER.REG.BILLING.RATES"]) regBillingRatesIds.addAll((String[]) it.values["CRS.OTHER.REG.BILLING.RATES"])
        }
        def fAcadReqmts = (acadReqmtsIds) ? dmiDataServiceAsync.batchKeysAsync("ST", "ACAD.REQMTS", acadReqmtsColumns, acadReqmtsIds) : null
        def fRegBillingRates = (regBillingRatesIds) ? dmiDataServiceAsync.batchKeysAsync("ST", "REG.BILLING.RATES", regBillingRatesColumns, regBillingRatesIds) : null

        // compile the data
        termData = fTerm?.get()
        acadReqmtsData = fAcadReqmts?.get()
        regBillingRatesData = fRegBillingRates?.get()
        courseStatuses = fCourseStatuses.get()
        castMisParmsUtil = (CastMisParmsUtil) fCastMisParms.get()
        def elfTranslations = fElfTranslations.get()
        if (elfTranslations != null) {
            cb04Translate = elfTranslations.find {i -> i.key == "CAST.CB04"}
            cb05Translate = elfTranslations.find {i -> i.key == "CAST.CB05"}
            xf01Translate = elfTranslations.find {i -> i.key == "CAST.XF01"}
        }

        if (courseStatuses == null) throw new InternalServerException("COURSES.STATUSES valcode not found")
        if (termData == null && sisTermId != null) throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")


        // filter data
        LocalDate termStart = (LocalDate) termData?.values?.get("TERM.START.DATE")
        LocalDate termEnd = (LocalDate) termData?.values?.get("TERM.END.DATE")

        def courseData = filterCourses(misCode, sisTermId, termEnd, coursesData, castMisParmsUtil, courseStatuses)
        if (courseData == null) throw new EntityNotFoundException("Course not found")


        // map data
        def values = courseData.values
        def result = new CACourse.Builder()
                .misCode(misCode)
                .sisCourseId((String) values["CRS.NAME"])
                .sisTermId(sisTermId)
                .c_id((String) values["CRS.STANDARD.ARTICULATION.NO"])
                .controlNumber((String) values["CRS.RPT.UNIQUE.ID"])
                .subject((String) values["CRS.SUBJECT"])
                .number((String) values["CRS.NO"])
                .title((String) values["CRS.SHORT.TITLE"])
                .description(((String) values["CRS.DESC"])?.replace(StringUtils.VM, " " as char))
                .outline(null) // not supported
                .minimumUnits(((BigDecimal) values["CRS.MIN.CRED"])?.floatValue())
                .maximumUnits(((BigDecimal) values["CRS.MAX.CRED"])?.floatValue())
                .fee(((BigDecimal) values["CRS.FEE"])?.floatValue())
                .start(fromLocalDate((LocalDate) values["CRS.START.DATE"]))
                .end(fromLocalDate((LocalDate) values["CRS.END.DATE"]))
                .build()

        if (result.maximumUnits == null)
            result.maximumUnits = result.minimumUnits

        setStatus(result, courseData, courseStatuses)
        setCreditStatusAndGradingMethod(result, courseData, cb04Translate)
        setTransferStatus(result, courseData, cb05Translate)
        setCourseContacts(result, courseData, xf01Translate)
        setReqs(result, courseData, acadReqmtsData)
        setFees(result, courseData, regBillingRatesData)

        return result
    }


    /**
     * Filter selected courses to find the best match for the term (if specified)
     *
     * 1. Filter by location for multi-college districts
     * 2. Filter by terms offered and/or courses that started before the end of the desired term
     * 3. If multiple matches are found, sort by active/inactive status and latest starting date
     */
    private ColleagueData filterCourses(String misCode, String sisTermId, LocalDate termEnd, List<ColleagueData> courses,
                                        CastMisParmsUtil castMisParmsUtil, Valcode courseStatuses) {

        // multi-college location filtering
        if (castMisParmsUtil?.castMisParms?.multiCollege) {
            def filtered = courses.findAll { i ->
                def crsLocations = (String[]) i.values["CRS.LOCATIONS"]
                if (!crsLocations) return true
                return castMisParmsUtil.checkLocations(misCode, Arrays.asList(crsLocations))
            }

            if (filtered.size() == 0)
                return null

            courses = filtered
        }

        // check terms offered and/or that have a start date prior to the end date of the term
        def found
        if (sisTermId) {
            found = courses.findAll { i -> ((List)i.values["CRS.TERMS.OFFERED"])?.contains(sisTermId) }
            if (found.size() == 0 && termEnd != null) {
                // find courses with a start date prior to the end of the term
                found = courses.findAll { c ->
                    def courseStart = (LocalDate) c.values["CRS.START.DATE"]

                    if (courseStart != null && !courseStart.isAfter(termEnd))
                        return true

                    return false
                }
            }
        } else {
            found = courses
        }

        if (found.size() == 0) return null
        if (found.size() == 1) return found[0]

        def activeStatuses = courseStatuses ? courseStatuses.entries
                .findAll { i -> i.action1 == "1" }
                .collect { i -> i.internalCode } : []

        // if multiple results are valid for the criteria, sort and return the best match
        def sorted = found.sort { a, b ->
            // sort by active status
            def active1 = activeStatuses.contains(((String[])a.values["CRS.STATUS"])?.first())
            def active2 = activeStatuses.contains(((String[])b.values["CRS.STATUS"])?.first())
            if (active1 && !active2) return -1
            if (active2 && !active1) return 1

            // sort by start date in descending order
            def start1 = (LocalDate) a.values["CRS.START.DATE"]
            def start2 = (LocalDate) b.values["CRS.START.DATE"]
            if (start1 && start2) return start2.compareTo(start1)
            return 0
        }

        return sorted[0]
    }

    /**
     * Set the status of the course (active or inactive)
     */
    private void setStatus(CACourse course, ColleagueData courseData, Valcode courseStatuses) {
        course.status = CourseStatus.Inactive

        def crsStatuses = (String[]) courseData.values["CRS.STATUS"]

        // check the first position of CRS.STATUS and see if action code #1 = "1" (indicating the course is active)
        if (crsStatuses) {
            def crsStatus = crsStatuses[0]
            def action1 = courseStatuses.asMap().get(crsStatus)?.action1

            course.status = (action1 == "1") ? CourseStatus.Active : CourseStatus.Inactive
        } else {
            course.status = CourseStatus.Inactive
        }
    }

    /**
     * Set Credit Status from CB04 translation and Grading method based on credit status and pass/no pass flag.
     */
    private void setCreditStatusAndGradingMethod(CACourse course, ColleagueData courseData, ElfTranslateTable cb04Translate) {
        def credType = (String) courseData.values["CRS.CRED.TYPE"]

        if (cb04Translate && credType) {
            def cb04 = cb04Translate.asMap().get(credType)?.newCode

            if (cb04 == "C")
                course.creditStatus = CreditStatus.NotDegreeApplicable
            else if (cb04 == "D")
                course.creditStatus = CreditStatus.DegreeApplicable
            else if (cb04 == "N")
                course.creditStatus = CreditStatus.NonCredit
        }

        if (course.creditStatus == CreditStatus.NonCredit)
            course.gradingMethod = GradingMethod.NotGraded
        else if (courseData.values["CRS.ONLY.PASS.NOPASS.FLAG"] == "Y")
            course.gradingMethod = GradingMethod.PassNoPassOnly
        else if (courseData.values["CRS.ALLOW.PASS.NOPASS.FLAG"] == "Y")
            course.gradingMethod = GradingMethod.PassNoPassOptional
        else
            course.gradingMethod = GradingMethod.Graded
    }

    /**
     * Set transfer status from CB05 translation
     */
    private void setTransferStatus(CACourse course, ColleagueData courseData, ElfTranslateTable cb05Translate) {
        def transferStatus = (String) courseData.values["CRS.TRANSFER.STATUS"]

        if (cb05Translate && transferStatus) {
            def cb05 = cb05Translate.asMap().get(transferStatus)?.newCode

            if (cb05 == "A")
                course.transferStatus = TransferStatus.CsuUc
            else if (cb05 == "B")
                course.transferStatus = TransferStatus.Csu
            else if (cb05 == "C")
                course.transferStatus = TransferStatus.NotTransferable
        }
    }

    /**
     * Set course contacts
     *
     * Notes:
     * 1. Instructional Method is translated through XF01
     * 2. Contact hours are multiplied by course weeks if contact measure is "W" (indicating weekly)
     */
    private void setCourseContacts(CACourse course, ColleagueData courseData, ElfTranslateTable xf01Translate) {
        def values = courseData.values
        def instrMethods = (String[]) values["CRS.INSTR.METHODS"]

        if (instrMethods) {
            def weeks = (Integer) values["CRS.NO.WEEKS"]
            def courseContacts = []

            def association = new TupleIterable(instrMethods, (BigDecimal[]) values["CRS.CONTACT.HOURS"], (String[]) values["CRS.CONTACT.MEASURES"])

            for(def a : association) {
                InstructionalMethod instructionalMethod

                def im = (String) a[0]
                def hours = (BigDecimal) a[1]
                def contactMeasure = (String) a[2]

                // translate instructional method through XF01 to determine lecture, lab or other
                def xf01 = xf01Translate?.asMap()?.get(im)?.newCode
                if (xf01 == "02") instructionalMethod = InstructionalMethod.Lecture
                else if (xf01 == "04") instructionalMethod = InstructionalMethod.Lab
                else instructionalMethod = InstructionalMethod.Other

                // multiply hours by weeks if this is "Weekly"
                if (hours != null && contactMeasure == "W" && weeks != null)
                    hours = hours * weeks

                courseContacts << new CourseContact.Builder()
                        .instructionalMethod(instructionalMethod)
                        .hours(hours?.floatValue())
                        .build()
            }

            course.courseContacts = courseContacts
        }
    }

    /**
     * Set prerequisites and co-requisites. This sets the printed specification of the requisites only, not the actual
     * courses.
     * <p>
     * Note: acadReqmtsData is unfiltered at this point and contains values for all versions of the courses, so it
     * needs to be filtered for records that apply to this course.
     */
    private void setReqs(CACourse course, ColleagueData courseData, List<ColleagueData> acadReqmtsData) {
        def reqs = (String[]) courseData.values["CRS.REQS"]

        if (reqs && acadReqmtsData) {
            def coreqs = []
            def prereqs = []

            for (def a : acadReqmtsData.findAll { i -> reqs.contains(i.key) }) {
                def spec = ((String) a.values["ACR.PRINTED.SPEC"])?.trim()?.replace(StringUtils.VM, " " as char)
                def timing = (String) a.values["ACR.REQS.TIMING"]

                if (timing && spec) {
                    if (timing == "C") coreqs << spec
                    else prereqs << spec
                }
            }

            if (coreqs.size() > 0) course.corequisites = coreqs.join(", ")
            if (prereqs.size() > 0) course.prerequisites = prereqs.join(", ")
        }
    }

    /**
     * Set fees from reg billing rates
     * <p>
     * Note: regBillingRatesData is unfiltered at this point and contains values for all versions of the courses, so it
     * needs to be filtered for records that apply to this course.
     */
    private void setFees(CACourse course, ColleagueData courseData, List<ColleagueData> regBillingRatesData) {
        def fees = (String[]) courseData.values["CRS.OTHER.REG.BILLING.RATES"]
        def units = ((BigDecimal) courseData.values["CRS.MIN.CRED"]) ?: 0

        if (fees && regBillingRatesData) {
            BigDecimal crsFees = 0

            for (def r : regBillingRatesData.findAll { i -> fees.contains(i.key) }) {
                def type = (String) r.values["RGBR.AMT.CALC.TYPE"]
                def amt = ((BigDecimal) r.values["RGBR.CHARGE.AMT"]) ?: 0

                if (type == "F") crsFees += amt
                else if (type == "A") crsFees += amt * units
            }

            course.fee = crsFees.floatValue()
        }
    }

}