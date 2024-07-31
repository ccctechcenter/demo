package api.colleague.transcript.base

import api.colleague.transcript.model.PersonLookupRecord
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.util.ColleagueUtils
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.adaptor.exception.InternalServerException
import com.ccctc.adaptor.exception.InvalidRequestException
import org.ccctc.colleaguedmiclient.service.DmiDataService

abstract class LookupService {

    // PERSON fields for lookup query
    final static String PERSON_ALT_IDS = "PERSON.ALT.IDS"
    final static String BIRTH_DATE = "BIRTH.DATE"
    final static String SSN = "SSN"

    // DMI services
    TranscriptDmiServices transcriptDmiServices
    DmiDataService dmiDataService
    DmiEntityService dmiEntityService

    // configuration
    List<String> cccIdTypes

    LookupService(LookupService a) {
        this(a.transcriptDmiServices, a.cccIdTypes)
    }

    LookupService(TranscriptDmiServices transcriptDmiServices, List<String> cccIdTypes) {
        this.transcriptDmiServices = transcriptDmiServices
        this.dmiDataService = transcriptDmiServices.dmiDataService
        this.dmiEntityService = transcriptDmiServices.dmiEntityService
    }

    /**
     * Lookup a student
     */
    List<String> lookupStudent(String firstName,
                               String lastName,
                               String birthDate,
                               String ssn,
                               String partialSSN,
                               String emailAddress,
                               String schoolAssignedStudentID,
                               String cccId) {

        def results = []
        def query = []

        // validate and parse/format search parameters
        if (partialSSN) partialSSN = parseSSN4(partialSSN)
        if (ssn) ssn = parseSSN(ssn)
        if (firstName) firstName = firstName.trim()
        if (lastName) lastName = lastName.trim()
        if (emailAddress) emailAddress = emailAddress.trim()

        // construct our query parameters with only the case sensitive values
        // since we cannot perform a case insensitive query in Colleague, the name / email wil be checked against the results later
        if (schoolAssignedStudentID) query << "ID = " + ColleagueUtils.quoteString(schoolAssignedStudentID)
        if (ssn) query << SSN + " = " + ColleagueUtils.quoteString(ssn)
        if (cccId) query << PERSON_ALT_IDS + " = " + ColleagueUtils.quoteString(cccId)
        if (birthDate) query << BIRTH_DATE + " = " + ColleagueUtils.quoteString(birthDate)
        if (partialSSN) query << SSN + " LIKE " + ColleagueUtils.quoteString("..." + partialSSN)

        // this should not happen, however, to be safe we need to prevent executing this select without a query
        if (query.size() == 0)
            throw new InternalServerException("No valid query parameters passed to lookupStudent")

        def queryString = query.join(" AND ")

        // perform the query against PERSON and further limit the IDs to those that are also in the STUDENTS table
        def ids = dmiDataService.selectKeys("PERSON", queryString)
        if (ids) ids = dmiDataService.selectKeys("STUDENTS", null, ids.toList())

        if (ids) {
            def persons = dmiEntityService.readForEntity(ids.toList(), PersonLookupRecord.class)

            if (persons) {

                // perform some filtering on our results:
                // 1. filter our results on case insensitive fields (name, email)
                // 1. verify that the CCC ID that was found was indeed a CCC ID by ensuring that the value
                //    of PERSON.ALT.ID.TYPES associated with the CCC ID matches the configuration
                for (def p : persons) {
                    if (cccId && cccIdTypes) {
                        def c = p.personAltIdsAssoc?.find { it.personAltIds == cccId }
                        if (!c || !cccIdTypes.contains(c.personAltIdTypes))
                            continue
                    }

                    if (emailAddress) {
                        if (!p.personEmailAddresses || !(p.personEmailAddresses.find { emailAddress.toLowerCase() == it?.toLowerCase() }))
                            continue
                    }

                    if (firstName && firstName.toLowerCase() != p.firstName.toLowerCase())
                        continue

                    if (lastName && lastName.toLowerCase() != p.lastName.toLowerCase())
                        continue

                    results << p.recordId
                }
            }
        }

        return results
    }

    /**
     * Parse and verify the partial ssn value
     */
    protected String parseSSN4(String ssn4) {
        try {
            if (ssn4 && ssn4.length() == 4) {
                return Integer.parseInt(ssn4)
            }
        } catch (NumberFormatException ignored) {
        }

        throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria,
                "partial ssn search must be 4 numeric digits")
    }

    /**
     * Validate SSN and put in format xxx-xx-xxxx as this is how its stored in the PERSON table
     */
    protected String parseSSN(String ssn) {
        if (ssn) {
            ssn = ssn.replace("-", "")

            try {
                if (ssn.length() == 9) {
                    ssn = Integer.parseInt(ssn).toString()
                    ssn = ssn[0..2] + "-" + ssn[3..4] + "-" + ssn[5..8]
                    return ssn
                }
            } catch (NumberFormatException ignored) {
            }

            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria,
                    "ssn must be 9 numeric digits with or without dashes")
        }
    }
}
