package api.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.PersonDB
import com.ccctc.adaptor.model.mock.StudentDB
import org.springframework.core.env.Environment

class Person {

    // injected using GroovyServiceImpl on creation of class
    private Environment environment
    private PersonDB personDB
    private StudentDB studentDB

    def get(String misCode, String sisPersonId) {
        return personDB.get(misCode, sisPersonId)
    }

    def getStudentPerson(String misCode, String sisPersonId, String eppn, String cccId) {

        def person = getPersonRecord(misCode, sisPersonId, eppn, cccId)
        try {
            if (studentDB.validate(misCode, person.sisPersonId) != null) {
                return person;
            }
        } catch (InvalidRequestException e) {
            throw new EntityNotFoundException("person record found but is not a student");
        }

    }

    def getPersonRecord(String misCode, String sisPersonId, String eppn, String cccid) {

        def result = personDB.validate(misCode, sisPersonId, cccid, eppn)

        if (result != null) {
            return result
        } else {
            throw new EntityNotFoundException("Person record not found");
        }
    }


    def getAll(String misCode, String[] sisPersonIds, String[] cccids) {
        if (!sisPersonIds && !cccids)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "sisPersonIds or cccids is required")

        def result = personDB.getAllSorted()

        // filter
        result = result.findAll { i -> (sisPersonIds && sisPersonIds.contains(i.sisPersonId)) || (cccids != null && cccids.contains(i.cccid)) }

        return result

    }
}