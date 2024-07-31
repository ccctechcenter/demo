package com.ccctc.adaptor.util.transcript;

import com.ccctc.adaptor.model.transcript.*;
import com.ccctc.core.coremain.v1_14.*;
import com.ccctc.core.coremain.v1_14.BirthType;
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript;
import com.ccctc.sector.academicrecord.v1_9.*;
import com.ccctc.sector.academicrecord.v1_9.AcademicProgramType;
import com.ccctc.sector.academicrecord.v1_9.AddressType;
import com.ccctc.sector.academicrecord.v1_9.ContactsType;
import com.ccctc.sector.academicrecord.v1_9.EmailType;
import com.ccctc.sector.academicrecord.v1_9.OrganizationType;
import com.ccctc.sector.academicrecord.v1_9.PersonType;
import com.ccctc.sector.academicrecord.v1_9.PhoneType;
import com.ccctc.sector.academicrecord.v1_9.ResidencyType;
import com.ccctc.sector.academicrecord.v1_9.SchoolType;
import com.ccctc.sector.academicrecord.v1_9.TransmissionDataType;
import com.xap.ccctran.AcademicRecordCCCExtensions;
import com.xap.ccctran.CourseCCCExtensions;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Created by james on 4/14/17.
 */
public class CollegeTranscriptBuilder {

    private static final com.ccctc.message.collegetranscript.v1_6.ObjectFactory collegeTranFactory = new com.ccctc.message.collegetranscript.v1_6.ObjectFactory();
    private static final com.ccctc.sector.academicrecord.v1_9.ObjectFactory academicRecordFactory = new com.ccctc.sector.academicrecord.v1_9.ObjectFactory();
    private static final com.ccctc.core.coremain.v1_14.ObjectFactory coreFactory = new com.ccctc.core.coremain.v1_14.ObjectFactory();
    private static final com.xap.ccctran.ObjectFactory cccFactory = new com.xap.ccctran.ObjectFactory();

    private String highSchoolName;
    private String highSchoolGraduationDate;
    private List<SchoolCode> highSchoolCodes = new ArrayList<SchoolCode>();
    private String schoolAssignedStudentID;
    private String agencyAssignedID;

    private List<AgencyIdentifierType> agencyIdentifiers = new ArrayList<AgencyIdentifierType>();
    private List<AddressType> studentAddressList;
    private String studentEmailAddress;
    private List<EmailType> studentEmails;
    private Date studentBirthDate;
    private String studentBirthCity;
    private StateProvinceCodeType studentBirthStateProvince;
    private CountryCodeType studentBirthCountry;

    private String studentPhoneNumber;
    private List<PhoneType> phoneTypes;
    private String studentAreaCode;
    private String studentPartialSSN;
    private String studentSSN;
    private String studentFirstname;
    private String studentMiddleName;
    private String studentLastName;
    private String studentNamePrefix;
    private GenderCodeType studentGender;
    private String studentResidentStateOrProvince;

    /**
     * JR,SR,I,II,III,IV,V,VI,VII,VIII,IX,X;
     */
    private String studentNameSuffix;

    private List<String> studentAddressLines = new ArrayList<String>();
    private String studentCity;
    private String studentStateOrProvince;
    private String studentZipCode;
    private String studentCountry;
    private String attentionLine;

    private List<AcademicRecordType> academicRecords = new ArrayList<AcademicRecordType>();

    private javax.xml.bind.JAXBContext cccJAXBContext;

    public CollegeTranscriptBuilder(javax.xml.bind.JAXBContext cccJAXBContext) {
        this.cccJAXBContext = cccJAXBContext;
    }


    /**
     * Intended to be used to construct note messages in the PESC college transcript.  Note messages can
     * only be 80 characaters long.
     *
     * @param text
     * @param size
     * @return
     */
    public static List<String> splitEqually(String text, int size) {
        // Give the list the right capacity to start with. You could use an array
        // instead if you wanted.
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }


    public void setAttentionLine(String attentionLine) {
        this.attentionLine = attentionLine;
    }


    public void setHighSchoolName(String highSchoolName) {
        this.highSchoolName = highSchoolName;
    }

    public void setHighSchoolGraduationDate(String graduationDate) {
        this.highSchoolGraduationDate = graduationDate;
    }

    public List<SchoolCode> getHighSchoolCodes() {
        return highSchoolCodes;
    }


    public void addHighSchoolCode(SchoolCode schoolCode) {
        highSchoolCodes.add(schoolCode);
    }

    public void addStudentAddressLine(String addressLine) {
        studentAddressLines.add(addressLine);
    }


    public void addSchoolCodeToOrganization(SchoolType school, String schoolCode, SchoolCodeTypes schoolCodeType) {
        switch (schoolCodeType) {
            case ACT:
                school.setACT(schoolCode);
                break;
            case OPEID:
                school.setOPEID(schoolCode);
                break;
            case IPEDS:
                school.setIPEDS(schoolCode);
                break;
            case ATP:
                school.setATP(schoolCode);
                break;
            case FICE:
                school.setFICE(schoolCode);
                break;
            case CEEBACT:
                school.setCEEBACT(schoolCode);
                break;
            default:
                break;
        }
    }


    public AcademicDegreeRequirementType createDegreeRequirement(String advisor, String title) {
        AcademicDegreeRequirementType degreeRequirement = coreFactory.createAcademicDegreeRequirementType();
        degreeRequirement.setThesisDissertationAdvisor(advisor);
        degreeRequirement.setThesisDissertationTitle(title);
        return degreeRequirement;

    }

    public AcademicSummaryE1Type createAcademicSummaryE1(AcademicSummaryTypeType summaryType) {
        AcademicSummaryE1Type academicSummary = academicRecordFactory.createAcademicSummaryE1Type();
        academicSummary.setAcademicSummaryType(summaryType);
        return academicSummary;
    }

    public AcademicAwardType createAcademicAward(String title,
                                                 String awardLevel,
                                                 Date awardDate,
                                                 Boolean completionIndicatetor,
                                                 Date awardCompletionDate) {
        AcademicAwardType academicAward = academicRecordFactory.createAcademicAwardType();
        academicAward.setAcademicAwardTitle(title);
        academicAward.setAcademicAwardLevel(awardLevel);
        academicAward.setAcademicAwardDate(awardDate);
        academicAward.setAcademicCompletionIndicator(completionIndicatetor);
        academicAward.setAcademicCompletionDate(awardCompletionDate);

        return academicAward;

    }

    /**
     * @param studentLevelCode
     * @return
     */
    public AcademicSessionType createAcademicSession(StudentLevelCodeType studentLevelCode) {
        AcademicSessionType academicSession = academicRecordFactory.createAcademicSessionType();

        StudentLevelType level = coreFactory.createStudentLevelType();
        level.setStudentLevelCode(studentLevelCode);
        academicSession.setStudentLevel(level);

        return academicSession;
    }

    public StudentLevelType constructStudentLevelType(StudentLevelCodeType studentLevelCode, String message) {
        StudentLevelType studentLevel = coreFactory.createStudentLevelType();
        studentLevel.setStudentLevelCode(studentLevelCode);

        if (message != null) {
            studentLevel.getNoteMessages().addAll(splitEqually(message, 80));
        }
        return studentLevel;
    }

    public AcademicSessionDetailType constructSessionDetail(Date begin, Date end, String sessionName, String sessionDesignatorSuffix, SessionTypeType sessionType) {
        AcademicSessionDetailType sessionDetail = coreFactory.createAcademicSessionDetailType();
        sessionDetail.setSessionBeginDate(begin);
        sessionDetail.setSessionEndDate(end);
        sessionDetail.setSessionName(sessionName);

        sessionDetail.setSessionDesignator(begin);

        if (sessionType != null) {
            sessionDetail.setSessionType(sessionType);
        }

        if (!StringUtils.isEmpty(sessionDesignatorSuffix)) {
            sessionDetail.setSessionDesignatorSuffix(sessionDesignatorSuffix);
        }

        return sessionDetail;
    }

    public AgencyIdentifierType createAgencyIdentifier(String agencyAssignedID, String agencyName, AgencyCodeType agencyCode, CountryCodeType agencyCountryCode, StateProvinceCodeType agencyStateProvinceCode) {
        AgencyIdentifierType agencyIdentifier = createAgencyIdentifierType();
        agencyIdentifier.setAgencyAssignedID(agencyAssignedID);
        agencyIdentifier.setAgencyName(agencyName);
        agencyIdentifier.setAgencyCode(agencyCode);
        agencyIdentifier.setCountryCode(agencyCountryCode);
        agencyIdentifier.setStateProvinceCode(agencyStateProvinceCode);

        return agencyIdentifier;
    }

    public void addAgencyIdentifier(String agencyAssignedID, String agencyName, AgencyCodeType agencyCode, CountryCodeType agencyCountryCode, StateProvinceCodeType agencyStateProvinceCode) {
        AgencyIdentifierType agencyIdentifier = createAgencyIdentifier(agencyAssignedID,
                agencyName,
                agencyCode,
                agencyCountryCode,
                agencyStateProvinceCode
        );

        this.agencyIdentifiers.add(agencyIdentifier);

    }

    public AcademicSummaryE2Type createAcademicSummaryE2() {
        return academicRecordFactory.createAcademicSummaryE2Type();
    }

    public AcademicSummaryFType createAcademicSummaryF() {

        AcademicSummaryFType academicSummary = academicRecordFactory.createAcademicSummaryFType();

        return academicSummary;

    }

    public List<AcademicRecordType> getAcademicRecords() {
        return academicRecords;
    }

    /**
     * The academic program embedded within the academic session.
     *
     * @param programName
     * @param programType
     * @param programCode
     * @param programCodeType
     * @return
     */
    public AcademicProgramType createAcademicProgram(String programName, AcademicProgramTypeType programType, String programCode, AcademicProgramCodes programCodeType) {
        AcademicProgramType academicProgram = academicRecordFactory.createAcademicProgramType();

        if (programCode != null) {
            switch (programCodeType) {
                case ProgramCIPCode:
                    academicProgram.setProgramCIPCode(programCode);
                    break;
                case ProgramCSISCode:
                    academicProgram.setProgramCSISCode(programCode);
                    break;
                case ProgramESISCode:
                    academicProgram.setProgramESISCode(programCode);
                    break;
                case ProgramHEGISCode:
                    academicProgram.setProgramHEGISCode(programCode);
                    break;
                case ProgramUSISCode:
                    academicProgram.setProgramUSISCode(programCode);
                    break;
                default:
                    break;

            }
        }

        academicProgram.setAcademicProgramName(programName);
        academicProgram.setAcademicProgramType(programType);

        return academicProgram;
    }

    public SchoolType createSchool(String schoolName, String schoolCode, SchoolCodeTypes schoolCodeType) {
        SchoolType school = academicRecordFactory.createSchoolType();
        addSchoolCodeToOrganization(school, schoolCode, schoolCodeType);
        school.setOrganizationName(schoolName);
        return school;
    }

    public SchoolType createSchool() {
        return academicRecordFactory.createSchoolType();
    }

    public AcademicRecordType createAcademicRecord() {
        AcademicRecordType academicRecord = academicRecordFactory.createAcademicRecordType();

        return academicRecord;
    }

    public AcademicRecordType createAcademicRecord(StudentLevelCodeType studentLevel) {

        AcademicRecordType academicRecord = createAcademicRecord();
        StudentLevelType studentLevelType = coreFactory.createStudentLevelType();
        studentLevelType.setStudentLevelCode(studentLevel);

        academicRecord.setStudentLevel(studentLevelType);

        return academicRecord;
    }

    public StudentType createStudent() {
        StudentType student = academicRecordFactory.createStudentType();

        return student;
    }

    public com.ccctc.core.coremain.v1_14.AdditionalStudentAchievementsType createAdditionalStudentAchievement() {
        return coreFactory.createAdditionalStudentAchievementsType();
    }

    public RAPType createRAPType() {
        return coreFactory.createRAPType();
    }

    public AcademicHonorsType constructHonors(HonorsRecognitionLevelType honorsRecognitionLevel, String title) {
        AcademicHonorsType honors = coreFactory.createAcademicHonorsType();
        honors.setHonorsLevel(honorsRecognitionLevel);
        honors.setHonorsTitle(title);
        return honors;
    }

    public CourseType createCourse() {
        return academicRecordFactory.createCourseType();
    }

    public CourseType constructCourse(CourseCreditBasisType creditBasis,
                                      CreditCourseUnitTypes creditUnits,
                                      CourseCreditLevelType creditLevel,
                                      CourseLevelType level,
                                      double creditValue,
                                      double creditEarned,
                                      String gradeScaleCode,
                                      String grade,
                                      double qualityPointsEarned,
                                      AcademicCourseCodes courseCodeType,
                                      String courseCode,
                                      CourseGPAApplicabilityCodeType applicabilityCode,
                                      String abbreviation,
                                      String number,
                                      String title,
                                      String stateArticulationID) throws JAXBException, SAXException {

        CourseType course = createCourse();

        course.setCourseCreditBasis(creditBasis);
        course.setCourseCreditUnits(creditUnits.toString());
        course.setCourseCreditLevel(creditLevel);
        course.setCourseLevel(level);
        course.setCourseCreditValue(BigDecimal.valueOf(creditValue));
        course.setCourseCreditEarned(BigDecimal.valueOf(creditEarned));
        course.setCourseAcademicGradeScaleCode(gradeScaleCode);
        course.setCourseAcademicGrade(grade);
        course.setCourseQualityPointsEarned(BigDecimal.valueOf(qualityPointsEarned));

        switch (courseCodeType) {
            case CourseCIPCode:
                course.setCourseCIPCode(courseCode);
                break;
            case CourseCSISCode:
                course.setCourseCSISCode(courseCode);
                break;
            case CourseUSISCode:
                course.setCourseUSISCode(courseCode);
                break;
            default:
                break;
        }

        course.setCourseGPAApplicabilityCode(applicabilityCode);
        course.setCourseSubjectAbbreviation(abbreviation);
        course.setCourseNumber(number);
        course.setCourseTitle(title);


        if (stateArticulationID != null) {
            CourseCCCExtensions cccExtensions = createCourseExtensions();
            cccExtensions.setStateArticulationID(stateArticulationID);

            if (course.getUserDefinedExtensions() == null)
                course.setUserDefinedExtensions(coreFactory.createUserDefinedExtensionsType());

            course.getUserDefinedExtensions().setCourseCCCExtensions(cccExtensions);
        }

        return course;

    }

    public CourseCreditBasisType convertCCCASCIICourseCreditBasisToPESC(String courseCreditBasis) {
        if ("AR".equals(courseCreditBasis)) {
            return CourseCreditBasisType.ACADEMIC_RENEWAL;
        } else if ("AP".equals(courseCreditBasis)) {
            return CourseCreditBasisType.ADVANCED_PLACEMENT;
        } else if ("CE".equals(courseCreditBasis)) {
            return CourseCreditBasisType.CREDIT_BY_EXAM;
        } else if ("HS".equals(courseCreditBasis)) {
            return CourseCreditBasisType.HIGH_SCHOOL_CREDIT_ONLY;
        } else if ("22".equals(courseCreditBasis)) {
            return CourseCreditBasisType.HIGH_SCHOOL_DUAL_CREDIT;
        }

        return CourseCreditBasisType.REGULAR;
    }

    public String convertCCCASCIICoureApplicabilityToPESC(char applibabilityCode) {
        String pescAppicability = "";
        switch (applibabilityCode) {
            case 'Y':
                pescAppicability = "BothPrograms";
                break;
            case 'N':
                pescAppicability = "NotApplicable";
                break;
            default:
                ;
        }
        return pescAppicability;
    }

    public String convertCCCASCIIRepeatStatusToPESC(char repeatStatus) {
        String pescStatus = "";
        switch (repeatStatus) {
            case 'D':
                pescStatus = "ReplacedNotCounted";
                break;
            case 'M':
                pescStatus = "ReplacementCounted";
                break;
            case 'R':
                pescStatus = "RepeatNotCounted";
                break;
            case 'T':
                pescStatus = "RepeatCounted";
                break;
            case 'I':
                pescStatus = "RepeatOtherInstitution";
                break;
            default:
                ;
        }
        return pescStatus;
    }

    public String convertAttemptedUnitsCalcFROMCCCASCIIToPESC(char bracket) {
        String pescBracket = "";

        switch (bracket) {
            case 'Y':
                pescBracket = "Counted";
                break;
            case 'B':
                pescBracket = "BracketedNotCounted";
                break;
            case 'C':
                pescBracket = "BracketedCounted";
                break;
            case 'N':
                pescBracket = "NotCounted";
                break;

        }
        return pescBracket;
    }

    public CourseCCCExtensions createCourseExtensions() {
        return cccFactory.createCourseCCCExtensions();
    }

    public AcademicRecordCCCExtensions createAcademicRecordExtensions() {
        return cccFactory.createAcademicRecordCCCExtensions();
    }

    public AcademicRecordCCCExtensions.GradeSchemeEntry createGradeSchemeEntry() {
        return cccFactory.createAcademicRecordCCCExtensionsGradeSchemeEntry();
    }

    public PhoneType createPhoneType() {
        return academicRecordFactory.createPhoneType();
    }

    public EmailType createEmailType() {
        return academicRecordFactory.createEmailType();
    }

    public AddressType createAddressType() {
        return academicRecordFactory.createAddressType();
    }

    public ContactsType createContactsType() {
        return academicRecordFactory.createContactsType();
    }

    public OrganizationType createOrganizationType() {
        return academicRecordFactory.createOrganizationType();
    }

    public com.ccctc.core.coremain.v1_14.OrganizationType createCoreOrganization() {
        return coreFactory.createOrganizationType();
    }

    public SourceDestinationType createSourceDestinationType() {
        return academicRecordFactory.createSourceDestinationType();
    }

    public TransmissionDataType createTransmissionDataType() {
        return academicRecordFactory.createTransmissionDataType();
    }

    public GPAType createGPA() {
        return coreFactory.createGPAType();
    }

    /**
     * @param creditHoursAttemped The total value of credit hours attempted whether or not included in the hours for GPA or hours earned.
     * @param creditHoursEarned   The total value of credit hours successfully completed or earned.
     * @param creditUnits         The type of credit (unit, semester, or quarter) associated with the credit hours earned for the course
     *                            NoCredit
     *                            Quarter
     *                            Semester
     *                            Units
     *                            ClockHours
     *                            CarnegieUnits
     *                            ContinuingEducationUnits
     *                            Unreported
     *                            Other
     * @param totalQualityPoints  The total value of quality points used for the calculation of the Grade Point Average.
     * @param creditHoursForGPA   The total value of credit hours included in the Grade Point Average.
     * @return the constructed GPA
     */
    public GPAType constructGpa(Double creditHoursAttemped,
                                Double creditHoursEarned,
                                AcademicGPACreditUnits creditUnits,
                                Double totalQualityPoints,
                                Double creditHoursForGPA,
                                Double gpaMin,
                                Double gpaMax) {
        GPAType gpa = createGPA();

        //Build gpa.

        //The total value of credit hours attempted whether or not included in the hours for GPA or hours earned.
        gpa.setCreditHoursAttempted(BigDecimal.valueOf(creditHoursAttemped));
        //The total value of credit hours successfully completed or earned.
        gpa.setCreditHoursEarned(BigDecimal.valueOf(creditHoursEarned));

        //The type of credit (unit, semester, or quarter) associated with the credit hours earned for the course
        /*
            NoCredit
            Quarter
            Semester
            Units
            ClockHours
            CarnegieUnits
            ContinuingEducationUnits
            Unreported
            Other
         */
        gpa.setCreditUnit(creditUnits.toString());

        //The value of the total quality points divided by the Credit Hours for Grade Point Average.
        gpa.setGradePointAverage(BigDecimal.valueOf((double) totalQualityPoints / creditHoursForGPA).setScale(3, RoundingMode.CEILING));

        //The total value of quality points used for the calculation of the Grade Point Average.
        gpa.setTotalQualityPoints(BigDecimal.valueOf(totalQualityPoints));

        //The total value of credit hours included in the Grade Point Average.
        gpa.setCreditHoursforGPA(BigDecimal.valueOf(creditHoursForGPA));

        if (gpaMax != null) {
            gpa.setGPARangeMaximum(BigDecimal.valueOf(gpaMax));
        }

        if (gpaMin != null) {
            gpa.setGPARangeMinimum(BigDecimal.valueOf(gpaMin));
        }

        return gpa;
    }


    public AgencyIdentifierType createAgencyIdentifierType() {
        return coreFactory.createAgencyIdentifierType();
    }

    public CollegeTranscript build() {
        CollegeTranscript transcript = collegeTranFactory.createCollegeTranscript();
        PersonType person = academicRecordFactory.createPersonType();
        person.setSchoolAssignedPersonID(schoolAssignedStudentID);

        HighSchoolType highSchool = academicRecordFactory.createHighSchoolType();
        person.setHighSchool(highSchool);

        NameType studentName = coreFactory.createNameType();
        studentName.setFirstName(studentFirstname);
        studentName.setLastName(studentLastName);

        if (studentMiddleName != null) {
            studentName.getMiddleNames().add(studentMiddleName);
        }
        if (studentNameSuffix != null) {
            studentName.setNameSuffix(NameSuffixType.fromValue(studentNameSuffix));
        }
        if (studentNamePrefix != null) {
            studentName.setNamePrefix(studentNameSuffix);
        }

        person.setPartialSSN(this.studentPartialSSN);
        person.setSSN(this.studentSSN);
        person.setName(studentName);
        GenderType gender = coreFactory.createGenderType();
        gender.setGenderCode(this.studentGender);

        person.setGender(gender);

        person.getAgencyIdentifiers().addAll(agencyIdentifiers);

        person.setAgencyAssignedID(agencyAssignedID);

        BirthType birth = coreFactory.createBirthType();
        birth.setBirthDate(studentBirthDate);
        birth.setBirthCity(studentBirthCity);
        birth.setBirthStateProvinceCode(studentBirthStateProvince);
        birth.setBirthCountry(studentBirthCountry);

        person.setBirth(birth);

        ContactsType studentContactInfo = academicRecordFactory.createContactsType();

        if (studentEmailAddress != null) {
            EmailType email = academicRecordFactory.createEmailType();
            email.setEmailAddress(studentEmailAddress);
            studentContactInfo.getEmails().add(email);
        }

        if (studentEmails != null) {
            for (EmailType email : studentEmails) {
                studentContactInfo.getEmails().add(email);
            }

        }

        person.getContacts().add(studentContactInfo);


        if (studentAddressList == null) {
            AddressType studentAddress = academicRecordFactory.createAddressType();
            studentContactInfo.getAddresses().add(studentAddress);
            for (String streetAddress : studentAddressLines) {
                studentAddress.getAddressLines().add(streetAddress);
            }
            studentAddress.setCity(studentCity);
            if (!StringUtils.isEmpty(studentStateOrProvince)) {
                studentAddress.setStateProvinceCode(StateProvinceCodeType.fromValue(studentStateOrProvince));
            }
            if (!StringUtils.isEmpty(studentCountry))
                studentAddress.setCountryCode(CountryCodeType.fromValue(studentCountry));

            if (!StringUtils.isEmpty(attentionLine))
                studentAddress.getAttentionLines().add(attentionLine);

            studentAddress.setPostalCode(studentZipCode);
        } else {
            for (AddressType address : studentAddressList) {
                studentContactInfo.getAddresses().add(address);
            }
        }
        if (studentPhoneNumber != null) {
            com.ccctc.sector.academicrecord.v1_9.PhoneType phone = academicRecordFactory.createPhoneType();
            if (studentAreaCode != null) {
                phone.setAreaCityCode(studentAreaCode);
            }
            phone.setPhoneNumber(studentPhoneNumber);
            studentContactInfo.getPhones().add(phone);

        }
        if (phoneTypes != null) {
            for (PhoneType phone : phoneTypes) {
                studentContactInfo.getPhones().add(phone);
            }

        }

        highSchool.setOrganizationName(highSchoolName);

        if (!StringUtils.isEmpty(highSchoolGraduationDate)) {
            person.getNoteMessages().add("High School Graduation Date: " + this.highSchoolGraduationDate);
        }
        for (SchoolCode schoolCode : highSchoolCodes) {
            switch (schoolCode.getCodeType()) {
                case ACT:
                    highSchool.setACT(schoolCode.getCode());
                    break;
                case OPEID:
                    highSchool.setOPEID(schoolCode.getCode());
                    break;
                case IPEDS:
                    highSchool.setIPEDS(schoolCode.getCode());
                    break;
                case ATP:
                    highSchool.setATP(schoolCode.getCode());
                    break;
                case FICE:
                    highSchool.setFICE(schoolCode.getCode());
                    break;
                case CEEBACT:
                    highSchool.setCEEBACT(schoolCode.getCode());
                    break;
                default:
                    break;
            }
        }

        ResidencyType residency = academicRecordFactory.createResidencyType();
        residency.setStateProvince(this.studentResidentStateOrProvince);

        person.setResidency(residency);
        StudentType student = createStudent();
        student.setPerson(person);

        for (AcademicRecordType ar : academicRecords) {
            student.getAcademicRecords().add(ar);
        }

        transcript.setStudent(student);
        return transcript;
    }


    public void setAgencyAssignedID(String agencyAssignedID) {
        this.agencyAssignedID = agencyAssignedID;
    }

    public void setSchoolAssignedStudentID(String schoolAssignedStudentID) {
        this.schoolAssignedStudentID = schoolAssignedStudentID;
    }

    public void setStudentEmailAddress(String studentEmailAddress) {
        this.studentEmailAddress = studentEmailAddress;
    }

    public void setStudentEmails(List<EmailType> studentEmails) {
        this.studentEmails = studentEmails;
    }

    public void setStudentAddressList(List<AddressType> studentAddressList) {
        this.studentAddressList = studentAddressList;
    }

    public void setStudentBirthDate(Date studentBirthDate) {
        this.studentBirthDate = studentBirthDate;
    }

    public void setStudentBirthCity(String birthCity) {
        this.studentBirthCity = birthCity;
    }

    public void setStudentBirthStateProvince(StateProvinceCodeType birthStateProvince) {
        this.studentBirthStateProvince = birthStateProvince;
    }

    public void setStudentBirthCountry(CountryCodeType birthCountry) {
        this.studentBirthCountry = birthCountry;
    }


    public void setStudentPhoneNumber(String studentPhoneNumber) {
        this.studentPhoneNumber = studentPhoneNumber;
    }

    public void setPhoneTypes(List<PhoneType> phoneTypes) {
        this.phoneTypes = phoneTypes;
    }

    public void setStudentAreaCode(String studentAreaCode) {
        this.studentAreaCode = studentAreaCode;
    }


    public void setStudentPartialSSN(String studentPartialSSN) {
        this.studentPartialSSN = studentPartialSSN;
    }


    public void setStudentSSN(String studentSSN) {
        this.studentSSN = studentSSN;
    }


    public void setStudentFirstname(String studentFirstname) {
        this.studentFirstname = studentFirstname;
    }


    public void setStudentMiddleName(String studentMiddleName) {
        this.studentMiddleName = studentMiddleName;
    }

    public void setStudentLastName(String studentLastName) {
        this.studentLastName = studentLastName;
    }


    public void setStudentNamePrefix(String studentNamePrefix) {
        this.studentNamePrefix = studentNamePrefix;
    }


    /**
     * For addresses outside the United States
     *
     * @param studentResidentStateOrProvince
     */
    public void setStudentResidentStateOrProvince(String studentResidentStateOrProvince) {
        this.studentResidentStateOrProvince = studentResidentStateOrProvince;
    }

    public GenderCodeType createGenderCodeType(String gender) {

        return gender.equals("M") ? GenderCodeType.MALE : (gender.equals("F") ? GenderCodeType.FEMALE : GenderCodeType.UNREPORTED);
    }

    public void setStudentGender(GenderCodeType studentGender) {
        this.studentGender = studentGender;
    }


    public void setStudentNameSuffix(String studentNameSuffix) {
        this.studentNameSuffix = studentNameSuffix;
    }

    public List<String> getStudentAddressLines() {
        return studentAddressLines;
    }


    public void setStudentCity(String studentCity) {
        this.studentCity = studentCity;
    }


    public void setStudentStateOrProvince(String studentStateOrProvince) {
        this.studentStateOrProvince = studentStateOrProvince;
    }


    public void setStudentZipCode(String studentZipCode) {
        this.studentZipCode = studentZipCode;
    }


    public void setStudentCountry(String studentCountry) {
        this.studentCountry = studentCountry;
    }

}
