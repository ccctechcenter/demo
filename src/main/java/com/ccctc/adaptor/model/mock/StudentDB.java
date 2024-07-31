package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityConflictException;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.ApplicationStatus;
import com.ccctc.adaptor.model.College;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.model.StudentHomeCollege;
import com.ccctc.adaptor.model.Term;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Conditional(MockCondition.class)
public class StudentDB extends BaseMockDataDB<Student> {

    private CollegeDB collegeDB;
    private TermDB termDB;
    private PersonDB personDB;

    // home college information
    // key = misCode:sisPersonId
    // value = misCode of home college for the student
    private Map<String, String> homeColleges = new HashMap<>();

    /**
     * Track update time for each key
     * key: [mis, sisTermId, sisPersonId]
     * value: lastUpdated
     */
    private Map<List<Object>, LocalDateTime> updateHistoryDB = new ConcurrentHashMap<>();

    @Autowired
    public StudentDB(CollegeDB collegeDB, TermDB termDB, PersonDB personDB) throws Exception {
        super("Students.vm", Student.class);
        this.collegeDB = collegeDB;
        this.termDB = termDB;
        this.personDB = personDB;
    }

    @Override
    public Student add(Student record) {
        if (record.getMisCode() != null && record.getSisTermId() != null && (record.getSisPersonId() != null || record.getCccid() != null)) {
            // accept either sis person id or ccc id, populate both in student record
            Person person = personDB.validate(record.getMisCode(), record.getSisPersonId(), record.getCccid());

            record.setSisPersonId(person.getSisPersonId());
            record.setCccid(person.getCccid());
        }

        return super.add(record);
    }

    @Override
    public List<Student> find(Map<String, Object> searchMap) {
        MockUtils.fixMap(searchMap);
        return super.find(searchMap);
    }

    public Student get(String misCode, String sisTermId, String id) {
        Person person = personDB.validate(misCode, id);

        Map<String, Object> searchMap = new HashMap<>();
        searchMap.put("misCode", misCode);
        searchMap.put("sisPersonId", person.getSisPersonId());

        List<Student> students = find(searchMap);

        if (students.size() == 0)
            throw new EntityNotFoundException("Student record not found");

        // return a direct match if found
        Optional<Student> directMatch = students.stream().filter(i -> i.getSisTermId().equals(sisTermId)).findFirst();
        if (directMatch.isPresent())
            return directMatch.get();

        Term term = termDB.validate(misCode, sisTermId);

        // if a direct hit was not found, find the nearest term and change term-specific fields
        List<TermStudent> termStudents = students.stream()
                .map(i -> new TermStudent(termDB.get(i.getMisCode(), i.getSisTermId()), i))
                .collect(Collectors.toList());

        // sort by start date of term
        termStudents.sort(Comparator.comparingLong(i -> i.getTerm().getStart().toInstant().toEpochMilli()));

        // find nearest term before the term we're looking for
        for (int x = termStudents.size() - 1; x >= 0; x--) {
            if (!termStudents.get(x).getTerm().getStart().after(term.getStart()))
                return translateStudent(termStudents.get(x).getStudent(), sisTermId, true);
        }

        // if there is no term record before the one we're looking for, return their first term
        // record, but indicate that their application status is not valid
        return translateStudent(termStudents.get(0).getStudent(), sisTermId, false);
    }

    public Student update(String misCode, String sisTermId, String id, Student student) {
        Person person = personDB.validate(misCode, id);
        return super.update(Arrays.asList(misCode, sisTermId, person.getSisPersonId()), student);
    }



    public List<Student> getUpdatedSince(LocalDateTime updatesSinceDT) {
        List<Student> result = new ArrayList<>();
        // search updateHistoryDB values for entries after passed updatedSinceDT
        Set<Map.Entry<List<Object>, LocalDateTime>> entries = updateHistoryDB.entrySet();
        Iterator it = entries.iterator();
        while( it.hasNext() ){
            ConcurrentHashMap.Entry o = (ConcurrentHashMap.Entry) it.next();
            List<Object> key = (List<Object>) o.getKey();
            LocalDateTime lastUpdate = (LocalDateTime)o.getValue();
            if( lastUpdate.isAfter( updatesSinceDT )) {
                // search for student based on key (mis/term/id) & add to result list
                String mis = (String) key.get(0);
                String term = (String) key.get(1);
                String id = (String) key.get(2);
                Student s = get(mis,term,id);
                result.add( s );
            }
        }
        return result;
    }
    public Student patch(String misCode, String sisTermId, String id, Map student) {
        Person person = personDB.validate(misCode, id);
        List<Object> key = Arrays.asList(misCode, sisTermId, person.getSisPersonId());

        Student exists = database.get(key);
        if(exists == null) {
            Student s = new Student.Builder()
                    .misCode(misCode)
                    .sisTermId(sisTermId)
                    .cccid(person.getCccid())
                    .sisPersonId(person.getSisPersonId())
                    .build();
            super.add(s);
        }

        return super.patch(key, student);
    }

    public Student delete(String misCode, String sisTermId, String id, boolean cascade) {
        Person person = personDB.validate(misCode, id);
        return super.delete(Arrays.asList(misCode, sisTermId, person.getSisPersonId()), cascade);
    }


    public StudentHomeCollege getHomeCollege(String misCode, String id) {
        Person person = personDB.validate(misCode, id);
        String homeCollege = homeColleges.get(misCode + ":" + person.getSisPersonId());
        if (homeCollege == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found");

        return new StudentHomeCollege.Builder()
                .misCode(homeCollege)
                .cccid(person.getCccid())
                .build();
    }

    public StudentHomeCollege updateHomeCollege(String misCode, String id, String newHomeCollege) {
        Person person = personDB.validate(misCode, id);
        String homeCollege = homeColleges.get(misCode + ":" + person.getSisPersonId());
        if (homeCollege == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: " + misCode + " " + id);

        // verify new home college exists and is part of the district
        College college = collegeDB.validate(misCode);
        College newHome = collegeDB.validate(newHomeCollege);
        if (!ObjectUtils.equals(college.getDistrictMisCode(), newHome.getDistrictMisCode()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Cannot change home college to a college outside of district " + college.getDistrictMisCode());

        homeColleges.put(misCode + ":" + person.getSisPersonId(), newHomeCollege);

        return new StudentHomeCollege.Builder()
                .misCode(newHomeCollege)
                .cccid(person.getCccid())
                .build();
    }

    public Student validate(String misCode, String sisTermId, String id) {
        try {
            return get(misCode, sisTermId, id);
        } catch (EntityNotFoundException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound,
                    "Student not found: " + misCode + " " + sisTermId + " " + id);
        }
    }

    public List<Student> validate(String misCode, String id) {
        Person person = personDB.validate(misCode, id);

        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisPersonId", person.getSisPersonId());

        List<Student> students = find(map);

        if (students.size() == 0)
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student not found: " + misCode + " " + id);

        return students;
    }

    private void cascadeUpdateFromPerson(Person person) {
        String misCode = person.getMisCode();
        String sisPersonId = person.getSisPersonId();
        String cccId = person.getCccid();

        // cascade update of CCC ID
        database.forEach((k, v) -> {
            if (ObjectUtils.equals(misCode, v.getMisCode()) && ObjectUtils.equals(sisPersonId, v.getSisPersonId())) {
                v.setCccid(cccId);
            }
        });
    }

    private void checkParameters(EventDetail<Student> eventDetail) {
        Map parameters = eventDetail.getParameters();

        String misCode = (String) parameters.get("misCode");
        String sisTermId = (String) parameters.get("sisTermId");
        String sisPersonId = (String) parameters.get("sisPersonId");
        String cccid = (String) parameters.get("cccid");

        if (misCode != null)
            collegeDB.validate(misCode);

        if (misCode != null && sisTermId != null)
            termDB.validate(misCode, sisTermId);

        if (misCode != null && (sisPersonId != null || cccid != null))
            personDB.validate(misCode, sisPersonId, cccid);
    }

    private void afterAdd(EventDetail<Student> eventDetail) {
        Student replacement = eventDetail.getReplacement();

        // add home college info if absent
        homeColleges.putIfAbsent(replacement.getMisCode() + ":" + replacement.getSisPersonId(), replacement.getMisCode());

        List<Object> key = Arrays.asList(replacement.getMisCode(), replacement.getSisTermId(), replacement.getSisPersonId());
        updateHistoryDB.put( key, LocalDateTime.now(ZoneOffset.UTC) );
    }

    private void afterDelete(EventDetail<Student> eventDetail) {
        Student original = eventDetail.getOriginal();

        List<Object> key = Arrays.asList(original.getMisCode(), original.getSisTermId(), original.getSisPersonId());
        updateHistoryDB.remove( key );

        // delete home college info, but only if there are no more records left for the misCode and sisPersonId
        Map<String, Object> query = new HashMap<>();
        query.put("misCode", original.getMisCode());
        query.put("sisPersonId", original.getSisPersonId());
        List<Student> b = find(query);

        if (b.size() == 0) {
            homeColleges.remove(original.getMisCode() + ":" + original.getSisPersonId());
        }
    }

    private void afterUpdate(EventDetail<Student> eventDetail) {
        Student original = eventDetail.getOriginal();
        Student replacement = eventDetail.getReplacement();

        // disallow CCC ID change, if null populate from original record
        if (replacement.getCccid() != null && !replacement.getCccid().equals(original.getCccid()))
            throw new InvalidRequestException("CCC ID may not be changed - update in Person record instead");

        replacement.setCccid(original.getCccid());

        List<Object> key = Arrays.asList(original.getMisCode(), original.getSisTermId(), original.getSisPersonId());
        updateHistoryDB.put( key, LocalDateTime.now(ZoneOffset.UTC));
    }

    private void cascadeDelete(String misCode, String sisTermId, String sisPersonId, Boolean cascadeDelete, Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        if (sisTermId != null) map.put("sisTermId", sisTermId);
        if (sisPersonId != null) map.put("sisPersonId", sisPersonId);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.afterAdd, this::afterAdd);
        addHook(EventType.afterDelete, this::afterDelete);
        addHook(EventType.afterUpdate, this::afterUpdate);

        // cascade update from person
        personDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromPerson(e.getReplacement()));

        // cascade delete from person
        personDB.addHook(EventType.beforeDelete, (e) -> {
            Person p = e.getOriginal();
            cascadeDelete(p.getMisCode(), null, p.getSisPersonId(), e.getCascadeDelete(), e.getTestDelete());
        });

        // cascade delete from term
        termDB.addHook(EventType.beforeDelete, (e) -> {
            Term t = e.getOriginal();
            cascadeDelete(t.getMisCode(), t.getSisTermId(), null, e.getCascadeDelete(), e.getTestDelete());
        });
    }

    public Student copy(String misCode, String sisTermId, String sisPersonId, String newSisTermId) {
        final Student record = get(misCode, sisTermId, sisPersonId);

        Student newStudent = deepCopy(record);
        newStudent.setSisTermId(newSisTermId);

        add(newStudent);

        return newStudent;
    }

    public List<String> getStudentCCCIds(String misCode, String sisPersonId) {
        Person person = null;
        List<String> cccIds = null;

        try {
            person = personDB.validate(misCode, sisPersonId, null);
        } catch (InvalidRequestException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student record not found: " + sisPersonId);
        }

        Map searchMap = new HashMap<String, Object>();
        searchMap.put("sisPersonId", sisPersonId);
        List<Student> studs = find(searchMap);
        if (CollectionUtils.isEmpty(studs)) {
            throw new EntityNotFoundException("person record found but is not a student");
        }
        cccIds = new ArrayList<String>();
        if (person.getCccid() != null) {
            cccIds.add(person.getCccid());
        }
        return cccIds;
    }

    public List<String> postStudentCCCId(String misCode, String sisPersonId, String cccId) {
        Person person;

        try {
            person = personDB.validate(misCode, sisPersonId, null);
            if (person.getCccid() != null && !person.getCccid().equals(cccId)) {
                throw new EntityConflictException("Conflict - CCCID already assigned for the student");
            }
        } catch (InvalidRequestException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.studentNotFound, "Student record not found: " + sisPersonId);
        }

        // will throw InvalidRequestException if student is not found
        validate(misCode, sisPersonId);

        // make sure CCC ID is not used yet
        HashMap<String, Object> searchMap = new HashMap<>();
        searchMap.put("misCode", misCode);
        searchMap.put("cccid", cccId);
        List<Person> persons = personDB.find(searchMap);
        if (persons.size() > 0) {
            throw new EntityConflictException("Conflict - CCCID '" + cccId + "' already exists");
        }
        // update person record which will cascade update to student
        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("cccid", cccId);
        personDB.patch(misCode, sisPersonId, patchMap);

        return getStudentCCCIds(misCode, sisPersonId);
    }

    /**
     * Translate a student record from one term to another
     *
     * @param student              Original student record
     * @param sisTermId            New SIS Term ID
     * @param useApplicationStatus Use Application Status from original record?
     * @return New student record
     */
    private Student translateStudent(Student student, String sisTermId, boolean useApplicationStatus) {
        student.setSisTermId(sisTermId);

        // remove certain term-specific values
        student.setCohorts(null);
        student.setRegistrationDate(null);
        student.setHasBogfw(false);
        student.setHasFinancialAidAward(false);

        // if their first term is after the term searched for, consider the student
        // as "not having an application" at this point in time
        if (!useApplicationStatus) student.setApplicationStatus(ApplicationStatus.NoApplication);

        return student;
    }

    private class TermStudent {
        private Term term;
        private Student student;

        public TermStudent(Term term, Student student) {
            this.term = term;
            this.student = student;
        }

        public Term getTerm() {
            return term;
        }

        public Student getStudent() {
            return student;
        }
    }
}