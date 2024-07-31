package api.mis_111.transcript

import api.colleague.transcript.model.InstitutionsAttendRecord
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join

@CompileStatic
@Entity(appl = "CORE", name = "INSTITUTIONS.ATTEND")
class InstitutionsAttendRecord_111 extends InstitutionsAttendRecord {

    @Join
    List<AcadCredentialsRecord_111> instaAcadCredentials
}