package api.colleague.model

import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord

import java.time.LocalDate

@CompileStatic
@Entity(appl = "ST", name = "TERMS.LOCATIONS")
class TermsLocationsRecord extends ColleagueRecord {

    LocalDate tlocRegStartDate
    LocalDate tlocRegEndDate
    LocalDate tlocPreregStartDate
    LocalDate tlocPreregEndDate
    LocalDate tlocAddEndDate
    LocalDate tlocDropEndDate
    LocalDate tlocDropGradeReqdDate
    LocalDate[] tlocCensusDates

}
