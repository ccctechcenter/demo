package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

@CompileStatic
@Entity(appl = "CORE", name = "PERSON")
class PersonLookupRecord extends ColleagueRecord {

    String firstName
    String lastName
    String[] personEmailAddresses

    @Association
    List<PersonAltIdsAssoc> personAltIdsAssoc

}
