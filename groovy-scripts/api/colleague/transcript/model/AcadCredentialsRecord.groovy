package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Join
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

import java.time.LocalDate

@CompileStatic
@Entity(appl = "CORE", name = "ACAD.CREDENTIALS")
class AcadCredentialsRecord extends ColleagueRecord {

    String acadDegree
    List<String> acadCcd
    LocalDate acadDegreeDate
    List<LocalDate> acadCcdDate

    @Join
    List<OtherHonorsRecord> acadHonors

    @Join
    AcadProgramsRecord acadAcadProgram

}
