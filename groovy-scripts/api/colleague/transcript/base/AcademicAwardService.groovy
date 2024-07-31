package api.colleague.transcript.base

import api.colleague.transcript.model.AcadCredentialsRecord
import api.colleague.transcript.model.InstitutionsAttendRecord
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.transcript.model.TranscriptException
import api.colleague.transcript.util.TranscriptUtils
import api.colleague.util.ColleagueUtils
import api.colleague.util.DmiDataServiceCached
import com.ccctc.core.coremain.v1_14.impl.AcademicHonorsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.AcademicAwardType
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicAwardTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.AcademicProgramTypeImpl
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import org.ccctc.colleaguedmiclient.model.ElfTranslateTable
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiEntityService

import java.util.concurrent.CompletableFuture

@CompileStatic
abstract class AcademicAwardService  {

    // DMI Services
    TranscriptDmiServices transcriptDmiServices
    DmiEntityService dmiEntityService
    DmiDataService dmiDataService
    DmiDataServiceCached dmiDataServiceCached

    // config / translations
    String defaultHostCorpId
    CompletableFuture<ElfTranslateTable> fCastSP02
    CompletableFuture<ElfTranslateTable> fCastSP02A

    // entity classes for reading data
    Class<? extends ColleagueRecord> institutionsAttendEntity = InstitutionsAttendRecord.class

    AcademicAwardService(AcademicAwardService a) {
        this(a.transcriptDmiServices)
    }

    AcademicAwardService(TranscriptDmiServices transcriptDmiServices) {
        this.transcriptDmiServices = transcriptDmiServices
        this.dmiEntityService = transcriptDmiServices.dmiEntityService
        this.dmiDataService = transcriptDmiServices.dmiDataService
        this.dmiDataServiceCached = transcriptDmiServices.dmiDataServiceCached

        // get default host corp id, which is the institution their degrees/certificates are associated
        defaultHostCorpId = transcriptDmiServices.dataUtils.getDefaultHostCorpId()

        // load ELF translation tables asynchronously for SP02, which translate degrees/certificates to MIS codes,
        // which we can more universally translate to PESC codes
        fCastSP02 = CompletableFuture.supplyAsync { dmiDataServiceCached.elfTranslationTable("CAST.SP02") }
        fCastSP02A = CompletableFuture.supplyAsync { dmiDataServiceCached.elfTranslationTable("CAST.SP02.A") }
    }

    /**
     * Get academic awards for a person
     */
    List<AcademicAwardType> getAcademicAwards(String personId) {
        def data = dmiEntityService.readForEntity(personId + "*" + defaultHostCorpId, institutionsAttendEntity)

        if (data)
            return parseInstitutionsAttend(data)

        return null
    }

    protected ColleagueRecord readInsitutionsAttend(String personId) {
        return dmiEntityService.readForEntity(personId + "*" + defaultHostCorpId, institutionsAttendEntity)
    }

    protected List<AcademicAwardType> parseInstitutionsAttend(ColleagueRecord data) {
        if (!data instanceof InstitutionsAttendRecord)
            throw new TranscriptException("Default implementation of parseInstitutionsAttend expecting InstitutionsAttendRecord, found " + data.class.name)

        def result = []
        def acadCredentials = ((InstitutionsAttendRecord) data).instaAcadCredentials

        if (acadCredentials) {
            for (def a : acadCredentials) {
                def o = parseOne(a)
                if (o) result << o
            }
        }

        return result
    }

    protected AcademicAwardType parseOne(AcadCredentialsRecord acadCredential) {
        if (!acadCredential.acadDegree && !acadCredential.acadCcd)
            return null

        def result = new AcademicAwardTypeImpl()

        // honors
        if (acadCredential.acadHonors) {
            result.getAcademicHonors().addAll(
                    acadCredential.acadHonors
                            .findAll { it.ohonDesc != null }
                            .collect { new AcademicHonorsTypeImpl(honorsTitle: it.ohonDesc) })
        }

        // values from ACAD.PROGRAMS
        if (acadCredential.acadAcadProgram) {
            result.academicAwardTitle = acadCredential.acadAcadProgram.acpgTitle

            def p = new AcademicProgramTypeImpl()
            p.academicProgramName = acadCredential.acadAcadProgram.acpgTitle

            def cip = acadCredential.acadAcadProgram.acpgCip?.replaceAll("[^0-9]", "")
            if (cip != null && cip.length() == 6)
                p.programCIPCode = cip[0..1] + "." + cip[2..5]

            result.getAcademicAwardPrograms().add(p)
        }


        // degree vs certificate results
        if (acadCredential.acadDegree)
            parseDegree(acadCredential, result)
        else if (acadCredential.acadCcd)
            parseCertificate(acadCredential, result)

        return result
    }

    protected void parseDegree(AcadCredentialsRecord acadCredential, AcademicAwardType academicAwardType) {

        academicAwardType.academicAwardDate = ColleagueUtils.fromLocalDate(acadCredential.acadDegreeDate)
        academicAwardType.academicCompletionDate = academicAwardType.academicAwardDate
        academicAwardType.academicCompletionIndicator = true

        // get award level from CAST.SP02 translation
        if (acadCredential.acadDegree) {
            def sp02Code = fCastSP02.get()?.asMap()?.get(acadCredential.acadDegree)?.newCode
            if (sp02Code)
                academicAwardType.academicAwardLevel = TranscriptUtils.translateAwardLevel(sp02Code)
        }
    }

    protected void parseCertificate(AcadCredentialsRecord acadCredential, AcademicAwardType academicAwardType) {
        academicAwardType.academicAwardDate = ColleagueUtils.fromLocalDate(acadCredential.acadCcdDate?.first())

        // get award level from CAST.SP02.A translation
        if (acadCredential.acadCcd) {
            def sp02Code = fCastSP02A.get()?.asMap()?.get(acadCredential.acadCcd[0])?.newCode
            if (sp02Code)
                academicAwardType.academicAwardLevel = TranscriptUtils.translateAwardLevel(sp02Code)
        }
    }
}
