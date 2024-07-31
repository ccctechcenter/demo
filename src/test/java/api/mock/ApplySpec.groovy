package api.mock

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.mock.ApplyDB
import com.ccctc.adaptor.model.mock.CollegeDB
import spock.lang.Specification

class ApplySpec extends Specification {

    def mockApplyDB = Mock(ApplyDBExtended)
    def groovyApply = new Apply(applyDB: mockApplyDB)


    // extend the class so we can mock methods from BaseDB
    class ApplyDBExtended extends ApplyDB {
        ApplyDBExtended() throws Exception {
            super(null)
        }

        @Override
        Application get(String misCode, long id) {
            return super.get(misCode, id)
        }

        @Override
        Application add(Application record) {
            return super.add(record)
        }
    }


    def "get"() {
        when:
        groovyApply.get("000", 1)

        then:
        1 * mockApplyDB.get("000", 1)
    }

    def "post"() {
        setup:
        def application = new Application(collegeId: "000", appId: 1)

        when:
        groovyApply.post("000", application)

        then:
        1 * mockApplyDB.add(application)
    }


}
