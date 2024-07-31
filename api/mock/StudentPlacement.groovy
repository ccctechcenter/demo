
package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.placement.StudentPlacementData
import com.ccctc.adaptor.model.mock.StudentPlacementDB
import org.springframework.core.env.Environment

/**
 * <h1>Student Placement Mock Interface</h1>
 * <summary>
 *     <p>Provides a dummy stub for storing and populating Student Placement data into a custom staging table</p>
 * </summary>
 *
 * @author Paul Weatherby
 * @version 2.1
 * @since 2018-12-04
 *
 */
class StudentPlacement {

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment
    private StudentPlacementDB studentPlacementDB

    def get(String misCode, String cccId, String theSSID) {
        def sp = new StudentPlacementData()
        sp.miscode = misCode
        sp.californiaCommunityCollegeId = cccId
        sp.statewideStudentId = theSSID

        return studentPlacementDB.get(misCode, cccId, theSSID);
    }

    /**
     * Post student placement data
     * @param misCode The College Code used for grabbing college specific settings from the environment
     * @param placementData The placement data to set into the staging table
     */
    void post(String misCode, StudentPlacementData placementData) {

        //****** Validate parameters ****
        if (placementData == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Student Placement data cannot be null")
        }
        if (!placementData.californiaCommunityCollegeId) {
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "californiaCommunityCollegeId cannot be null or blank")
        }
        if (placementData != null) {
            if (!placementData.miscode)
                placementData.miscode = misCode
            else if (placementData.miscode != misCode)
                throw new InvalidRequestException(null, "misCode does not match placementData")
        }
        // throws 409 if record already exists
        studentPlacementDB.add(placementData);
    }
}
