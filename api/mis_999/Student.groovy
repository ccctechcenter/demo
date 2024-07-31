package api.mis_999

import com.ccctc.adaptor.model.ApplicationStatus
import com.ccctc.adaptor.model.OrientationStatus
import com.ccctc.adaptor.model.ResidentStatus
import org.springframework.core.env.Environment


public class Student {
    Environment environment

    def get(String misCode, String cccId, String termId) {

        def builder = new com.ccctc.adaptor.model.Student.Builder()

        builder
                .cccid(cccId)
                .applicationStatus(ApplicationStatus.ApplicationAccepted)
                .orientationStatus(OrientationStatus.COMPLETED)
                .hasBogfw(true)
                .hasCaliforniaAddress(true)
                .hasEducationPlan(true)
                .hasEnglishAssessment(true)
                .hasMathAssessment(true)
                .isConcurrentlyEnrolled(false)
                .hasFinancialAidAward(false)
                .hasHold(false)
                .residentStatus(ResidentStatus.Resident)
                .isIncarcerated(false)
                .sisStudentId("abc123")
                .sisTermId(termId)
                .visaType("NA")
                .accountBalance(0.0)
                .dspsEligible(true)
                .registrationDate(new Date())

        // This is all QA Test data setup beyond this point!
        // These requires these terms to exist, which will likely have to be re-created multiple times.
        // TermIds following Butte's format -- can be changed as needed.
        System.out.println( "cccId=" + cccId )
        System.out.println( "termId=" + termId)
        switch(termId) {
            // Begin CE-586
            case "2011SP":
                builder
                    .residentStatus(ResidentStatus.Resident)
                break
            case "2011FA":
                builder
                    .residentStatus(ResidentStatus.NonResident)
                break
            case "2012SP":
                builder
                    .hasCaliforniaAddress(true)
                break
            case "2012FA":
                builder
                    .hasCaliforniaAddress(false)
                break
            // End CE-586
            // Begin CE-380
            // Refer to Metriculation Matrix:
            case "2007SP":
                // Case 1 FFF
                builder
                    .orientationStatus(OrientationStatus.REQUIRED)
                    .hasEducationPlan(false)
                    .hasEnglishAssessment(false)
                    .hasMathAssessment(false)
                break
            case "2007FA":
                // Case 2 TFF
                builder
                    .orientationStatus(OrientationStatus.COMPLETED)
                    .hasEducationPlan(false)
                    .hasEnglishAssessment(false)
                    .hasMathAssessment(false)
                break
            case "2008SP":
                // Case 3 TTF
                builder
                    .orientationStatus(OrientationStatus.COMPLETED)
                    .hasEducationPlan(true)
                    .hasEnglishAssessment(false)
                    .hasMathAssessment(false)
                break
            case "2008FA":
                // Case 4 FTF
                builder
                    .orientationStatus(OrientationStatus.REQUIRED)
                    .hasEducationPlan(true)
                    .hasEnglishAssessment(false)
                    .hasMathAssessment(false)
                break
            case "2009SP":
                // Case 5 FTT
                builder
                    .orientationStatus(OrientationStatus.REQUIRED)
                    .hasEducationPlan(true)
                    .hasEnglishAssessment(true)
                    .hasMathAssessment(true)
                break
            case "2009FA":
                // Case 6 FFT
                builder
                    .orientationStatus(OrientationStatus.REQUIRED)
                    .hasEducationPlan(false)
                    .hasEnglishAssessment(true)
                    .hasMathAssessment(true)
                break
            case "2010SP":
                // Case 7 TFT
                builder
                    .orientationStatus(OrientationStatus.COMPLETED)
                    .hasEducationPlan(false)
                    .hasEnglishAssessment(true)
                    .hasMathAssessment(true)
                break
            case "2010FA":
                // Case 8 TTT
                builder
                    .orientationStatus(OrientationStatus.COMPLETED)
                    .hasEducationPlan(true)
                    .hasEnglishAssessment(true)
                    .hasMathAssessment(true)
                break
            // End CE-380
        }

        // End of QA Test data
        return builder
            .build()

    }
}