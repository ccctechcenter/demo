package api.mock

import com.ccctc.adaptor.model.mock.SharedApplicationDB
import org.apache.commons.beanutils.BeanUtils

class SharedApplication {

    //****** Populated by injection in Groovy Service Implementation ****
    SharedApplicationDB sharedApplicationDB

    com.ccctc.adaptor.model.apply.SharedApplication get(String misCode, Long id) {
        return sharedApplicationDB.get(misCode, id)
    }

    void post(String teachMisCode, com.ccctc.adaptor.model.apply.Application origin) {
        com.ccctc.adaptor.model.apply.SharedApplication destination = new com.ccctc.adaptor.model.apply.SharedApplication()
        BeanUtils.copyProperties(destination,origin)
        destination.misCode = teachMisCode
        sharedApplicationDB.add(destination)
    }
}