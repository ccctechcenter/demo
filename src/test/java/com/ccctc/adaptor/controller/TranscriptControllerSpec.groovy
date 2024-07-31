package com.ccctc.adaptor.controller

import com.ccctc.adaptor.util.GroovyService
import com.ccctc.adaptor.util.impl.TranscriptService
import org.eclipse.persistence.jaxb.JAXBContext
import org.eclipse.persistence.jaxb.JAXBContextFactory
import com.ccctc.message.collegetranscript.v1_6.impl.CollegeTranscriptImpl
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.StreamUtils
import org.springframework.web.util.NestedServletException
import spock.lang.Specification

import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller
import java.nio.charset.Charset

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by Rasul on 04/20/17.
 */
class TranscriptControllerSpec extends Specification {


    def "Transcript Controller"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil,
                transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&studentId=1&firstName=John&lastName=Doe&birthDate=1995-01-01&SSN=000000000&partialSSN=0000&emailAddress=john.doe@test.com').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> new CollegeTranscriptImpl()
        result.andExpect(status().isOk())
    }

    def "Transcript Controller no student id"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&firstName=John&lastName=Doe&birthDate=1995-01-01&SSN=000000000&partial-SSN=0000&email-address=john.doe@test.com').contentType(APPLICATION_JSON))

        then:
        1 * groovyUtil.run(_,_,_,_) >> new CollegeTranscriptImpl()
        result.andExpect(status().isOk())
    }

    def "Transcript Controller cccid parameter"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&cccid=ABC1234').contentType(APPLICATION_JSON))

        then:
        thrown NestedServletException
    }

    def "Transcript Controller missing first name parameter"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&lastName=Doe&birthDate=1995-01-01&SSN=000000000&partial-SSN=0000&email-address=john.doe@test.com').contentType(APPLICATION_JSON))

        then:
        thrown NestedServletException
    }

    def "Transcript Controller missing last name parameter"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&firstName=John&birthDate=1995-01-01&SSN=000000000&partial-SSN=0000&email-address=john.doe@test.com').contentType(APPLICATION_JSON))

        then:
        thrown NestedServletException
    }

    def "Transcript Controller missing birth date parameter"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        JAXBContext transcriptJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
        JAXBContext cccJAXBContext = (JAXBContext) JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
        TranscriptService transcriptService = new TranscriptService( transcriptJAXBContext, cccJAXBContext );

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService )
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()

        def result

        when:
        result = mockMvc.perform(get('/transcripts?mis=mis&firstName=John&lastName=Doe&SSN=000000000&partial-SSN=0000&email-address=john.doe@test.com').contentType(APPLICATION_JSON))

        then:
        thrown NestedServletException
    }

    def "Transcript Controller post"() {
        setup:
        def groovyUtil = Mock( GroovyService )
        def transcriptService = Mock( TranscriptService )
        def unmarshaller = Mock( Unmarshaller )

        transcriptService.createTranscriptUnmarshaller(false,true) >> unmarshaller

        TranscriptApiController transcriptApiController = new TranscriptApiController(groovyService: groovyUtil, transcriptService: transcriptService)
        def mockMvc = MockMvcBuilders.standaloneSetup(transcriptApiController).build()
        String transcriptText = StreamUtils.copyToString(getClass().getClassLoader().getResourceAsStream("college-transcript.json"), Charset.defaultCharset());
        def result

        // success
        when:
        result = mockMvc.perform(post('/transcripts?mis=mis').contentType(APPLICATION_JSON)
                        .content(transcriptText))

        then:
        1 * unmarshaller.unmarshal(_) >> new CollegeTranscriptImpl()
        result.andExpect(status().isNoContent())

        // JAXBException
        when:
        mockMvc.perform(post('/transcripts?mis=mis').contentType(APPLICATION_JSON).content(transcriptText))

        then:
        1 * unmarshaller.unmarshal(_) >> { throw new JAXBException("error") }
        thrown Exception
    }
}