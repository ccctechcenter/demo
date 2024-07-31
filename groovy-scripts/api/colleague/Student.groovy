package api.colleague

import api.colleague.model.RuleResult
import api.colleague.util.CastMisParmsUtil
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceAsync
import com.ccctc.adaptor.config.JacksonConfig
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.*
import com.ccctc.adaptor.model.students.StudentFieldSet
import com.ccctc.adaptor.util.ClassMap
import com.ccctc.adaptor.util.MisEnvironment
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.StringUtils
import org.ccctc.colleaguedmiclient.model.CTXData
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.model.KeyValuePair
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.ccctc.colleaguedmiclient.service.EntityMetadataService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.core.env.Environment
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * <h1>Student Colleague Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native colleague tables for:</p>
 *     <ol>
 *         <li>Retrieving a List of Students by term(s)</li>
 *         <li>Retrieving a Student's data</li>
 *         <li><span>Setting/updating student data:</span>
 *             <ol>
 *                 <li>Orientation Status</li>
 *             </ol>
 *         </li>
 *     <ol>
 * </summary>
 *
 * @version 4.0.0
 *
 */
class Student {

    private static final Logger log = LoggerFactory.getLogger(Student.class)

    protected final static ObjectMapper mapper = new JacksonConfig().jacksonObjectMapper()

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    //******* Populated by colleagueInit function ****
    protected ClassMap Cinit
    protected DmiDataService dmiDataService
    protected DmiDataServiceAsync dmiDataServiceAsync
    protected DmiCTXService dmiCTXService
    protected DataUtils dataUtils
    protected CastMisParmsUtil castMisParmsUtil
    protected EntityMetadataService entityMetadataService

    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        this.Cinit = services
        DmiService dmiService = services.get(DmiService.class)
        Cache cache = services.get(Cache.class)
        this.dmiDataService = services.get(DmiDataService.class)
        this.dmiDataServiceAsync = new DmiDataServiceAsync(dmiDataService, cache)
        this.dmiCTXService = services.get(DmiCTXService.class)
        this.dataUtils = new DataUtils(misCode, environment, dmiService, dmiCTXService, dmiDataService, cache)
        this.castMisParmsUtil = new CastMisParmsUtil(dmiDataService, cache, false)
        this.entityMetadataService = dmiDataService.getEntityMetadataService()
        this.environment = environment


        ColleagueUtils.keepAlive(dmiService)

    }

    /**
     * Gets the student details for the given cccid for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report student information on (required)
     * @return a Student record matching the search params provided if found
     */
    com.ccctc.adaptor.model.Student get(String misCode, String cccId, String sisTermId){

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.misCodeNotFound, "misCode cannot be null or blank")
        }

        if (!cccId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "cccId cannot be null or blank")
        }

        String sisPersonId
        List<String> sisPersonIds = dataUtils.getColleagueIds(cccId)
        if(!sisPersonIds || sisPersonIds.size() <= 0) {
            String errMsg = "get: Student mapping returned no results: student not found"
            log.error(errMsg)
            throw new EntityNotFoundException(errMsg)
        } else if (sisPersonIds.size() > 1) {
            String errMsg = "get: Student mapping returned several results: too many students found"
            log.error(errMsg)
            throw new EntityNotFoundException(errMsg)
        } else {
            sisPersonId = sisPersonIds[0]
        }

        if (!sisTermId) {

            // Get current term code
            api.colleague.Term termAPI = new api.colleague.Term();
            termAPI.colleagueInit(misCode, environment, Cinit);
            List<String> currentTermIds = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC));
            if(currentTermIds != null && currentTermIds.size() > 0) {
                sisTermId = currentTermIds[0]
            }

            if (!sisTermId) {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "could not determine sisTermId")
            }
        } else if (!dataUtils.validateTerm(sisTermId)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")
        }

        //****** Parameters validated, lets get the data ****
        log.info("get: parameters validated")


        //****** Get orientation status ****
        log.info("get: retrieving orientation status")
        OrientationStatus orientationStatus = OrientationStatus.UNKNOWN
        try {
            String orientationCodeUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.code.app_user.field")
            String orientationTermUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.term.app_user.field")

            if (!orientationCodeUserFld || !orientationTermUserFld) {
                String errMsg = "Orientation Status field information not defined in the configuration file"
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
            }

            ColleagueData orientationRecord = dmiDataService.singleKey("ST", "APPLICANTS", [orientationCodeUserFld, orientationTermUserFld], sisPersonId)
            if (orientationRecord) {
                log.info("get: orientation status record found")
                String orientStatus = orientationRecord.values[orientationCodeUserFld]
                String orientStatusTerm = orientationRecord.values[orientationTermUserFld]
                if (sisTermId == orientStatusTerm) {
                    log.info("get: orientation status term matches; parsing")
                    orientationStatus = this.parseOrientationStatus(misCode, orientStatus)
                }
            }
        }
        finally {
            log.info("get: done with Orientation Status")
        }

        //****** Get Admissions/Application status ****
        log.info("get: retrieving Application status")
        ApplicationStatus applicationStatus = ApplicationStatus.NoApplication
        try
        {
            String query = "APPL.START.TERM EQ  " + ColleagueUtils.quoteString(sisTermId) + " AND WITH APPL.APPLICANT EQ " + ColleagueUtils.quoteString(sisPersonId)
            List<ColleagueData> applicationRecords = dmiDataService.batchSelect("ST", "APPLICATIONS", ["APPL.STATUS"], query)
            if (applicationRecords && applicationRecords.size() > 0){
                log.info("get: found Application records")
                List<String> firstApplicationAllStatuses = applicationRecords[0].values["APPL.STATUS"] as List<String>
                if (firstApplicationAllStatuses && firstApplicationAllStatuses.size() > 0) {
                    log.info("get: parsing first Application for first status found")
                    applicationStatus = this.parseApplicationStatus(misCode, firstApplicationAllStatuses[0])
                }
            }
        }
        finally {
            log.info("get: done with Application status")
        }

        //****** Get contact types for engl/math assessments and education plan ****
        log.info("get: retrieving contact types")
        boolean HasEducationPlan = null
        boolean HasEnglAssessment = null
        boolean HasMathAssessment = null
        try {
            ColleagueData personRecord = dmiDataService.singleKey("CORE", "PERSON", ["PERSON.CONTACT.HISTORY"], sisPersonId)
            if (personRecord) {
                List<String> contacts = personRecord.values["PERSON.CONTACT.HISTORY"] as List<String>
                if(contacts && contacts.size() > 0) {
                    List<ColleagueData> contactRecords = dmiDataService.batchKeys("CORE", "CONTACT", ["CONTACT.TYPE"], contacts)
                    List<String> contactHistory = contactRecords.collect { i -> (String) i.values["CONTACT.TYPE"] }

                    if (contactHistory && contactHistory.size() > 0) {

                        String edPlanContactType = MisEnvironment.getProperty(environment, misCode, "colleague.student.educationPlan.contact_type")
                        if (edPlanContactType) {
                            String[] edPlanContactTypeConfigs = edPlanContactType.split(",")
                            if (edPlanContactTypeConfigs) {
                                boolean found = edPlanContactTypeConfigs.any { contactHistory.contains(it) }
                                if (found) {
                                    HasEducationPlan = true
                                }
                            }
                        }


                        String englAssessmentContactType = MisEnvironment.getProperty(environment, misCode, "colleague.student.englishAssessment.contact_type")
                        if (englAssessmentContactType) {
                            String[] englAssessmentContactTypeConfigs = englAssessmentContactType.split(",")
                            if (englAssessmentContactTypeConfigs) {
                                boolean found = englAssessmentContactTypeConfigs.any { contactHistory.contains(it) }
                                if (found) {
                                    HasEnglAssessment = true
                                }
                            }
                        }


                        String mathAssessmentContactType = MisEnvironment.getProperty(environment, misCode, "colleague.student.mathAssessment.contact_type")
                        if (mathAssessmentContactType) {
                            String[] mathAssessmentContactTypeConfigs = mathAssessmentContactType.split(",")
                            if (mathAssessmentContactTypeConfigs) {
                                boolean found = mathAssessmentContactTypeConfigs.any { contactHistory.contains(it) }
                                if (found) {
                                    HasMathAssessment = true
                                }
                            }
                        }

                    }
                }
            }
        }
        finally {
            log.info("get: retrieved contact types")
        }

        //****** if has edu plan still undetermined, check rules engine ****
        if(!HasEducationPlan) {
            String edPlanRuleConfig = MisEnvironment.getProperty(environment, misCode, "colleague.student.educationPlan.rule")
            if (edPlanRuleConfig) {
                log.info("get: checking rules engine for has edu plan")
                List<String> edPlanRules = edPlanRuleConfig.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(edPlanRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    HasEducationPlan = true
                }
                log.info("get: rules engine checked for has edu plan")
            }
        }

        //****** if has engl assessment still undetermined, check rules engine ****
        if(!HasEnglAssessment) {
            String englAssessmentRuleConfig = MisEnvironment.getProperty(environment, misCode, "colleague.student.englishAssessment.rule")
            if (englAssessmentRuleConfig) {
                log.info("get: checking rules engine for has engl assessment")
                List<String> englRules = englAssessmentRuleConfig.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(englRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    HasEnglAssessment = true
                }
                log.info("get: rules engine checked for has engl assessment")
            }
        }

        //****** if has math assessment still undetermined, check rules engine ****
        if(!HasMathAssessment) {
            String mathAssessmentRuleConfig = MisEnvironment.getProperty(environment, misCode, "colleague.student.mathAssessment.rule")
            if (mathAssessmentRuleConfig) {
                log.info("get: checking rules engine for has math assessment")
                List<String> mathRules = mathAssessmentRuleConfig.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(mathRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    HasMathAssessment = true
                }
                log.info("get: rules engine checked for has math assessment")
            }
        }

        //****** Get residency status (checking incarceration and concurrently enrolled as well) ****
        log.info("get: retrieving residency status")
        ResidentStatus residentStatus
        boolean IsIncarcerated = null
        boolean IsConcurrentlyEnrolled = null
        try
        {
            ColleagueData residencyRecord = dmiDataService.singleKey("ST", "STUDENTS", ["STU.RESIDENCY.STATUS"], sisPersonId)
            if (residencyRecord) {
                List<String> stuResidentStatus = residencyRecord.values["STU.RESIDENCY.STATUS"] as List<String>
                if (stuResidentStatus && stuResidentStatus.size() > 0){
                    residentStatus = this.parseResidentStatus(misCode, stuResidentStatus[0])

                    String incarceratedRestypes = MisEnvironment.getProperty(environment, misCode,"colleague.student.incarcerationStatus.residency_status")
                    if (incarceratedRestypes){
                        String[] incarceratedConfigs = incarceratedRestypes.split(",")
                        boolean found = incarceratedConfigs.any { stuResidentStatus.contains(it) }
                        if(found) {
                            IsIncarcerated = true
                        }
                    }

                    String dualCreditRestypes = MisEnvironment.getProperty(environment, misCode,"colleague.student.dualCredit.residency_status")
                    if (dualCreditRestypes){
                        String[] dualCreditConfigs = dualCreditRestypes.split(",")
                        boolean found = dualCreditConfigs.any { stuResidentStatus.contains(it) }
                        if(found) {
                            IsConcurrentlyEnrolled = true
                        }
                    }
                }
            }
        }
        finally {
            log.info("get: done with Residency Status")
        }

        //****** Get Student Types to check incarceration and concurrently (if needed) ****
        if(!IsIncarcerated || !IsConcurrentlyEnrolled) {
            log.info("get: retrieving student types")
            List<String> studentTypes
            try {
                ColleagueData studentRecord = dmiDataService.singleKey("ST", "STUDENTS", ["STU.TYPES"], sisPersonId)
                if (studentRecord) {
                    log.info("get: got student types record")
                    studentTypes = studentRecord.values["STU.TYPES"] as List<String>

                    if(!IsIncarcerated) {
                        log.info("get: checking student types for incarceration")
                        String incarceratedStutypes = MisEnvironment.getProperty(environment, misCode, "colleague.student.incarcerationStatus.stu_type")
                        if (incarceratedStutypes) {
                            String[] incarceratedConfigs = incarceratedStutypes.split(",")
                            boolean found = incarceratedConfigs.any { studentTypes.contains(it) }
                            if(found) {
                                IsIncarcerated = true
                            }
                        }
                    }

                    if(!IsConcurrentlyEnrolled) {
                        log.info("get: checking student types for concurrently enrolled")
                        String dualCreditStutypes = MisEnvironment.getProperty(environment, misCode,"colleague.student.dualCredit.stu_type")
                        if (dualCreditStutypes){
                            String[] dualCreditConfigs = dualCreditStutypes.split(",")
                            boolean found = dualCreditConfigs.any { studentTypes.contains(it) }
                            if(found) {
                                IsConcurrentlyEnrolled = true
                            }
                        }
                    }

                }
            }
            finally {
                log.info("get: done with Student Types")
            }
        }

        //****** if incarceration still undetermined, check rules engine ****
        if(!IsIncarcerated) {
            String incarceratedRuleConfig = MisEnvironment.getProperty(environment, misCode,"colleague.student.incarcerationStatus.rule")
            if (incarceratedRuleConfig) {
                log.info("get: checking rules engine for incarceration")
                List<String> incarcerationRules = incarceratedRuleConfig.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(incarcerationRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    IsIncarcerated = true
                }
                log.info("get: rules engine checked for incarceration")
            }
        }

        //****** if concurrent enrollment still undetermined, check rules engine ****
        if(!IsConcurrentlyEnrolled) {
            String concurrentEnrollmentRules = MisEnvironment.getProperty(environment, misCode,"colleague.student.dualCredit.rule")
            if (concurrentEnrollmentRules) {
                log.info("get: checking rules engine for concurrently enrolled")
                List<String> concurrentEnrollRules = concurrentEnrollmentRules.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(concurrentEnrollRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    IsConcurrentlyEnrolled = true
                }
                log.info("get: rules engine checked for concurrently enrolled")
            }
        }

        //****** Get student visa ****
        log.info("get: retrieving student visa")
        String VisaType = ""
        try {
            ColleagueData personRecord = dmiDataService.singleKey("CORE", "PERSON", ["VISA.TYPE"], sisPersonId)
            if (personRecord) {
                VisaType = personRecord.values["VISA.TYPE"] as String
            }
        }
        finally {
            log.info("get: done with Student Visa")
        }

        //****** Get Has CA address ****
        log.info("get: retrieving student addresses")
        Boolean HasCaliforniaAddress = false
        try
        {
            ColleagueData personRecord = dmiDataService.singleKey("CORE", "PERSON", ["PERSON.ADDRESSES"], sisPersonId)
            if (personRecord) {
                List<String> addressKeys = personRecord.values["PERSON.ADDRESSES"] as List<String>
                if(addressKeys && addressKeys.size() > 0) {
                    List<ColleagueData> addressRecords = dmiDataService.batchKeys("CORE", "ADDRESS", ["STATE"], addressKeys)
                    if (addressRecords && addressRecords.size() > 0) {
                        HasCaliforniaAddress = addressRecords.any { "CA".equalsIgnoreCase(it.values["STATE"]) }
                    }
                }
            }
        }
        finally {
            log.info("get: done checking for CA address")
        }

        //****** Get Enrollment Holds ****
        log.info("get: retrieving student enrollment holds")
        Boolean hasEnrollmentHold = false
        try {
            String enrollmentHoldRules = MisEnvironment.getProperty(environment, misCode, "colleague.student.enrollmentHolds.rule")
            if (enrollmentHoldRules) {
                List<String> holdRules = enrollmentHoldRules.split(",").toList()
                RuleResult ruleResponse = dataUtils.executeRule(holdRules, [sisPersonId], null, null, false, null)
                if (ruleResponse.result == true && ruleResponse.validIds.contains(sisPersonId)) {
                    hasEnrollmentHold = true
                }
            }
        }
        finally {
            log.info("get: done checking for enrollment holds")
        }

        //****** Get Registration Start date ****
        log.info("get: retrieving student registration start date")
        Date regDate = null
        try {
            String query = "RGPR.TERM = " + ColleagueUtils.quoteString(sisTermId) + " AND WITH RGPR.STUDENT = " + ColleagueUtils.quoteString(sisPersonId) + " BY RGPR.START.DATE "
            List<ColleagueData> registrationDates = dmiDataService.batchSelect("ST", "REG.PRIORITIES", ["RGPR.START.DATE"], query)
            if (registrationDates && registrationDates.size() > 0) {
                String firstDate = registrationDates[0].values["RGPR.START.DATE"] as String
                if (firstDate) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")
                    regDate = df.parse(firstDate)
                }
            }
        }
        finally {
            log.info("get: done checking registration start date")
        }


        //****** Get Student Tuition Balance ****
        log.info("get: retrieving student tuition balance ")
        Float stuBalance
        try {
            List<KeyValuePair<String, String>> parms = [
                    new KeyValuePair("Student", sisPersonId),
                    new KeyValuePair("Terms", sisTermId)
            ]
            CTXData transactionResult = dmiCTXService.execute("ST", "X.CCCTC.STUDENT.BALANCE.CA", parms)
            dataUtils.validateExecution(transactionResult)

            String balance = transactionResult.variables["Balance"] as String
            if (balance) {
                stuBalance = Float.parseFloat(balance)
            }
        }
        finally {
            log.info("get: done checking student tuition balance ")
        }


        //****** Get Student DSPS eligibility ****
        log.info("get: retrieving student disability eligibility ")
        Boolean dspsEligible
        try {
            String query = " STUDENT.DSPS.ID EQ " + ColleagueUtils.quoteString(sisPersonId) + " AND WITH STDSPS.OVR.DISB.TERM "
            List<String> stdspsKeys = dmiDataService.selectKeys("STUDENT.DSPS", query) as List<String>
            if (stdspsKeys && stdspsKeys.size() > 0) {
                dspsEligible = true
            } else {
                String hquery = " PERSON.HEALTH.ID EQ " + ColleagueUtils.quoteString(sisPersonId) + " AND WITH PHL.DISABILITY "
                List<String> phKeys = dmiDataService.selectKeys("PERSON.HEALTH", hquery) as List<String>
                if (phKeys && phKeys.size() > 0) {
                    dspsEligible = true
                }
            }
        }
        finally {
            log.info("get: done checking student disability eligibility")
        }

        //****** Get Financial Aid data ****
        log.info("get: retrieving student financial aid data")
        Boolean hasFinAidAward
        Boolean hasBogfw
        try
        {
            String fileSuiteYear = MisEnvironment.getProperty(environment, misCode,"colleague.student.financialAid.rule.file_suite_year")
            String andOrFlag = MisEnvironment.getProperty(environment, misCode,"colleague.student.financialAid.rule.and_or_flag")
            String faActiveAwardStatus = MisEnvironment.getProperty(environment, misCode, "colleague.student.financialAid.award_status.active")
            String faBogfwAwards = MisEnvironment.getProperty(environment, misCode, "colleague.student.financialAid.ccPromise.award_id")

            List<KeyValuePair<String, String>> parms = [
                    new KeyValuePair("Student", sisPersonId),
                    new KeyValuePair("SisTermId", sisTermId),
                    new KeyValuePair("FileSuiteYear", fileSuiteYear),
                    new KeyValuePair("AndOrFlag", andOrFlag),
                    new KeyValuePair("FaActiveAwardStatus", faActiveAwardStatus),
                    new KeyValuePair("FaBogfwAwards", faBogfwAwards)
            ]
            CTXData executeResult = dmiCTXService.execute("ST", "X.CCCTC.GET.FIN.AID.INFO", parms)
            dataUtils.validateExecution(executeResult)

            hasFinAidAward = executeResult.variables["HasFinancialAidAward"]
            hasBogfw = executeResult.variables["HasBogfw"]
        }
        finally {
            log.info("get: done checking student financial aid info")
        }


        //****** Get Cohorts ****
        log.info("get: retrieving student cohort data")
        List<Cohort> cohorts = new ArrayList<Cohort>()
        try {
            String cccIdtype = MisEnvironment.getProperty(environment, misCode, "colleague.identity.cccId.alt_id_type")
            String ce_cohortPrefix = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.cohort_prefix.cohortTypeEnum.COURSE_EXCHANGE")
            String ce_cohortEnum = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.cohortTypeEnum.cohort_prefix.CE")
            String termCodeFormat = MisEnvironment.getProperty(environment, misCode, "colleague.student.cohorts.term.format")

            List<KeyValuePair<String, String>> parms = [
                    new KeyValuePair("Cccid", cccId),
                    new KeyValuePair("CccidType", cccIdtype),
                    new KeyValuePair("SisTermId", sisTermId),
                    new KeyValuePair("TermCodeFormat", termCodeFormat),
                    new KeyValuePair("CohortPrefix", ce_cohortPrefix),
                    new KeyValuePair("CohortCode", ce_cohortEnum)
            ]
            /** Course Exchange Cohort Format:
             // Prefix + T + YY
             //   Prefix is defined in the config. Must be 1 or 2 characters. Default is CE.
             //   T is a one character term code of F, W, S, U for Fall, Winter, Spring, Summer respectively
             //   YY is the last two digits of the year
             // Example: CEF17 = Fall 2017
             */
            CTXData collresult = dmiCTXService.execute("ST", "X.CCCTC.GET.COHORT", parms)
            dataUtils.validateExecution(collresult)

            List<String> studentCohorts = collresult.variables["Cohorts"] as List<String>

            if (studentCohorts) {
                for (int j = 0; j < studentCohorts.size(); j = j + 2) {
                    String Name = studentCohorts[j]
                    String description = studentCohorts[j + 1]

                    Cohort curCohort = new Cohort()
                    CohortTypeEnum found = CohortTypeEnum.values().find {
                        return it.name().equalsIgnoreCase(Name)
                    }
                    curCohort.setName(found)
                    cohorts.add(curCohort)
                }
            } else {
                cohorts = null
            }
        }
        finally {
            log.info("get: done checking student cohorts")
        }

        com.ccctc.adaptor.model.Student result = new com.ccctc.adaptor.model.Student.Builder()
                .cccid(cccId)
                .sisPersonId(sisPersonId)
                .sisTermId(sisTermId)
                .hasCaliforniaAddress(HasCaliforniaAddress)
                .visaType(VisaType)
                .isConcurrentlyEnrolled(IsConcurrentlyEnrolled)
                .isIncarcerated(IsIncarcerated)
                .orientationStatus(orientationStatus)
                .hasEnglishAssessment(HasEnglAssessment)
                .hasMathAssessment(HasMathAssessment)
                .hasEducationPlan(HasEducationPlan)
                .hasHold(hasEnrollmentHold)
                .accountBalance(stuBalance)
                .dspsEligible(dspsEligible)
                .registrationDate(regDate)
                .applicationStatus(applicationStatus)
                .residentStatus(residentStatus)
                .hasFinancialAidAward(hasFinAidAward)
                .hasBogfw(hasBogfw)
                .cohorts(cohorts)
                .build()

        return result
    }


    List<Map> getAllByTerm(String misCode, List<String> sisTermIds, StudentFieldSet fieldSet) {

        //****** Validate parameters ****
        if (fieldSet == null || fieldSet == StudentFieldSet.ALL) {
            String errMsg = "getAllByTerm: retrieving all fields not currently supported. use IDENTIFIERS or MATRICULATION"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!misCode) {
            String errMsg = "getAllByTerm: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (sisTermIds) {
            sisTermIds.removeAll(Arrays.asList("", null))
        }

        if (!sisTermIds || sisTermIds.empty) {
            api.colleague.Term termAPI = new api.colleague.Term()
            termAPI.colleagueInit(misCode, environment, Cinit)
            List<String> currentTermIds = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC))
            if(currentTermIds != null && currentTermIds.size() > 0) {
                sisTermIds.push(currentTermIds[0])
            }
        }

        if (!sisTermIds || sisTermIds.empty) {
            String errMsg = "getAllByTerm: valid sisTermIds not provided and could not find default"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        for(String sisTermId in sisTermIds) {
            if (!dataUtils.validateTerm(sisTermId)) {
                throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")
            }
        }

        Map inMemoryStudentDB
        if(fieldSet == StudentFieldSet.ALL) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.Student>()
        } else if (fieldSet == StudentFieldSet.IDENTIFIERS) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.students.StudentIdentityDTO>()
        } else if (fieldSet == StudentFieldSet.MATRICULATION) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.students.StudentMatriculationDTO>()
        }

        for(String sisTermId : sisTermIds) {
            log.info("getAllByTerm: fetching data for term [" + sisTermId + "]")

            List<String> termPersonIds = new ArrayList<String>()

            // Pull from applicants table where start_term is target
            String query = " APPL.START.TERM EQ " + ColleagueUtils.quoteString(sisTermId)
            List<String> applicationColumns = ["APPL.APPLICANT"]
            if (fieldSet == StudentFieldSet.MATRICULATION || fieldSet == StudentFieldSet.ALL) {
                applicationColumns.add("APPL.STATUS")
            }
            List<ColleagueData> applicationRecords = dmiDataService.batchSelect("ST", "APPLICATIONS", applicationColumns, query)
            if (applicationRecords && applicationRecords.size() > 0) {
                for (ColleagueData applicationRecord in applicationRecords) {
                    String sisPersonId = applicationRecord.values["APPL.APPLICANT"]
                    termPersonIds.add(sisPersonId)
                    Tuple key = new Tuple(sisTermId, sisPersonId)
                    def record = inMemoryStudentDB.get(key)
                    if (!record) {
                        record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                    }

                    if (fieldSet == StudentFieldSet.MATRICULATION || fieldSet == StudentFieldSet.ALL) {
                        //****** Get Admissions/Application status ****
                        List<String> currentApplicationAllStatuses = applicationRecord.values["APPL.STATUS"] as List<String>
                        if (currentApplicationAllStatuses && currentApplicationAllStatuses.size() > 0) {
                            ApplicationStatus applicationStatus = this.parseApplicationStatus(misCode, currentApplicationAllStatuses[0])
                            record.setApplicationStatus(applicationStatus)
                        }
                    }

                    inMemoryStudentDB.put(key, record)
                }
            } else {
                continue
            }

            // Future Enhancement Potential:
            // Pull from student term tables where student_term is target
            // String stQuery = " STTR.TERM = " + ColleagueUtils.quoteString(sisTermId) + " SAVING UNIQUE STTR.STUDENT "
            // List<String> studentTermRecords = dmiDataService.selectKeys("STUDENT.TERMS", stQuery)
            // if (studentTermRecords && studentTermRecords.size() > 0) {
            //     log.info("getAllByTerm: Parsing Student Term records [" + studentTermRecords.size() + "]")
            //     for (String sisPersonId in studentTermRecords) {
            //         termPersonIds.add(sisPersonId)
            //         Tuple key = new Tuple(sisTermId, sisPersonId)
            //         def record = inMemoryStudentDB.get(key)
            //         if (!record) {
            //             record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
            //             inMemoryStudentDB.put(key, record)
            //         }
            //     }
            // }

            // make sure they are unique
            termPersonIds.unique()

            if (fieldSet == StudentFieldSet.MATRICULATION || fieldSet == StudentFieldSet.ALL) {
                //****** Get orientation status ****
                log.info("getAllByTerm: retrieving orientation status")
                try {
                    String orientationCodeUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.code.app_user.field")
                    String orientationTermUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.term.app_user.field")

                    if (!orientationCodeUserFld || !orientationTermUserFld) {
                        String errMsg = "Orientation Status field information not defined in the configuration file"
                        throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
                    }

                    String criteria = orientationTermUserFld + " EQ " + ColleagueUtils.quoteString(sisTermId)
                    List<ColleagueData> orientationRecords = dmiDataService.batchSelect("ST", "APPLICANTS", [orientationCodeUserFld], criteria)
                    if (orientationRecords && orientationRecords.size() > 0) {
                        log.info("getAllByTerm: orientation status records found [" + orientationRecords.size() + "]")
                        for (ColleagueData orientationRecord in orientationRecords) {
                            String sisPersonId = orientationRecord.key
                            String orientStatus = orientationRecord.values[orientationCodeUserFld]

                            OrientationStatus orientationStatus = this.parseOrientationStatus(misCode, orientStatus)

                            Tuple key = new Tuple(sisTermId, sisPersonId)
                            def record = inMemoryStudentDB.get(key)
                            if (!record) {
                                record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                            }

                            record.setOrientationStatus(orientationStatus)
                            inMemoryStudentDB.put(key, record)
                        }
                    }
                }
                finally {
                    log.info("getAllByTerm: done with Orientation Status")
                }

                //****** Get contact types for education plan ****
                log.info("getAllByTerm: retrieving education plan for students")
                try {
                    List<String> sisPersonIdsToRunRuleFor = termPersonIds
                    String edPlanContactTypeConfig = MisEnvironment.getProperty(environment, misCode, "colleague.student.educationPlan.contact_type")
                    if (edPlanContactTypeConfig) {
                        List<String> edPlanContactTypes = edPlanContactTypeConfig.split(",")

                        List<ColleagueData> personRecords = dmiDataService.batchKeys("CORE", "PERSON", ["PERSON.CONTACT.HISTORY"], termPersonIds)
                        List<String> contactIDs = personRecords.collect { it.values["PERSON.CONTACT.HISTORY"] as List<String> }.flatten()
                        contactIDs.unique().removeAll(Arrays.asList("", null))
                        // Some colleges (im looking at you, Butte) have bad data; missing contact records
                        // so select the keys for contact records that really exist
                        // note: selecting an ID for record that does not exists, stops the processing and silently returns/fails.
                        // e.g. if 8100 records provided, but only 7625 actually exist, its possible that only 825 records would be returned! (thats bad)
                        // I consider it a bug in the unidata layer (not anything our code has done wrong)
                        contactIDs = dmiDataService.selectKeys("CONTACT", "", contactIDs)
                        if(contactIDs && contactIDs.size() > 0) {
                            List<ColleagueData> contactRecords = dmiDataService.batchKeys("CORE", "CONTACT", ["CONTACT.TYPE"], contactIDs)
                            if (contactRecords && contactRecords.size() > 0) {
                                List<String> edPlanRecords = contactRecords
                                        .findAll { i -> edPlanContactTypes.contains(i.values["CONTACT.TYPE"]) }
                                        .collect { i -> personRecords.find { j -> (j.values["PERSON.CONTACT.HISTORY"] as List<String>)?.contains(i.key) }?.key }
                                        .unique()
                                for (String sisPersonId in edPlanRecords) {
                                    sisPersonIdsToRunRuleFor.remove(sisPersonId)
                                    Tuple key = new Tuple(sisTermId, sisPersonId)
                                    def record = inMemoryStudentDB.get(key)
                                    if (!record) {
                                        record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                                    }

                                    record.setHasEducationPlan(true)
                                    inMemoryStudentDB.put(key, record)
                                }
                            }
                        }
                    }

                    String edPlanRuleConfig = MisEnvironment.getProperty(environment, misCode, "colleague.student.educationPlan.rule")
                    if (edPlanRuleConfig && sisPersonIdsToRunRuleFor && sisPersonIdsToRunRuleFor.size() > 0) {
                        List<String> edPlanRules = edPlanRuleConfig.split(",").toList()
                        RuleResult ruleResponse = dataUtils.executeRule(edPlanRules, sisPersonIdsToRunRuleFor, null, null, false, null)
                        if (ruleResponse.result == true) {
                            for (String sisPersonId in ruleResponse.validIds) {
                                Tuple key = new Tuple(sisTermId, sisPersonId)
                                def record = inMemoryStudentDB.get(key)
                                if (!record) {
                                    record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                                }

                                record.setHasEducationPlan(true)
                                inMemoryStudentDB.put(key, record)
                            }
                        }
                    }
                }
                finally {
                    log.info("getAllByTerm: retrieved education plan status for students")
                }
            }
        }

        List<String> sisPersonIds = inMemoryStudentDB.keySet().collect { it.get(1) }.unique()
        //****** pull and parse cccIds ****
        Map<String, String> cccIds = dataUtils.getCccIdsByColleagueIds(sisPersonIds)
        for (def s : cccIds) {
            for(String sisTermId : sisTermIds) {
                Tuple key = new Tuple(sisTermId, s.key)
                def record = inMemoryStudentDB.get(key)
                if(record != null) {
                    record.setCccid(s.value)
                    inMemoryStudentDB.put(key, record)
                }
            }
        }

        log.info("getAllByTerm: data retrieved and parsed; converting to map")
        List<java.util.Map> filterFieldResults = []
        for (def s : inMemoryStudentDB.values()) {
            HashMap fieldMap = mapper.convertValue(s, HashMap.class)
            filterFieldResults.add(fieldMap)
        }
        log.info("getAllByTerm: returning data. rows [" + filterFieldResults.size() + "]")
        return filterFieldResults
    }

    /**
     * Get a student's home college
     *
     * @param misCode MIS Code
     * @param cccId CCC ID
     * @return Student Home College
     */
    StudentHomeCollege getHomeCollege(String misCode, String cccId) {
        throw new InternalServerException("Unsupported")
    }

    /**
     * Add a student to a cohort
     *
     * @param cccId CCC ID
     * @param cohortName Cohort
     * @param misCode MIS Code
     * @param sisTermId SIS Term ID
     */
    void postCohort(String cccId, CohortTypeEnum cohortName, String misCode, String sisTermId) {
        assert cccId != null
        assert cohortName != null
        assert misCode != null
        assert sisTermId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Remove a student from a cohort
     *
     * @param cccId CCC ID
     * @param cohortName Cohort
     * @param misCode MIS Code
     * @param sisTermId SIS Term ID
     */
    void deleteCohort(String cccId, CohortTypeEnum cohortName, String misCode, String sisTermId) {
        assert cccId != null
        assert cohortName != null
        assert misCode != null
        assert sisTermId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Get a list of the CCC IDs assigned to a student
     *
     * @param misCode MIS Code
     * @param sisPersonId SIS Person ID
     * @return List of CCC IDs assigned to student
     */
    List<String> getStudentCCCIds(String misCode, String sisPersonId) {

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if (!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "sisPersonId cannot be null or blank")
        }

        Map<String, String> result = dataUtils.getCccIdsByColleagueIds([sisPersonId])
        if(result && result.containsKey(sisPersonId) && result.get(sisPersonId)) {
            return [ result.get(sisPersonId) ]
        }

        return [ ]
    }

    /**
     * Assign a CCC ID to a student. Will throw an exception of the student already has a CCC ID or if the
     * CCC ID being assigned has already been assigned to another student.
     *
     * @param misCode MIS Code
     * @param sisPersonId SIS Person ID
     * @param cccId CCC ID
     * @return List of CCC IDs assigned to student
     */
    void postStudentCCCId(String misCode, String sisPersonId, String cccId) {
        assert misCode != null
        assert sisPersonId != null
        assert cccId != null

        throw new InternalServerException("Unsupported")
    }

    /**
     * Sets the student details for the given cccid for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report student information on (optional; if blank, tries to find 'current' term, or closest next term)
     * @Note when performing updates to multiple fields, each update is separately executed. Function bails out as soon as any one error
     * occurs. Its possible that one update succeeds and then a second one fails; the first would stick, but an exception would be thrown
     */
    void patch(String misCode, String cccId, String sisTermId, com.ccctc.adaptor.model.Student updates) {

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "patch: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.misCodeNotFound, errMsg)
        }

        if (!updates) {
            String errMsg = "patch: Student updates cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }

        if(updates.applicationStatus != null) {
            String errMsg = "patch: updating Application/Admissions status is not supported"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }

        if(updates.hasEducationPlan != null) {
            String errMsg = "patch: updating hasEducationPlan is not supported"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        }

        if (!cccId) {
            String errMsg = "patch: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
        } else {
            cccId = cccId.toUpperCase()
        }

        String sisPersonId
        List<String> sisPersonIds = dataUtils.getColleagueIds(cccId)
        if(!sisPersonIds || sisPersonIds.size() <= 0) {
            String errMsg = "patch: Student mapping returned no results: student not found"
            log.error(errMsg)
            throw new EntityNotFoundException(errMsg)
        } else if (sisPersonIds.size() > 1) {
            String errMsg = "patch: Student mapping returned several results: too many students found"
            log.error(errMsg)
            throw new EntityNotFoundException(errMsg)
        } else {
            sisPersonId = sisPersonIds[0]
        }

        if (!sisTermId) {
            sisTermId = updates.sisTermId
        }

        if (!sisTermId) {
            // Get current term code
            api.colleague.Term termAPI = new api.colleague.Term();
            termAPI.colleagueInit(misCode, environment, Cinit);
            List<String> currentTermIds = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC));
            if(currentTermIds != null && currentTermIds.size() > 0) {
                sisTermId = currentTermIds[0]
            }

            if (!sisTermId) {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "could not determine sisTermId")
            }
        } else if (!dataUtils.validateTerm(sisTermId)) {
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound, "Term not found")
        }

        if(updates.orientationStatus != null) {
            if(updates.orientationStatus == OrientationStatus.UNKNOWN) {
                String errMsg = "patch: Setting Orientation Status to UNKNOWN is unsupported"
                log.error(errMsg)
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
            }

            String orientationCodeUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.code.app_user.field")
            if(StringUtils.isEmpty(orientationCodeUserFld)) {
                String errMsg = "patch: Orientation Status code app_userField config setting not set"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.internalServerError, errMsg)
            }
            String orientationTermUserFld = MisEnvironment.getProperty(environment, misCode, "colleague.student.orientationStatus.term.app_user.field")
            if(StringUtils.isEmpty(orientationTermUserFld)) {
                String errMsg = "patch: Orientation Status term app_userField config setting not set"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.internalServerError, errMsg)
            }

            String statusToSet = this.translateOrientationStatus(misCode, updates.orientationStatus)
            if(StringUtils.isEmpty(statusToSet)) {
                String errMsg = "patch: No mapping for the given orientation status found"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.internalServerError, errMsg)
            }

            /**
             * execute Colleague transaction
             */
            List<KeyValuePair<String, String>> parms = [
                    new KeyValuePair("SisPersonId", sisPersonId),
                    new KeyValuePair("SisTermId", sisTermId),
                    new KeyValuePair("OrientationStatus", statusToSet),
                    new KeyValuePair("OrientationCodeUserFld", orientationCodeUserFld),
                    new KeyValuePair("OrientationTermUserFld", orientationTermUserFld),
            ]
            CTXData result = dmiCTXService.execute("ST", "X.CCCTC.POST.ORIENT.STATUS", parms)
            dataUtils.validateExecution(result)
        }
    }

    protected def getDefaultStudentDTO(String sisTermId, String sisPersonId, StudentFieldSet fs) {
        def result
        if(fs == StudentFieldSet.IDENTIFIERS) {
            result = new com.ccctc.adaptor.model.students.StudentIdentityDTO()
        } else if(fs == StudentFieldSet.MATRICULATION) {
            result = new com.ccctc.adaptor.model.students.StudentMatriculationDTO()
        } else {
            result = new com.ccctc.adaptor.model.Student()
        }
        result.setSisTermId(sisTermId)
        result.setSisPersonId(sisPersonId)
        return result
    }

    /**
     * Translates/Parses the oStatus colleague value into an OrientationStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param oStatus The code stored in the colleague table for orientation status
     * @return An OrientationStatus value that maps to the oStatus provided
     */
    protected OrientationStatus parseOrientationStatus(String misCode, String oStatus) {

        OrientationStatus result = OrientationStatus.UNKNOWN
        if (oStatus) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.orientationStatus.app_userField." + oStatus)
            if (configMapping) {
                OrientationStatus found = OrientationStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping as String)
                }
                if (found) {
                    result = found
                }  else {
                    log.warn("Could not parse app_UserFiled [" + oStatus + "] mapping")
                }
            }
        }
        return result
    }

    /**
     * Translates/Parses an orientation status into an colleague value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param orientationStatus (potentially from MyPath) that indicates whether student has completed all orientation requirements
     * @return A colleague app_userField value that maps to the status provided
     */
    protected String translateOrientationStatus(String misCode, OrientationStatus oStatus) {
        String result
        if(oStatus) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.app_userField.orientationStatus." + oStatus)
            if (configMapping) {
                result = configMapping
            } else {
                log.warn("orientation status [" + oStatus + "] to internal code mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the applstatus into a ApplicationStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param applStatus The Application status code
     * @return A ApplicationStatus enum value that maps to the applStatus provided
     */
    protected ApplicationStatus parseApplicationStatus(String misCode, String applStatus) {
        ApplicationStatus result = ApplicationStatus.NoApplication
        if (applStatus) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.applicationStatus.app_status." + applStatus)
            if (configMapping) {
                ApplicationStatus found = ApplicationStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping as String)
                }
                if (found) {
                    result = found
                }
            }
        }
        return result
    }

    /**
     * Translates/Parses the residency fields into a ResidentStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param residency Residency code
     * @return A ResidentStatus enum value that maps to the residency
     */
    protected ResidentStatus parseResidentStatus(String misCode,  String residency) {

        ResidentStatus result = null

        String defaultStatus = MisEnvironment.getProperty(environment, misCode, "colleague.student.residentStatus.default")
        if (defaultStatus) {
            result = ResidentStatus.values().find {
                return it.name().equalsIgnoreCase(defaultStatus as String)
            }
        }

        if (residency) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "colleague.student.mappings.residentStatus.residency_status." + residency)
            if (configMapping) {
                ResidentStatus found = ResidentStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (found) {
                    result = found
                }
            }
        }
        return result
    }
}