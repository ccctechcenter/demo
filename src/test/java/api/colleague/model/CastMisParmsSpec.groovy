package api.colleague.model

import api.colleague.model.CastMisParms
import api.colleague.model.InstitutionLocationMap
import spock.lang.Specification

class CastMisParmsSpec extends Specification {


    def "getMisCodes"() {
        setup:
        CastMisParms cmp = new CastMisParms(defaultMisCode : "001",
                defaultInstitutionId: "1", institutionLocationMapList: [
                new InstitutionLocationMap(institution: "1", location: "LOC", misCode: "001"),
                new InstitutionLocationMap(institution: "1", location: "LCT", misCode: "001"),
                new InstitutionLocationMap(institution: "2", location: "LOC", misCode: "002")
        ])

        when:
        List<String> result = cmp.getMisCodes()

        then:
        result.size() == 2
    }
}
