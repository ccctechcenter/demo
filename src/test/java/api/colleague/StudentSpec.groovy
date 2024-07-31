package api.colleague

import api.colleague.Student as GroovyStudent
import api.colleague.model.CastMisParms
import api.colleague.model.InstitutionLocationMap
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import com.ccctc.adaptor.model.StudentHomeCollege
import com.ccctc.adaptor.model.students.StudentFieldSet
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.text.SimpleDateFormat

class StudentSpec extends Specification {


    def entityMetadataService = Mock(EntityMetadataService)
    def environment = Mock(Environment)
    def dmiService = Mock(DmiService)
    def dmiDataService = Mock(DmiDataService)
    def dmiCTXService = Mock(DmiCTXService)
    def cache = Mock(Cache)

    GroovyStudent groovyStudent = new GroovyStudent()

    String misCode = "000"

    def setup() {
        ClassMap services = new ClassMap()
        services.putAll([(DmiService.class)    : dmiService,
                         (DmiCTXService.class) : dmiCTXService,
                         (DmiDataService.class): dmiDataService,
                         (Cache.class)         : cache])

        dmiDataService.getEntityMetadataService() >> entityMetadataService

        environment.getProperty("000.colleague.identity.cccId.alt_id_type") >> "CCID"

        groovyStudent.colleagueInit(misCode, environment, services)
        groovyStudent.castMisParmsUtil.castMisParms = new CastMisParms(
                defaultMisCode: misCode,
                defaultInstitutionId: "1",
                institutionLocationMapList: [new InstitutionLocationMap(
                        institution: "1",
                        location: "LOC",
                        misCode: misCode
                )
                ]
        )
    }

    def "parseApplicationStatus - no application"() {

        setup:
        environment.getProperty("000.colleague.student.mappings.applicationStatus.app_status.STU") >> ""

        String applCode = "STU"
        com.ccctc.adaptor.model.ApplicationStatus applStatus = com.ccctc.adaptor.model.ApplicationStatus.NoApplication

        when:
        com.ccctc.adaptor.model.ApplicationStatus result = groovyStudent.parseApplicationStatus(misCode, applCode)

        then:
        result == applStatus
    }

    def "parseApplicationStatus - application accepted"() {

        setup:
        environment.getProperty("000.colleague.student.mappings.applicationStatus.app_status.MS") >> "ApplicationAccepted"

        String applCode = "MS"
        com.ccctc.adaptor.model.ApplicationStatus applStatus = com.ccctc.adaptor.model.ApplicationStatus.ApplicationAccepted

        when:

        com.ccctc.adaptor.model.ApplicationStatus result = groovyStudent.parseApplicationStatus(misCode, applCode)

        then:

        result == applStatus
    }

    def "parseResidentStatus - Resident"() {

        setup:
        environment.getProperty("000.colleague.student.mappings.residentStatus.residency_status.CAR") >> "Resident"
        String resCode = "CAR"
        com.ccctc.adaptor.model.ResidentStatus resStatus = com.ccctc.adaptor.model.ResidentStatus.Resident

        when:

        com.ccctc.adaptor.model.ResidentStatus result = groovyStudent.parseResidentStatus(misCode, resCode)

        then:
        result == resStatus
    }

    def "parseResidentStatus - AB540"() {

        setup:
        environment.getProperty("000.colleague.student.mappings.residentStatus.residency_status.AB540") >> "AB540"
        String resCode = "AB540"
        com.ccctc.adaptor.model.ResidentStatus resStatus = com.ccctc.adaptor.model.ResidentStatus.AB540

        when:

        com.ccctc.adaptor.model.ResidentStatus result = groovyStudent.parseResidentStatus(misCode, resCode)

        then:

        result == resStatus
    }

    def "get - missing params"() {
        when:
        groovyStudent.get(null, null, null)

        then:
        thrown InvalidRequestException

        when:
        groovyStudent.get(misCode, null, null)

        then:
        thrown InvalidRequestException
    }

    def "get - success"() {

        setup:

        environment.getProperty("000.colleague.student.dualCredit.residency_status") >> "RES"
        environment.getProperty("000.colleague.student.dualCredit.stu_type") >> "STU"
        environment.getProperty("000.colleague.student.dualCredit.rule") >> "ERULES"

        environment.getProperty("000.colleague.student.incarcerationStatus.residency_status") >> "IRES"
        environment.getProperty("000.colleague.student.incarcerationStatus.stu_type") >> "ISTU"
        environment.getProperty("000.colleague.student.incarcerationStatus.rule") >> "IRULES"

        environment.getProperty("000.colleague.student.financialAid.rule.file_suite_year") >> "2020"
        environment.getProperty("000.colleague.student.financialAid.rule.and_or_flag") >> "Y"
        environment.getProperty("000.colleague.student.financialAid.award_status.active") >> "A"
        environment.getProperty("000.colleague.student.financialAid.ccPromise.award_id") >> "A"

        environment.getProperty("000.colleague.student.cohorts.term.format") >> "TTYYYY"
        environment.getProperty("000.colleague.student.mappings.cohort_prefix.cohortTypeEnum.COURSE_EXCHANGE") >> "CE"
        environment.getProperty("000.colleague.student.mappings.cohortTypeEnum.cohort_prefix.CE") >> "COURSE_EXCHANGE"

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"
        environment.getProperty("000.colleague.student.mappings.orientationStatus.app_userField.REQUIRED") >> "REQUIRED"

        environment.getProperty("000.colleague.student.mappings.applicationStatus.app_status.MS") >> "ApplicationAccepted"

        environment.getProperty("000.colleague.student.residentStatus.default") >> "NonResident"
        environment.getProperty("000.colleague.student.mappings.residentStatus.residency_status.CAR") >> "Resident"

        environment.getProperty("000.colleague.student.englishAssessment.contact_type") >> "AC,TR"
        environment.getProperty("000.colleague.student.englishAssessment.rule") >> "TEST"

        environment.getProperty("000.colleague.student.mathAssessment.contact_type") >> "ACRRR,TRRRRR"
        environment.getProperty("000.colleague.student.mathAssessment.rule") >> "TESTR"

        environment.getProperty("000.colleague.student.educationPlan.contact_type") >> "EMAIL"
        environment.getProperty("000.colleague.student.educationPlan.rule") >> "EDRUL"

        environment.getProperty("000.colleague.student.enrollmentHolds.rule") >> "HRULES"


        String sisPersonId = "123456"
        String sisTermId = "2019FA"
        String cccId = "12345"

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")
        com.ccctc.adaptor.model.Student student = new com.ccctc.adaptor.model.Student.Builder()
                .cccid(cccId)
                .sisTermId(sisTermId)
                .sisPersonId(sisPersonId)
                .hasCaliforniaAddress(true)
                .visaType("vType")
                .isConcurrentlyEnrolled(true)
                .isIncarcerated(true)
                .orientationStatus(OrientationStatus.REQUIRED)
                .hasEnglishAssessment(true)
                .hasMathAssessment(true)
                .hasEducationPlan(true)
                .hasHold(true)
                .accountBalance(200)
                .dspsEligible(true)
                .registrationDate(df.parse("2020-07-14"))
                .applicationStatus(ApplicationStatus.ApplicationAccepted)
                .residentStatus(ResidentStatus.Resident)
                .hasFinancialAidAward(true)
                .hasBogfw(true)
                .cohorts([new Cohort.Builder().name(CohortTypeEnum.COURSE_EXCHANGE).build()])
                .build();


        when:
        com.ccctc.adaptor.model.Student result = groovyStudent.get(misCode, cccId, sisTermId)

        then:

        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]


        //*** Orientation Data Pull
        1 * dmiDataService.singleKey("ST", "APPLICANTS", *_) >> new ColleagueData(sisPersonId, [
                "APP.USER1": "REQUIRED", "APP.USER2": sisTermId
        ])

        //*** Application Status Data Pull
        1 * dmiDataService.batchSelect("ST", "APPLICATIONS", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("APPLICATION_ID_1", ["APPL.STATUS": ["MS"]])
        ])


        //*** Contact Types Data Pull
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> new ColleagueData(sisPersonId, [
                "PERSON.CONTACT.HISTORY": ["1", "2"],
        ])
        1 * dmiDataService.batchKeys("CORE", "CONTACT", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("1", ["CONTACT.TYPE": ["MATHA"]]),
                new ColleagueData("2", ["CONTACT.TYPE": ["ENGLA"]])
        ])

        //*** ed Plan rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** engl assessment rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** math assessment rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** Residency Status Data Pull
        1 * dmiDataService.singleKey("ST", "STUDENTS", *_) >> new ColleagueData(sisPersonId, [
                "STU.RESIDENCY.STATUS": ["CAR"]
        ])

        //*** Student Types Data Pull
        1 * dmiDataService.singleKey("ST", "STUDENTS", *_) >> new ColleagueData(sisPersonId, [
                "STU.TYPES": ["DUALHS"]
        ])

        //*** Incarceration Rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** Concurrent Enrollment Rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** Visa Type Data Pull
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> new ColleagueData(sisPersonId, [
                "VISA.TYPE": "vType"
        ])

        //*** Ca Addresses Data Pull
        1 * dmiDataService.singleKey("CORE", "PERSON", *_) >> new ColleagueData(sisPersonId, [
                "PERSON.ADDRESSES": ["ADDRESS_ID_1", "ADDRESS_ID_2"]
        ])
        1 * dmiDataService.batchKeys("CORE", "ADDRESS", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("ADDRESS_ID_1", ["STATE": "CA"]),
                new ColleagueData("ADDRESS_ID_2", ["STATE": "NV"])
        ])

        //*** Enrollment Holds Rule
        1 * dmiCTXService.execute("ST", "X.CCCTC.EVAL.RULE.ST", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorMsg"     : [],
                "Result"       : true,
                "ValidIds"     : [sisPersonId],
                "InvalidIds"   : []
        ], [:])

        //*** Registration Date Data Pull
        1 * dmiDataService.batchSelect("ST", "REG.PRIORITIES", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("REG_PROIRITY_ID_1", ["RGPR.START.DATE": "2020-07-14"])
        ])

        //*** Student Tuition Owed Data Pull
        1 * dmiCTXService.execute("ST", "X.CCCTC.STUDENT.BALANCE.CA", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorCodes"   : [],
                "ErrorMsgs"    : [],
                "Balance": 200
        ], [:])

        //*** Student DSPS Data Pull
        1 * dmiDataService.selectKeys("STUDENT.DSPS", *_) >> ["DSPS_ID_1"]

        if (false) {
            //*** Student Health Data Pull
            1 * dmiDataService.selectKeys("STUDENT.HEALTH", *_) >> ["HEALTH_ID_1"]
        }

        //*** Student Financial Aid Data Pull
        1 * dmiCTXService.execute("ST", "X.CCCTC.GET.FIN.AID.INFO", *_) >> new CTXData([
                "ErrorOccurred"       : false,
                "ErrorCodes"          : [],
                "ErrorMsgs"           : [],
                "HasFinancialAidAward": "true",
                "HasBogfw"            : "true"
        ], [:])

        //*** Student Cohort Data Pull
        1 * dmiCTXService.execute("ST", "X.CCCTC.GET.COHORT", *_) >> new CTXData([
                "ErrorOccurred": false,
                "ErrorCodes"   : [],
                "ErrorMsgs"    : [],
                "Cohorts": ["COURSE_EXCHANGE", "Course Exchange Spring 2019"]
        ], [:])

        student.cccid == result.cccid
        student.sisPersonId == result.sisPersonId
        student.sisTermId == result.sisTermId
        student.hasCaliforniaAddress == result.hasCaliforniaAddress
        student.visaType == result.visaType
        student.isConcurrentlyEnrolled == result.isConcurrentlyEnrolled
        student.isIncarcerated == result.isIncarcerated
        student.orientationStatus == result.orientationStatus
        student.hasEnglishAssessment == result.hasEnglishAssessment
        student.hasMathAssessment == result.hasMathAssessment
        student.hasEducationPlan == result.hasEducationPlan
        student.hasHold == result.hasHold
        student.accountBalance == result.accountBalance
        student.dspsEligible == result.dspsEligible
        student.registrationDate == result.registrationDate
        student.applicationStatus == result.applicationStatus
        student.residentStatus == result.residentStatus
        student.hasFinancialAidAward == result.hasFinancialAidAward
        student.hasBogfw == result.hasBogfw
        student.cohorts[0].name == result.cohorts[0].name
    }


    def "getAllByTerm Invalid field set"() {
        setup:
        def terms = ['2019FA']
        when:
        def result = groovyStudent.getAllByTerm("0000", terms, "abcd" as StudentFieldSet)

        then:
        thrown IllegalArgumentException
    }

    def "getAllByTerm - success - IDENTIFIERS"() {

        setup:

        String sisTermId = "2019FA"
        String sisPersonId = "1234567"

        com.ccctc.adaptor.model.students.StudentIdentityDTO student = new com.ccctc.adaptor.model.students.StudentIdentityDTO()
        student.cccid = "A95982"
        student.sisTermId = sisTermId
        student.sisPersonId = sisPersonId

        when:
        List<Map> result = groovyStudent.getAllByTerm(misCode, [sisTermId], StudentFieldSet.IDENTIFIERS)

        then:

        then:

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]

        //*** Term persons Pull
        1 * dmiDataService.batchSelect("ST", "APPLICATIONS", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("APPLICATION_ID_1", ["APPL.APPLICANT": sisPersonId])
        ])

        //*** Translation from siPersonIds to cccIds
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": ["A95982"], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        result.size() == 1
        student.sisPersonId == result[0].get("sisPersonId")
        student.cccid == result[0].get("cccid")
        student.sisTermId == result[0].get("sisTermId")
    }

    def "getAllByTerm - success - MATRICULATION"() {
        setup:

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"
        environment.getProperty("000.colleague.student.mappings.orientationStatus.app_userField.COMPLETED") >> "COMPLETED"

        environment.getProperty("000.colleague.student.mappings.applicationStatus.app_status.MS") >> "ApplicationAccepted"

        environment.getProperty("000.colleague.student.educationPlan.contact_type") >> "CEP"
        environment.getProperty("000.colleague.student.educationPlan.rule") >> "EDRUL"

        String sisTermId = "2019FA"
        String sisPersonId = "1234567"

        com.ccctc.adaptor.model.students.StudentMatriculationDTO student = new com.ccctc.adaptor.model.students.StudentMatriculationDTO()
        student.cccid = "A95982"
        student.sisTermId = sisTermId
        student.sisPersonId = sisPersonId
        student.hasEducationPlan = true
        student.applicationStatus = ApplicationStatus.ApplicationAccepted
        student.orientationStatus = OrientationStatus.COMPLETED

        when:
        List<Map> result = groovyStudent.getAllByTerm(misCode, [sisTermId], StudentFieldSet.MATRICULATION)

        then:

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]

        //*** Term persons Pull (along with application status)
        1 * dmiDataService.batchSelect("ST", "APPLICATIONS", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("APPLICATION_ID_1", ["APPL.APPLICANT": sisPersonId, "APPL.STATUS": ["MS"]])
        ])

        //*** Orientation Data Pull
        1 * dmiDataService.batchSelect("ST", "APPLICANTS", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["APP.USER1": "COMPLETED", "APP.USER2": sisTermId])
        ])

        //*** Education Plan Data Pull
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.CONTACT.HISTORY": ["CONTACT_ID_1", "CONTACT_ID_2"]])
        ])
        1 * dmiDataService.selectKeys("CONTACT", *_) >> ["CONTACT_ID_2"]
        1 * dmiDataService.batchKeys("CORE", "CONTACT", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData("CONTACT_ID_2", ["CONTACT.TYPE": "CEP"])
        ])

        //*** Translation from siPersonIds to cccIds
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": ["A95982"], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        result.size() == 1
        student.sisPersonId == result[0].get("sisPersonId")
        student.cccid == result[0].get("cccid")
        student.sisTermId == result[0].get("sisTermId")
        student.hasEducationPlan == result[0].get("hasEducationPlan")
        student.orientationStatus.toString() == result[0].get("orientationStatus")
        student.applicationStatus.toString() == result[0].get("applicationStatus")

    }

    def "getHomeCollege - unsupported"() {
        setup:
        String cccId = "ABS345"
        StudentHomeCollege hc = new StudentHomeCollege(misCode: misCode, cccid: cccId)

        when:
        StudentHomeCollege result = groovyStudent.getHomeCollege(misCode, cccId)

        then:
        thrown InternalServerException
    }


    def "postCohort - missing parms"() {
        when:
        groovyStudent.postCohort(null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.postCohort("cccId", null, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, misCode, null)

        then:
        thrown AssertionError
    }

    def "postCohort - unsupported"() {
        when:
        groovyStudent.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, misCode, "sisTermId")

        then:
        thrown InternalServerException
    }


    def "deleteCohort - missing parms"() {
        when:
        groovyStudent.deleteCohort(null, null, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.deleteCohort("cccId", null, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, misCode, null)

        then:
        thrown AssertionError
    }

    def "deleteCohort - unsupported"() {
        when:
        groovyStudent.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, misCode, "sisTermId")

        then:
        thrown InternalServerException

    }


    def "getStudentCCCIds - missing parms"() {
        when:
        groovyStudent.getStudentCCCIds(null, null)

        then:
        thrown InvalidRequestException

        when:
        groovyStudent.getStudentCCCIds(misCode, null)

        then:
        thrown InvalidRequestException
    }

    def "getStudentCCCIds - student not found"() {

        setup:
        String sisPersonId = "12345"

        when:
        List<String> result = groovyStudent.getStudentCCCIds(misCode, sisPersonId)

        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [], "PERSON.ALT.ID.TYPES": []])
        ])

        result.size() == 0
    }

    def "getStudentCCCIds - success"() {
        setup:
        String sisPersonId = "12345"
        String cccId = "ABN4578"

        when:
        List<String> result = groovyStudent.getStudentCCCIds(misCode, sisPersonId)

        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchKeys("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        result.size() == 1
        result[0] == cccId
    }


    def "postStudentCCCId - missing parms"() {
        when:
        groovyStudent.postStudentCCCId(null, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.postStudentCCCId(misCode, null, null)

        then:
        thrown AssertionError

        when:
        groovyStudent.postStudentCCCId(misCode, "sisPersonId", null)

        then:
        thrown AssertionError
    }

    def "postStudentCCCId - unsupported"() {
        setup:
        def cccId = "cccId"

        when:
        groovyStudent.postStudentCCCId(misCode, "1234567", cccId)

        then:
        thrown InternalServerException
    }


    def "patch - missing parms"() {
        setup:

        def termId = "2012FA"
        def updates = new com.ccctc.adaptor.model.Student()
        updates.setSisTermId(termId)
        updates.setOrientationStatus(OrientationStatus.COMPLETED)

        when:
        groovyStudent.patch(null, null, termId, updates)


        then:
        thrown InvalidRequestException
    }

    def "patch - missing cccid"() {

        setup:
        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.COMPLETED)
                .build()

        when:
        groovyStudent.patch(misCode, null, "2012FA", updates)


        then:
        thrown InvalidRequestException
    }

    def "patch - Invalid Orientation Status"() {

        setup:

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"

        String sisPersonId = "123456"
        String sisTermId = "2019FA"
        String cccId = "12345"

        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.UNKNOWN)
                .build()

        when:
        groovyStudent.patch(misCode, cccId, sisTermId, updates)

        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]

        // orientation status then validated (yet should fail)
        thrown InvalidRequestException
    }

    def "patch - update ApplicationStatus unsupported"() {
        setup:
        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .applicationStatus(ApplicationStatus.ApplicationAccepted)
                .build()

        when:
        groovyStudent.patch(misCode, "12345", "2012FA", updates)

        then:
        thrown InvalidRequestException
    }

    def "patch - update hasEducationPlan unsupported"() {
        setup:
        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .hasEducationPlan(true)
                .build()

        when:
        groovyStudent.patch(misCode, "12345", "2012FA", updates)

        then:
        thrown InvalidRequestException
    }

    def "patch - success"() {
        setup:
        environment.getProperty("000.colleague.student.mappings.app_userField.orientationStatus.COMPLETED") >> "CMPLTD"

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"

        String sisPersonId = "123456"
        String sisTermId = "2019FA"
        String cccId = "12345"

        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.COMPLETED)
                .build();

        def getStudentGoodResponse1 = new CTXData([
                "ErrorOccurred": false,
                "ErrorCodes"   : [],
                "ErrorMsgs"    : []
        ], [:])

        when:
        groovyStudent.patch(misCode, cccId, sisTermId, updates)

        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]

        1 * dmiCTXService.execute(*_) >> getStudentGoodResponse1
    }

    def "patch - invalid term"() {
        setup:
        environment.getProperty("000.colleague.student.mappings.app_userField.orientationStatus.COMPLETED") >> "CMPLTD"

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"

        String sisPersonId = "123456"
        String sisTermId = "SOME_BAD_VALUE"
        String cccId = "12345"

        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.COMPLETED)
                .build();

        when:
        groovyStudent.patch(misCode, cccId, sisTermId, updates)


        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> []
        thrown InvalidRequestException
    }


    def "patch - invalid cccId"() {
        setup:
        environment.getProperty("000.colleague.student.mappings.app_userField.orientationStatus.COMPLETED") >> "CMPLTD"

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"

        String sisTermId = "FALL2019"
        String cccId = "some_bad_value"

        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.COMPLETED)
                .build();

        when:
        groovyStudent.patch(misCode, cccId, sisTermId, updates)


        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
        ])
        thrown EntityNotFoundException
    }

    def "patch - transaction error"() {
        setup:
        environment.getProperty("000.colleague.student.mappings.app_userField.orientationStatus.COMPLETED") >> "CMPLTD"

        environment.getProperty("000.colleague.student.orientationStatus.code.app_user.field") >> "APP.USER1"
        environment.getProperty("000.colleague.student.orientationStatus.term.app_user.field") >> "APP.USER2"

        String sisPersonId = "123456"
        String sisTermId = "Fall2019"
        String cccId = "12345"

        com.ccctc.adaptor.model.Student updates = new com.ccctc.adaptor.model.Student.Builder()
                .orientationStatus(OrientationStatus.COMPLETED)
                .build();

        def getStudentBadResponse = new CTXData([
                "ErrorOccurred": true,
                "ErrorCodes"   : ["0"],
                "ErrorMsgs"    : ["Some Internal Error"]
        ], [:])

        when:
        groovyStudent.patch(misCode, cccId, sisTermId, updates)


        then:
        //*** Translation from cccId to sisPersonId
        1 * dmiDataService.batchSelect("CORE", "PERSON", *_) >> new ArrayList<ColleagueData>([
                new ColleagueData(sisPersonId, ["PERSON.ALT.IDS": [cccId], "PERSON.ALT.ID.TYPES": ["CCID"]])
        ])

        //*** Validate Term
        1 * dmiDataService.selectKeys("TERMS", *_) >> [sisTermId]

        1 * dmiCTXService.execute(*_) >> getStudentBadResponse
        thrown InternalServerException
    }
}
