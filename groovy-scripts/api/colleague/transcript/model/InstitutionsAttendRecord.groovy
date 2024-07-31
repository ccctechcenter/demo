package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

@CompileStatic
@Entity(appl = "CORE", name = "INSTITUTIONS.ATTEND")
class InstitutionsAttendRecord extends ColleagueRecord {

    @Join
    List<AcadCredentialsRecord> instaAcadCredentials
}
