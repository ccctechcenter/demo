package api.mock

import com.ccctc.adaptor.model.apply.CCPromiseGrant
import com.ccctc.adaptor.model.mock.CCPromiseGrantDB
import spock.lang.Specification

import api.mock.CCPromiseGrant as CCPromiseGrantGroovy

class CCPromiseGrantSpec extends Specification {

    def mockCCPromiseGrantDB = Mock(CCPromiseGrantDBExtended)
    def groovyApply = new CCPromiseGrantGroovy(ccPromiseGrantDB: mockCCPromiseGrantDB)


    // extend the class so we can mock methods from BaseDB
    class CCPromiseGrantDBExtended extends CCPromiseGrantDB {
        CCPromiseGrantDBExtended() throws Exception {
            super(null)
        }

        @Override
        CCPromiseGrant get(String misCode, long id) {
            return super.get(misCode, id)
        }

        @Override
        CCPromiseGrant add(CCPromiseGrant record) {
            return super.add(record)
        }
    }


    def "get"() {
        when:
        groovyApply.get("000", 1)

        then:
        1 * mockCCPromiseGrantDB.get("000", 1)
    }

    def "post"() {
        setup:
        def application = new CCPromiseGrant(collegeId: "000", appId: 1)

        when:
        groovyApply.post("000", application)

        then:
        1 * mockCCPromiseGrantDB.add(application)
    }


}
