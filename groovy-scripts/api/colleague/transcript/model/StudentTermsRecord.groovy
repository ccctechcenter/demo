package api.colleague.transcript.model

import org.ccctc.colleaguedmiclient.annotation.Entity
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import groovy.transform.CompileStatic

@CompileStatic
@Entity(appl = "ST", name = "STUDENT.TERMS")
class StudentTermsRecord extends ColleagueRecord {

    String sttrPrintedComments

}
