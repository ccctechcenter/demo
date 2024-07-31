import com.ccctc.adaptor.util.impl.TranscriptService
import com.ccctc.message.collegetranscript.v1_6.CollegeTranscript
import org.springframework.core.env.Environment

import java.text.SimpleDateFormat

/**
 * Created by rcshishe on 5/25/17.
 */

class Transcript {

    Environment environment

    void post(String miscode, CollegeTranscript transcript) {

    }


    def get(String misCode,
            String firstName,
            String lastName,
            String birthDate,
            String ssn,
            String partialSSN,
            String emailAddress,
            String schoolAssignedStudentID,
            String cccID,
            TranscriptService transcriptService) {

        CollegeTranscript transcript = transcriptService.createMockTranscript(firstName, lastName,
                birthDate != null ? new SimpleDateFormat("yyyy-MM-dd").parse(birthDate) : null,ssn, partialSSN,cccID,schoolAssignedStudentID,emailAddress)
        return transcript

    }
}