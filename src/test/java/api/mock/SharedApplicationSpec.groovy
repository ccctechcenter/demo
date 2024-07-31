package api.mock

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.apply.SharedApplication
import api.mock.SharedApplication as GroovySharedApplication
import com.ccctc.adaptor.model.mock.SharedApplicationDB
import spock.lang.Specification

class SharedApplicationSpec extends Specification {

    def mockSharedApplicationDB = Mock(SharedApplicationDBExtended)
    def groovySharedApplication = new GroovySharedApplication(sharedApplicationDB: mockSharedApplicationDB)


    // extend the class so we can mock methods from BaseDB
    class SharedApplicationDBExtended extends SharedApplicationDB {
        SharedApplicationDBExtended() throws Exception {
            super(null)
        }

        @Override
        SharedApplication get(String misCode, long id) {
            return super.get(misCode, id)
        }

        @Override
        SharedApplication add(SharedApplication record) {
            return super.add(record)
        }
    }


    def "get"() {
        when:
        groovySharedApplication.get("000", 1)

        then:
        1 * mockSharedApplicationDB.get("000", 1)
    }

    def "post"() {
        setup:
        def application = new Application(collegeId: "000", appId: 1)

        when:
        groovySharedApplication.post("000", application)

        then:
        1 * mockSharedApplicationDB.add(_)
    }


}
