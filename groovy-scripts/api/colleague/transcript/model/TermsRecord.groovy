package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

import java.time.LocalDate

@CompileStatic
@Entity(appl = "ST", name = "TERMS")
class TermsRecord extends ColleagueRecord{

    Integer termReportingYear
    LocalDate termStartDate
    LocalDate termEndDate
    String termDesc

}
