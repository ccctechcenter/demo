package api.colleague.etran

import com.ccctc.sector.academicrecord.v1_9.impl.AddressTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.ContactsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.EmailTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.PhoneTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleagueContactsTypeImpl extends ContactsTypeImpl {

    @JsonProperty("addresses")
    void setAddresses(List<AddressTypeImpl> value) {
        super.addresses = value
    }

    @JsonProperty("phones")
    void setPhones(List<PhoneTypeImpl> value) {
        super.phones = value
    }

    @JsonProperty("emails")
    void setEmails(List<EmailTypeImpl> value) {
        super.emails = value
    }
}
