package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.*;
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
public class EnrollmentDB extends BaseMockDataDB<Enrollment> {

    private CollegeDB collegeDB;
    private TermDB termDB;
    private CourseDB courseDB;
    private SectionDB sectionDB;
    private StudentDB studentDB;
    private PersonDB personDB;

    @Autowired
    public EnrollmentDB(CollegeDB collegeDB, TermDB termDB, CourseDB courseDB, SectionDB sectionDB,
                        StudentDB studentDB, PersonDB personDB) throws Exception {
        super("Enrollments.vm", Enrollment.class);
        this.collegeDB = collegeDB;
        this.termDB = termDB;
        this.courseDB = courseDB;
        this.sectionDB = sectionDB;
        this.studentDB = studentDB;
        this.personDB = personDB;
    }

    @Override
    public Enrollment add(Enrollment record) {
        if (record.getMisCode() != null && (record.getSisPersonId() != null || record.getCccid() != null)) {
            // accept either sis person id or ccc id, populate both in student record
            Person person = personDB.validate(record.getMisCode(), record.getSisPersonId(), record.getCccid());

            record.setSisPersonId(person.getSisPersonId());
            record.setCccid(person.getCccid());
        }

        return super.add(record);
    }

    @Override
    public List<Enrollment> find(Map<String, Object> searchMap) {
        MockUtils.fixMap(searchMap);
        return super.find(searchMap);
    }

    public Enrollment get(String misCode, String id, String sisTermId, String sisSectionId) {
        Person person = personDB.validate(misCode, id);
        return super.get(Arrays.asList(misCode, person.getSisPersonId(), sisTermId, sisSectionId));
    }

    public Enrollment update(String misCode, String id, String sisTermId, String sisSectionId, Enrollment enrollment) {
        Person person = personDB.validate(misCode, id);
        return super.update(Arrays.asList(misCode, person.getSisPersonId(), sisTermId, sisSectionId), enrollment);
    }

    public Enrollment patch(String misCode, String id, String sisTermId, String sisSectionId, Map enrollment) {
        Person person = personDB.validate(misCode, id);
        return super.patch(Arrays.asList(misCode, person.getSisPersonId(), sisTermId, sisSectionId), enrollment);
    }

    public Enrollment delete(String misCode, String id, String sisTermId, String sisSectionId, boolean cascade) {
        Person person = personDB.validate(misCode, id);
        return super.delete(Arrays.asList(misCode, person.getSisPersonId(), sisTermId, sisSectionId), cascade);
    }

    private void checkParameters(EventDetail<Enrollment> eventDetail) {
        Map parameters = eventDetail.getParameters();

        String misCode = (String) parameters.get("misCode");
        String sisTermId = (String) parameters.get("sisTermId");
        String sisCourseId = (String) parameters.get("sisCourseId");
        String sisSectionId = (String) parameters.get("sisSectionId");
        String sisPersonId = (String) parameters.get("sisPersonId");
        String cccid = (String) parameters.get("cccid");

        if (misCode != null)
            collegeDB.validate(misCode);

        if (misCode != null && sisTermId != null)
            termDB.validate(misCode, sisTermId);

        if (misCode != null && sisTermId != null && sisCourseId != null)
            courseDB.validate(misCode, sisTermId, sisCourseId);

        if (misCode != null && sisTermId != null && sisSectionId != null) {
            Section section = sectionDB.validate(misCode, sisTermId, sisSectionId);

            if (sisCourseId != null && !sisCourseId.equals(section.getSisCourseId())) {
                throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound,
                        "Section " + sisSectionId + " found in term but does not belong to course " + sisCourseId);
            }
        }

        if (misCode != null && (sisPersonId != null || cccid != null)) {
            Person person = personDB.validate(misCode, sisPersonId, cccid);

            if (sisTermId != null) {
                studentDB.validate(misCode, person.getSisPersonId());
            }
        }
    }

    private void afterAdd(EventDetail<Enrollment> eventDetail) {
        Enrollment replacement = eventDetail.getReplacement();
        validateUnits(replacement);

        // touch section, course and person to cascade updates. This will ensure various values like title,
        // sisCourseId, c_id and CCC ID are set
        sectionDB.touch(Arrays.asList(replacement.getMisCode(), replacement.getSisTermId(), replacement.getSisSectionId()));
        courseDB.touch(Arrays.asList(replacement.getMisCode(), replacement.getSisTermId(), replacement.getSisCourseId()));
        personDB.touch(Arrays.asList(replacement.getMisCode(), replacement.getSisPersonId()));
    }

    private void afterUpdate(EventDetail<Enrollment> eventDetail) {
        Enrollment original = eventDetail.getOriginal();
        Enrollment replacement = eventDetail.getReplacement();

        // disallow CCC ID change, if null populate from original record
        if (replacement.getCccid() != null && !replacement.getCccid().equals(original.getCccid()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "CCC ID may not be changed - update in Person record instead");
        replacement.setCccid(original.getCccid());

        // disallow updates to these fields by forcing back to original value
        replacement.setTitle(original.getTitle());
        replacement.setSisCourseId(original.getSisCourseId());
        replacement.setC_id(original.getC_id());

        validateUnits(replacement);
    }

    private void validateUnits(Enrollment record) {
        // check units
        Section section = sectionDB.validate(record.getMisCode(), record.getSisTermId(), record.getSisSectionId());
        if (record.getUnits() == null) {
            record.setUnits(section.getMinimumUnits());
        } else if (section.getMinimumUnits() == null || record.getUnits() < section.getMinimumUnits() || record.getUnits() > section.getMaximumUnits()) {
            throw new InvalidRequestException("Units entered for enrollment are out of range for section");
        }
    }

    private void cascadeUpdateFromSection(Section section) {
        String misCode = section.getMisCode();
        String sisTermId = section.getSisTermId();
        String sisSectionId = section.getSisSectionId();
        String title = section.getTitle();
        String sisCourseId = section.getSisCourseId();

        // cascade update of title, sisCourseId and status
        database.forEach((k,v) -> {
            if (ObjectUtils.equals(misCode, v.getMisCode()) && ObjectUtils.equals(sisTermId, v.getSisTermId())
                    && ObjectUtils.equals(sisSectionId, v.getSisSectionId())) {
                v.setTitle(title);
                v.setSisCourseId(sisCourseId);
                if (ObjectUtils.equals(section.getStatus(), SectionStatus.Cancelled)) {
                    v.setEnrollmentStatus(EnrollmentStatus.Cancelled);
                } else if (ObjectUtils.equals(v.getEnrollmentStatus(), EnrollmentStatus.Cancelled)) {
                    v.setEnrollmentStatus(EnrollmentStatus.Dropped);
                }

                // adjust units as necessary
                if (section.getMinimumUnits() == null) {
                    v.setUnits(null);
                } else if (v.getUnits() == null) {
                    v.setUnits(section.getMinimumUnits());
                } else if (section.getMinimumUnits() > v.getUnits()) {
                    v.setUnits(section.getMinimumUnits());
                } else if (section.getMaximumUnits() < v.getUnits()) {
                    v.setUnits(section.getMaximumUnits());
                }
            }
        });
    }

    private void cascadeUpdateFromCourse(Course course) {
        String misCode = course.getMisCode();
        String sisTermId = course.getSisTermId();
        String sisCourseId = course.getSisCourseId();
        String c_id = course.getC_id();

        // cascade update of C-ID
        database.forEach((k,v) -> {
            if (ObjectUtils.equals(misCode, v.getMisCode()) && ObjectUtils.equals(sisTermId, v.getSisTermId())
                    && ObjectUtils.equals(sisCourseId, v.getSisCourseId())) {
                v.setC_id(c_id);
            }
        });
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

    private void cascadeDelete(String misCode, String sisTermId, String sisSectionId, String sisPersonId, Boolean cascadeDelete, Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        if (sisSectionId != null) map.put("sisSectionId", sisSectionId);
        if (sisPersonId != null) map.put("sisPersonId", sisPersonId);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.afterAdd, this::afterAdd);
        addHook(EventType.afterUpdate, this::afterUpdate);

        // add cascade update hooks from Course, Section and Person
        courseDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromCourse(e.getReplacement()));
        sectionDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromSection(e.getReplacement()));
        personDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromPerson(e.getReplacement()));

        // add cascade delete hook from section
        sectionDB.addHook(EventType.beforeDelete, (e) -> {
            Section s = e.getOriginal();
            cascadeDelete(s.getMisCode(), s.getSisTermId(), s.getSisSectionId(), null, e.getCascadeDelete(), e.getTestDelete());
        });

        // add cascade delete hook from student
        studentDB.addHook(EventType.beforeDelete, (e) -> {
            Student s = e.getOriginal();
            cascadeDelete(s.getMisCode(), s.getSisTermId(), null, s.getSisPersonId(), e.getCascadeDelete(), e.getTestDelete());
        });
    }
}