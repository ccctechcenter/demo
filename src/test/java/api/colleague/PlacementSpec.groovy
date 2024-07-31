package api.colleague

import api.colleague.Placement as GroovyPlacement
import com.ccctc.adaptor.model.placement.PlacementSubjectArea
import com.ccctc.adaptor.model.placement.PlacementTransaction
import org.springframework.core.env.Environment
import spock.lang.Specification

class PlacementSpec extends Specification {

    def environment = Mock(Environment)
    def groovyPlacement = new GroovyPlacement()

    def misCode = "000"
    def cccId = "ABC123"

    def placement = new PlacementTransaction(misCode: misCode, cccid: cccId, subjectArea: new PlacementSubjectArea())

    def "colleagueInit - unsupported"() {
        when:
        groovyPlacement.colleagueInit(misCode, environment, null)
        then:
        thrown UnsupportedOperationException
    }


    def "post - missing and bad params"() {
        when: groovyPlacement.post(null, null)
        then: thrown UnsupportedOperationException

        when: groovyPlacement.post(misCode, null)
        then: thrown UnsupportedOperationException

        when: groovyPlacement.post(misCode, new PlacementTransaction())
        then: thrown UnsupportedOperationException

        when: groovyPlacement.post(misCode, new PlacementTransaction(misCode: misCode))
        then: thrown UnsupportedOperationException

        when: groovyPlacement.post(misCode, new PlacementTransaction(misCode: misCode, cccid: cccId))
        then: thrown UnsupportedOperationException

        when: groovyPlacement.post(misCode, new PlacementTransaction(misCode: "bad-mis-code", cccid: cccId, subjectArea: new PlacementSubjectArea()))
        then: thrown UnsupportedOperationException

    }

    def "post - unsupported"() {
        when:
        groovyPlacement.post(misCode, placement)

        then:
        thrown UnsupportedOperationException
    }
}
