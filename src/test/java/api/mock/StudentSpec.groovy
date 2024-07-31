package api.mock

import api.mock.Student as GroovyStudent
import com.ccctc.adaptor.exception.EntityConflictException
import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.model.Cohort
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.Student as CAStudent
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.CohortTypeEnum
import com.ccctc.adaptor.model.Term
import com.ccctc.adaptor.model.mock.StudentDB
import com.ccctc.adaptor.model.mock.TermDB
import com.ccctc.adaptor.model.students.StudentFieldSet
import spock.lang.Specification

import java.beans.Introspector
import java.beans.PropertyDescriptor

class StudentSpec extends Specification {

    TermDB termDB
    StudentDB studentDB
    GroovyStudent groovyClass

    def "setup"() {
        termDB = GroovyMock(TermDB, global: true)
        studentDB = GroovyMock(StudentDB, global: true)
        groovyClass = new GroovyStudent(termDB: termDB, studentDB: studentDB)
    }

    def "getAllByTerm - single term - All"() {
        setup:
        List<String> propNames = []
        for (PropertyDescriptor p : Introspector.getBeanInfo(CAStudent, Object).getPropertyDescriptors()) {
            if(!p.getName().equalsIgnoreCase("misCode")) {// ignoring the misCode which is not included in output
                propNames.add(p.getName())
            }
        }

        when:
        def result = groovyClass.getAllByTerm("misCode", Arrays.asList("sisTermId"), StudentFieldSet.ALL)

        then:
        1 * studentDB.find(_) >> [ new CAStudent() ]
        result.size() > 0
        def first = result[0]

        for (String pName : propNames) {
            assert first.containsKey(pName)
        }

        for(String key in first.keySet()) {
            assert propNames.contains(key)
        }
    }

    def "getAllByTerm - single term - Identifiers"() {
        setup:
        List<String> propNames = []
        for (PropertyDescriptor p : Introspector.getBeanInfo(com.ccctc.adaptor.model.students.StudentIdentityDTO, Object).getPropertyDescriptors()) {
            propNames.add(p.getName())
        }

        when:
        def result = groovyClass.getAllByTerm("misCode", Arrays.asList("sisTermId"), StudentFieldSet.IDENTIFIERS)

        then:
        1 * studentDB.find(_) >> [ new CAStudent() ]
        result.size() > 0
        def first = result[0]

        for (String pName : propNames) {
            assert first.containsKey(pName)
        }

        for(String key in first.keySet()) {
            assert propNames.contains(key)
        }
    }

    def "getAllByTerm - single term - Matriculation"() {
        setup:
        List<String> propNames = []
        for (PropertyDescriptor p : Introspector.getBeanInfo(com.ccctc.adaptor.model.students.StudentMatriculationDTO, Object).getPropertyDescriptors()) {
            propNames.add(p.getName())
        }

        when:
        def result = groovyClass.getAllByTerm("misCode", Arrays.asList("sisTermId"), StudentFieldSet.MATRICULATION)

        then:
        1 * studentDB.find(_) >> [ new CAStudent() ]
        result.size() > 0
        def first = result[0]

        for (String pName : propNames) {
            assert first.containsKey(pName)
        }

        for(String key in first.keySet()) {
            assert propNames.contains(key)
        }
    }

    def "getAllByTerm - multiple terms"() {
        when:
        groovyClass.getAllByTerm("misCode", ["sisTermId1", "sisTermId2"], StudentFieldSet.ALL)

        then:
        2 * studentDB.find(_)
    }

    def "getAllByTerm - current term"() {
        when:
        groovyClass.getAllByTerm("misCode", null, StudentFieldSet.ALL)

        then:
        1 * termDB.getCurrentTerm(_) >> new Term(sisTermId: "sisTermId")
        1 * studentDB.find(_)
    }

    def "getAllByTerm - no current term"() {
        when:
        groovyClass.getAllByTerm("misCode", null, StudentFieldSet.ALL)

        then:
        1 * termDB.getCurrentTerm(_) >> null
        thrown InvalidRequestException
    }

    def "get"() {
        when:
        groovyClass.get("misCode", "cccId", "sisTermId")

        then:
        1 * studentDB.get(_, _, _)
    }

    def "get - current term"() {
        when:
        groovyClass.get("misCode", "cccId", null)

        then:
        1 * termDB.getCurrentTerm(_) >> new Term(sisTermId: "sisTermId")
        1 * studentDB.get(*_)
        0 * _
    }

    def "get - no current term"() {
        when:
        groovyClass.get("misCode", "cccId", null)

        then:
        1 * termDB.getCurrentTerm(_) >> null
        thrown InvalidRequestException
    }

    def "get - not found"() {
        when:
        groovyClass.get("misCode", "cccId", "sisTermId")

        then:
        1 * studentDB.get(_, _, _) >> { throw new EntityNotFoundException("student not found")}
        thrown EntityNotFoundException

        when:
        groovyClass.get("misCode", "cccId", "sisTermId")

        then:
        1 * studentDB.get(_, _, _) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "person not found")}
        thrown EntityNotFoundException
    }


    def "getHomeCollege"() {
        when:
        groovyClass.getHomeCollege("misCode", "cccId")

        then:
        1 * studentDB.getHomeCollege(*_)
    }

    def "getHomeCollege - not found"() {
        when:
        groovyClass.getHomeCollege("misCode", "cccId")

        then:
        1 * studentDB.getHomeCollege(*_) >> { throw new EntityNotFoundException("student not found")}
        thrown EntityNotFoundException

        when:
        groovyClass.getHomeCollege("misCode", "cccId")

        then:
        1 * studentDB.getHomeCollege(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "person not found")}
        thrown EntityNotFoundException
    }


    def "postCohort - no cohortName"() {
        when:
        groovyClass.postCohort("cccId", null, "misCode", "sisTermId")

        then:
        thrown InvalidRequestException
    }

    def "postCohort - conflict"() {
        when:
        groovyClass.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> new CAStudent(cohorts: [new Cohort(name: CohortTypeEnum.COURSE_EXCHANGE)])
        thrown EntityConflictException
    }

    def "postCohort"() {
        when:
        groovyClass.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> new CAStudent()
        1 * studentDB.patch(*_)
        0 * _

        // extra coverage
        when:
        groovyClass.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> new CAStudent(cohorts: [])
        1 * studentDB.patch(*_)
        0 * _
    }

    def "postCohort - student not found"() {
        when:
        groovyClass.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "not found")}
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound

        when:
        groovyClass.postCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "not found")}
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

    def "deleteCohort - no cohortName"() {
        when:
        groovyClass.deleteCohort("cccId", null, "misCode", "sisTermId")

        then:
        thrown InvalidRequestException
    }

    def "deleteCohort - not found"() {
        when:
        groovyClass.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> new CAStudent()
        thrown EntityNotFoundException
    }

    def "deleteCohort"() {
        when:
        groovyClass.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> new CAStudent(cohorts: [new Cohort(name: CohortTypeEnum.COURSE_EXCHANGE)])
        1 * studentDB.patch(*_)
        0 * _
    }

    def "deleteCohort - student not found"() {
        when:
        groovyClass.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "not found")}
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound

        when:
        groovyClass.deleteCohort("cccId", CohortTypeEnum.COURSE_EXCHANGE, "misCode", "sisTermId")

        then:
        1 * studentDB.validate(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "not found")}
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

    def "getStudentCCCIds"() {
        when:
        groovyClass.getStudentCCCIds("misCode", "sisPersonId")

        then:
        1 * studentDB.getStudentCCCIds(*_)
    }

    def "getStudentCCCIds - student not found"() {
        when:
        groovyClass.getStudentCCCIds("misCode", "sisPersonId")

        then:
        1 * studentDB.getStudentCCCIds(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "not found")}
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound

        when:
        groovyClass.getStudentCCCIds("misCode", "sisPersonId")

        then:
        1 * studentDB.getStudentCCCIds(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "not found")}
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

    def "postStudentCCCId"() {
        when:
        groovyClass.postStudentCCCId("misCode", "sisPersonId", "cccId")

        then:
        1 * studentDB.postStudentCCCId(*_)
    }

    def "postStudentCCCId - student not found"() {
        when:
        groovyClass.postStudentCCCId("misCode", "sisPersonId", "cccId")

        then:
        1 * studentDB.postStudentCCCId(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "not found")}
        def e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound

        when:
        groovyClass.postStudentCCCId("misCode", "sisPersonId", "cccId")

        then:
        1 * studentDB.postStudentCCCId(*_) >> { throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "not found")}
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

    def "patch"() {
        setup:
        def updates = new com.ccctc.adaptor.model.Student.Builder().misCode('001').cccid('AAA1111').orientationStatus(OrientationStatus.COMPLETED).build()

        when:
        groovyClass.patch("001", "AAA1111", "sisTermId", updates)

        then:
        1 * studentDB.patch("001", "sisTermId", "cccid:AAA1111", [ cccid:"AAA1111", "orientationStatus": "COMPLETED" ])
    }

    def "patch - student not found"() {
        setup:
        def e
        def updates = new com.ccctc.adaptor.model.Student.Builder().misCode('001').cccid('AAA1111').orientationStatus(OrientationStatus.COMPLETED).build()
        1 * studentDB.patch("001", "sisTermId", "cccid:AAA1111", _) >> { throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "not found")}

        when:
        groovyClass.patch("001", "AAA1111", "sisTermId", updates)

        then:
        e = thrown InvalidRequestException
        e.code == InvalidRequestException.Errors.studentNotFound
    }

}
