package api.mis_999

import com.ccctc.adaptor.model.CreditStatus
import com.ccctc.adaptor.model.GradingMethod
import com.ccctc.adaptor.model.TransferStatus
import org.springframework.core.env.Environment

public class Course {
    Environment environment

    // Get a course. term is optional.
    def get(String misCode, String name, String term) {
            // Build course record for return
            def builder = new com.ccctc.adaptor.model.Course.Builder();
            builder
                    .misCode(misCode)
                    .sisCourseId(name)
                    .sisTermId(term)
                    .c_id("C-ID")
                    .controlNumber("control_number")
                    .subject("Subject")
                    .number("100")
                    .title("title")
                    .description("descriptions")
                    .outline("outline")
                    .prerequisites("prereq")
                    .corequisites("coreq")
                    .minimumUnits(1.0)
                    .maximumUnits(3.0)
                    .transferStatus(TransferStatus.Csu)
                    .creditStatus(CreditStatus.DegreeApplicable)
                    .gradingMethod(GradingMethod.Graded)
                    .fee(100.0)

        return builder.build()
    }
}
