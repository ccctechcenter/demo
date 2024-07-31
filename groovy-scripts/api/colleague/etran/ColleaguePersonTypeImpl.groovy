package api.colleague.etran

import com.ccctc.core.coremain.v1_14.impl.AgencyIdentifierTypeImpl
import com.ccctc.core.coremain.v1_14.impl.BirthTypeImpl
import com.ccctc.core.coremain.v1_14.impl.GenderTypeImpl
import com.ccctc.core.coremain.v1_14.impl.NameTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.PersonTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.ResidencyTypeImpl
import com.fasterxml.jackson.annotation.JsonProperty

class ColleaguePersonTypeImpl extends PersonTypeImpl {

    @JsonProperty("name")
    void setName(NameTypeImpl value) {
        super.name = value
    }

    @JsonProperty("birth")
    void setBirth(BirthTypeImpl value) {
        super.birth = value
    }

    @JsonProperty("gender")
    void setGender(GenderTypeImpl value) {
        super.gender = value
    }

    @JsonProperty("agencyIdentifiers")
    void setAgencyIdentifiers(List<AgencyIdentifierTypeImpl> value) {
        super.agencyIdentifiers = value
    }

    @JsonProperty("contacts")
    void setContacts(List<ColleagueContactsTypeImpl> value) {
        super.contacts = value
    }

    @JsonProperty("residency")
    void setResidency(ResidencyTypeImpl value) {
        super.residency = value
    }
}