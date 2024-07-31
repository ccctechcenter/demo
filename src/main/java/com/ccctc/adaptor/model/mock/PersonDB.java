package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class PersonDB extends BaseMockDataDB<Person> {

    private CollegeDB collegeDB;

    @Autowired
    public PersonDB(CollegeDB collegeDB) throws Exception {
        super("Persons.vm", Person.class);
        this.collegeDB = collegeDB;
    }

    @Override
    public List<Person> find(Map<String, Object> searchMap) {
        MockUtils.fixMap(searchMap);
        return super.find(searchMap);
    }

    public Person get(String misCode, String id) {
        return super.get(Arrays.asList(misCode, translateId(misCode, id)));
    }

    public Person update(String misCode, String id, Person person) {
        return super.update(Arrays.asList(misCode, translateId(misCode, id)), person);
    }

    public Person patch(String misCode, String id, Map person) {
        return super.patch(Arrays.asList(misCode, translateId(misCode, id)), person);
    }

    public Person delete(String misCode, String id, boolean cascade) {
        return super.delete(Arrays.asList(misCode, translateId(misCode, id)), cascade);
    }

    private void checkParameters(EventDetail<Person> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String misCode = (String) parameters.get("misCode");
        if (misCode != null)
            collegeDB.validate(misCode);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);

        // add cascade delete hook for college
        collegeDB.addHook(EventType.beforeDelete, (e) -> {
            Map<String, Object> deleteMap = new HashMap<>();
            deleteMap.put("misCode", e.getOriginal().getMisCode());
            deleteMany(deleteMap, e.getCascadeDelete(), e.getTestDelete());
        });
    }

    /**
     * Validate a Person by sisPersonId or cccid in the format "cccid:ABC1234".
     * <p>
     * InvalidRequestException will be thrown if the operation is not successful (person does not exist, or multiple
     * persons are found with same cccid).
     *
     * @param misCode MIS Code
     * @param id      SIS Person ID or CCC ID prefixed by cccid:
     * @return Person
     */
    public Person validate(String misCode, String id) {
        String cccid = MockUtils.extractCccId(id);
        return validate(misCode, (cccid == null) ? id : null, cccid);
    }

    /**
     * Validate a Person by sisPersonId and/or cccid. If both are passed in, it will also check to ensure they correspond
     * to the same person.
     * <p>
     * InvalidRequestException will be thrown if the operation is not successful (person does not exist, sisPersonId and
     * cccid do not match or multiple persons are found with same cccid).
     *
     * @param misCode     MIS Code
     * @param sisPersonId SIS Person ID
     * @param cccid       CCC ID
     * @return Person
     */
    public Person validate(String misCode, String sisPersonId, String cccid) {
        try {
            // first try by sisPersonId
            if (sisPersonId != null) {
                Person person = get(misCode, sisPersonId);
                if (cccid != null && !cccid.equals(person.getCccid())) {
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                            "sisPersonId and cccid in request do not match same person");
                }

                return person;
            }

            // next try by cccid
            if (cccid != null) {
                return getByCccId(misCode, cccid);
            }


        } catch (EntityNotFoundException e) {
            // ignore
        }

        String id = sisPersonId != null ? sisPersonId : cccid;
        throw new InvalidRequestException(InvalidRequestException.Errors.personNotFound, "Person record not found: " + misCode + " " + id);
    }

    /**
     * Validate a Person by sisPersonId or cccid or eppn. If three are passed in, it will also check to ensure they correspond
     * to the same person.
     *
     * @param misCode     MIS Code
     * @param sisPersonId SIS Person ID
     * @param cccid       CCC ID
     * @param eppn        eppn
     * @return Person
     */
    public Person validate(String misCode, String sisPersonId, String cccid, String eppn) {
        try {
            // first try by sisPersonId
            if (sisPersonId != null) {
                Person person = get(misCode, sisPersonId);
                if (person != null) {
                    String personEmail = person.getLoginId() + "@" + person.getLoginSuffix();

                    if (eppn != null && !personEmail.equalsIgnoreCase(eppn)) {
                        throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                                "sisPersonId and eppn in request do not match same person");
                    }

                    if (cccid != null && !cccid.equals(person.getCccid())) {
                        throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                                "sisPersonId and cccid in request do not match same person");
                    }
                }
                return person;
            }

            // next try by cccid
            if (cccid != null) {
                Person person = getByCccId(misCode, cccid);
                if (person != null) {
                    String personEmail = person.getLoginId() + "@" + person.getLoginSuffix();
                    if (eppn != null && !personEmail.equalsIgnoreCase(eppn)) {
                        throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest,
                                "cccid and eppn in request do not match same person");
                    }
                }
                return person;
            }

            //next by eppn
            if (eppn != null) {
                return getByEppn(misCode, eppn);
            }


        } catch (EntityNotFoundException e) {
            // ignore
        }


        throw new EntityNotFoundException("Person record not found ");
    }

    /**
     * Get a Person record by CCC ID
     *
     * @param misCode MIS Code
     * @param cccid   CCC ID
     * @return Person
     */
    public Person getByCccId(String misCode, String cccid) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("cccid", cccid);

        List<Person> persons = super.find(map);
        if (persons.size() == 0) {
            throw new EntityNotFoundException("No record found with CCC ID: " + cccid);
        } else if (persons.size() > 1) {
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple records found with CCC ID: " + cccid);
        }

        return persons.get(0);
    }

    /**
     * Get a Person record by EPPN
     *
     * @param misCode MIS Code
     * @param eppn    EPPN
     * @return Person
     */
    public Person getByEppn(String misCode, String eppn) {
        Map<String, Object> map = new HashMap<>();
        String loginId = null;
        String loginSuffix = null;
        if (eppn.indexOf("@") != -1) {
            loginId = eppn.substring(0, eppn.indexOf("@"));
            loginSuffix = eppn.substring(eppn.indexOf("@") + 1, eppn.length());
        }
        map.put("misCode", misCode);
        map.put("loginId", loginId);
        map.put("loginSuffix", loginSuffix);
        List<Person> persons = super.find(map);
        if (persons.size() == 0) {
            throw new EntityNotFoundException("No record found with eppn : " + eppn);
        } else if (persons.size() > 1) {
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple records found with eppn: " + eppn);
        }

        return persons.get(0);
    }

    /**
     * Checks the value of id to see if its in the format cccid:ABC1234. If it is it will translate the CCC ID back to
     * an SIS Person ID. Otherwise it will return the value of id untouched.
     *
     * @param id ID to check
     * @return SIS Person ID
     */
    public String translateId(String misCode, String id) {
        String cccid = MockUtils.extractCccId(id);
        if (cccid != null)
            return translateCccId(misCode, cccid);

        return id;
    }


    /**
     * Translate a CCC ID to a SIS Person ID. Will throw an InvalidRequestException if multiple persons are found with
     * the same CCC ID.
     *
     * @param misCode MIS Code
     * @param cccid   CCC ID
     * @return SIS Person ID
     */
    public String translateCccId(String misCode, String cccid) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("cccid", cccid);

        List<Person> matches = find(map);

        if (matches.size() > 1)
            throw new InvalidRequestException(InvalidRequestException.Errors.multipleResultsFound, "Multiple persons found with CCC ID: " + cccid);

        if (matches.size() == 1)
            return matches.get(0).getSisPersonId();

        return null;
    }
}