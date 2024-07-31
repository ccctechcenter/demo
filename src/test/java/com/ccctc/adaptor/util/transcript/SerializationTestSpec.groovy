package com.ccctc.adaptor.util.transcript

import com.ccctc.adaptor.model.transcript.*
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.core.coremain.v1_14.AcademicDegreeRequirementType
import com.ccctc.core.coremain.v1_14.AcademicHonorsType
import com.ccctc.core.coremain.v1_14.AcademicProgramTypeType
import com.ccctc.core.coremain.v1_14.AcademicSummaryTypeType
import com.ccctc.core.coremain.v1_14.AgencyCodeType
import com.ccctc.core.coremain.v1_14.CountryCodeType
import com.ccctc.core.coremain.v1_14.CourseApplicabilityType
import com.ccctc.core.coremain.v1_14.CourseCreditBasisType
import com.ccctc.core.coremain.v1_14.CourseCreditLevelType
import com.ccctc.core.coremain.v1_14.CourseGPAApplicabilityCodeType
import com.ccctc.core.coremain.v1_14.CourseLevelType
import com.ccctc.core.coremain.v1_14.CourseRepeatCodeType
import com.ccctc.core.coremain.v1_14.DocumentTypeCodeType
import com.ccctc.core.coremain.v1_14.GPAType
import com.ccctc.core.coremain.v1_14.StateProvinceCodeType
import com.ccctc.core.coremain.v1_14.TransmissionTypeType
import com.ccctc.core.coremain.v1_14.impl.UserDefinedExtensionsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.OrganizationType
import com.ccctc.core.coremain.v1_14.SessionTypeType
import com.ccctc.core.coremain.v1_14.StudentLevelCodeType
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript
import com.ccctc.sector.academicrecord.v1_9.*
import com.google.common.collect.Lists
import com.xap.ccctran.AcademicRecordCCCExtensions
import com.xap.ccctran.CourseCCCExtensions
import org.eclipse.persistence.jaxb.JAXBContext
import org.eclipse.persistence.jaxb.JAXBContextFactory
import org.eclipse.persistence.jaxb.MarshallerProperties
import spock.lang.Specification

import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import java.text.DateFormat
import java.text.SimpleDateFormat

import static com.ccctc.core.coremain.v1_14.HonorsRecognitionLevelType.*

/**
 * Created by james on 4/13/17.
 */
class SerializationTestSpec extends Specification {

    public final static String note = "Lorem ipsum dolor sit amet, consectetur " +
            "adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna " +
            "aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
            "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit " +
            "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint " +
            "occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim " +
            "id est laborum.";

    def "testNoteMessageSplit" () {
        setup:

        List<String> stringList = CollegeTranscriptBuilder.splitEqually(note, 80);

        expect:
        stringList.size() == 6;
    }

    def "testJSONSerializationOfJAXBPESCCollegeTranscript" () {
        setup:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );
        Unmarshaller unmarshaller = transcriptService.createTranscriptUnmarshaller(false, false);

        Object object = unmarshaller.unmarshal(getClass().getClassLoader().getResourceAsStream("college-transcript.xml"));
        Marshaller marshaller = transcriptService.createTranscriptMarshaller(true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        marshaller.marshal(object, outputStream);
        System.out.print(outputStream.toString());
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        transcriptService.json(unmarshaller);


        when:

        object = unmarshaller.unmarshal(inputStream);
        CollegeTranscript collegeTranscript = (CollegeTranscript)object;

        then:
        object instanceof CollegeTranscript
        collegeTranscript.getStudent().getPerson().getName().getFirstName().equals("Test")


    }

    def "testSerializationOfGeneratedTranscript" ()  {
        setup:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        Unmarshaller unmarshaller = transcriptService.createTranscriptUnmarshaller(false, false);

        CollegeTranscript transcript = createTranscript();
        transcript.setTransmissionData(buildTransmissionData());

        Marshaller marshaller = transcriptService.createTranscriptMarshaller(false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        marshaller.marshal(transcript, outputStream);
        System.out.print(outputStream.toString());
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ValidationUtils.validateDocument(inputStream, XmlFileType.COLLEGE_TRANSCRIPT, XmlSchemaVersion.V1_6_0);
        inputStream =  new ByteArrayInputStream(outputStream.toByteArray());

        try {
            ValidationUtils.validateDocument(inputStream, XmlFileType.HIGH_SCHOOL_TRANSCRIPT, XmlSchemaVersion.V1_5_0);
            fail();
        } catch (Exception e) {
            e.getMessage();
        }

        inputStream =  new ByteArrayInputStream(outputStream.toByteArray());

        when:
        Object object = unmarshaller.unmarshal(inputStream);
        CollegeTranscript collegeTranscript = (CollegeTranscript)object;

        then:
        object instanceof CollegeTranscript
        collegeTranscript.getStudent().getPerson().getName().getFirstName().equals("John")

    }

    def "setSchoolCode" (schoolCodes, organizationType) {
        for(SchoolCodeTypes schoolCodeType: SchoolCodeTypes.values()) {
            switch (schoolCodeType) {
                case SchoolCodeTypes.ACT:
                    organizationType.setACT(schoolCodes.get(schoolCodeType));
                    break;
                case SchoolCodeTypes.ATP:
                    organizationType.setATP(schoolCodes.get(schoolCodeType));
                    break;
                case SchoolCodeTypes.FICE:
                    organizationType.setFICE(schoolCodes.get(schoolCodeType));
                    break;
                case SchoolCodeTypes.IPEDS:
                    organizationType.setIPEDS(schoolCodes.get(schoolCodeType));
                    break;
                case SchoolCodeTypes.OPEID:
                    organizationType.setOPEID(schoolCodes.get(schoolCodeType));
                    break;
                case SchoolCodeTypes.CEEBACT:
                    organizationType.setOPEID(schoolCodes.get(schoolCodeType));
                    break;
                default:
                    throw new IllegalStateException(schoolCodeType + " is not supported");
            }
        }
    }

    def "createSourceDestinationType" (transcriptBuilder,
                                       organizationNames,
                                       organizationAddressLines,
                                       organizationCity,
                                       organizationStateProvinceCode,
                                       organizationStateProvince,
                                       organizationPostalCode,
                                       organizationCountryCode,
                                       schoolCodes,
                                       phone, email){

        SourceDestinationType sourceDestination = transcriptBuilder.createSourceDestinationType();

        OrganizationType organization = transcriptBuilder.createOrganizationType();

        sourceDestination.setOrganization(organization);
        setSchoolCode(schoolCodes, organization);
        organization.getOrganizationNames().addAll(organizationNames);
        ContactsType contact = transcriptBuilder.createContactsType();
        organization.getContacts().add(contact);

        if(organizationAddressLines) {
            AddressType address = transcriptBuilder.createAddressType();
            contact.getAddresses().add(address);
            address.getAddressLines().addAll(organizationAddressLines);
            address.setCity(organizationCity);
            if (organizationStateProvinceCode != null) {
                address.setStateProvinceCode(StateProvinceCodeType.fromValue(organizationStateProvinceCode));//domestic only (required for domestic)
            } else {
                address.setStateProvince(organizationStateProvince);//international only (optional for international)
                address.setCountryCode(CountryCodeType.fromValue(organizationCountryCode));//international only (required for international)
            }
            address.setPostalCode(organizationPostalCode);
        }
        if(phone!=null && phone.getPhoneNumber()){
            contact.getPhones().add(phone);
        }
        if(email){
            def emailType = transcriptBuilder.createEmailType();
            contact.getEmails().add(emailType);
            emailType.setEmailAddress(email);
        }
        return sourceDestination;
    }


    def "buildTransmissionData" () {


        CollegeTranscriptBuilder transcriptBuilder = new CollegeTranscriptBuilder(JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null));

        TransmissionDataType transmissionData = transcriptBuilder.createTransmissionDataType();
        transmissionData.setDocumentID("1");
        transmissionData.setCreatedDateTime(new Date());
        transmissionData.setDocumentTypeCode(DocumentTypeCodeType.REQUESTED_RECORD);
        transmissionData.setTransmissionType(TransmissionTypeType.ORIGINAL);
        transmissionData.setRequestTrackingID("1");

        List<String> destinationOrganizationNames = Lists.newArrayList();
        List<String> sourceOrganizationNames = Lists.newArrayList();
        destinationOrganizationNames.add("Test School 1");
        sourceOrganizationNames.add("Test School 2");

        Map<SchoolCodeTypes, String> schoolCodesMap = new HashMap<SchoolCodeTypes, String>();
        schoolCodesMap.put(SchoolCodeTypes.FICE, "008073");

        PhoneType phone = transcriptBuilder.createPhoneType();
        phone.setAreaCityCode("000");
        phone.setPhoneNumber("0000000");


        SourceDestinationType source = createSourceDestinationType(transcriptBuilder,
                sourceOrganizationNames, "5000 J Street", "Sacramento", "CA", null, "95826", "US", schoolCodesMap, phone, "test@test.com");
        transmissionData.setSource(source);

        SourceDestinationType destination =  createSourceDestinationType(transcriptBuilder, destinationOrganizationNames, "1000 Capital Mall", "Oxford", "CA", "CA", "UK", null, schoolCodesMap, null, null);
        transmissionData.setDestination(destination);
        /**
         <UserDefinedExtensions>
         <ns2:DocumentInfo>
         <ns2:FileName>2f7c4e21337341918d8a69ddaa15e6db_document.pdf</ns2:FileName>
         <ns2:DocumentType>Letter of Recommendation - Teacher</ns2:DocumentType>
         <ns2:ExchangeType>PESC XML Document Request and PDF or XML</ns2:ExchangeType>
         </ns2:DocumentInfo>
         </UserDefinedExtensions>
         </TransmissionData>
         */

        return transmissionData;


    }


    def "setJSONProperties" (unmarshaller) {
        unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
    }

    def "createJSONMarshaller" (jc) {

        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);

        return marshaller;
    }


    def "createTranscript" () {
        CollegeTranscriptBuilder transcriptBuilder = new CollegeTranscriptBuilder(JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null));

        /***Student.Person***/
        transcriptBuilder.setStudentFirstname("John");
        transcriptBuilder.setStudentMiddleName("James");
        transcriptBuilder.setStudentLastName("Doe");
        transcriptBuilder.setStudentNamePrefix("Mr.");
        transcriptBuilder.setStudentNameSuffix("III");
        //transcriptBuilder.setStudentCountry("US");
        transcriptBuilder.setAttentionLine("Mr. John Doe");
        transcriptBuilder.setStudentSSN("000000000");
        transcriptBuilder.setStudentPartialSSN("0000");
        transcriptBuilder.setSchoolAssignedStudentID("0");

        Calendar c = Calendar.getInstance();
        c.set(1995, 0, 1, 0, 0, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        transcriptBuilder.setStudentBirthDate(c.getTime());
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("052705", SchoolCodeTypes.CEEBACT));
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("000000", SchoolCodeTypes.ATP));
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("0000", SchoolCodeTypes.ACT));
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("000000", SchoolCodeTypes.FICE));
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("000000", SchoolCodeTypes.IPEDS));
        transcriptBuilder.getHighSchoolCodes().add(new SchoolCode("000000", SchoolCodeTypes.OPEID));
        transcriptBuilder.setHighSchoolName("McClatchy High School");
        transcriptBuilder.setHighSchoolGraduationDate("20110605")
        transcriptBuilder.getStudentAddressLines().add("2406 Capitol Ave. #4");
        transcriptBuilder.setStudentCity("Sacramento");
        // transcriptBuilder.getAddress().setCountryCode(CountryCodeType.US);
        transcriptBuilder.setStudentZipCode("95861");
        transcriptBuilder.setStudentStateOrProvince(StateProvinceCodeType.CA.name());
        transcriptBuilder.setStudentGender(transcriptBuilder.createGenderCodeType("M"))

       
        transcriptBuilder.setStudentAreaCode("916");
        transcriptBuilder.setStudentPhoneNumber("3837016");
        transcriptBuilder.setStudentEmailAddress("john.doe@ccctechcenter.org");

        transcriptBuilder.setStudentBirthCity("Sacramento")
        transcriptBuilder.setStudentBirthCountry(CountryCodeType.US)
        transcriptBuilder.setStudentBirthStateProvince(StateProvinceCodeType.CA)


        transcriptBuilder.setStudentResidentStateOrProvince("CA");

        //Academic records
        /***Student.AcademicRecord***/
        /*
            AcademicRecord element may be repeated. There must be at least one occurrence which normally
            represents the record from the sending institution; additional occurrences may be included for transferred work or
            secondary school data.

            There should be an academic record for each institution that the student has attended.
         */
        //Sections:  School,StudentLevel,AcademicAward,AcademicSummary,AcademicSession,Course

        /***Student.AcademicRecord.School***/
        //IMPORTANT NOTE: Set school when the sending school is NOT the owner of the academic record.

        SchoolType recordHolderSchool = transcriptBuilder.createSchool("Butte College", "008073", SchoolCodeTypes.FICE);
        transcriptBuilder.addSchoolCodeToOrganization(recordHolderSchool, "0000", SchoolCodeTypes.ACT);
        transcriptBuilder.addSchoolCodeToOrganization(recordHolderSchool, "000000", SchoolCodeTypes.ATP);
        transcriptBuilder.addSchoolCodeToOrganization(recordHolderSchool, "000000", SchoolCodeTypes.CEEBACT);
        transcriptBuilder.addSchoolCodeToOrganization(recordHolderSchool, "000000", SchoolCodeTypes.IPEDS);
        transcriptBuilder.addSchoolCodeToOrganization(recordHolderSchool, "000000", SchoolCodeTypes.OPEID);

        //transcriptBuilder.constructCourse()
        AcademicRecordType academicRecord = transcriptBuilder.createAcademicRecord(StudentLevelCodeType.COLLEGE_FIRST_YEAR);
        academicRecord.setSchool(recordHolderSchool);

        /***Student.AcademicRecord.StudentLevel ***/

        /*** Student.AcademicRecord.AcademicAward ***/
        //AcademicAward in this position is not tied to an AcademicSession. Some institutions may wish to indicate
        //AcademicAward in the AcademicSession in which it was earned so academic award can also be included in the academic session(s)
        //completed by the student.

        DateFormat dataParser = new SimpleDateFormat("MM/DD/YYYY");

        //AcademicAward.AcademicAwardProgram to indicate degree major;
        com.ccctc.sector.academicrecord.v1_9.AcademicProgramType academicAwardProgram = transcriptBuilder.createAcademicProgram("Computer and Information Sciences, General", AcademicProgramTypeType.MAJOR, "11.0101", AcademicProgramCodes.ProgramCIPCode);


        //Specify the student's degree major GPA and honors. I.e the student's GPA and honors within the award program.
        AcademicSummaryE2Type programAcademicSummary = transcriptBuilder.createAcademicSummaryE2();
        programAcademicSummary.setAcademicSummaryType(AcademicSummaryTypeType.SENDER_ONLY);
        programAcademicSummary.setAcademicSummaryLevel(CourseCreditLevelType.LOWER_DIVISION);
        academicAwardProgram.setProgramSummary(programAcademicSummary);

        GPAType gpa = transcriptBuilder.constructGpa(13D, 13D, AcademicGPACreditUnits.Units, 52D, 47D, null, null);
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.GPA
        programAcademicSummary.setGPA(gpa);

        AcademicHonorsType honors = transcriptBuilder.constructHonors(FIRST_HIGHEST, "Dean's List");
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.AcademicHonors
        programAcademicSummary.getAcademicHonors().add(honors);


        //Baccalaureate Degree 2.4
        AcademicAwardType academicAward = transcriptBuilder.createAcademicAward("Baccalaureate Degree", "2.4", dataParser.parse("06/01/2012"),
                true, null);

        academicAward.getAcademicAwardPrograms().add(academicAwardProgram);

        AcademicDegreeRequirementType degreeRequirement = transcriptBuilder.createDegreeRequirement("Dave Wilson","Sorting algorithm efficiency" );
        //Student.AcademicRecord.AcademicAward.AcademicDegreeRequirement
        academicAward.getAcademicDegreeRequirements().add(degreeRequirement);


        //Student.AcademicRecord.AcademicAward.AcademicSummary
        //We've already specifified the GPA and honors related to the studen's award program (e.g. Computer Science), but
        //we still need to provide a summmary, which includes the GPA and honors for the student's overall degree, including
        //all courses and not just those that are related to the program.

        AcademicSummaryE1Type awardAcademicSummary = transcriptBuilder.createAcademicSummaryE1(AcademicSummaryTypeType.SENDER_ONLY);
        academicAward.getAcademicSummaries().add(awardAcademicSummary);

        gpa = transcriptBuilder.constructGpa(13D, 13D, AcademicGPACreditUnits.Semester, 52D, 47D, 0D, 4D);
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.GPA
        awardAcademicSummary.setGPA(gpa);
        honors = transcriptBuilder.constructHonors(THIRD_HIGHEST, "Cum Laude");
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.AcademicHonors
        awardAcademicSummary.getAcademicHonors().add(honors);

        academicRecord.getAcademicAwards().add(academicAward);


        /*** Student.AcademicRecord.AcademicSummary ***/
        //AcademicSummary in this position is not tied to an AcademicSession. Some institutions may wish to
        //indicate AcademicSummary in the AcademicSession in which it was earned.
        //In other words, this is the academic summary that covers all the studennt's work across all
        //sessions.

        AcademicSummaryFType academicRecordSummary = transcriptBuilder.createAcademicSummaryF();

        academicRecordSummary.setAcademicSummaryLevel(CourseCreditLevelType.LOWER_DIVISION);
        academicRecordSummary.setAcademicSummaryType(AcademicSummaryTypeType.SENDER_ONLY);

        academicRecordSummary.setGPA(transcriptBuilder.constructGpa(13D, 13D, AcademicGPACreditUnits.Semester, 52D, 47D, 0D, 4D));

        academicRecordSummary.getAcademicHonors().add(transcriptBuilder.constructHonors(SECOND_HIGHEST, "Dean's List"));


        gpa =transcriptBuilder.constructGpa(13D, 13D, AcademicGPACreditUnits.Semester, 52D, 47D, 0D, 4D);
        academicRecordSummary.setGPA(gpa);

        academicRecordSummary.getAcademicHonors().add(transcriptBuilder.constructHonors(FIRST_HIGHEST, "Suma Cum Laude"));


        //This is the appropriate place to list the student’s current major or degree objective if the award has not yet been granted.
        //TODO: encapusulate academic award program creation.
        //Create unused program to get better code coverage.
        com.ccctc.sector.academicrecord.v1_9.AcademicProgramType majorProgram = transcriptBuilder.createAcademicProgram("Computer Science",
                AcademicProgramTypeType.MAJOR,
                "11101",
                AcademicProgramCodes.ProgramESISCode);

        majorProgram = transcriptBuilder.createAcademicProgram("Computer Science",
                AcademicProgramTypeType.MAJOR,
                "11101",
                AcademicProgramCodes.ProgramHEGISCode);


        //Create unused program to get better code coverage.
        majorProgram = transcriptBuilder.createAcademicProgram("Computer Science",
                AcademicProgramTypeType.MAJOR,
                "11101",
                AcademicProgramCodes.ProgramUSISCode);

        majorProgram = transcriptBuilder.createAcademicProgram("Computer Science",
                AcademicProgramTypeType.MAJOR,
                "11101",
                AcademicProgramCodes.ProgramCSISCode);


        academicRecordSummary.setAcademicProgram(majorProgram);

        academicRecord.getAcademicSummaries().add(academicRecordSummary);

        transcriptBuilder.setAgencyAssignedID((String)"000")

        transcriptBuilder.addAgencyIdentifier((String)"000", "CCC Technology Center", AgencyCodeType.STATE, CountryCodeType.US, StateProvinceCodeType.CA)

        /*** Student.AcademicSession ***/
        /* Course, award and summary information related to a specific time period. Typically arranged into
         * successive quarters or semesters and is the preferred method for conveying academic coursework.
         */


        AcademicSessionType session = transcriptBuilder.createAcademicSession(StudentLevelCodeType.COLLEGE_FIRST_YEAR);

        //Simulate 2 years, each with 2 semesters.
        Calendar schoolCalendar = Calendar.getInstance();
        Date endDate =  schoolCalendar.getTime();

        schoolCalendar.add(Calendar.YEAR, -2);
        schoolCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
        Date startDate = schoolCalendar.getTime();

        session.setAcademicSessionDetail(transcriptBuilder.constructSessionDetail(
                startDate, endDate, "Fall Semester " + schoolCalendar.get(Calendar.YEAR), '01', SessionTypeType.SEMESTER));

        //Student.AcademicRecord.AcademicSession.StudentLevel
        session.setStudentLevel(transcriptBuilder.constructStudentLevelType(StudentLevelCodeType.COLLEGE_FIRST_YEAR, note));

        com.ccctc.sector.academicrecord.v1_9.AcademicProgramType sessionProgram = transcriptBuilder.createAcademicProgram("Computer Science",
                AcademicProgramTypeType.MAJOR, "11.1101", AcademicProgramCodes.ProgramESISCode);


        session.setAcademicSessionDetail(transcriptBuilder.constructSessionDetail(
                startDate, endDate, "Spring Semester " + schoolCalendar.get(Calendar.YEAR), "02", SessionTypeType.SEMESTER));


        //Optionally add a program summary...
        programAcademicSummary = transcriptBuilder.createAcademicSummaryE2();
        programAcademicSummary.setAcademicSummaryType(AcademicSummaryTypeType.SENDER_ONLY);
        programAcademicSummary.setAcademicSummaryLevel(CourseCreditLevelType.UNDERGRADUATE);

        sessionProgram.setProgramSummary(programAcademicSummary);

        gpa = transcriptBuilder.constructGpa(12, 12, AcademicGPACreditUnits.Semester, 45D, 45D, 0D, 12D);
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.GPA
        programAcademicSummary.setGPA(gpa);

        honors = transcriptBuilder.constructHonors(THIRD_HIGHEST, "Cum Laude");
        //Student.AcademicRecord.AcademicAward.AcademicAwardProgram.ProgramSummary.AcademicHonors
        programAcademicSummary.getAcademicHonors().add(honors);

        //Academic awards associated with this session.
        // sessionBuilder.setAcademicAward();

        //Add courses taken in this session;

        CourseType course = transcriptBuilder.constructCourse(
                CourseCreditBasisType.REGULAR,
                CreditCourseUnitTypes.Semester,
                CourseCreditLevelType.LOWER_DIVISION,
                CourseLevelType.LOWER_DIVISION,
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
                "1"
        );

        course.setCourseRepeatCode(CourseRepeatCodeType.fromValue(transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)'M')))
        course.setCourseRepeatCode(CourseRepeatCodeType.fromValue(transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)'I')))
        course.setCourseRepeatCode(CourseRepeatCodeType.fromValue(transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)'T')))
        course.setCourseApplicability(CourseApplicabilityType.fromValue(transcriptBuilder.convertCCCASCIICoureApplicabilityToPESC((char)'N')))
        course.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC("CE"))


        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 8);
        c.set(Calendar.DATE, 1);
        course.setCourseBeginDate(c.getTime());

        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DATE, 7);
        course.setCourseEndDate(c.getTime());

        session.getCourses().add(course);

        course = transcriptBuilder.constructCourse(
                CourseCreditBasisType.REGULAR,
                CreditCourseUnitTypes.Semester,
                CourseCreditLevelType.LOWER_DIVISION,
                CourseLevelType.LOWER_DIVISION,
                4.0D,
                4.0D,
                "25",
                "A",
                16.0D,
                AcademicCourseCodes.CourseCSISCode,
                "11.0101",
                CourseGPAApplicabilityCodeType.APPLICABLE,
                "CISC",
                "340",
                "Programming in C",
                null
        );

        course.setCourseRepeatCode(CourseRepeatCodeType.fromValue(transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)'D')))
        course.setCourseApplicability(CourseApplicabilityType.fromValue(transcriptBuilder.convertCCCASCIICoureApplicabilityToPESC((char)'Y')))
        course.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC("AP"))
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 8);
        c.set(Calendar.DATE, 1);
        course.setCourseBeginDate(c.getTime());

        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DATE, 7);
        course.setCourseEndDate(c.getTime());


        CourseCCCExtensions courseCCCExtensions = transcriptBuilder.createCourseExtensions()

        course.userDefinedExtensions = new UserDefinedExtensionsTypeImpl(courseCCCExtensions:courseCCCExtensions )

        session.getCourses().add(course)


        course = transcriptBuilder.constructCourse(
                CourseCreditBasisType.REGULAR,
                CreditCourseUnitTypes.Semester,
                CourseCreditLevelType.LOWER_DIVISION,
                CourseLevelType.LOWER_DIVISION,
                4.0D,
                4.0D,
                "25",
                "A",
                16.0D,
                AcademicCourseCodes.CourseUSISCode,
                "11.0101",
                CourseGPAApplicabilityCodeType.APPLICABLE,
                "CISC",
                "101",
                "Understanding Software",
                null
        );
        course.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC("AR"))
        course.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC("HS"))
        course.setCourseCreditBasis(transcriptBuilder.convertCCCASCIICourseCreditBasisToPESC("22"))
        course.setCourseRepeatCode(CourseRepeatCodeType.fromValue(transcriptBuilder.convertCCCASCIIRepeatStatusToPESC((char)'R')))
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 8);
        c.set(Calendar.DATE, 1);
        course.setCourseBeginDate(c.getTime());

        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DATE, 7);
        course.setCourseEndDate(c.getTime());


        session.getCourses().add(course);

        academicRecord.getAcademicSessions().add(session);

        AcademicRecordCCCExtensions academicRecordCCCExtensions = transcriptBuilder.createAcademicRecordExtensions()

        AcademicRecordCCCExtensions.GradeSchemeEntry gradeSchemeEntry = transcriptBuilder.createGradeSchemeEntry()

        gradeSchemeEntry.setDescription("Test")
        gradeSchemeEntry.setGrade("A")
        gradeSchemeEntry.setGradePoints(new BigDecimal((double)4.0d))
        gradeSchemeEntry.setEDIGradeQualifier("Y")
        gradeSchemeEntry.setAttemptedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.AttemptedUnitsCalcType.fromValue(transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)'Y')))
        gradeSchemeEntry.setEarnedUnitsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.EarnedUnitsCalcType.fromValue(transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)'B')))
        gradeSchemeEntry.setGradePointsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.fromValue(transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)'C')))
        gradeSchemeEntry.setGradePointsCalc(AcademicRecordCCCExtensions.GradeSchemeEntry.GradePointsCalcType.fromValue(transcriptBuilder.convertAttemptedUnitsCalcFROMCCCASCIIToPESC((char)'N')))


        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
        gradeSchemeEntry.setEffectiveFromDate(format.parse((String)"20000101"))
        gradeSchemeEntry.setEffectiveToDate(format.parse((String)"20200101"))

        academicRecordCCCExtensions.getGradeSchemeEntries().add(gradeSchemeEntry)
        academicRecord.userDefinedExtensions = new UserDefinedExtensionsTypeImpl(academicRecordCCCExtensions: academicRecordCCCExtensions )

        /*** Student.Course ***/
        //Academic coursework not related to a specific academic session.


        transcriptBuilder.getAcademicRecords().add(academicRecord);

        return transcriptBuilder.build()
    }
}
