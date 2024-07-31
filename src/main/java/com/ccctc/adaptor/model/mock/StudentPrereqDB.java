package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.Course;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.PrimaryKey;
import com.ccctc.adaptor.model.Student;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class StudentPrereqDB extends BaseMockDataDB<StudentPrereqDB.PrereqInfo> {

    private CollegeDB collegeDB;
    private PersonDB personDB;
    private StudentDB studentDB;
    private CourseDB courseDB;

    @Autowired
    public StudentPrereqDB(CollegeDB collegeDB, PersonDB personDB, StudentDB studentDB, CourseDB courseDB) throws Exception {
        super("StudentPrereqs.vm", PrereqInfo.class);
        this.collegeDB = collegeDB;
        this.personDB = personDB;
        this.studentDB = studentDB;
        this.courseDB = courseDB;
    }

    @Override
    public PrereqInfo add(PrereqInfo record) {
        if (record.getMisCode() != null && (record.getSisPersonId() != null || record.getCccid() != null)) {
            // accept either sis person id or ccc id, populate both in student record
            Person person = personDB.validate(record.getMisCode(), record.getSisPersonId(), record.getCccid());

            record.setSisPersonId(person.getSisPersonId());
            record.setCccid(person.getCccid());
        }

        return super.add(record);
    }

    @Override
    public List<PrereqInfo> find(Map<String, Object> searchMap) {
        MockUtils.fixMap(searchMap);
        return super.find(searchMap);
    }

    public PrereqInfo get(String misCode, String id, String sisCourseId) {
        return super.get(Arrays.asList(misCode, getSisPersonId(misCode, id), sisCourseId));
    }

    public PrereqInfo update(String misCode, String id, String sisCourseId, PrereqInfo prereqInfo) {
        return super.update(Arrays.asList(misCode, getSisPersonId(misCode, id), sisCourseId), prereqInfo);
    }

    public PrereqInfo patch(String misCode, String id, String sisCourseId, Map prereqInfo) {
        return super.patch(Arrays.asList(misCode, getSisPersonId(misCode, id), sisCourseId), prereqInfo);
    }

    public PrereqInfo delete(String misCode, String id, String sisCourseId, boolean cascade) {
        return super.delete(Arrays.asList(misCode, getSisPersonId(misCode, id), sisCourseId), cascade);
    }

    private String getSisPersonId(String misCode, String id) {
        List<Student> students = studentDB.validate(misCode, id);
        return students.get(0).getSisPersonId();
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

    private void checkParameters(EventDetail<StudentPrereqDB.PrereqInfo> eventDetail) {
        Map parameters = eventDetail.getParameters();

        String misCode = (String) parameters.get("misCode");
        String sisPersonId = (String) parameters.get("sisPersonId");
        String cccid = (String) parameters.get("cccid");
        String sisCourseId = (String) parameters.get("sisCourseId");

        if (misCode != null)
            collegeDB.validate(misCode);

        if (misCode != null && sisCourseId != null)
            courseDB.validate(misCode, sisCourseId);

        if (misCode != null && (sisPersonId != null || cccid != null)) {
            Person person = personDB.validate(misCode, sisPersonId, cccid);
            studentDB.validate(misCode, person.getSisPersonId());
        }

    }

    private void afterUpdate(EventDetail<StudentPrereqDB.PrereqInfo> eventDetail) {
        StudentPrereqDB.PrereqInfo original = eventDetail.getOriginal();
        StudentPrereqDB.PrereqInfo replacement = eventDetail.getReplacement();

        // disallow CCC ID change, if null populate from original record
        if (replacement.getCccid() != null && !replacement.getCccid().equals(original.getCccid()))
            throw new InvalidRequestException(null, "CCC ID may not be changed - update in Person record instead");

        replacement.setCccid(original.getCccid());

        // update complete based on whether completed date is populated
        replacement.setComplete(replacement.getCompleted() != null);
    }

    private void afterAdd(EventDetail<StudentPrereqDB.PrereqInfo> eventDetail) {
        StudentPrereqDB.PrereqInfo replacement = eventDetail.getReplacement();

        // update complete based on whether completed date is populated
        replacement.setComplete(replacement.getCompleted() != null);
    }

    private void cascadeCourseDelete(EventDetail<Course> e) {
        Course original = e.getOriginal();

        Map<String, Object> deleteMap = new HashMap<>();
        deleteMap.put("misCode", original.getMisCode());
        deleteMap.put("sisCourseId", original.getSisCourseId());

        List<Course> a = courseDB.find(deleteMap);
        if (a.size() == 1) {
            // if this is the last record for this course, cascade the delete
            deleteMany(deleteMap, e.getCascadeDelete(), e.getTestDelete());
        }
    }

    private void cascadeStudentDelete(EventDetail<Student> e) {
        Student original = e.getOriginal();

        Map<String, Object> deleteMap = new HashMap<>();
        deleteMap.put("misCode", original.getMisCode());
        deleteMap.put("sisPersonId", original.getSisPersonId());

        List<Student> a = studentDB.find(deleteMap);
        if (a.size() == 1) {
            // if this is the last record for this student, cascade the delete
            deleteMany(deleteMap, e.getCascadeDelete(), e.getTestDelete());
        }
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.afterUpdate, this::afterUpdate);
        addHook(EventType.afterAdd, this::afterAdd);

        // cascade update from person
        personDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromPerson(e.getReplacement()));

        // cascade delete from course and student, but only if its the last record of each
        // since pre-requisite information is not term based whereas course and student are
        courseDB.addHook(EventType.beforeDelete, this::cascadeCourseDelete);
        studentDB.addHook(EventType.beforeDelete, this::cascadeStudentDelete);
    }


    @PrimaryKey({"misCode", "sisPersonId", "sisCourseId"})
    public static class PrereqInfo implements Serializable {
        private String misCode;
        private String sisPersonId;
        private String cccid;
        private String sisCourseId;
        private boolean complete;
        private Date started;
        private Date completed;

        public PrereqInfo() {
        }

        public String getMisCode() {
            return misCode;
        }

        public void setMisCode(String misCode) {
            this.misCode = misCode;
        }

        public String getSisPersonId() {
            return sisPersonId;
        }

        public void setSisPersonId(String sisPersonId) {
            this.sisPersonId = sisPersonId;
        }

        public String getCccid() {
            return cccid;
        }

        public void setCccid(String cccid) {
            this.cccid = cccid;
        }

        public String getSisCourseId() {
            return sisCourseId;
        }

        public void setSisCourseId(String sisCourseId) {
            this.sisCourseId = sisCourseId;
        }

        public boolean isComplete() {
            return complete;
        }

        public void setComplete(boolean complete) {
            this.complete = complete;
        }

        public Date getStarted() {
            return started;
        }

        public void setStarted(Date started) {
            this.started = started;
        }

        public Date getCompleted() {
            return completed;
        }

        public void setCompleted(Date completed) {
            this.completed = completed;
        }

        @Override
        public String toString() {
            return "PrereqInfo{" +
                    "misCode='" + misCode + '\'' +
                    ", sisPersonId='" + sisPersonId + '\'' +
                    ", cccid='" + cccid + '\'' +
                    ", sisCourseId='" + sisCourseId + '\'' +
                    ", complete=" + complete +
                    ", started=" + started +
                    ", completed=" + completed +
                    '}';
        }
    }
}
