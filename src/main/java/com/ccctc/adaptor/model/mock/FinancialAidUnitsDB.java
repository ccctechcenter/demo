package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.FinancialAidUnits;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class FinancialAidUnitsDB extends BaseMockDataDB<FinancialAidUnits> {

    private CollegeDB collegeDB;
    private TermDB termDB;
    private StudentDB studentDB;
    private PersonDB personDB;

    @Autowired
    public FinancialAidUnitsDB(CollegeDB collegeDB, TermDB termDB, StudentDB studentDB, PersonDB personDB) throws Exception {
        super("FinancialAidUnits.vm", FinancialAidUnits.class);
        this.collegeDB = collegeDB;
        this.termDB = termDB;
        this.studentDB = studentDB;
        this.personDB = personDB;
    }

    @Override
    public FinancialAidUnits add(FinancialAidUnits record) {
        if (record.getMisCode() != null && (record.getSisPersonId() != null || record.getCccid() != null)) {
            // accept either sis person id or ccc id, populate both in fa units record
            Person person = personDB.validate(record.getMisCode(), record.getSisPersonId(), record.getCccid());

            record.setSisPersonId(person.getSisPersonId());
            record.setCccid(person.getCccid());
        }

        return super.add(record);
    }

    @Override
    public List<FinancialAidUnits> find(Map<String, Object> searchMap) {
        MockUtils.fixMap(searchMap);
        return super.find(searchMap);
    }

    public FinancialAidUnits get(String misCode, String sisTermId, String id, String enrolledMisCode, String c_id) {
        Person person = personDB.validate(misCode, id);
        return super.get(Arrays.asList(misCode, sisTermId, person.getSisPersonId(), enrolledMisCode, c_id));
    }

    public FinancialAidUnits update(String misCode, String sisTermId, String id, String enrolledMisCode, String c_id, FinancialAidUnits financialAidUnits) {
        Person person = personDB.validate(misCode, id);
        return super.update(Arrays.asList(misCode, sisTermId, person.getSisPersonId(), enrolledMisCode, c_id), financialAidUnits);
    }

    public FinancialAidUnits patch(String misCode, String sisTermId, String id, String enrolledMisCode, String c_id, Map financialAidUnits) {
        Person person = personDB.validate(misCode, id);
        return super.patch(Arrays.asList(misCode, sisTermId, person.getSisPersonId(), enrolledMisCode, c_id), financialAidUnits);
    }

    public FinancialAidUnits delete(String misCode, String sisTermId, String id, String enrolledMisCode, String c_id, boolean cascade) {
        Person person = personDB.validate(misCode, id);
        return super.delete(Arrays.asList(misCode, sisTermId, person.getSisPersonId(), enrolledMisCode, c_id), cascade);
    }

    private void checkParameters(EventDetail<FinancialAidUnits> eventDetail) {
        Map parameters = eventDetail.getParameters();

        String misCode = (String) parameters.get("misCode");
        String sisTermId = (String) parameters.get("sisTermId");
        String sisPersonId = (String) parameters.get("sisPersonId");
        String cccid = (String) parameters.get("cccid");

        if (misCode != null)
            collegeDB.validate(misCode);

        if (misCode != null && sisTermId != null)
            termDB.validate(misCode, sisTermId);

        if (misCode != null && (sisPersonId != null || cccid != null)) {
            Person person = personDB.validate(misCode, sisPersonId, cccid);
            if (sisTermId != null) {
                studentDB.validate(misCode, person.getSisPersonId());
            }
        }

    }

    private void beforeUpdate(EventDetail<FinancialAidUnits> eventDetail) {
        FinancialAidUnits original = eventDetail.getOriginal();
        FinancialAidUnits replacement = eventDetail.getReplacement();

        // disallow CCC ID change, if null populate from original record
        if (replacement.getCccid() != null && !replacement.getCccid().equals(original.getCccid()))
            throw new InvalidRequestException(null, "CCC ID may not be changed - update in Person record instead");

        replacement.setCccid(original.getCccid());
    }

    private void cascadeUpdateFromPerson(Person person) {
        String misCode = person.getMisCode();
        String sisPersonId = person.getSisPersonId();
        String cccId = person.getCccid();

        // cascade update of CCC ID
        database.forEach((k,v) -> {
            if (ObjectUtils.equals(misCode, v.getMisCode()) && ObjectUtils.equals(sisPersonId, v.getSisPersonId())) {
                v.setCccid(cccId);
            }
        });
    }

    private void cascadeDelete(String misCode, String sisTermId, String sisPersonId, Boolean cascadeDelete,
                               Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisPersonId", sisPersonId);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.beforeUpdate, this::beforeUpdate);

        // cascade update from
        personDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromPerson(e.getReplacement()));

        // cascade delete hook from student
        studentDB.addHook(EventType.beforeDelete, (e) -> {
            Student s = e.getOriginal();
            cascadeDelete(s.getMisCode(), s.getSisTermId(), s.getSisPersonId(), e.getCascadeDelete(), e.getTestDelete());
        });
    }
}