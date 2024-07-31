package api.mock

import com.ccctc.adaptor.exception.InvalidRequestException
import com.ccctc.adaptor.model.mock.PlacementDB
import com.ccctc.adaptor.model.placement.PlacementTransaction

class Placement {

    PlacementDB placementDB

    void post(String misCode, PlacementTransaction placementTransaction) {
        assert misCode != null
        assert placementTransaction != null

        if (misCode != placementTransaction.misCode)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MIS Code on URL and MIS Code in body do not match")

        placementDB.add(new PlacementDB.MockPlacementTransaction(null, placementTransaction))
    }
}
