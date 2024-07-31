package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Association
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

import java.time.LocalDate

@Entity(appl = "CORE", name = "PERSON")
class PersonRecord extends ColleagueRecord {

    String firstName
    String lastName
    LocalDate birthDate
    String ssn
    String gender
    String residenceState

    @Association
    List<PersonAltIdsAssoc> personAltIdsAssoc

    @Association
    List<PeopleEmailAssoc> peopleEmailAssoc

    @Association
    List<PerphoneAssoc> perphoneAssoc

    @Join
    AddressRecord preferredAddress

}
