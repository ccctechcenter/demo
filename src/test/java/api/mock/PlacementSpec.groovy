package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.mock.CollegeDB
import com.ccctc.adaptor.model.mock.PersonDB
import com.ccctc.adaptor.model.mock.PlacementDB
import com.ccctc.adaptor.model.placement.PlacementTransaction
import spock.lang.Specification
import api.mock.Placement as PlacementGroovy

class PlacementSpec extends Specification {

    CollegeDB collegeDB
    PersonDB personDB
    PlacementDB placementDB

    PlacementGroovy groovyClass

    def misCode = "001"
    def sisPersonId = "sisPersonId"
    def cccId = "cccId"

    def setup() {
        // due to the inability to use mocking for methods in parent class BaseMockDataDB (such as find),
        // create actual instances of databases and seed with data
        collegeDB = new CollegeDB()
        personDB = new PersonDB(collegeDB)
        placementDB = new PlacementDB(personDB)

        // seed some base data
        collegeDB.add(new College(misCode: misCode))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))

        groovyClass = new PlacementGroovy(placementDB: placementDB)
    }

    def "post"() {
        setup:
        def transaction = new PlacementTransaction(misCode: misCode, cccid: cccId)

        when:
        groovyClass.post(null, null)

        then:
        thrown AssertionError

        when:
        groovyClass.post(misCode, null)

        then:
        thrown AssertionError

        when:
        groovyClass.post(misCode, new PlacementTransaction(misCode: "999"))

        then:
        thrown InvalidRequestException

        when:

        groovyClass.post(misCode, transaction)
        def result = placementDB.getAll()[0]

        then:
        result.placementTransaction == transaction
    }
}