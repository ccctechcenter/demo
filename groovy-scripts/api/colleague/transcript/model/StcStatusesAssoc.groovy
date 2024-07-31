package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.AssociationEntity
import groovy.transform.CompileStatic

import java.time.LocalDate

@CompileStatic
@AssociationEntity
class StcStatusesAssoc {

    String stcStatus
    LocalDate stcStatusDate

}
