package com.ccctc.adaptor.util.impl;

import com.ccctc.adaptor.model.transcript.*;
import com.ccctc.adaptor.util.transcript.CollegeTranscriptBuilder;
import com.ccctc.adaptor.util.transcript.ValidationUtils;
import com.ccctc.adaptor.util.transcript.XmlFileType;
import com.ccctc.adaptor.util.transcript.XmlSchemaVersion;
import com.ccctc.core.coremain.v1_14.*;
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript;
import com.ccctc.sector.academicrecord.v1_9.AcademicSessionType;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.naming.OperationNotSupportedException;
import javax.xml.bind.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by james on 6/5/17.
 */
@Service
public class TranscriptService {

    private final static org.slf4j.Logger log = LoggerFactory.getLogger(TranscriptService.class);

    private JAXBContext transcriptJAXBContext;
    private JAXBContext cccJAXBContext;

    @Autowired
    public TranscriptService(@Qualifier("transcriptJAXBContext") JAXBContext transcriptJAXBContext,
                             @Qualifier("cccExtensionsContext") JAXBContext cccJAXBContext) {
        this.cccJAXBContext = cccJAXBContext;
        this.transcriptJAXBContext = transcriptJAXBContext;
    }

    public JAXBContext getTranscriptJAXBContext() {
        return transcriptJAXBContext;
    }

    public JAXBContext getCccJAXBContext() {
        return cccJAXBContext;
    }

    public Unmarshaller json(Unmarshaller unmarshaller) throws PropertyException {

        unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
        unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, Boolean.TRUE);
        unmarshaller.setProperty(UnmarshallerProperties.JSON_USE_XSD_TYPES_WITH_PREFIX, Boolean.TRUE);

        Map<String, String> namespacePrefixMapper = new HashMap<String, String>(4);
        namespacePrefixMapper.put("urn:org:pesc:core:CoreMain:v1.14.0", "core");
        namespacePrefixMapper.put("urn:org:pesc:sector:AcademicRecord:v1.9.0", "AcRec");
        namespacePrefixMapper.put("urn:org:pesc:message:CollegeTranscript:v1.6.0", "ColTrn");
        namespacePrefixMapper.put("http://www.xap.com/CCCTran", "CCC");

        unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_PREFIX_MAPPER, namespacePrefixMapper);
        unmarshaller.setProperty(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');

        return unmarshaller;
    }


    public Marshaller json(Marshaller marshaller) throws PropertyException {
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
        marshaller.setProperty(MarshallerProperties.JSON_USE_XSD_TYPES_WITH_PREFIX, Boolean.TRUE);

        Map<String, String> namespacePrefixMapper = new HashMap<String, String>(4);
        namespacePrefixMapper.put("urn:org:pesc:core:CoreMain:v1.14.0", "core");
        namespacePrefixMapper.put("urn:org:pesc:sector:AcademicRecord:v1.9.0", "AcRec");
        namespacePrefixMapper.put("urn:org:pesc:message:CollegeTranscript:v1.6.0", "ColTrn");
        namespacePrefixMapper.put("http://www.xap.com/CCCTran", "CCC");

        marshaller.setProperty(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, namespacePrefixMapper);
        marshaller.setProperty(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        marshaller.setProperty(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
        return marshaller;
    }

    private Marshaller marshaller(javax.xml.bind.JAXBContext context) throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        return marshaller;
    }

    private Unmarshaller unmarshaller(javax.xml.bind.JAXBContext context) throws JAXBException {
        return context.createUnmarshaller();
    }

    public Marshaller createTranscriptMarshaller(boolean useJSON) throws JAXBException {
        Marshaller marshaller = marshaller(transcriptJAXBContext);
        if (useJSON) {
            json(marshaller);
        }
        return marshaller;
    }

    public Unmarshaller createTranscriptUnmarshaller(boolean validate, boolean useJSON) throws JAXBException, SAXException, OperationNotSupportedException {
        Unmarshaller unmarshaller =  unmarshaller(transcriptJAXBContext);
        if (validate == true) {
            unmarshaller.setSchema(ValidationUtils.getSchema(XmlFileType.COLLEGE_TRANSCRIPT, XmlSchemaVersion.V1_6_0));
        }
        if (useJSON == true) {
            json(unmarshaller);
        }
        return unmarshaller;
    }

    public OutputStream toJSONStream(CollegeTranscript collegeTranscript) throws JAXBException {

        Marshaller marshaller = createTranscriptMarshaller(true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        marshaller.marshal(collegeTranscript, outputStream);

        return outputStream;
    }

    public OutputStream toJSONStream(CollegeTranscript collegeTranscript, OutputStream outputStream) throws JAXBException {

        Marshaller marshaller = createTranscriptMarshaller(true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        marshaller.marshal(collegeTranscript, outputStream);
        return outputStream;
    }

    /**
     *
     * @param firstName Set the student's first name (required)
     * @param lastName Set the student's last name (required)
     * @param birthDate Set the student date of birth (required)
     * @param SSN Set the social security number of the student (optional)
     * @param partialSSN Set the partial ssn of the student (optional)
     * @param cccId Set the CCCID of the student.  This is the agency identifier in terms of
     *              PESC. (optional)
     * @param schoolAssignedStudentId Set the student id as defined by the record holder. (optional)
     * @param emailAddress Set the studen't email address (optional)
     * @return
     */
    public CollegeTranscript createMockTranscript(String firstName,
                                                  String lastName,
                                                  Date birthDate,
                                                  String SSN,
                                                  String partialSSN,
                                                  String cccId,
                                                  String schoolAssignedStudentId,
                                                  String emailAddress
                                                  ) {
        CollegeTranscript transcript = null;

        try {
            transcript = (CollegeTranscript) this.createTranscriptUnmarshaller(false, true).unmarshal(
                    new ClassPathResource("mock/college-transcript.json").getInputStream()
            );

            transcript.getStudent().getPerson().getName().setFirstName(firstName);
            transcript.getStudent().getPerson().getName().setLastName(lastName);
            transcript.getStudent().getPerson().getBirth().setBirthDate(birthDate);

            if (StringUtils.isNotEmpty(SSN)) {
                transcript.getStudent().getPerson().setSSN(SSN);
            }

            if (StringUtils.isNotEmpty(partialSSN)) {
                transcript.getStudent().getPerson().setPartialSSN(partialSSN);
            }

            if (StringUtils.isNotEmpty(schoolAssignedStudentId)) {
                transcript.getStudent().getPerson().setSchoolAssignedPersonID(schoolAssignedStudentId);
            }
            if (StringUtils.isNotEmpty(emailAddress)){
                transcript.getStudent().getPerson().getContacts().get(0).getEmails().get(0).setEmailAddress(emailAddress);
            }
            if (StringUtils.isNotEmpty(cccId)) {
                transcript.getStudent().getPerson().setAgencyAssignedID(cccId);
                CollegeTranscriptBuilder builder = new CollegeTranscriptBuilder(this.getCccJAXBContext());
                transcript.getStudent().getPerson().getAgencyIdentifiers().add(
                        builder.createAgencyIdentifier(cccId,"CCC Technology Center",
                                AgencyCodeType.STATE,CountryCodeType.US, StateProvinceCodeType.CA)
                );

            }

        }
        catch (Exception e) {
            //This should never happen.
        }

        return transcript;
    }

    public OutputStream getMockTranscript() throws ParseException, JAXBException, SAXException {

        CollegeTranscriptBuilder builder = new CollegeTranscriptBuilder(cccJAXBContext);


        builder.setHighSchoolName("McClatchy High School");
        SchoolCode hsSchoolCode = new SchoolCode();
        hsSchoolCode.setCode("052705");
        hsSchoolCode.setCodeType(SchoolCodeTypes.CEEBACT);
        builder.addHighSchoolCode(hsSchoolCode);
        builder.setSchoolAssignedStudentID("0");

        //Person;
        builder.setStudentEmailAddress("john.doe@edexchange.edu");
        builder.setStudentPhoneNumber("0000000000");
        builder.setStudentPartialSSN("0000");
        builder.setStudentBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1995-01-01"));
        builder.setStudentSSN("000000000");

        builder.setStudentFirstname("John");
        builder.setStudentLastName("Doe");

        //Student address

        builder.addStudentAddressLine("2406 Capitol Ave. #4");

        builder.setStudentCity("Sacramento");
        builder.setStudentStateOrProvince("CA");
        builder.setStudentZipCode("95816");


        com.ccctc.sector.academicrecord.v1_9.AcademicRecordType academicRecord = builder.createAcademicRecord(com.ccctc.core.coremain.v1_14.StudentLevelCodeType.COLLEGE_SOPHOMORE);

        AcademicSessionType session = builder.createAcademicSession(com.ccctc.core.coremain.v1_14.StudentLevelCodeType.COLLEGE_SOPHOMORE);

        //School that owns the academic record.
        academicRecord.setSchool(builder.createSchool("Sacramento City College", "001233", SchoolCodeTypes.FICE));

        //Build academic session
        academicRecord.getAcademicSessions().add(session);

        //Build academic program
        session.getAcademicPrograms().add(
                builder.createAcademicProgram("Computer and Information Sciences, General",
                        com.ccctc.core.coremain.v1_14.AcademicProgramTypeType.MAJOR,
                        "11.0101", AcademicProgramCodes.ProgramCIPCode)
        );

        com.ccctc.sector.academicrecord.v1_9.AcademicSummaryFType sessionSummary = builder.createAcademicSummaryF();

        //Build academic summary for the session.

        sessionSummary.setAcademicSummaryLevel(com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION);
        sessionSummary.setAcademicSummaryType(com.ccctc.core.coremain.v1_14.AcademicSummaryTypeType.SENDER_ONLY);

        sessionSummary.setGPA(builder.constructGpa(13D, 13D, AcademicGPACreditUnits.Semester, 52D, 47D, 0D, 4D));

        sessionSummary.getAcademicHonors().add(builder.constructHonors(HonorsRecognitionLevelType.SECOND_HIGHEST, "Dean's List"));

        session.getAcademicSummaries().add(sessionSummary);


        com.ccctc.sector.academicrecord.v1_9.CourseType course = builder.constructCourse(
                com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                CreditCourseUnitTypes.Semester,
                com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                4.0D,
                4.0D,
                "25",
                "A",
                16.0D,
                AcademicCourseCodes.CourseCIPCode,
                "11.0101",
                CourseGPAApplicabilityCodeType.APPLICABLE,
                "CISC",
                "360",
                "Information Technology 360",
                null
        );


        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        4.0D,
                        4.0D,
                        "25",
                        "A",
                        16.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "11.0101",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "CISC",
                        "360",
                        "Information Technology 360",
                        null
                )
        );

        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        4.0D,
                        4.0D,
                        "25",
                        "A",
                        16.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "11.0201",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "CISP",
                        "301",
                        "Algorithm Design and 4 Units Implementation",
                        null
                )
        );

        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        5.0D,
                        5.0D,
                        "25",
                        "B",
                        15.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "27.0102",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "MATH",
                        "120",
                        "Intermediate Algebra",
                        null
                )
        );


        Calendar schoolCalendar = Calendar.getInstance();
        schoolCalendar.add(Calendar.YEAR, -2);
        schoolCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        schoolCalendar.set(Calendar.DATE, 1);

        Date begin =  schoolCalendar.getTime();
        schoolCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        Date end = schoolCalendar.getTime();
        session.setAcademicSessionDetail(builder.constructSessionDetail(begin, end, "Fall Semester " + schoolCalendar.get(Calendar.YEAR), null, SessionTypeType.SEMESTER));


        //prior session...

        session = builder.createAcademicSession(com.ccctc.core.coremain.v1_14.StudentLevelCodeType.COLLEGE_SOPHOMORE);


        //Build academic program
        session.getAcademicPrograms().add(
                builder.createAcademicProgram("Computer and Information Sciences, General",
                        com.ccctc.core.coremain.v1_14.AcademicProgramTypeType.MAJOR,
                        "11.0101", AcademicProgramCodes.ProgramCIPCode)
        );


        sessionSummary = builder.createAcademicSummaryF();

        //Build academic summary for the session.
        sessionSummary.setAcademicSummaryLevel(com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION);
        sessionSummary.setAcademicSummaryType(com.ccctc.core.coremain.v1_14.AcademicSummaryTypeType.SENDER_ONLY);

        sessionSummary.setGPA(builder.constructGpa(15D, 15D, AcademicGPACreditUnits.Semester, 60D, 60D, 0D, 4D));

        sessionSummary.getAcademicHonors().add(builder.constructHonors(HonorsRecognitionLevelType.FIRST_HIGHEST, "Dean's List"));

        session.getAcademicSummaries().add(sessionSummary);

        //Courses

        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        5.0D,
                        5.0D,
                        "25",
                        "A",
                        20.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "27.0102",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "MATH",
                        "100",
                        "Elementary Algebra",
                        null
                )
        );


        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        3.0D,
                        3.0D,
                        "25",
                        "A",
                        12.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "11.0801",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "CISW",
                        "304",
                        "Cascading Style Sheets",
                        null
                )
        );


        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        3.0D,
                        3.0D,
                        "25",
                        "A",
                        12.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "11.0801",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "CISW",
                        "320",
                        "Introduction to Web Site",
                        null
                )
        );

        session.getCourses().add(
                builder.constructCourse(
                        com.ccctc.core.coremain.v1_14.CourseCreditBasisType.REGULAR,
                        CreditCourseUnitTypes.Semester,
                        com.ccctc.core.coremain.v1_14.CourseCreditLevelType.LOWER_DIVISION,
                        com.ccctc.core.coremain.v1_14.CourseLevelType.LOWER_DIVISION,
                        4.0D,
                        4.0D,
                        "25",
                        "A",
                        16.0D,
                        AcademicCourseCodes.CourseCIPCode,
                        "11.0801",
                        CourseGPAApplicabilityCodeType.APPLICABLE,
                        "CISW",
                        "400",
                        "Client-side Web Scripting",
                        null
                )
        );


        schoolCalendar = Calendar.getInstance();
        schoolCalendar.add(Calendar.YEAR, -1);
        schoolCalendar.set(Calendar.MONTH, Calendar.JANUARY);
        schoolCalendar.set(Calendar.DATE, 15);

        begin =  schoolCalendar.getTime();
        schoolCalendar.set(Calendar.MONTH, Calendar.JUNE);
        end = schoolCalendar.getTime();

        session.setAcademicSessionDetail(builder.constructSessionDetail(begin, end, "Spring Semester " + schoolCalendar.get(Calendar.YEAR), null, SessionTypeType.SEMESTER));


        academicRecord.getAcademicSessions().add(session);

        builder.getAcademicRecords().add(academicRecord);

        com.ccctc.message.collegetranscript.v1_6.CollegeTranscript transcript = builder.build();
        transcript.setTransmissionData(builder.createTransmissionDataType());

        return toJSONStream(transcript);
    }

}
