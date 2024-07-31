package com.ccctc.adaptor.model.mock

import com.ccctc.adaptor.exception.EntityNotFoundException
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.College
import com.ccctc.adaptor.model.Person
import com.ccctc.adaptor.model.placement.PlacementTransaction
import spock.lang.Specification

class PlacementDBSpec extends Specification {
    List<PlacementDB.MockPlacementTransaction> all
    PlacementDB.MockPlacementTransaction first

    def misCode = "001"
    def sisPersonId = "person1"
    def cccId = "cccid1"

    def collegeDB = new CollegeDB()
    def personDB = new PersonDB(collegeDB)
    def placementDB = new PlacementDB(personDB)

    def setup() {
        // initialize databases
        collegeDB.init()
        personDB.init()
        placementDB.init()

        // seed databases
        collegeDB.add(new College(misCode: misCode, districtMisCode: "000"))
        personDB.add(new Person(misCode: misCode, sisPersonId: sisPersonId, cccid: cccId))
        placementDB.add(new PlacementDB.MockPlacementTransaction(sisPersonId, new PlacementTransaction(misCode: misCode)))

        all = placementDB.getAll()
        first = all[0]
    }

    def "get - direct hit"() {
        setup:
        def result

        when:
        result = placementDB.get(first.id)

        then:
        result == first
    }

    def "add by sisPersonId or cccid"() {
        setup:
        def p1 = new PlacementDB.MockPlacementTransaction(sisPersonId, new PlacementTransaction(misCode: misCode))
        def p2 = new PlacementDB.MockPlacementTransaction(null, new PlacementTransaction(misCode: misCode, cccid: cccId))

        when:
        def r1 = placementDB.add(p1)
        def r2 = placementDB.add(p2)

        then:
        r1.sisPersonId != null
        r1.placementTransaction.cccid != null
        r1 == r2
    }

    def "person not found"() {
        when:
        placementDB.add(new PlacementDB.MockPlacementTransaction(null, new PlacementTransaction()))

        then:
        thrown InvalidRequestException

        when:
        placementDB.add(new PlacementDB.MockPlacementTransaction(null, new PlacementTransaction(misCode: misCode)))

        then:
        thrown InvalidRequestException

        when:
        placementDB.add(new PlacementDB.MockPlacementTransaction("notfound", new PlacementTransaction(misCode: misCode)))

        then:
        thrown InvalidRequestException

        when:
        placementDB.add(new PlacementDB.MockPlacementTransaction(null, new PlacementTransaction(misCode: misCode, cccid: "notfound")))

        then:
        thrown InvalidRequestException
    }

    def "update from person"() {
        setup:
        def person = personDB.get(misCode, sisPersonId)

        when:
        personDB.patch(misCode, sisPersonId, [cccid: "newcccid"])
        def result = placementDB.get(first.id)

        then:
        result.placementTransaction.cccid == "newcccid"
    }

    def "cascade delete"() {
        when:
        personDB.delete(misCode, sisPersonId, true)
        placementDB.get(first.id)

        then:
        thrown EntityNotFoundException
    }
}
