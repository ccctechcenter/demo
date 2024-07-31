package api.colleague.base

import api.colleague.transcript.base.AcademicRecordService
import api.colleague.transcript.base.LookupService
import api.colleague.transcript.base.PersonService
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.util.ColleagueUtils
import api.colleague.util.DataUtils
import api.colleague.util.DmiDataServiceCached
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript
import com.ccctc.message.collegetranscript.v1_6.impl.CollegeTranscriptImpl
import com.ccctc.sector.academicrecord.v1_9.impl.StudentTypeImpl
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.service.DmiCTXService
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.cache.Cache
import org.springframework.core.env.Environment

import java.util.concurrent.CompletableFuture

@CompileStatic
abstract class Transcript {

    // DMI services wrapped in a class so they can easily be passed into each service
    TranscriptDmiServices transcriptDmiServices

    // services used to create the transcript
    PersonService personService
    AcademicRecordService academicRecordService
    LookupService lookupService

    /**
     * Initialize services, read properties
     */
    void colleagueInit(String misCode, Environment environment, ClassMap services) {

        def dmiService = services.get(DmiService.class)
        def dmiCTXService = services.get(DmiCTXService.class)
        def dmiDataService = services.get(DmiDataService.class)
        def cache = services.get(Cache.class)
        def dmiEntityService = services.get(DmiEntityService.class)

        def dmiDataServiceCached = new DmiDataServiceCached(dmiDataService, cache)
        def dataUtils = new DataUtils(misCode, environment, dmiService, dmiCTXService, dmiDataService, cache)

        // read configuration
        def cccIdTypes = ColleagueUtils.getColleaguePropertyAsList(environment, misCode, "ccc.id.types")

        // verify connectivity
        ColleagueUtils.keepAlive(dmiService)

        // wire in services
        transcriptDmiServices = new TranscriptDmiServices(misCode, dmiService, dmiDataService,
                dmiCTXService, dmiDataServiceCached, dmiEntityService, dataUtils, environment, cache)

        if (this.personService == null) this.personService = new PersonService(transcriptDmiServices, cccIdTypes) {}
        if (this.academicRecordService == null) this.academicRecordService = new AcademicRecordService(transcriptDmiServices) {}
        if (this.lookupService == null) this.lookupService = new LookupService(transcriptDmiServices, cccIdTypes) {}
    }


    /**
     * Get a College Transcript for a student
     *
     * @param misCode                 MIS Code
     * @param firstName               First Name
     * @param lastName                Last Name
     * @param birthDate               Birth Date
     * @param ssn                     SSN
     * @param partialSSN              Partial SSN (last 4 digits)
     * @param emailAddress            Email Address
     * @param schoolAssignedStudentID School Assigned Student ID
     * @param cccID                   CCC ID
     * @param transcriptService       Transcript Service
     * @return Transcript
     */
    CollegeTranscript get(String misCode,
                          String firstName,
                          String lastName,
                          String birthDate,
                          String ssn,
                          String partialSSN,
                          String emailAddress,
                          String schoolAssignedStudentID,
                          String cccId,
                          TranscriptService transcriptService) {

        def studentId = preProcess(misCode, firstName, lastName, birthDate, ssn, partialSSN, emailAddress, schoolAssignedStudentID, cccId, transcriptService)

        def result = read(misCode, studentId)
        result = postProcess(misCode, studentId, result)

        return result
    }

    /**
     * Post a transcript
     */
    void post(String misCode, CollegeTranscript transcript) {
        //colleagueAPIService.post("transcript/$misCode", null, transcript)
        throw new InternalServerException("Unsupported")
    }

    /**
     * Pre-processing: lookup and return student ID
     */
    protected String preProcess(String misCode,
                                String firstName,
                                String lastName,
                                String birthDate,
                                String ssn,
                                String partialSSN,
                                String emailAddress,
                                String schoolAssignedStudentID,
                                String cccId,
                                TranscriptService transcriptService) {

        if (birthDate) birthDate = (birthDate[5..6] + "/" + birthDate[8..9] + "/" + birthDate[0..3])

        def ids = lookupService.lookupStudent(firstName, lastName, birthDate, ssn, partialSSN, emailAddress, schoolAssignedStudentID, cccId)

        if (ids.size() == 1)
            return ids[0]

        if (ids.size() > 1)
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple Results Found")

        throw new EntityNotFoundException("Student Not Found")
    }

    /**
     * Post-processing: no default implementation
     */
    protected CollegeTranscript postProcess(String misCode, String studentId, CollegeTranscript transcript) {
        return transcript
    }

    /**
     * Read a transcript for a Student
     */
    protected CollegeTranscript read(String misCode, String studentId) {
        def transcript = new CollegeTranscriptImpl()
        transcript.student = new StudentTypeImpl()

        // get data in parallel
        def p = CompletableFuture.supplyAsync { personService.getPerson(studentId) }
        def p2 = CompletableFuture.supplyAsync { academicRecordService.getAcademicRecord(studentId) }

        // assign results
        transcript.student.person = p.get()
        transcript.student.getAcademicRecords() << p2.get()

        return transcript
    }
}