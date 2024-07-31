package api.mock

import com.ccctc.adaptor.model.apply.InternationalApplication
import com.ccctc.adaptor.model.mock.InternationalApplicationDB

class InternationalApply {

    InternationalApplicationDB InternationalApplicationDB

    InternationalApplication get(String misCode, Long id) {
        return InternationalApplicationDB.get(misCode, id)
    }

    InternationalApplication post(String misCode, InternationalApplication a) {
        return InternationalApplicationDB.add(a)
    }
}