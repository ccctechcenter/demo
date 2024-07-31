package api.mock

import com.ccctc.adaptor.config.JacksonConfig
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.TermDB
import com.ccctc.adaptor.model.students.StudentFieldSet
import com.ccctc.adaptor.model.students.StudentIdentityDTO
import com.ccctc.adaptor.model.students.StudentMatriculationDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper

import java.time.LocalDate

class Student {
    private final static Logger log = LoggerFactory.getLogger(Student.class)

    Environment environment
    // injected using GroovyServiceImpl on creation of class
    private StudentDB studentDB
    private TermDB termDB
    private ObjectMapper mapper = new JacksonConfig().jacksonObjectMapper()

    List<Map> getAllByTerm(String misCode, List<String> sisTermIds, StudentFieldSet fieldSet) {

        // remove any empty terms provided
        if (sisTermIds) {
            sisTermIds.removeAll(Arrays.asList("", null));
        }

        // get "current term" if no term is supplied
        if (!sisTermIds || sisTermIds.empty) {
            def termId = termDB.getCurrentTerm(misCode)?.sisTermId
            if(termId) {
                sisTermIds = [ termId ]
            }
        }

        if (!sisTermIds || sisTermIds.empty) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "No current term found in mock database")
        }

        List<com.ccctc.adaptor.model.Student> results = []
        for (String sisTermId : sisTermIds) {
            Map<String, Object> searchMap = new HashMap<>()
            searchMap.put("misCode", misCode)
            searchMap.put("sisTermId", sisTermId)
            def termResults = studentDB.find(searchMap);
            if(termResults && termResults.size() > 0) {
                results.addAll(termResults.asCollection())
            }
        }
        if(fieldSet == StudentFieldSet.IDENTIFIERS) {
            List<java.util.Map> filterFieldResults = []
            for(com.ccctc.adaptor.model.Student s: results) {
                StudentIdentityDTO dto = mapper.convertValue(s, StudentIdentityDTO.class)
                HashMap fieldMap = mapper.convertValue(dto, HashMap.class)
                filterFieldResults.add(fieldMap)
            }
            return filterFieldResults
        } else if(fieldSet == StudentFieldSet.MATRICULATION) {
            List<java.util.Map> filterFieldResults = []
            for(com.ccctc.adaptor.model.Student s: results) {
                StudentMatriculationDTO dto = mapper.convertValue(s, StudentMatriculationDTO.class)
                HashMap fieldMap = mapper.convertValue(dto, HashMap.class)
                filterFieldResults.add(fieldMap)
            }
            return filterFieldResults
        }
        else {
            List<java.util.Map> filterFieldResults = []
            for(com.ccctc.adaptor.model.Student s: results) {
                HashMap fieldMap = mapper.convertValue(s, HashMap.class)
                filterFieldResults.add(fieldMap)
            }
            return filterFieldResults
        }
    }

    def get(String misCode, String cccId, String sisTermId) {
        // get "current term" if no term is supplied
        def termId = sisTermId ?: termDB.getCurrentTerm(misCode)?.sisTermId

        if (!termId)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidSearchCriteria, "No current term found in mock database")

        try {
            return studentDB.get(misCode, termId, "cccid:$cccId")
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a 404 entity not found
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new EntityNotFoundException("Student not found: $cccId")

            throw e
        }
    }

    def getHomeCollege(String misCode, String cccId) {
        try {
            return studentDB.getHomeCollege(misCode, "cccid:$cccId")
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a 404 entity not found
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new EntityNotFoundException("Student not found: $cccId")

            throw e
        }
    }

    void postCohort(String cccId, CohortTypeEnum cohortName, String misCode, String sisTermId) {
        if (!cohortName)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Cohort Name cannot be null")

        def student
        try {
            student = studentDB.validate(misCode, sisTermId, "cccid:$cccId")
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a "student not found" error
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $cccId")

            throw e
        }

        if (student.cohorts && student.cohorts.find { i -> i.name == cohortName })
            throw new EntityConflictException("Conflict - Cohort '" + cohortName + "' already exists")

        if (student.cohorts == null)
            studentDB.patch(misCode, sisTermId, student.sisPersonId, [cohorts: [new Cohort(name: cohortName)]])
        else
            studentDB.patch(misCode, sisTermId, student.sisPersonId, [cohorts: student.cohorts.add(new Cohort(name: cohortName))])
    }

    void deleteCohort(String cccId, CohortTypeEnum cohortName, String misCode, String sisTermId) {
        if (!cohortName)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Cohort Name cannot be null")

        def student
        try {
            student = studentDB.validate(misCode, sisTermId, "cccid:$cccId")
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a "student not found" error
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $cccId")

            throw e
        }

        if (!student.cohorts || !student.cohorts.find { i -> i.name == cohortName })
            throw new EntityNotFoundException("Cohort '" + cohortName + "' not found")

        student.cohorts.removeAll { i -> i.name == cohortName }
        studentDB.patch(misCode, sisTermId, student.sisPersonId, [cohorts: student.cohorts])
    }

    def getStudentCCCIds(String misCode, String sisPersonId) {
        try {
            return studentDB.getStudentCCCIds(misCode, sisPersonId)
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a "student not found" error
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $sisPersonId")

            throw e
        }
    }

    def postStudentCCCId(String misCode, String sisPersonId, String cccId) {
        try {
            return studentDB.postStudentCCCId(misCode, sisPersonId, cccId)
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a "student not found" error
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $cccId")

            throw e
        }
    }

    def put(String misCode, String cccId, String termId, com.ccctc.adaptor.model.Student updates) {
        // can only update student for "current term"
        if( !termId ) termId = termDB.getCurrentTerm(misCode)?.sisTermId

        try {
            // these key fields are not updatable; they need to match the path keys
            updates.setMisCode(misCode)
            updates.setCccid(cccId)
            updates.setSisTermId(termId)
            studentDB.update(misCode, termId, "cccid:$cccId", updates)
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a 404 entity not found
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $cccId")
            throw e
        }
    }

    def patch(String misCode, String cccId, String termId, com.ccctc.adaptor.model.Student updates) {
        // can only update student for "current term"
        if( !termId ) termId = termDB.getCurrentTerm(misCode)?.sisTermId

        final Map updatesMap = this.getUpdatesMap(updates)
        try {
            studentDB.patch(misCode, termId, "cccid:$cccId", updatesMap)
        } catch (InvalidRequestException e) {
            // convert a "person not found" error into a 404 entity not found
            if (e.code && e.code == InvalidRequestException.Errors.personNotFound)
                throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: $cccId")
            throw e
        }
    }

    def Map getUpdatesMap(com.ccctc.adaptor.model.Student updates) {
        mapper.setSerializationInclusion(Include.NON_NULL);
        final Map updatesMap = mapper.convertValue(updates, HashMap.class)
        // these key fields are not updatable
        updatesMap.remove("misCode")
        updatesMap.remove("sisPersonId")
        updatesMap.remove("sisTermId")
        updatesMap.remove("cccId")
        return updatesMap
    }

}
