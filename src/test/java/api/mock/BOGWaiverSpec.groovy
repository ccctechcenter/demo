package api.mock

import api.mock.BOGWaiver as BOGWaiverGroovy
import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.BOGWaiver
import com.ccctc.adaptor.model.mock.BOGWaiverDB
import spock.lang.Specification

class BOGWaiverSpec extends Specification {

    def bogWaiverDB = Mock(BOGWaiverDB)
    def groovyClass = new BOGWaiverGroovy(bogWaiverDB: bogWaiverDB)

    def "get"() {
        setup:
        def bogWaiver = new BOGWaiver()
        def result

        when:
        result = groovyClass.get("misCode", "cccid", "sisTermId")

        then:
        1 * bogWaiverDB.get("misCode","sisTermId", "cccid:cccid") >> bogWaiver
        result == bogWaiver
    }

    def "post"() {
        setup:
        def bogWaiver = new BOGWaiver()
        def result

        when:
        result = groovyClass.post("misCode", bogWaiver)

        then:
        1 * bogWaiverDB.add(bogWaiver) >> bogWaiver
        result == bogWaiver
    }

    def "post - misCode"() {
        setup:
        def bogWaiver = new BOGWaiver(misCode: "misCode")
        def result

        when:
        result = groovyClass.post("misCode", bogWaiver)

        then:
        1 * bogWaiverDB.add(bogWaiver) >> bogWaiver
        result == bogWaiver
    }    

    def "post - invalid mis"() {
        setup:
        def bogWaiver = new BOGWaiver(misCode: "invalid")
        def result

        when:
        groovyClass.post("misCode", bogWaiver)

        then:
        thrown InvalidRequestException
    }
}
