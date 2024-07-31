package api.colleague

import api.colleague.Utility as GroovyUtility
import com.ccctc.adaptor.util.ClassMap
import org.ccctc.colleaguedmiclient.exception.DmiServiceException
import org.ccctc.colleaguedmiclient.model.ColleagueData
import org.ccctc.colleaguedmiclient.service.DmiDataService
import org.ccctc.colleaguedmiclient.service.DmiService
import org.springframework.boot.actuate.health.Status
import spock.lang.Specification

class UtilitySpec extends Specification {

    def dmiService = Mock(DmiService)
    def dmiDataService = Mock(DmiDataService)
    def groovyUtility = new GroovyUtility()

    def setup() {
        def services = new ClassMap()
        services.put(DmiService.class, dmiService)
        services.put(DmiDataService.class, dmiDataService)
        groovyUtility.colleagueInit("000", null, services)
    }

    def "getSisVersion"() {

        // exception
        when:
        def version = groovyUtility.getSisVersion()

        then:
        1 * dmiDataService.batchSelect(*_) >> { throw new Exception("oops") }
        version."Installed Envision Packages" == "unknown - connection error: oops"

        // version
        when:
        version = groovyUtility.getSisVersion()

        then:
        1 * dmiDataService.batchSelect(*_) >> [ new ColleagueData("package", ["XCTC.PRV.VERSION": "version"]) ]
        version."Installed Envision Packages" == ["package" : "version"]
    }

    def "getSisConnectionStatus"() {
        when:
        def status1 = groovyUtility.getSisConnectionStatus()
        def status2 = groovyUtility.getSisConnectionStatus()
        then:
        1 * dmiService.keepAlive()
        1 * dmiService.keepAlive() >> { throw new DmiServiceException("darn") }

        status1 == Status.UP
        status2 == Status.DOWN

    }
}
