package api.mock

import com.ccctc.adaptor.model.mock.CCPromiseGrantDB

class CCPromiseGrant {

    CCPromiseGrantDB ccPromiseGrantDB

    com.ccctc.adaptor.model.apply.CCPromiseGrant get(String misCode, Long id) {
        return ccPromiseGrantDB.get(misCode, id)
    }

    com.ccctc.adaptor.model.apply.CCPromiseGrant post(String misCode, com.ccctc.adaptor.model.apply.CCPromiseGrant a) {
        return ccPromiseGrantDB.add(a)
    }

}
