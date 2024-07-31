package api.colleague.model

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

import java.time.LocalDate

@CompileStatic
@Entity(appl = "ST", name = "TERMS")
class TermsRecord extends ColleagueRecord {

    String termDesc
    String termSession
    LocalDate termStartDate
    LocalDate termEndDate
    LocalDate termRegStartDate
    LocalDate termRegEndDate
    LocalDate termPreregStartDate
    LocalDate termPreregEndDate
    LocalDate termAddEndDate
    LocalDate termDropEndDate
    LocalDate termDropGradeReqdDate
    LocalDate[] termCensusDates

    // this tells it to join on @ID + TERMS.LOCATIONS, where @ID is the id of the TERMS record
    // for example, if the term is 2017FA and the location is MAIN, the key witll be 2017FA*MAIN
    @Join(prefixKeys = "@ID")
    List<TermsLocationsRecord> termLocations
}
