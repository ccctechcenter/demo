package api.colleague.transcript.base

import api.colleague.transcript.model.PersonRecord
import api.colleague.transcript.model.TranscriptDmiServices
import api.colleague.transcript.model.TranscriptException
import api.colleague.util.ColleagueUtils
import org.ccctc.colleaguedmiclient.model.ColleagueRecord
import org.ccctc.colleaguedmiclient.service.DmiEntityService
import com.ccctc.core.coremain.v1_14.GenderCodeType
import com.ccctc.core.coremain.v1_14.impl.AgencyIdentifierTypeImpl
import com.ccctc.core.coremain.v1_14.impl.BirthTypeImpl
import com.ccctc.core.coremain.v1_14.impl.GenderTypeImpl
import com.ccctc.core.coremain.v1_14.impl.NameTypeImpl
import com.ccctc.sector.academicrecord.v1_9.ContactsType
import com.ccctc.sector.academicrecord.v1_9.EmailType
import com.ccctc.sector.academicrecord.v1_9.PersonType
import com.ccctc.sector.academicrecord.v1_9.impl.AddressTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.ContactsTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.EmailTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.PersonTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.PhoneTypeImpl
import com.ccctc.sector.academicrecord.v1_9.impl.ResidencyTypeImpl
import groovy.transform.CompileStatic

@CompileStatic
abstract class PersonService {

    // DMI Services
    TranscriptDmiServices transcriptDmiServices
    DmiEntityService dmiEntityService
    List<String> cccIdTypes

    // entity classes for reading data
    Class<? extends ColleagueRecord> personEntity = PersonRecord.class

    PersonService(PersonService a) {
        this(a.transcriptDmiServices, a.cccIdTypes)
    }

    PersonService(TranscriptDmiServices transcriptDmiServices, List<String> cccIdTypes) {
        this.transcriptDmiServices = transcriptDmiServices
        this.dmiEntityService = transcriptDmiServices.dmiEntityService
        this.cccIdTypes = cccIdTypes
    }

    PersonType getPerson(String personId) {
        def data = readPerson(personId)
        if (!data) return null

        def person = parsePerson(data)
        def contacts = parseContacts(data)
        if (contacts) person.getContacts().addAll(contacts)

        return person
    }

    protected ColleagueRecord readPerson(String personId) {
        return dmiEntityService.readForEntity(personId, personEntity)
    }

    protected PersonType parsePerson(ColleagueRecord data) {
        if (!data instanceof PersonRecord)
            throw new TranscriptException("Default implementation of parsePerson expecting PersonRecord, found " + data.class.name)

        def source = (PersonRecord) data
        def person = new PersonTypeImpl()

        person.schoolAssignedPersonID = source.recordId

        person.name = new NameTypeImpl(firstName: source.firstName, lastName: source.lastName)

        if (source.birthDate)
            person.birth = new BirthTypeImpl(birthDate: ColleagueUtils.fromLocalDate(source.birthDate))


        switch(source.gender) {
            case "M":
                person.gender = new GenderTypeImpl(genderCode: GenderCodeType.MALE)
                break
            case "F":
                person.gender = new GenderTypeImpl(genderCode: GenderCodeType.FEMALE)
                break
            default:
                person.gender = new GenderTypeImpl(genderCode: GenderCodeType.UNREPORTED)
        }

        if (source.ssn) {
            def ssn = source.ssn.replace("-", "")
            if (ssn.length() == 9) {
                person.SSN = ssn
                person.partialSSN = ssn[5..8]
            }
        }

        if (source.personAltIdsAssoc) {
            Set<String> cccIds = []
            for (def altId : source.personAltIdsAssoc) {
                if (!cccIdTypes || cccIdTypes.contains(altId.personAltIdTypes)) {
                    cccIds << altId.personAltIds
                }
            }

            for (String c : cccIds) {
                person.getAgencyIdentifiers() << new AgencyIdentifierTypeImpl(agencyAssignedID: c)
            }
        }

        if (source.residenceState)
            person.residency = new ResidencyTypeImpl(stateProvince: source.residenceState)

        return person
    }

    /**
     * This is a pretty simplistic implementation of contacts that gets the address and phones associated with the
     * "preferred" address as well as any "personal" phones. It also returns all email addresses with the "preferred"
     * email as the first one listed.
     *
     * @param person Person data
     * @return Contacts
     */
    protected List<ContactsType> parseContacts(ColleagueRecord data) {
        if (!data instanceof PersonRecord)
            throw new TranscriptException("Default implementation of parsePerson expecting PersonRecord, found " + data.class.name)

        def source = (PersonRecord) data

        List<PhoneTypeImpl> phones = []
        AddressTypeImpl address
        List<EmailType> emails = []

        // add emails
        if (source.peopleEmailAssoc) {
            for (def e : source.peopleEmailAssoc) {
                if (e.personEmailAddresses) {
                    def et = new EmailTypeImpl(emailAddress: e.personEmailAddresses)
                    if ("Y".equals(e.personPreferredEmail)) {
                        emails.add(0, et)
                    } else {
                        emails << et
                    }
                }
            }
        }

        // add "personal phone numbers"
        if (source.perphoneAssoc) {
            for (def p : source.perphoneAssoc) {
                if (p.personalPhoneNumber)
                    phones << new PhoneTypeImpl(phoneNumber: p.personalPhoneNumber, phoneNumberExtension: p.personalPhoneExtension)
            }
        }

        if (source.preferredAddress) {
            def adr = source.preferredAddress

            // get address
            if (adr.addressLines) {
                address = new AddressTypeImpl(city: adr.city, stateProvince: adr.state, postalCode: adr.zip)
                address.getAddressLines().addAll(adr.addressLines)
            }

            // add phones from preferred address
            if (adr.adrPhonesAssoc) {
                for (def p : adr.adrPhonesAssoc) {
                    if (p.addressPhones)
                        phones << new PhoneTypeImpl(phoneNumber: p.addressPhones, phoneNumberExtension: p.addressPhoneExtension)
                }
            }
        }

        if (phones || address || emails) {
            def contact = new ContactsTypeImpl()
            if (phones)
                contact.getPhones().addAll(phones)
            if (address)
                contact.getAddresses().add(address)
            if (emails)
                contact.getEmails().addAll(emails)

            return [(ContactsType) contact]
        }

        return null
    }
}
