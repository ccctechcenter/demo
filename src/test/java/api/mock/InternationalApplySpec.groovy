package api.mock


import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.mock.InternationalApplicationDB
import spock.lang.Specification

class InternationalApplySpec extends Specification {

    def mockInternationalApplyDB = Mock(InternationalApplicationDBExtended)
    def groovyInternationalApply = new InternationalApply(InternationalApplicationDB: mockInternationalApplyDB)


    // extend the class so we can mock methods from BaseDB
    class InternationalApplicationDBExtended extends InternationalApplicationDB {
        InternationalApplicationDBExtended() throws Exception {
            super(collegeDB: null)

        }

        @Override
        InternationalApplication get(String misCode, long id) {
            return super.get(misCode, id)
        }

        @Override
        InternationalApplication add(InternationalApplication record) {
            return super.add(record)
        }
    }


    def "get"() {
        when:
        groovyInternationalApply.get("000", 1)

        then:
        1 * mockInternationalApplyDB.get("000", 1)
    }

    def "post"() {
        setup:
        def application = new InternationalApplication(collegeId: "000", appId: 1)

        when:
        groovyInternationalApply.post("000", application)

        then:
        1 * mockInternationalApplyDB.add(application)
    }


}
