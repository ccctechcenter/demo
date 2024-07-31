package api.mis_999

import com.ccctc.adaptor.model.PrerequisiteStatusEnum

/**
 * Created by Rasul on 2/18/16.
 */

def environment

def get( String misCode, String sisSectionId, String sisTermId, String cccid ) {
    def enrollment = new com.ccctc.adaptor.model.Enrollment.Builder()
    enrollment
            .sisTermId('111')
    .cccid('ABC12345')
    .sisSectionId('11111')
    def enrollmentList = [enrollment.build()]
    return enrollmentList

}

def getStudent(String cccId, String sisTermId, String misCode, String sisSectionId) {
    if (sisTermId == 'SP16') {
        return []
    }

    def enrollment = new com.ccctc.adaptor.model.Enrollment.Builder()
    enrollment
            .sisTermId(sisTermId)
            .cccid(cccId)
            .sisSectionId(sisSectionId)
    def enrollmentList = [enrollment.build()]
    return enrollmentList

}

def getPreReqStatus(misCode, sisCourseId, sisTermId, ccccid) {

    def preReqStatus = new com.ccctc.adaptor.model.PrerequisiteStatus.Builder()

    return preReqStatus.prerequisiteStatusEnum(PrerequisiteStatusEnum.Complete).message("We are good").build()
}

def post(String misCode, com.ccctc.adaptor.model.Enrollment enrollment ) {
    System.out.println(enrollment.cccid)
    return get(misCode,enrollment.sisSectionId, enrollment.sisTermId, enrollment.cccid)
}