package api.mock

import com.ccctc.adaptor.model.apply.Application
import com.ccctc.adaptor.model.mock.ApplyDB

class Apply {

    ApplyDB applyDB

    Application get(String misCode, Long id) {
        return applyDB.get(misCode, id)
    }

    Application post(String misCode, Application a) {
        return applyDB.add(a)
    }
}