package api.mis_111.transcript

import api.colleague.transcript.model.AcadCredentialsRecord
import groovy.transform.CompileStatic
import org.ccctc.colleaguedmiclient.annotation.Entity

/**
 * Add ACAD.COMMENTS
 */
@CompileStatic
@Entity(appl = "CORE", name = "ACAD.CREDENTIALS")
class AcadCredentialsRecord_111 extends AcadCredentialsRecord {

    String acadComments

}

