package api.peoplesoft

import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.model.Email
import com.ccctc.adaptor.model.transcript.*
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.MisEnvironment
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.adaptor.util.transcript.CollegeTranscriptBuilder
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript
import com.ccctc.core.coremain.v1_14.*
import com.ccctc.sector.academicrecord.v1_9.*
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

import java.text.SimpleDateFormat

import com.xap.ccctran.*
import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl

/**
 * <h1>Student Transcript Peoplesoft Interface</h1>
 * <summary>
 *     <p>Provides a Wrapper around the native peoplesoft java API for:</p>
 *     <ol>
 *         <li>Retrieving the student's current Academic Transcript of courses from the legit peoplesoft tables</li>
 *     <ol>
 *     <p>Most logic for College Adaptor's Peoplesoft support is found within each Peoplesoft instance itself as
 *        this renders the highest level of transparency to each college for code-reviews and deployment authorization.
 *        See App Designer/PeopleTools projects latest API info
 *     </p>
 * </summary>
 * <reference>
 *     <p>See https://cccnext.jira.com/wiki/spaces/CE/overview for more details about Course Exchange (aka EdExchange)</p>
 * </reference>
 *
 * @version 3.3.0
 *
 */
class Transcript {

    protected final static Logger log = LoggerFactory.getLogger(Transcript.class)

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    /**
     * Default Constructor needed since we also specify an alternative Constructor
     */
    Transcript(){
        // do nothing, except be available
    }

    /**
     * Alternative Constructor to set the environment during initialization.
     * Useful for when other groovy PS APIs need to use these Functions that depend on environment
     */
    Transcript(Environment e) {
        this.environment = e
    }

    /**
     * Unsupported Insert/Set Function to add Transcript information to the underlying peoplesoft tables
     * @param miscode The College Code used for grabbing college specific settings from the environment. (unused)
     * @param transcript The data to insert into peoplesoft (unused)
     * @returns void; nothing.
     * @throws InternalServerException, as this functionality is unsupported for Peoplesoft
     */
    void post(String miscode, CollegeTranscript transcript) {
        throw new InternalServerException("Unsupported")
    }

    /**
     * Get: College Transcript for given person. Limited to the acad_career and institution set in config.
     * Provides a listing and summary for the courses attempted by the student and the resultant grades.
     * Also includes degrees, awards, and student bio-demographics.
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param firstName the primary name for the student (semi-optional)
     * @param lastName the surname for the student (semi-optional)
     * @param birthDate the Date in which student was born (semi-optional)
     * @param ssn the full social security number (or taxpayed id number) for the student (unused)
     * @param partialSSN The last 4 digits of the student's ssn/taxpayer id number (unused)
     * @param emailAddress The electronic address for sending electronic mail of student (unused)
     * @param sisPersonId the internal Peoplesoft specific identifier for student (semi-optional)
     * @param cccId California Community Colleges unique Identifier for student (semi-optional)
     * @param transcriptService Service that provides the JAXBContext (required)
     * @Note: If sisPersonId is not provided, falls back to the cccId and converts it to sisPersonId.
     * if sisPersonId and cccId both not provided (or cccId fails to convert to sisPersonId),
     * falls back to name/dob and converts them to sisPersonId.
     * if after conversion done, still no sisPersonId, errors out.
     * @returns Full College Transcript for the given Student at the institution
     */
    CollegeTranscript get(String misCode,
                          String firstName,
                          String lastName,
                          String birthDate,
                          String ssn,
                          String partialSSN,
                          String emailAddress,
                          String sisPersonId,
                          String cccId,
                          TranscriptService transcriptService) {

        log.debug("get: retrieving transcript. validating parameters")

        //****** Validate parameters ****
        if (!misCode) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "misCode cannot be null or blank")
        }

        if(!sisPersonId) {
            // schoolAssignedStudentID not provided.
            // first see if we can translate from cccId if provided
            if(cccId) {
                api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
                sisPersonId = identityApi.translateCCCIdToSisPersonId(misCode, cccId)
            }
            // second, if not able to translate from cccId, then try by name and birthdate
            if(!sisPersonId){
                if(firstName && lastName && birthDate) {
                    api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(this.environment)
                    sisPersonId = identityApi.translateNameDobToSisPersonId(misCode, firstName, lastName, birthDate)
                }
            }
        }

        if(!sisPersonId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "No sisPersonId provided and could not find the student with the provided parameters.")
        }

        if(!transcriptService) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "transcript service may not be null")
        }

        log.debug("get: params ok; establishing PS Connection")
        CollegeTranscriptBuilder builder = new CollegeTranscriptBuilder(transcriptService.getCccJAXBContext())
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")

        //****** Create Connection to Peoplesoft and call remote methods ******
        def peoplesoftSession = api.peoplesoft.PSConnection.getSession(environment, misCode)
        try {

            /******** Build Person Record *********/
            log.debug("get: connection ok; retrieving person bio")
            String[] personBioData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_IDENTITY_PKG"
                String className = "CCTC_Person"
                String methodName = "getPersonBio"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId)
                ]
                log.debug("get: calling the remote peoplesoft method to get Person Bio data")

                personBioData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve person bio failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            if (!personBioData || personBioData.length < 10) {
                log.debug("get: got person bio data back, but it was either empty or too short")
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Could not find the person with the provided parameters.")
            }

            log.debug("get: got person bio. translating to object")
            Integer i = 0
            String emplid = personBioData[i++]
            builder.setStudentPartialSSN(personBioData[i++])
            builder.setStudentFirstname(personBioData[i++])
            builder.setStudentLastName(personBioData[i++])
            builder.setStudentMiddleName(personBioData[i++])
            String suffix = personBioData[i++]
            if (suffix) {
                builder.setStudentNameSuffix(suffix)
            }
            builder.setStudentGender(builder.createGenderCodeType(personBioData[i++]))
            builder.setStudentBirthDate(df.parse(personBioData[i++]))
            String birthCity = personBioData[i++]
            if(birthCity) {
                builder.setStudentBirthCity(birthCity)
            }
            String birthState = personBioData[i++]
            if (birthState) {
                builder.setStudentBirthStateProvince(StateProvinceCodeType.fromValue(birthState))
            }


            log.debug("get: built bio ok; retrieving person address")
            String[] personAddressData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_IDENTITY_PKG"
                String className = "CCTC_Person"
                String methodName = "getPersonAddress"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId)
                ]
                log.debug("get: calling the remote peoplesoft method to get person address data")

                personAddressData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve person address failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            if(personAddressData && personAddressData.length >= 8) {
                // if address data found, fill it in. if no address data found, that's ok- no error
                String home_address_type = MisEnvironment.getProperty(environment, misCode,"peoplesoft.person.homeAddress.address_type")
                if(home_address_type) {
                    i = 0
                    while (i + 8 <= personAddressData.length) {
                        emplid = personAddressData[i++]
                        String cur_address_type = personAddressData[i++]
                        String addressLine1 = personAddressData[i++]
                        String addressLine2 = personAddressData[i++]
                        String addressLine3 = personAddressData[i++]
                        String city = personAddressData[i++]
                        String state = personAddressData[i++]
                        String zip = personAddressData[i++]

                        if(StringUtils.equalsIgnoreCase(cur_address_type, home_address_type)) {

                            if (addressLine1) {
                                builder.getStudentAddressLines().add(addressLine1)
                            }

                            if (addressLine2) {
                                builder.getStudentAddressLines().add(addressLine2)
                            }

                            if (addressLine3) {
                                builder.getStudentAddressLines().add(addressLine3)
                            }

                            builder.setStudentCity(city)
                            builder.setStudentResidentStateOrProvince(state)
                            builder.setStudentStateOrProvince(state)
                            builder.setStudentZipCode(zip)
                        }
                    }
                }
            }

            log.debug("get: built address ok; retrieving student high school")
            String[] studentHighSchoolData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getExternalCareer"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.student.highSchool.ext_career"))//"HS"
                ]
                log.debug("get: calling the remote peoplesoft method to get student high school data")

                studentHighSchoolData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve student high school failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            if(studentHighSchoolData && studentHighSchoolData.length >= 3) {
                // if high school data found, fill it in. if no high school data found, that's ok- no error
                i = 0
                emplid = studentHighSchoolData[i++]
                String highSchoolName = studentHighSchoolData[i++]
                String hsConferDt = studentHighSchoolData[i++]
                if(highSchoolName) {
                    builder.setHighSchoolName(highSchoolName)
                }
                if(hsConferDt) {
                    builder.setHighSchoolGraduationDate(hsConferDt)
                }
            }

            /******** Build Academic Record *********/
            AcademicRecordType academicRecord = builder.createAcademicRecord()

            log.debug("get: built high school ok; retrieving institution details")
            String[] currentInstitutionData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Institution"
                String methodName = "getInstitutionDetails"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution"))
                ]
                log.debug("get: calling the remote peoplesoft method to get institution data")

                currentInstitutionData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve institution data failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            if (!currentInstitutionData || currentInstitutionData.length < 3) {
                log.debug("get: got institution data back, but it was either empty or too short")
                throw new InternalServerException(InternalServerException.Errors.sisQueryError, "Could not find the configured institution.")
            } else {
                log.debug("get: got institution data. translating to object")
                i = 0
                String institution_code = currentInstitutionData[i++]
                com.ccctc.sector.academicrecord.v1_9.SchoolType schoolType = builder.createSchool()
                schoolType.setOrganizationName(currentInstitutionData[i++])
                schoolType.setFICE(currentInstitutionData[i++])
                academicRecord.setSchool(schoolType)
            }

            log.debug("get: built institution data ok; retrieving academic degrees")
            String[] studentDegreeData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getAcademicDegrees"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get academic degrees data")

                studentDegreeData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve academic degrees failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            log.debug("get: got academic degree data; retrieving degree plans")
            String[] studentPlansData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getDegreePlans"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get degree plans data")

                studentPlansData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve degree plans failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            log.debug("get: got degree plans data; retrieving degree honors")
            String[] degreeHonorsData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getDegreeHonors"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get degree honors data")

                degreeHonorsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve degree honors failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            log.debug("get: retrieved degrees, degree honors, and degree plans data; building awards")

            if (studentDegreeData) {
                // if degree data found, fill it in. if no degree data found, that's ok
                i = 0
                while (i + 4 <= studentDegreeData.length) {
                    emplid = studentDegreeData[i++]
                    String degreeNumber = studentDegreeData[i++]
                    String degreeLevel = this.parseDegree(misCode, studentDegreeData[i++])
                    if(degreeLevel) {
                        String degreeTitle = studentDegreeData[i++]
                        Date conferDate = df.parse(studentDegreeData[i++])
                        Date degreeDate = df.parse(studentDegreeData[i++])

                        AcademicAwardType award = builder.createAcademicAward(degreeTitle, degreeLevel, conferDate, conferDate != degreeDate, degreeDate)
                        if(studentPlansData) {
                            Integer k = 0
                            while(k + 5 <= studentPlansData.length) {
                                emplid = studentPlansData[k++]
                                String plan_degreeNumber = studentPlansData[k++]
                                String programName = studentPlansData[k++]
                                String programInternalCode = studentPlansData[k++]
                                String programCipCode = studentPlansData[k++]

                                if(plan_degreeNumber == degreeNumber) {
                                    if (programCipCode) {
                                        programCipCode = programCipCode.replaceAll("[^0-9]", "")
                                        if (programCipCode.length() == 6) {
                                            programCipCode = programCipCode[0..1] + "." + programCipCode[2..5]
                                        }
                                    }
                                    AcademicProgramTypeType programType = (AcademicProgramTypeType) null // Value is blank per specs for community colleges
                                    AcademicProgramType program = builder.createAcademicProgram(programName, programType, programCipCode, AcademicProgramCodes.ProgramCIPCode)
                                    if (programInternalCode) {
                                        program.getNoteMessages().add(programInternalCode)
                                    }
                                    award.getAcademicAwardPrograms().add(program)
                                }
                            }
                        }

                        if(degreeHonorsData) {
                            Integer k = 0
                            while(k + 4 <= degreeHonorsData.length) {
                                emplid = degreeHonorsData[k++]
                                String honor_degreeNumber = degreeHonorsData[k++]
                                String honorCode = degreeHonorsData[k++]
                                String honorTitle = degreeHonorsData[k++]

                                if(honor_degreeNumber == degreeNumber) {
                                    HonorsRecognitionLevelType honorLevel = this.parseHonorsCode(misCode, honorCode)
                                    AcademicHonorsType honor = builder.constructHonors(honorLevel, honorTitle)
                                    award.getAcademicHonors().add(honor)
                                }
                            }
                        }

                        academicRecord.getAcademicAwards().add(award)
                    }
                }
            }


            // TODO: add additional achievements (proficiencies) to academic Record

            log.debug("get: built awards; retrieving registered sessions data")
            String[] academicSummaryData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getAcademicSummary"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get academic summary data")

                academicSummaryData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve academic summary failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            if(academicSummaryData) {
                i = 0
                while(i + 7 <= academicSummaryData.length) {
                    emplid = academicSummaryData[i++]
                    AcademicSummaryFType academicSummary = builder.createAcademicSummaryF()

                    Double creditHoursAttempted = Double.parseDouble(academicSummaryData[i++])
                    Double creditHoursEarned = Double.parseDouble(academicSummaryData[i++])
                    Double totalQualityPoints = Double.parseDouble(academicSummaryData[i++])
                    Double creditHoursForGPA = Double.parseDouble(academicSummaryData[i++])
                    Double cumulativeGPA = Double.parseDouble(academicSummaryData[i++])

                    GPAType gpa = builder.createGPA()
                    gpa.setCreditHoursAttempted(BigDecimal.valueOf(creditHoursAttempted))
                    gpa.setCreditHoursEarned(BigDecimal.valueOf(creditHoursEarned))
                    gpa.setCreditUnit(AcademicGPACreditUnits.Units.toString())
                    gpa.setGradePointAverage(BigDecimal.valueOf(cumulativeGPA))
                    gpa.setTotalQualityPoints(BigDecimal.valueOf(totalQualityPoints))
                    gpa.setCreditHoursforGPA(BigDecimal.valueOf(creditHoursForGPA))
                    gpa.setGPARangeMaximum(BigDecimal.valueOf(4.0D))
                    gpa.setGPARangeMinimum(BigDecimal.valueOf(0D))
                    academicSummary.setGPA(gpa)

                    academicSummary.setAcademicSummaryType(AcademicSummaryTypeType.ALL)
                    academicSummary.setAcademicSummaryLevel(CourseCreditLevelType.UNDERGRADUATE)

                    academicRecord.getAcademicSummaries().add(academicSummary)

                    String academicLevel = academicSummaryData[i++]
                    StudentLevelCodeType studentLevelCode = this.parseAcadLevel(misCode, academicLevel)
                    if(studentLevelCode) {
                        StudentLevelType studentLevel = builder.constructStudentLevelType(studentLevelCode, "")
                        academicRecord.setStudentLevel(studentLevel)
                    }
                }
            }


            log.debug("get: built academic Summary; retrieving registered terms data")
            String[] studentTermData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getTermSummaries"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get registered terms data")

                studentTermData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve registered terms failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            log.debug("get: got registered terms data; retrieving term honors")
            String[] termHonorsData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getTermHonors"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get term honors")

                termHonorsData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve term honors data failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            log.debug("get: got term honors data; retrieving student courses")
            String[] studentCourseData
            try {
                //****** Build parameters to send to our custom Peopletools API ****
                String packageName = "CCTC_TRANSCRIPT_PKG"
                String className = "CCTC_Student"
                String methodName = "getCoursework"

                String[] args = [
                        PSParameter.ConvertStringToCleanString(sisPersonId),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.institution")),
                        PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode,"peoplesoft.global.acad_career"))
                ]
                log.debug("get: calling the remote peoplesoft method to get course data")

                studentCourseData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
            }
            catch (psft.pt8.joa.JOAException e) {
                String messageToThrow = e.getMessage()
                if (peoplesoftSession != null) {
                    String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                    if (msgs && msgs.length > 0) {
                        messageToThrow = msgs[0]
                        msgs.each {
                            log.error("get: retrieve course data failed: [" + it + "]")
                        }
                    }
                }
                throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
            }

            String[] gradeScalesReported = []
            log.debug("get: got student course data; building session objects")
            if(studentTermData) {
                i = 0
                while(i + 13 <= studentTermData.length){
                    emplid = studentTermData[i++]
                    String termCode = studentTermData[i++]
                    String termTypeCode = studentTermData[i++]
                    String termBeginDt = studentTermData[i++]
                    String termEndDt = studentTermData[i++]
                    String termDescr = studentTermData[i++]
                    String schoolYear = studentTermData[i++]
                    String studentProgress = studentTermData[i++]

                    SessionTypeType sessionType = this.parseTermType(misCode, termTypeCode)
                    StudentLevelCodeType studentLevelCode = this.parseAcadLevel(misCode, studentProgress)

                    AcademicSessionType session = builder.createAcademicSession(studentLevelCode)
                    AcademicSessionDetailType sessionDetail  = builder.constructSessionDetail(df.parse(termBeginDt),
                            df.parse(termEndDt),
                            termDescr,
                            "",
                            sessionType)
                    sessionDetail.setSessionSchoolYear(schoolYear)

                    session.academicSessionDetail = sessionDetail


                    AcademicSummaryFType sessionSummary = builder.createAcademicSummaryF()

                    Double sessionCreditHoursAttempted = Double.parseDouble(studentTermData[i++])
                    Double sessionCreditHoursEarned = Double.parseDouble(studentTermData[i++])
                    Double sessionTotalQualityPoints = Double.parseDouble(studentTermData[i++])
                    Double sessionCreditHoursForGPA = Double.parseDouble(studentTermData[i++])
                    Double sessionGPA = Double.parseDouble(studentTermData[i++])

                    GPAType gpa = builder.createGPA()
                    gpa.setCreditHoursAttempted(BigDecimal.valueOf(sessionCreditHoursAttempted))
                    gpa.setCreditHoursEarned(BigDecimal.valueOf(sessionCreditHoursEarned))
                    gpa.setCreditUnit(AcademicGPACreditUnits.Units.toString())
                    gpa.setGradePointAverage(BigDecimal.valueOf(sessionGPA))
                    gpa.setTotalQualityPoints(BigDecimal.valueOf(sessionTotalQualityPoints))
                    gpa.setCreditHoursforGPA(BigDecimal.valueOf(sessionCreditHoursForGPA))
                    gpa.setGPARangeMaximum(BigDecimal.valueOf(4.0D))
                    gpa.setGPARangeMinimum(BigDecimal.valueOf(0D))
                    sessionSummary.setGPA(gpa)

                    sessionSummary.setAcademicSummaryType(AcademicSummaryTypeType.ALL)
                    sessionSummary.setAcademicSummaryLevel(CourseCreditLevelType.UNDERGRADUATE)


                    if(termHonorsData) {
                        Integer k = 0
                        while(k + 4 <= termHonorsData.length) {
                            emplid = termHonorsData[k++]
                            String honorTermCode = termHonorsData[k++]
                            String honorCode = termHonorsData[k++]
                            String honorTitle = termHonorsData[k++]

                            if(honorTermCode == termCode) {
                                HonorsRecognitionLevelType honorLevel = this.parseAwardCode(misCode, honorCode)
                                AcademicHonorsType honor = builder.constructHonors(honorLevel, honorTitle)
                                sessionSummary.getAcademicHonors().add(honor)
                            }
                        }
                    }

                    session.getAcademicSummaries().add(sessionSummary)

                    if(studentCourseData) {
                        Integer k = 0
                        while(k + 16 <= studentCourseData.length) {
                            emplid = studentCourseData[k++]
                            String crseTermCode = studentCourseData[k++]

                            String crseCIPCode = studentCourseData[k++]
                            if (crseCIPCode) {
                                crseCIPCode = crseCIPCode.replaceAll("[^0-9]", "")
                                if (crseCIPCode.length() == 6) {
                                    crseCIPCode = crseCIPCode[0..1] + "." + crseCIPCode[2..5]
                                }
                            }
                            String abbreviation = studentCourseData[k++]
                            String number = studentCourseData[k++]
                            String title = studentCourseData[k++]
                            String addDt = studentCourseData[k++]
                            String dropDt = studentCourseData[k++]
                            String stateArticulationID = null

                            String grade = studentCourseData[k++]
                            String gradeScaleCode = studentCourseData[k++]

                            double creditValue = new BigDecimal(studentCourseData[k++])
                            double creditEarned = new BigDecimal(studentCourseData[k++])
                            double qualityPointsEarned = new BigDecimal(studentCourseData[k++])

                            String gpaApplicability = studentCourseData[k++]
                            String repeatCode = studentCourseData[k++]

                            String note = studentCourseData[k++]

                            if(crseTermCode == termCode) {
                                if(gradeScaleCode) {
                                    gradeScalesReported += gradeScaleCode
                                }
                                AcademicCourseCodes crseCodeType = AcademicCourseCodes.CourseCIPCode
                                CourseLevelType crseLevel = CourseLevelType.COLLEGE_LEVEL
                                CourseCreditBasisType creditBasis = this.determineCourseCreditBasis(misCode, repeatCode, gradeScaleCode)
                                CreditCourseUnitTypes creditUnits = CreditCourseUnitTypes.Units
                                CourseCreditLevelType creditLevel = CourseCreditLevelType.UNDERGRADUATE
                                CourseGPAApplicabilityCodeType applicabilityCode = CourseGPAApplicabilityCodeType.NOT_APPLICABLE
                                if(StringUtils.equalsIgnoreCase(gpaApplicability ,"Y")) {
                                    applicabilityCode = CourseGPAApplicabilityCodeType.APPLICABLE
                                }
                                CourseRepeatCodeType repeat = this.parseRepeatCode(misCode, repeatCode)

                                CourseType courseType = builder.constructCourse(creditBasis,
                                        creditUnits,
                                        creditLevel,
                                        crseLevel,
                                        creditValue,
                                        creditEarned,
                                        gradeScaleCode,
                                        grade,
                                        qualityPointsEarned,
                                        crseCodeType,
                                        crseCIPCode,
                                        applicabilityCode,
                                        abbreviation,
                                        number,
                                        title,
                                        stateArticulationID)
                                courseType.setCourseAddDate(df.parse(addDt))
                                if(dropDt) {
                                    courseType.setCourseDropDate(df.parse(dropDt))
                                }
                                if(repeat) {
                                    courseType.setCourseRepeatCode(repeat)
                                }
                                if(note) {
                                    courseType.getNoteMessages().add(note)
                                }

                                session.getCourses().add(courseType)
                            }
                        }
                    }

                    academicRecord.getAcademicSessions().add(session)
                }
            }

            if(gradeScalesReported && gradeScalesReported.length > 0) {
                log.debug("get: built session objects; retrieving grade scheme data")
                String[] gradeSchemeData
                try {
                    //****** Build parameters to send to our custom Peopletools API ****
                    String packageName = "CCTC_TRANSCRIPT_PKG"
                    String className = "CCTC_Institution"
                    String methodName = "getGradingBasis"

                    String[] args = [
                            PSParameter.ConvertStringToCleanString(MisEnvironment.getProperty(environment, misCode, "peoplesoft.global.institution"))
                    ]
                    log.debug("get: calling the remote peoplesoft method to get grade scheme data")

                    gradeSchemeData = api.peoplesoft.PSConnection.callMethod(peoplesoftSession, packageName, className, methodName, "<", args)
                }
                catch (psft.pt8.joa.JOAException e) {
                    String messageToThrow = e.getMessage()
                    if (peoplesoftSession != null) {
                        String[] msgs = api.peoplesoft.PSConnection.getErrorMessages(peoplesoftSession)
                        if (msgs && msgs.length > 0) {
                            messageToThrow = msgs[0]
                            msgs.each {
                                log.error("get: retrieve grade scheme data failed: [" + it + "]")
                            }
                        }
                    }
                    throw new InternalServerException(InternalServerException.Errors.internalServerError, messageToThrow)
                }
                log.debug("get: got grade scheme data; building grade objects")
                if (gradeSchemeData) {
                    i = 0
                    if (academicRecord.userDefinedExtensions == null) {
                        academicRecord.userDefinedExtensions = new UserDefinedExtensionsTypeImpl()
                    }
                    if(academicRecord.userDefinedExtensions.academicRecordCCCExtensions == null) {
                        academicRecord.userDefinedExtensions.academicRecordCCCExtensions = builder.createAcademicRecordExtensions()
                    }

                    AcademicRecordCCCExtensions extensions = academicRecord.userDefinedExtensions.academicRecordCCCExtensions
                    while (i + 7 <= gradeSchemeData.length) {
                        String gradeDescription = gradeSchemeData[i++]
                        String gradeCode = gradeSchemeData[i++]
                        Double gradePoints = Double.parseDouble(gradeSchemeData[i++])
                        String gradeScaleCode = gradeSchemeData[i++]
                        String validAttempt = gradeSchemeData[i++]
                        String earnsCredit = gradeSchemeData[i++]
                        String includeInGpa = gradeSchemeData[i++]

                        if (gradeScalesReported.contains(gradeScaleCode)) {
                            AcademicRecordCCCExtensions.GradeSchemeEntry gradeSchemeEntry = builder.createGradeSchemeEntry()

                            gradeSchemeEntry.setDescription(gradeDescription)
                            gradeSchemeEntry.setGrade(gradeCode)
                            gradeSchemeEntry.setGradePoints(new BigDecimal(gradePoints))
                            gradeSchemeEntry.setEDIGradeQualifier(gradeScaleCode)

                            if (validAttempt) {
                                if (validAttempt.equalsIgnoreCase("Y")) {
                                    gradeSchemeEntry.setAttemptedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.COUNTED)
                                }
                                if (validAttempt.equalsIgnoreCase("N")) {
                                    gradeSchemeEntry.setAttemptedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.BRACKETED_NOT_COUNTED)
                                }
                            }
                            if (earnsCredit) {
                                if (earnsCredit.equalsIgnoreCase("Y")) {
                                    gradeSchemeEntry.setEarnedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.COUNTED)
                                }
                                if (earnsCredit.equalsIgnoreCase("N")) {
                                    gradeSchemeEntry.setEarnedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.NOT_COUNTED)
                                }
                            }
                            if (includeInGpa) {
                                if (includeInGpa.equalsIgnoreCase("Y")) {
                                    gradeSchemeEntry.setGradePointsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.COUNTED)
                                }
                                if (includeInGpa.equalsIgnoreCase("N")) {
                                    gradeSchemeEntry.setGradePointsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.NOT_COUNTED)
                                }
                            }

                            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
                            gradeSchemeEntry.setEffectiveFromDate(format.parse("19010101"))
                            gradeSchemeEntry.setEffectiveToDate(format.parse("29990101"))

                            extensions.getGradeSchemeEntries().add(gradeSchemeEntry)
                        }
                    }
                }
            }
            builder.getAcademicRecords().add(academicRecord)
        }
        finally {
            log.debug("get: disconnecting")
            if(peoplesoftSession != null) {
                peoplesoftSession.disconnect()
            }
        }

        api.peoplesoft.Person personApi = new api.peoplesoft.Person(environment)
        List<Email> emails = personApi.getPersonEmails(misCode, sisPersonId)
        if(emails && emails.size() > 0) {
            List<EmailType> studentEmails = []
            emails.each { j ->
                def emailType = builder.createEmailType()
                emailType.setEmailAddress(j.getEmailAddress())
                emailType.getNoteMessages().add(j.getType().name())
                studentEmails += emailType
            }
            builder.setStudentEmails(studentEmails)
        }

        api.peoplesoft.Identity identityApi = new api.peoplesoft.Identity(environment)
        cccId = identityApi.translateSisPersonIdToCCCId(misCode, sisPersonId)
        if (cccId) {
            builder.addAgencyIdentifier(cccId, "CCC Technology Center", AgencyCodeType.STATE, CountryCodeType.US, StateProvinceCodeType.CA)
        }

        return builder.build()
    }

    /**
     * Converts/parses the Peoplesoft Specific Degree code into a Standard PESC degree code using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param degree The peoplesofot specific degree code to convert (required)
     * @return a PESC standard degree code
     */
    protected String parseDegree(String misCode, String degree) {
        if(degree) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.mappings.pescSP02.degree." + degree)
            if(configMapping) {
                Double d = 0D
                try {
                    d = Double.parseDouble(configMapping)
                }
                catch (Exception) {
                    d = 0
                }
                if(d > 0) {
                    return configMapping
                }
            } else {
                log.warn("degree [" + degree + "] mapping not found")
            }
        }
        return null
    }

    /**
     * Converts/parses the Peoplesoft Specific term_type code into a Standard SessionTypeType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param term_type The peoplesoft specific term_type code to convert (required)
     * @return a SessionTypeType with a value associated with the given term_type
     */
    protected SessionTypeType parseTermType(String misCode, String term_type) {
        SessionTypeType result = SessionTypeType.OTHER
        if(term_type) {
            String configMapping = MisEnvironment.getProperty(environment, misCode,"peoplesoft.transcript.mappings.sessionTypeType.term_type." + term_type)
            if(configMapping) {
                SessionTypeType found = SessionTypeType.values().find {
                    return it.name().equalsIgnoreCase(configMapping) || it.value().equalsIgnoreCase(configMapping)
                }
                if(!found) {
                    log.warn("Could not parse term_type [" + term_type + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("term_type [" + term_type + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Converts/parses the Peoplesoft Specific acad_level code into a Standard StudentLevelCodeType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param acad_level The peoplesoft specific acad_level code to convert (required)
     * @return a StudentLevelCodeType with a value associated with the given acad_level
     */
    protected StudentLevelCodeType parseAcadLevel(String misCode, String acad_level) {
        StudentLevelCodeType result
        if (acad_level) {
            String configMapping = MisEnvironment.getProperty(environment, misCode,"peoplesoft.transcript.mappings.studentLevelCodeType.acad_level." + acad_level)
            if(configMapping) {
                StudentLevelCodeType found = StudentLevelCodeType.values().find {
                    return it.name().equalsIgnoreCase(configMapping) || it.value().equalsIgnoreCase(configMapping)
                }
                if(!found) {
                    log.warn("Could not parse acad_level [" + acad_level + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("acad_level [" + acad_level + "] mapping not found")
            }
        }

        return result
    }

    /**
     * Converts/parses the Peoplesoft Specific honors_code code into the corresponding HonorRecognitionLevelType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param honors_code The peoplesoft specific honors_code to convert (required)
     * @return a HonorRecognitionLevelType with a value associated with the given honors_code
     */
    protected HonorsRecognitionLevelType parseHonorsCode(String misCode, String honors_code){
        HonorsRecognitionLevelType result = HonorsRecognitionLevelType.THIRD_HIGHEST
        if(honors_code){
            String configMapping = MisEnvironment.getProperty(environment, misCode,"peoplesoft.transcript.mappings.honorsRecognitionLevelType.honors_code." + honors_code)
            if(configMapping) {
                HonorsRecognitionLevelType found = HonorsRecognitionLevelType.values().find {
                    return it.name().equalsIgnoreCase(configMapping) || it.value().equalsIgnoreCase(configMapping)
                }
                if(!found) {
                    log.warn("Could not parse honors_code [" + honors_code + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("honors_code [" + honors_code + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Converts/parses the Peoplesoft Specific award_code code into the corresponding HonorRecognitionLevelType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param award_code The peoplesoft specific award_code to convert (required)
     * @return a HonorRecognitionLevelType with a value associated with the given award_code
     */
    protected HonorsRecognitionLevelType parseAwardCode(String misCode, String award_code) {
        HonorsRecognitionLevelType result = HonorsRecognitionLevelType.THIRD_HIGHEST
        if (award_code) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.mappings.honorsRecognitionLevelType.award_code." + award_code)
            if (configMapping) {
                HonorsRecognitionLevelType found = HonorsRecognitionLevelType.values().find {
                    return it.name().equalsIgnoreCase(configMapping) || it.value().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse award_code [" + award_code + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("award_code [" + award_code + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Converts/parses the Peoplesoft Specific award_code code into the corresponding CourseRepeatCodeType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param repeat_code The peoplesoft specific repeat_code to convert (required)
     * @return a CourseRepeatCodeType with a value associated with the given repeat_code
     */
    protected CourseRepeatCodeType parseRepeatCode(String misCode, String repeat_code) {
        CourseRepeatCodeType result
        if (repeat_code) {
            String configMapping = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.mappings.courseRepeatCodeType.repeat_code." + repeat_code)
            if (configMapping) {
                CourseRepeatCodeType found = CourseRepeatCodeType.values().find {
                    return it.name().equalsIgnoreCase(configMapping) || it.value().equalsIgnoreCase(configMapping)
                }
                if (!found) {
                    log.warn("Could not parse repeat_code [" + repeat_code + "]")
                } else {
                    result = found
                }
            } else {
                log.warn("repeat_code [" + repeat_code + "] mapping not found")
            }
        }
        return result
    }

    /**
     * Determines based on several fields, with cascading logic, the appropriate CourseCreditBasisType enum value using the environment configs
     * @param misCode The College Code used for grabbing college specific settings from the environment. (required)
     * @param repeatCode The peoplesoft specific repeat_code to convert (required)
     * @param gradingBasis The peoplesoft specific grading_Basis to convert (required)
     * @return a CourseCreditBasisType with a value associated with the given repeat_code and grading_basis
     */
    protected CourseCreditBasisType determineCourseCreditBasis(String misCode,
                                                              String repeatCode,
                                                              String gradingBasis){
        if(gradingBasis) {
            String credit_by_exam_code = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.courseCreditBasisType.credit_by_exam.grading_basis")
            if (credit_by_exam_code) {
                if (credit_by_exam_code.split(",").any { it -> StringUtils.equalsIgnoreCase(gradingBasis, it) }) {
                    return CourseCreditBasisType.CREDIT_BY_EXAM
                }
            }

            String advanced_placement_code = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.courseCreditBasisType.advanced_placement.grading_basis")
            if (advanced_placement_code) {
                if (advanced_placement_code.split(",").any { it -> StringUtils.equalsIgnoreCase(gradingBasis, it) }) {
                    return CourseCreditBasisType.ADVANCED_PLACEMENT
                }
            }
        }

        if(repeatCode) {
            String academic_renewal_code = MisEnvironment.getProperty(environment, misCode, "peoplesoft.transcript.courseCreditBasisType.academic_renewal.repeat_code")
            if (academic_renewal_code) {
                if (academic_renewal_code.split(",").any { it -> StringUtils.equalsIgnoreCase(repeatCode, it) }) {
                    return CourseCreditBasisType.ACADEMIC_RENEWAL
                }
            }
        }
        // TODO: Expand the logic to Consider High School Credit and/or High School Dual Credit

        return CourseCreditBasisType.REGULAR
    }
}