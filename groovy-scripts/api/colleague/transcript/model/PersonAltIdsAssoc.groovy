package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.AssociationEntity
import groovy.transform.CompileStatic

@CompileStatic
@AssociationEntity
class PersonAltIdsAssoc {

    String personAltIds
    String personAltIdTypes

}
