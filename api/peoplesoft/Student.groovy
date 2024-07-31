package api.peoplesoft

import com.ccctc.adaptor.config.JacksonConfig
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import com.ccctc.adaptor.model.students.*
import com.ccctc.adaptor.util.MisEnvironment
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import java.text.SimpleDateFormat
import java.time.LocalDate
import org.springframework.core.env.Environment

import java.time.ZoneId
import java.time.ZoneOffset

/**
 * <h1>Student Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Retrieving a List of Students by term(s)</li>
 *         <li>Retrieving a Student's data (which has List of Cohorts)</li>
 *         <li><span>Setting/updating student data:</span>
 *             <ol>
 *                 <li>Orientation Status</li>
 *             </ol>
 *         </li>
 *         <li>Adding a Student to a Cohort for a term</li>
 *         <li>Removing a Student from a Cohort for a term</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 *
 * @version 4.0.0
 *
 */
class Student {

    protected final static Logger log = LoggerFactory.getLogger(Student.class)

    protected final static ObjectMapper mapper = new JacksonConfig().jacksonObjectMapper()

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment


    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Student(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Student(Environment e) {
        this.environment = e
    }

    /**
     * Gets the student details for the given cccid for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report student information on (required)
     * @return a Student record matching the search params provided if found
     */
    com.ccctc.adaptor.model.Student get(String misCode, String cccid, String sisTermId) {

        log.debug("get: retrieving student data")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "get: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!cccid) {
            String errMsg = "get: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cccid = cccid.toUpperCase()
        }

        api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
        String sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccid)
        if(!sisPersonId) {
            String errMsg = "get: Student mapping returned no results: student not found"
            log.error(errMsg)
            throw new EntityNotFoundException(errMsg)
        }

        if(!sisTermId) {
            log.debug("get: term code not provided; attempting to find a default")
            api.peoplesoft.Term termAPI = new api.peoplesoft.Term(this.environment)
            List<com.ccctc.adaptor.model.Term> terms = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC))
            if(terms != null && terms.size() > 0) {
                sisTermId = terms[0].sisTermId
            }
        }

        if (!sisTermId) {
            String errMsg = "get: sisTermId not provided and could not be defaulted"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("get: params ok; attempting to get a new peoplesoft Session")


        String[] orientationData
        String[] assessmentData
        String[] educationPlanData
        String[] dspsData
        String[] enrollmentHoldsData
        String[] dualCreditData
        String[] addressesData
        String[] visaPermitData
        String[] admissionsData
        String[] enrollApptData
        String[] prisonData
        String[] residencyData
        String[] faAwardsData
        String[] tuitionBalanceData

        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {

            //****** Build parameters to send to our custom Peopletools API ****
            log.debug("get: building params to retrieve Orientation data")
            String packageName = "CCTC_IDENTITY_PKG"
            String className = "CCTC_Student"
            String methodName = "getOrientationStatus"

            String cleanPersonId = PSParameter.ConvertStringToCleanString(sisPersonId)
            String cleanTermId = PSParameter.ConvertStringToCleanString(sisTermId)
            String cleanInstitution = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution"))
            String cleanAcadCareer = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
            String[] args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Orientation data")
            orientationData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve assessment data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getAssessments"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Assessment data")
            assessmentData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Education Plan data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getEducationPlan"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Education Plan data")
            educationPlanData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve DSPS eligibility data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getDisabilitySupportEligibility"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting DSPS eligibility data")
            dspsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Enrollment Hold data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getEnrollmentHolds"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Enrollment Hold data")
            enrollmentHoldsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Dual Credit data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getDualCreditStatus"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer,
                    PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.dualCredit.stdnt_attr")),
                    PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.dualCredit.stdnt_attr_value"))
            ]

            log.debug("get: getting Dual Credit data")
            dualCreditData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Address data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Person"
            methodName = "getPersonAddress"

            args = [
                    cleanPersonId
            ]

            log.debug("get: getting Address data")
            addressesData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Visa data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getVisaStatus"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Visa data")
            visaPermitData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Admissions data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getAdmissionStatus"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Admissions data")
            admissionsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Enrollment Appointment data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getEnrollmentAppt"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Enrollment Appointment data")
            enrollApptData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Prison Incarceration data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getIncarcerationStatus"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Prison Incarceration data")
            prisonData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Financial Aid data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getFinancialAidAwards"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Financial Aid data")
            faAwardsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)


            log.debug("get: building params to retrieve Residency data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getResidencyStatus"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Residency data")
            residencyData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            log.debug("get: building params to retrieve Tuition data")
            packageName = "CCTC_IDENTITY_PKG"
            className = "CCTC_Student"
            methodName = "getTuitionOwed"

            args = [
                    cleanPersonId,
                    cleanTermId,
                    cleanInstitution,
                    cleanAcadCareer
            ]

            log.debug("get: getting Tuition data")
            tuitionBalanceData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs != null && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("get: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("get: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("get: all data retrieved. parsing...")

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")

        Integer i = 0


        OrientationStatus oStatus = OrientationStatus.UNKNOWN
        if(orientationData != null && orientationData.length > 0) {
            i = 0
            while(i + 3 <= orientationData.length) {
                String person = orientationData[i++]
                String orien_svcs = orientationData[i++]
                String orien_exmp = orientationData[i++]
                oStatus = this.parseOrientationServices(misCode, orien_svcs)
                if(oStatus == OrientationStatus.UNKNOWN ||
                   oStatus == OrientationStatus.REQUIRED) {
                    Boolean exempt = this.isOrientationExempt(misCode, orien_exmp)
                    if (exempt) {
                        oStatus = this.parseOrientationExemption(misCode, orien_exmp)
                    }
                }
            }
        }


        Boolean hasMathAssessment = false
        Boolean hasEnglishAssessment = false
        if(assessmentData != null && assessmentData.length > 0) {
            i = 0
            while(i + 3 <= assessmentData.length) {
                String person = assessmentData[i++]
                String english = assessmentData[i++]
                String math = assessmentData[i++]
                if(StringUtils.equalsIgnoreCase(english, "Y")) {
                    hasEnglishAssessment = true
                }
                if(StringUtils.equalsIgnoreCase(math, "Y")) {
                    hasMathAssessment = true
                }
            }
        }

        Boolean hasEducationPlan = false
        if(educationPlanData != null && educationPlanData.length > 0) {
            i = 0
            while (i + 2 <= educationPlanData.length) {
                String person = educationPlanData[i++]
                String educationPlan = educationPlanData[i++]
                if (StringUtils.isNotBlank(educationPlan)) {
                    hasEducationPlan = true
                }
            }
        }

        Boolean dspsEligible = false
        if(dspsData != null && dspsData.length > 0) {
            i = 0
            while(i + 2 <= dspsData.length) {
                String person = dspsData[i++]
                String dspsFlag = dspsData[i++]
                if(StringUtils.equalsIgnoreCase(dspsFlag, "Y")) {
                    dspsEligible = true
                }
            }
        }

        Boolean hasEnrollmentHold = false
        if(enrollmentHoldsData != null && enrollmentHoldsData.length > 0) {
            i = 0
            while(i + 2 <= enrollmentHoldsData.length) {
                String person = enrollmentHoldsData[i++]
                String eHold = enrollmentHoldsData[i++]
                if(StringUtils.equalsIgnoreCase(eHold, "Y")) {
                    hasEnrollmentHold = true
                }
            }
        }

        Boolean isConcurrentlyEnrolled = false
        if(dualCreditData != null && dualCreditData.length > 0) {
            i = 0
            while(i + 2 <= dualCreditData.length) {
                String person = dualCreditData[i++]
                String hsEnrollment = dualCreditData[i++]
                if(StringUtils.equalsIgnoreCase(hsEnrollment, "Y")) {
                    isConcurrentlyEnrolled = true
                }
            }
        }


        Boolean hasCaliforniaAddress = false
        if(addressesData != null && addressesData.length > 0) {
            i = 0
            while(i + 8 <= addressesData.length) {
                String person = addressesData[i++]
                String addressType = addressesData[i++]
                String addressLine1 = addressesData[i++]
                String addressLine2 = addressesData[i++]
                String addressLine3 = addressesData[i++]
                String city = addressesData[i++]
                String state = addressesData[i++]
                String zip = addressesData[i++]
                if(StringUtils.equalsIgnoreCase(state, "CA")) {
                    hasCaliforniaAddress = true
                }
            }
        }

        String visaType
        if(visaPermitData != null && visaPermitData.length > 0) {
            i = 0
            while(i + 2 <= visaPermitData.length) {
                String person = visaPermitData[i++]
                visaType = visaPermitData[i++]
            }
        }

        ApplicationStatus applicationStatus
        if(admissionsData != null && admissionsData.length > 0) {
            i = 0
            while(i + 2 <= admissionsData.length) {
                String person = admissionsData[i++]
                applicationStatus = this.parseApplicationStatus(misCode, admissionsData[i++])
            }
        }

        ResidentStatus residentStatus
        if(residencyData != null && residencyData.length > 0) {
            i = 0
            while(i + 3 <= residencyData.length) {
                String person = residencyData[i++]
                String adm_exp = residencyData[i++]
                String residency = residencyData[i++]
                residentStatus = this.parseResidentStatus(misCode, residency, adm_exp)
            }
        }

        Date registrationDate
        if(enrollApptData != null && enrollApptData.length > 0) {
            i = 0
            while(i + 3 <= enrollApptData.length) {
                String person = enrollApptData[i++]
                String start = enrollApptData[i++]
                String end = enrollApptData[i++]
                if(start) {
                    Date start_dt = df.parse(start)
                    if(start_dt) {
                        if (!registrationDate || start_dt.before(registrationDate)) {
                            registrationDate = start_dt
                        }
                    }
                }
            }
        }

        Boolean isIncarcerated = false
        if(prisonData != null && prisonData.length > 0) {
            i = 0
            while(i + 2 <= prisonData.length) {
                String person = prisonData[i++]
                String incarceration = prisonData[i++]
                if(StringUtils.equalsIgnoreCase(incarceration, "Y")) {
                    isIncarcerated = true
                }
            }
        }

        Boolean hasFeeWaiver = false
        Boolean hasFinancialAidAward = false
        if(faAwardsData != null && faAwardsData.length > 0) {
            i = 0
            while(i + 2 <= faAwardsData.length) {
                String person = faAwardsData[i++]
                String awardReportCode = faAwardsData[i++]
                String ccPromiseReportCodes = MisEnvironment.getProperty(environment, misCode,"peoplesoft.student.financialAid.ccPromise.report_code")
                Boolean thisRowIsCCPromise = false
                StringUtils.split(ccPromiseReportCodes, ",").each {
                    if(StringUtils.equalsIgnoreCase(it, awardReportCode)) {
                        thisRowIsCCPromise = true
                    }
                }
                if(thisRowIsCCPromise) {
                    hasFeeWaiver = true
                } else {
                    hasFinancialAidAward = true
                }
            }
        }

        Float accountBalance = 0
        if(tuitionBalanceData != null && tuitionBalanceData.length > 0) {
            i = 0
            while(i + 2 <= tuitionBalanceData.length) {
                String person = tuitionBalanceData[i++]
                String acctBalance = tuitionBalanceData[i++]
                accountBalance = Float.valueOf(acctBalance)
            }
        }

        List<CohortTypeEnum> cohort_list = this.listCohorts(misCode, cccid, sisTermId)

        // Build term record for return
        def builder = new com.ccctc.adaptor.model.Student.Builder()
        builder.cccid(cccid)
               .sisPersonId(sisPersonId)
               .sisTermId(sisTermId)
               .orientationStatus(oStatus)
               .hasMathAssessment(hasMathAssessment)
               .hasEnglishAssessment(hasEnglishAssessment)
               .hasEducationPlan(hasEducationPlan)
               .dspsEligible(dspsEligible)
               .hasHold(hasEnrollmentHold)
               .isConcurrentlyEnrolled(isConcurrentlyEnrolled)
               .hasCaliforniaAddress(hasCaliforniaAddress)
               .visaType(visaType)
               .applicationStatus(applicationStatus)
               .residentStatus(residentStatus)
               .registrationDate(registrationDate)
               .isIncarcerated(isIncarcerated)
               .hasBogfw(hasFeeWaiver)
               .hasFinancialAidAward(hasFinancialAidAward)
               .accountBalance(accountBalance)
               .cohorts(cohort_list)

        com.ccctc.adaptor.model.Student result = builder.build()

        return result
    }

    List<Map> getAllByTerm(String misCode, List<String> sisTermIds, StudentFieldSet fieldSet) {

        log.debug("getAllByTerm: retrieving student data by terms")

        //****** Validate parameters ****
        if(fieldSet == null || fieldSet == StudentFieldSet.ALL) {
            String errMsg = "getAllByTerm: retrieving all fields not currently supported. use IDENTIFIERS or MATRICULATION"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!misCode) {
            String errMsg = "getAllByTerm: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if(sisTermIds) {
            sisTermIds.removeAll(Arrays.asList("", null));
        }

        if(!sisTermIds || sisTermIds.empty) {

            log.debug("getAllByTerm: no terms provided; attempting to find a default")
            api.peoplesoft.Term termAPI = new api.peoplesoft.Term(this.environment)
            List<com.ccctc.adaptor.model.Term> terms = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC))
            if (terms != null && terms.size() > 0) {
                sisTermIds = [terms[0].sisTermId]
            }
        }

        if(!sisTermIds || sisTermIds.empty) {
            String errMsg = "getAllByTerm: valid sisTermIds not provided and could not find default"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("getAllByTerm: params ok; attempting to get a new peoplesoft Session")

        Map inMemoryStudentDB
        if(fieldSet == StudentFieldSet.ALL) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.Student>()
        } else if (fieldSet == StudentFieldSet.IDENTIFIERS) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.students.StudentIdentityDTO>()
        } else if (fieldSet == StudentFieldSet.MATRICULATION) {
            inMemoryStudentDB = new HashMap<Tuple<String, String>, com.ccctc.adaptor.model.students.StudentMatriculationDTO>()
        }

        for(String sisTermId : sisTermIds) {
            String[] cccIdData
            String[] orientationData
            String[] educationPlanData
            String[] admissionsData

            //****** Create Connection to Peoplesoft ******
            def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
            try {

                //****** Build parameters to send to our custom Peopletools API ****
                log.debug("getAllByTerm: building params to retrieve a list of students with their external Id (cccId)")
                String packageName = "CCTC_DATASYNC_PKG"
                String className = "CCTC_Students"
                String methodName = "getStudents"

                String cleanTermId = PSParameter.ConvertStringToCleanString(sisTermId)
                String cleanInstitution = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.global.institution"))
                String cleanAcadCareer = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.global.acad_career"))
                String cleanExternalSystem = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.identity.cccId.external_system"))
                String[] args = [
                        cleanTermId,
                        cleanInstitution,
                        cleanAcadCareer,
                        cleanExternalSystem
                ]

                log.debug("getAllByTerm: getting list of students")
                cccIdData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

                if(fieldSet == StudentFieldSet.MATRICULATION || fieldSet == StudentFieldSet.ALL) {

                    log.debug("getAllByTerm: building params to retrieve Orientation data")
                    packageName = "CCTC_DATASYNC_PKG"
                    className = "CCTC_Students"
                    methodName = "getOrientationStatus"

                    args = [
                            cleanTermId,
                            cleanInstitution,
                            cleanAcadCareer
                    ]

                    log.debug("getAllByTerm: getting Orientation data")
                    orientationData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

                    log.debug("getAllByTerm: building params to retrieve Education Plan data")
                    packageName = "CCTC_DATASYNC_PKG"
                    className = "CCTC_Students"
                    methodName = "getEducationPlan"

                    args = [
                            cleanTermId,
                            cleanInstitution,
                            cleanAcadCareer
                    ]

                    log.debug("getAllByTerm: getting Education Plan data")
                    educationPlanData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)


                    log.debug("getAllByTerm: building params to retrieve Admissions data")
                    packageName = "CCTC_DATASYNC_PKG"
                    className = "CCTC_Students"
                    methodName = "getAdmissionStatus"

                    args = [
                            cleanTermId,
                            cleanInstitution,
                            cleanAcadCareer
                    ]

                    log.debug("getAllByTerm: getting Admissions data")
                    admissionsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                }
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs != null && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("getAllByTerm: retrieval failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }
            finally {
                log.debug("getAllByTerm: disconnecting")
                if (peoplesoftSession != null) {
                        peoplesoftSession.disconnect()
                }
            }

            log.debug("getAllByTerm: all data retrieved. parsing...")

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")

            Integer i = 0
            log.info("getAllByTerm: parsing {} cccId records", cccIdData ? (cccIdData.length / 2) : 0)
            if(cccIdData != null && cccIdData.length > 0) {
                i = 0
                while (i + 2 <= cccIdData.length) {

                    String sisPersonId = cccIdData[i++]
                    String externalId = cccIdData[i++]

                    Tuple key = new Tuple(sisTermId, sisPersonId)
                    def record = inMemoryStudentDB.get(key)
                    if (!record) {
                        record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                    }

                    record.setCccid(externalId)

                    inMemoryStudentDB.put(key, record)
                }
            }

            if(fieldSet == StudentFieldSet.MATRICULATION || fieldSet == StudentFieldSet.ALL) {
                log.info("getAllByTerm: parsing {} orientation records", orientationData ? (orientationData.length / 3) : 0)
                if (orientationData != null && orientationData.length > 0) {
                    i = 0
                    while (i + 3 <= orientationData.length) {
                        OrientationStatus oStatus = OrientationStatus.UNKNOWN
                        String sisPersonId = orientationData[i++]
                        String orien_svcs = orientationData[i++]
                        String orien_exmp = orientationData[i++]

                        oStatus = this.parseOrientationServices(misCode, orien_svcs)
                        if (oStatus == OrientationStatus.UNKNOWN ||
                                oStatus == OrientationStatus.REQUIRED) {
                            Boolean exempt = this.isOrientationExempt(misCode, orien_exmp)
                            if (exempt) {
                                oStatus = this.parseOrientationExemption(misCode, orien_exmp)
                            }
                        }

                        Tuple key = new Tuple(sisTermId, sisPersonId)
                        def record = inMemoryStudentDB.get(key)
                        if (!record) {
                            record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                            log.warn("getAllByTerm: found orientationStatus record for student not already in list")
                        }

                        record.setOrientationStatus(oStatus)
                        inMemoryStudentDB.put(key, record)
                    }
                }

                log.info("getAllByTerm: parsing {} educationPlanData records", educationPlanData ? (educationPlanData.length / 2) : 0)
                if (educationPlanData != null && educationPlanData.length > 0) {
                    i = 0
                    while (i + 2 <= educationPlanData.length) {
                        Boolean hasEducationPlan = false

                        String sisPersonId = educationPlanData[i++]
                        String educationPlan = educationPlanData[i++]
                        if (StringUtils.isNotBlank(educationPlan)) {
                            hasEducationPlan = true
                        }

                        if(hasEducationPlan) {
                            Tuple key = new Tuple(sisTermId, sisPersonId)
                            def record = inMemoryStudentDB.get(key)
                            if (!record) {
                                record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                                log.warn("getAllByTerm: found educationPlan record for student not already in list")
                            }

                            record.setHasEducationPlan(hasEducationPlan)
                            inMemoryStudentDB.put(key, record)
                        }
                    }
                }

                log.info("getAllByTerm: parsing {} admissionsData records", admissionsData ? (admissionsData.length / 2) : 0)
                if (admissionsData != null && admissionsData.length > 0) {
                    i = 0
                    while (i + 2 <= admissionsData.length) {

                        String sisPersonId = admissionsData[i++]
                        ApplicationStatus applicationStatus = this.parseApplicationStatus(misCode, admissionsData[i++])

                        Tuple key = new Tuple(sisTermId, sisPersonId)
                        def record = inMemoryStudentDB.get(key)
                        if (!record) {
                            record = this.getDefaultStudentDTO(sisTermId, sisPersonId, fieldSet)
                            log.warn("getAllByTerm: found ApplicationStatus record for student not already in list")
                        }

                        record.setApplicationStatus(applicationStatus)

                        inMemoryStudentDB.put(key, record)
                    }
                }
            }
        }

        log.info("getAllByTerm: returning {} records", inMemoryStudentDB.size())

        List<java.util.Map> filterFieldResults = []
        for (def s : inMemoryStudentDB.values()) {
            HashMap fieldMap = mapper.convertValue(s, HashMap.class)
            filterFieldResults.add(fieldMap)
        }
        return filterFieldResults
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
     * Sets the student details for the given cccid for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report student information on (optional; if blank, tries to find 'current' term, or closest next term)
     * @Note when performing updates to multiple fields, each update is separately executed. Function bails out as soon as any one error
     * occurs. Its possible that one update succeeds and then a second one fails; the first would stick, but an exception would be thrown
     */
     void patch(String misCode, String cccId, sisTermId, com.ccctc.adaptor.model.Student updates) {
         log.debug("patch: updating student data")

         //****** Validate parameters ****
         if (!misCode) {
             String errMsg = "patch: misCode cannot be null or blank"
             log.error(errMsg)
             throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
         }

         if(!updates) {
             String errMsg = "patch: Student updates cannot be null or blank"
             log.error(errMsg)
             throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
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
             throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
         } else {
             cccId = cccId.toUpperCase()
         }

         api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
         String sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccId)
         if(!sisPersonId) {
             String errMsg = "patch: Student mapping returned no results: student not found"
             log.error(errMsg)
             throw new EntityNotFoundException(errMsg)
         }

         if(!sisTermId) sisTermId = updates.sisTermId;
         if(!sisTermId) {
             log.debug("patch: term code not provided; attempting to find a default")
             api.peoplesoft.Term termAPI = new api.peoplesoft.Term(this.environment)
             List<com.ccctc.adaptor.model.Term> terms = termAPI.getTermsByDate(misCode, LocalDate.now(ZoneOffset.UTC))
             if(terms != null && terms.size() > 0) {
                 sisTermId = terms[0].sisTermId
             }
         }

         if (!sisTermId) {
             String errMsg = "patch: sisTermId not provided and could not be defaulted"
             log.error(errMsg)
             throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
         }

         log.debug("patch: params ok; checking for any updates")

         if(updates.orientationStatus != null) {
             log.debug("patch: orientation status value detected; attempting to set it")
             if(updates.orientationStatus == OrientationStatus.UNKNOWN) {
                 String errMsg = "patch: Setting Orientation Status to UNKNOWN is unsupported"
                 log.error(errMsg)
                 throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, errMsg)
             }
             log.debug("patch: attempting to get a new peoplesoft Session to update orientation Status")
             //****** Create Connection to Peoplesoft and call remote method ******
             def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
             try {

                 //****** Build parameters to send to our custom Peopletools API ****
                 log.debug("patch: building params to retrieve Orientation data")
                 String packageName = "CCTC_IDENTITY_PKG"
                 String className = "CCTC_Student"
                 String methodName = "getOrientationStatus"


                 String cleanPersonId = PSParameter.ConvertStringToCleanString(sisPersonId)
                 String cleanTermId = PSParameter.ConvertStringToCleanString(sisTermId)
                 String cleanInstitution = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.global.institution"))
                 String cleanAcadCareer = PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.global.acad_career"))
                 String[] args = [
                         cleanPersonId,
                         cleanTermId,
                         cleanInstitution,
                         cleanAcadCareer
                 ]
                 log.debug("patch: getting Orientation data to confirm exists/not exempt")
                 String[] orientationData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

                 if(orientationData == null || orientationData.length <= 0) {
                     String errMsg = "patch: Orientation record must exist before trying to update"
                     log.error(errMsg)
                     throw new EntityNotFoundException(errMsg)
                 }

                 packageName = "CCTC_MATRIC_PKG"
                 className = "CCTC_StudentMatriculation"
                 methodName = "setOrientationStatus"
                 String statusToSet = this.parseOrientationStatus(misCode, updates.orientationStatus)
                 if(StringUtils.isEmpty(statusToSet)) {
                     String errMsg = "patch: No mapping for the given orientation status found"
                     log.error(errMsg)
                     throw new InternalServerException(InternalServerException.Errors.internalServerError, errMsg)
                 }
                 args = [
                         cleanPersonId,
                         cleanTermId,
                         cleanInstitution,
                         cleanAcadCareer,
                         statusToSet
                 ]

                 log.debug("patch: setting Orientation Status")
                 String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                 if (rowsAffected == null || rowsAffected.length != 1) {
                     String errMsg = "patch: invalid number of rows affected. empty array"
                     log.error(errMsg)
                     throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
                 }
                 if (!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                     String errMsg = "patch: invalid number of rows affected [" + rowsAffected[0] + "]"
                     log.error(errMsg)
                     throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
                 }
                 log.debug("patch: Orientation Status field was updated")

                 String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
                 if (StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                     try {
                         log.debug("patch: triggering the remote peoplesoft event onStudentOrientationUpdated")

                         api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onStudentOrientationUpdated", "<", args)
                     }
                     catch (Exception exc) {
                         log.warn("patch: Triggering the onStudentOrientationUpdated event failed with message [" + exc.getMessage() + "]")
                     }
                 }
             }
             catch(psft.pt8.joa.JOAException e) {
                 String messageToThrow = e.getMessage()
                 if(peoplesoftSession != null) {
                     String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                     if(msgs != null && msgs.length > 0) {
                         messageToThrow = msgs[0]
                         msgs.each {
                             log.error("patch: update failed: [" + it + "]")
                         }
                     }
                 }
                 throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
             }
             finally {
                 log.debug("patch: disconnecting from orientation session")
                 if(peoplesoftSession != null) {
                     peoplesoftSession.disconnect()
                 }
             }
         }

         log.debug("patch: all updates applied")
     }


    /**
     * Associates the given Cohort with the given student for the given term
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisTermId the internal/college-specific Term code to report cohort information on (required)
     */
    List<CohortTypeEnum> listCohorts(String misCode, String cccid, String sisTermId) {

        log.debug("listCohorts: retrieving student cohort data")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "listCohorts: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!cccid) {
            String errMsg = "listCohorts: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cccid = cccid.toUpperCase()
        }

        if (!sisTermId) {
            String errMsg = "listCohorts: sisTermId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("listCohorts: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COHORT_PKG"
        String className = "CCTC_StudentCohort"
        String methodName = "getList"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId)
        ]

        List<CohortTypeEnum> results
        String[] cohortData

        log.debug("listCohorts: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("listCohorts: calling the remote peoplesoft method")
            cohortData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs != null && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("listCohorts: retrieval failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("listCohorts: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        if(cohortData != null) {
            Integer i = 0
            results = []
            while(i + 8 <= cohortData.length) {
                String cur_cccId = cohortData[i++]
                String cur_term = cohortData[i++]
                String cur_cohort_code = cohortData[i++]
                String cur_status_flag = cohortData[i++]
                String cur_status_dttm = cohortData[i++]
                String cur_notes = cohortData[i++]
                String cur_update_opr = cohortData[i++]
                String cur_update_dt = cohortData[i++]
                CohortTypeEnum cohort_type = this.parseCohortType(misCode, cur_cohort_code)
                if(cohort_type) {
                    results.add(cohort_type)
                }
            }
        }

        log.debug("listCohorts: done")
        return results
    }

    /**
     * Associates the given Cohort with the given student for the given term
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param cohortName the cohort/student-group to associate the student to for the given term (required)
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisTermId the internal/college-specific Term code to report cohort information on (required)
     */
    void postCohort(String cccid, CohortTypeEnum cohortName, String misCode, String sisTermId) {

        log.debug("postCohort: inserting student cohort data")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "postCohort: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!cccid) {
            String errMsg = "postCohort: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cccid = cccid.toUpperCase()
        }

        if (!sisTermId) {
            String errMsg = "postCohort: sisTermId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        String cohortCode
        if(!cohortName) {
            String errMsg = "postCohort: Cohort cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cohortCode = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.cohort_code.cohortTypeEnum." + cohortName.name())
        }

        if(!cohortCode) {
            String errMsg = "postCohort: Cohort could not be convert to a code via configs"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("postCohort: params ok; building method params")
        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COHORT_PKG"
        String className = "CCTC_StudentCohort"
        String methodName = "getCohort"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(cohortCode)
        ]

        log.debug("postCohort: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            String[] existCheck = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(existCheck != null && existCheck.length > 0) {
                String errMsg = "postCohort: Record already exists"
                log.error(errMsg)
                throw new EntityConflictException(errMsg)
            }

            methodName = "setCohort"

            log.debug("postCohort: calling the remote peoplesoft method")
            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected == null || rowsAffected.length != 1) {
                String errMsg = "postCohort: invalid number of rows affected. empty array"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                String errMsg = "postCohort: invalid number of rows affected [" + rowsAffected[0] + "]"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled,"true")) {
                try {
                    log.debug("postCohort: triggering the remote peoplesoft event onStudentCohortInserted")

                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onStudentCohortInserted", "<", args)
                }
                catch (Exception exc) {
                    log.warn("postCohort: Triggering the onStudentCohortInserted event failed with message [" + exc.getMessage() + "]")
                }
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs != null && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("postCohort: insert failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("postCohort: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("postCohort: done")
    }

    /**
     * Un-associates the given Cohort for the given student for the given term
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param cohortName the cohort/student-group to associate the student to for the given term (required)
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisTermId the internal/college-specific Term code to report cohort information on (required)
     */
    void deleteCohort(String cccid, CohortTypeEnum cohortName, String misCode, String sisTermId) {

        log.debug("deleteCohort: removing student cohort data")

        //****** Validate parameters ****
        if (!misCode) {
            String errMsg = "deleteCohort: misCode cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        if (!cccid) {
            String errMsg = "deleteCohort: cccId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cccid = cccid.toUpperCase()
        }

        if (!sisTermId) {
            String errMsg = "deleteCohort: sisTermId cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        String cohortCode
        if(!cohortName) {
            String errMsg = "deleteCohort: Cohort cannot be null or blank"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        } else {
            cohortCode = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.cohort_code.cohortTypeEnum." + cohortName.name())
        }

        if(!cohortCode) {
            String errMsg = "deleteCohort: Cohort could not be convert to a code via configs"
            log.error(errMsg)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
        }

        log.debug("deleteCohort: params ok; building method params")

        //****** Build parameters to send to our custom Peopletools API ****
        String packageName = "CCTC_COHORT_PKG"
        String className = "CCTC_StudentCohort"
        String methodName = "getCohort"

        String[] args = [
                PSParameter.ConvertStringToCleanString(cccid),
                PSParameter.ConvertStringToCleanString(sisTermId),
                PSParameter.ConvertStringToCleanString(cohortCode)
        ]

        log.debug("deleteCohort: attempting to get a new peoplesoft Session")
        //****** Create Connection to Peoplesoft and call remote method ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {
            log.debug("deleteCohort: calling the remote peoplesoft method")

            String[] existCheck = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)

            if(existCheck != null && existCheck.length <= 0) {
                String errMsg = "deleteCohort: Record not found"
                log.error(errMsg)
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, errMsg)
            }

            methodName = "removeCohort"


            String[] rowsAffected = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            if(rowsAffected == null || rowsAffected.length != 1) {
                String errMsg = "deleteCohort: invalid number of rows affected. empty array"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
            }
            if(!StringUtils.equalsIgnoreCase(rowsAffected[0], "1")) {
                String errMsg = "deleteCohort: invalid number of rows affected [" + rowsAffected[0] + "]"
                log.error(errMsg)
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, errMsg)
            }

            String dataEventsEnabled = MisEnvironment.getProperty(environment, misCode, "peoplesoft.dataEvents.enabled")
            if(StringUtils.equalsIgnoreCase(dataEventsEnabled, "true")) {
                try {
                    log.debug("deleteCohort: triggering the remote peoplesoft event onStudentCohortRemoved")

                    api.peoplesoft.PSConnection.triggerEvent(peoplesoftSession, "onStudentCohortRemoved", "<", args)
                }
                catch (Exception exc) {
                    log.warn("deleteCohort: Triggering the onStudentCohortRemoved event failed with message [" + exc.getMessage() + "]")
                }
            }
        }
        catch(psft.pt8.joa.JOAException e) {
            String messageToThrow = e.getMessage()
            if(peoplesoftSession != null) {
                String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                if(msgs != null && msgs.length > 0) {
                    messageToThrow = msgs[0]
                    msgs.each {
                        log.error("deleteCohort: removal failed: [" + it + "]")
                    }
                }
            }
            throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
        }
        finally {
            log.debug("deleteCohort: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        log.debug("deleteCohort: done")
        return
    }


    /**
     * Gets the CCC Id for the given student
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param sisPersonId The internal/College-specific person Id to limit results to. (required)
     * @return an array containing the one CCC ID for the given student if found
     */
    List<String> getStudentCCCIds(String misCode, String sisPersonId) {
        String cccId = new api.peoplesoft.Identity(environment).translateSisPersonIdToCCCId(misCode, sisPersonId)
        if(cccId) {
            // return array with single element
            return [ cccId ]
        }
        // no cccId found; return empty array
        return [  ]
    }

    /**
     * Associates the CCC Id with the given student
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param cccId The California Community College person Id to limit results to. (required)
     * @param sisPersonId The internal/College-specific person Id to associated the cccId with. (required)
     * @return an array containing the one CCC ID for the given student if found
     */
    List<String> postStudentCCCId(String misCode, String sisPersonId, String cccid) {
        new api.peoplesoft.Identity(environment).assignCCCIDToSisPersonId(misCode, sisPersonId, cccid)
        return [ cccid ]
    }

    /**
     * Translates/Parses the cohort_code into a CohortTypeEnum value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param cohort_code The code stored in the staging table
     * @return A CohortTypeEnum enum value that maps to the cohort_code provided
     */
    protected CohortTypeEnum parseCohortType(String misCode, String cohort_code) {
        CohortTypeEnum result
        if(cohort_code) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.cohortTypeEnum.cohort_code." + cohort_code)
            if (configMapping) {
                CohortTypeEnum found = CohortTypeEnum.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse cohort_code [" + cohort_code + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("cohort_code [" + cohort_code + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the sc_matr_orien_exmp into a Boolean value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param sc_matr_orien_exmp The code stored in the Peoplesoft table
     * @return A Boolean value that maps to the sc_matr_orien_exmp provided
     */
    protected Boolean isOrientationExempt(String misCode, String sc_matr_orien_exmp) {
        Boolean exempt = false
        if(sc_matr_orien_exmp) {
            String exemptedConfig = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.orientationStatus.exempt.sc_matr_orien_exmp")
            if(exemptedConfig) {
                String[] exemptedCodes = exemptedConfig.split(",")
                if(exemptedCodes != null && exemptedCodes.contains(sc_matr_orien_exmp)) {
                    exempt = true
                }
            }
        }
        return exempt
    }

    /**
     * Translates/Parses the sc_matr_orien_exmp into an OrientationStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param sc_matr_orien_exmp The code stored in the Peoplesoft table
     * @return An OrientationStatus value that maps to the sc_matr_orien_exmp provided
     */
    protected OrientationStatus parseOrientationExemption(String misCode, String sc_matr_orien_exmp) {
        OrientationStatus result = OrientationStatus.UNKNOWN
        if (sc_matr_orien_exmp) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.orientationStatus.sc_matr_orien_exmp." + sc_matr_orien_exmp)
            if (configMapping) {
                OrientationStatus found = OrientationStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse sc_matr_orien_exmp [" + sc_matr_orien_exmp + "] mapping")
                } else {
                    result = found
                }
            } else {
                log.warn("sc_matr_orien_exmp [" + sc_matr_orien_exmp + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the sc_matr_orien_svcs into an OrientationStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param sc_matr_orien_svcs The code stored in the Peoplesoft table
     * @return An OrientationStatus value that maps to the sc_matr_orien_svcs provided
     */
    protected OrientationStatus parseOrientationServices(String misCode, String sc_matr_orien_svcs) {
        OrientationStatus result = OrientationStatus.UNKNOWN
        if (sc_matr_orien_svcs) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.orientationStatus.sc_matr_orien_svcs." + sc_matr_orien_svcs)
            if (configMapping) {
                OrientationStatus found = OrientationStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse sc_matr_orien_svcs [" + sc_matr_orien_svcs + "] mapping")
                } else {
                    result = found
                }
            } else {
                log.warn("sc_matr_orien_svcs [" + sc_matr_orien_svcs + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses an orientation status into a sc_matr_orien_svcs value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param oreintationStatus (potentially from MyPath) that indicates whether student has completed all orientation requirements
     * @return A sc_matr_orien_svcs value that maps to the status provided
     */
    protected String parseOrientationStatus(String misCode, OrientationStatus oStatus) {
        String result
        if(oStatus) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.sc_matr_orien_svcs.orientationStatus." + oStatus)
            if (configMapping) {
                result = configMapping
            } else {
                log.warn("orientation status [" + oStatus + "] to internal code mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the prog_status into a ApplicationStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param prog_status The peoplesoft specific code
     * @return A ApplicationStatus enum value that maps to the prog_status provided
     */
    protected ApplicationStatus parseApplicationStatus(String misCode, String prog_status) {
        ApplicationStatus result = ApplicationStatus.NoApplication
        if(prog_status) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.applicationStatus.prog_status." + prog_status)
            if (configMapping) {
                ApplicationStatus found = ApplicationStatus.values().find {
                    return it.name().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse prog_status [" + prog_status + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("prog_status [" + prog_status + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Translates/Parses the residency and admission_excpt fields into a ResidentStatus value using the mapping config values
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param residency The peoplesoft specific code
     * @param admission_excpt The peoplesoft specific code
     * @return A ResidentStatus enum value that maps to the residency / admission_excpt fields provided
     */
    protected ResidentStatus parseResidentStatus(String misCode, String residency, String admission_excpt) {
        ResidentStatus result
        // first, see if a default is configured
        String defaultStatus = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.residentStatus.default")
        if(defaultStatus) {
            result = ResidentStatus.values().find {
                return it.name().equalsIgnoreCase(defaultStatus)
            }
        }

        // then, check admission_excpt for AB540 status
        Boolean valueSet = false
        if(admission_excpt) {
            String ab540Code = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.residentStatus.ab540.admission_excpt")
            if(ab540Code) {
                if (StringUtils.equalsIgnoreCase(admission_excpt, ab540Code)) {
                    result = ResidentStatus.AB540
                    valueSet = true
                }
            } else {
                log.warn("resident status config value for ab540 not found")
            }
        }

        // then, if not set above, do regular mapping
        if(!valueSet) {
            if (residency) {
                String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.student.mappings.residentStatus.residency." + residency)
                if (configMapping) {
                    ResidentStatus found = ResidentStatus.values().find {
                        return it.name().equalsIgnoreCase(configMapping)
                    }
                    if (!found) {
                        log.warn("Could not parse residency [" + residency + "]")
                    } else {
                        result = found
                    }
                } else {
                    log.warn("residency [" + residency + "] mapping not found")
                }
            }
        }
        return result
    }
}