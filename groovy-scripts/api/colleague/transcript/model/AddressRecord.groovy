package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

@Entity(appl = "CORE", name = "ADDRESS")
class AddressRecord extends ColleagueRecord {

    String[] addressLines
    String city
    String state
    String zip

    @Association
    List<AdrPhonesAssoc> adrPhonesAssoc

}
