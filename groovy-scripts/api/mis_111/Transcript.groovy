package api.mis_111

import api.mis_111.transcript.AcademicAwardService_111
import api.mis_111.transcript.AcademicSessionService_111
import com.ccctc.adaptor.util.ClassMap
import groovy.transform.CompileStatic
import org.springframework.core.env.Environment

/**
 * This customization for Butte College adds extra filtering / processing of AcademicSession data.
 */
@CompileStatic
class Transcript extends api.colleague.base.Transcript {

    @Override
    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        super.colleagueInit(misCode, environment, services)

        //
        // Customization: replace the Academic Session Service with our custom version. We use the copy constructor to
        // create an AcademicSessionService_111 object that has the same initial values as the one we're replacing.
        //
        def oldAcademicSessionService = this.academicRecordService.academicSessionService
        def newAcademicSessionService = new AcademicSessionService_111(oldAcademicSessionService)

        this.academicRecordService.academicSessionService = newAcademicSessionService

        //
        // Customization: replace the Academic Awards Service with our custom version. We use the copy constructor to
        // create an AcademicAwardService_111 object that has the same initial values as the one we're replacing.
        //
        def oldAcademicAwardService = this.academicRecordService.academicAwardService
        def newAcademicAwardService = new AcademicAwardService_111(oldAcademicAwardService)

        this.academicRecordService.academicAwardService = newAcademicAwardService
    }
}
