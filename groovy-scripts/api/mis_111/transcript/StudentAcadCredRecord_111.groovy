package api.mis_111.transcript

import api.colleague.transcript.model.StudentAcadCredRecord
import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.annotation.Field

/**
 * Add extra fields to STUDENT.ACAD.CRED for parsing Butte Data
 */
@Entity(appl = "ST", name = "STUDENT.ACAD.CRED")
class StudentAcadCredRecord_111 extends StudentAcadCredRecord {

    // credit type - for things like Academic Renewal and HS Credit Only
    String stcCredType

    // current STC.STATUS value for filtering of drops
    @Field(value = "STC.STATUS")
    String currentStatus
}