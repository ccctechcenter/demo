package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

@CompileStatic
@Entity(appl = "CORE", name = "OTHER.HONORS")
class OtherHonorsRecord extends ColleagueRecord {

    String ohonDesc
}
