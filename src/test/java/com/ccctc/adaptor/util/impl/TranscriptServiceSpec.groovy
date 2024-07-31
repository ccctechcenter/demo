package com.ccctc.adaptor.util.impl

import com.ccctc.adaptor.model.transcript.SchoolCode
import com.ccctc.adaptor.model.transcript.SchoolCodeTypes
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.adaptor.util.transcript.DateConverter
import com.ccctc.adaptor.util.transcript.ValidationUtils
import com.ccctc.adaptor.util.transcript.XmlFileType
import com.ccctc.adaptor.util.transcript.XmlSchemaVersion
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript
import org.eclipse.persistence.jaxb.JAXBContext
import org.eclipse.persistence.jaxb.JAXBContextFactory
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * Created by James on 6/9/2017.
 */
class TranscriptServiceSpec extends Specification {

    def "test transcript service"() {

        given:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        expect:
        transcriptService.createTranscriptUnmarshaller(true, true) != null
        transcriptService.createTranscriptUnmarshaller(false, true) != null
        transcriptService.createTranscriptUnmarshaller(true, false) != null
        transcriptService.createTranscriptUnmarshaller(false, false) != null
        transcriptService.createTranscriptMarshaller(true) != null
        transcriptService.createTranscriptMarshaller(false) != null
    }

    def "create mock transcript"() {

        given:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null)
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null)
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext )
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd")

        expect:
        CollegeTranscript transcript = transcriptService.createMockTranscript(firstName,
                lastName,
                df.parse(birthDate),
                ssn,
                partialSSN,
                cccId,
                studentId,
                emailAddress)

        transcript.getStudent().getPerson().getName().getFirstName().equals(firstName) &&
        transcript.getStudent().getPerson().getName().getLastName().equals(lastName) &&
        df.format(transcript.getStudent().getPerson().getBirth().getBirthDate()).equals(birthDate)

        where:
        firstName << ["John", "James"]
        lastName << ["Wilcox", "Smith"]
        cccId << [null, "TYI9000"]
        birthDate << ["1978-09-06", "1995-01-01"]
        ssn << [null, "000-00-0000"]
        partialSSN << [null, "0000"]
        emailAddress << [null, "james@test.com"]
        studentId << [null, "5000000"]


    }

    def "get properties"() {
        given:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null)
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null)
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext )

        expect:
        transcriptService.getCccJAXBContext() != null
        transcriptService.getTranscriptJAXBContext() != null
    }

    def "testValidationUtils"() {
        given:
        InputStream is = getClass().getClassLoader().getResourceAsStream("college-transcript.xml");


        when:
        ValidationUtils.validateDocument(is, XmlFileType.COLLEGE_TRANSCRIPT, XmlSchemaVersion.V1_0_0);

        then:
        thrown(javax.naming.OperationNotSupportedException)

    }

    def "testMalformedCollegeTranscript"() {
        given:
        InputStream is = getClass().getClassLoader().getResourceAsStream("malformed-college-transcript.xml");


        when:
        ValidationUtils.validateDocument(is, XmlFileType.COLLEGE_TRANSCRIPT, XmlSchemaVersion.V1_6_0);

        then:
        thrown(org.xml.sax.SAXParseException)

    }

    def "testMockTranscript" () {
        given:
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        expect:
        transcriptService.getMockTranscript() != null
    }

    def "testDateConverter" () {
        given:
        com.ccctc.adaptor.util.transcript.DateConverter converter = new DateConverter();

        when:

        Date time = converter.parseTime("11:11:11")
        Date yearMonth = converter.parseYearMonth("1995-01")
        Date year = converter.parseYear("1995")
        String printableDate = converter.printDate(yearMonth)
        String printableTime = converter.printTime(time)
        String printableDateTime = converter.printDateTime(yearMonth)
        String printableYearMonth = converter.printYearMonth(yearMonth)
        yearMonth = converter.parseYearMonth("1995:01")

        then:
        time != null
        yearMonth != null
        year != null
        printableDate.startsWith("1995-01")
        printableTime.startsWith("11:11:11")
        printableDateTime.startsWith("1995-01")
        printableYearMonth.startsWith("1995-01")
        thrown(java.lang.IllegalArgumentException)
    }

    def "testSchoolCodes"() {
        given:

        SchoolCode schoolCode = new SchoolCode();

        when:
        schoolCode.setCode("000000");
        schoolCode.setCodeType(SchoolCodeTypes.FICE);

        then:
        schoolCode.getCode().equals("000000");
        schoolCode.getCodeType().equals(SchoolCodeTypes.FICE);


    }


}
