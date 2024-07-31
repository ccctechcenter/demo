package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.Course;
import com.ccctc.adaptor.model.Term;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class CourseDB extends BaseMockDataDB<Course> {

    private TermDB termDB;

    @Autowired
    public CourseDB(TermDB termDB) throws Exception {
        super("Courses.vm", Course.class);
        this.termDB = termDB;
    }

    public Course get(String misCode, String sisTermId, String sisCourseId) {
        termDB.validate(misCode, sisTermId);
        return super.get(Arrays.asList(misCode, sisTermId, sisCourseId));
    }

    public Course update(String misCode, String sisTermId, String sisCourseId, Course course) {
        termDB.validate(misCode, sisTermId);
        return super.update(Arrays.asList(misCode, sisTermId, sisCourseId), course);
    }

    public Course patch(String misCode, String sisTermId, String sisCourseId, Map course) {
        termDB.validate(misCode, sisTermId);
        return super.patch(Arrays.asList(misCode, sisTermId, sisCourseId), course);
    }

    public Course delete(String misCode, String sisTermId, String sisCourseId, boolean cascade) {
        termDB.validate(misCode, sisTermId);
        return super.delete(Arrays.asList(misCode, sisTermId, sisCourseId), cascade);
    }

    public Course validate(String misCode, String sisTermId, String sisCourseId) {
        try {
            return get(misCode, sisTermId, sisCourseId);
        } catch (EntityNotFoundException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound,
                    "Course not found: " + misCode + " " + sisTermId + " " + sisCourseId);
        }
    }

    public List<Course> validate(String misCode, String sisCourseId) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisCourseId", sisCourseId);

        List<Course> courses = find(map);

        if (courses.size() == 0)
            throw new InvalidRequestException(InvalidRequestException.Errors.courseNotFound,
                    "Course not found: " + misCode + " " + sisCourseId);

        return courses;
    }

    private Course getPrereq(Course course, String prereqSisCourseId) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", course.getMisCode());
        map.put("sisCourseId", prereqSisCourseId);

        List<Course> courses = find(map);

        // find the course with a start date just prior to this one
        if (!courses.isEmpty()) {
            courses.sort((a, b) -> -a.getStart().compareTo(b.getStart()));
            for (Course c : courses) {
                if (c.getStart().before(course.getStart()))
                    return c;
            }

            // no match? use earliest
            return courses.get(courses.size()-1);
        }

        return null;
    }

    private Course getCoreq(Course course, String coreqSisCourseId) {
        try {
            // co-requisites may point to each other, so bypass hooks to avoid an indefinite loop
            return getNoHooks(Arrays.asList(course.getMisCode(), course.getSisTermId(), coreqSisCourseId));
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    private void populateRequisiteCourses(Course course) {
        if (course.getPrerequisiteList() != null) {
            List<Course> courses = new ArrayList<>();
            for (Course c : course.getPrerequisiteList()) {
                Course r = getPrereq(course, c.getSisCourseId());
                if (r != null) courses.add(r);
            }
            course.setPrerequisiteList(courses);
        }

        if (course.getCorequisiteList() != null) {
            List<Course> courses = new ArrayList<>();
            for (Course c : course.getCorequisiteList()) {
                Course r = getCoreq(course, c.getSisCourseId());
                if (r != null) courses.add(r);
            }
            course.setCorequisiteList(courses);
        }
    }

    private void beforeAdd(EventDetail<Course> eventDetail) {
        Course replacement = eventDetail.getReplacement();
        termDB.validate(replacement.getMisCode(), replacement.getSisTermId());
    }

    private void afterGet(EventDetail<Course> eventDetail) {
        // update prerequisites and corequisites with full course records
        populateRequisiteCourses(eventDetail.getOriginal());
    }

    private void afterAddAndUpdate(EventDetail<Course> eventDetail) {
        Course replacement = eventDetail.getReplacement();

        // set start and end dates to be the same as the term if left blank
        Term term = termDB.get(replacement.getMisCode(), replacement.getSisTermId());
        if (replacement.getStart() == null) replacement.setStart(term.getStart());
        if (replacement.getEnd() == null) replacement.setEnd(term.getEnd());

        // remove time component
        replacement.setStart(MockUtils.removeTime(replacement.getStart()));
        replacement.setEnd(MockUtils.removeTime(replacement.getEnd()));

        // verify start and end dates
        if (replacement.getStart().after(term.getEnd()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Course start date cannot be after term end date");
        if (replacement.getEnd().before(term.getStart()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Course end date cannot be before term start date");
        if (replacement.getStart().after(replacement.getEnd()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Course start date cannot be after course end date");

        // verify and cleanse prereqs (only keep misCode and sisCourseId)
        if (replacement.getPrerequisiteList() != null) {
            List<Course> cleansed = new ArrayList<>();

            for (Course p : replacement.getPrerequisiteList()) {
                if (p.getMisCode() != null && !p.getMisCode().equals(replacement.getMisCode()))
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MIS Code of prerequisite cannot be different than MIS Code of course");

                if (getPrereq(replacement, p.getSisCourseId()) == null)
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Invalid prerequisite: " + p.getSisCourseId());

                cleansed.add(new Course.Builder().misCode(replacement.getMisCode()).sisCourseId(p.getSisCourseId()).build());
            }

            replacement.setPrerequisiteList(cleansed);
        }

        // verify and cleanse coreqs (only keep misCode and sisCourseId)
        if (replacement.getCorequisiteList() != null) {
            List<Course> cleansed = new ArrayList<>();

            for (Course p : replacement.getCorequisiteList()) {
                if (p.getMisCode() != null && !p.getMisCode().equals(replacement.getMisCode()))
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MIS Code of corequisite cannot be different than MIS Code of course");

                if (getCoreq(replacement, p.getSisCourseId()) == null)
                    throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Invalid corequisite: " + p.getSisCourseId() + ". Note - corequisite must have course definition in same term to be valid.");

                cleansed.add(new Course.Builder().misCode(replacement.getMisCode()).sisCourseId(p.getSisCourseId()).build());
            }

            replacement.setCorequisiteList(cleansed);
        }

        // fix units
        if (replacement.getMaximumUnits() == null)
            replacement.setMaximumUnits(replacement.getMinimumUnits());
    }

    private void cascadeDelete(String misCode, String sisTermId, Boolean cascadeDelete, Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.beforeAdd, this::beforeAdd);
        addHook(EventType.afterGet, this::afterGet);
        addHook(EventType.afterAdd, this::afterAddAndUpdate);
        addHook(EventType.afterUpdate, this::afterAddAndUpdate);

        // cascade delete hook from term
        termDB.addHook(EventType.beforeDelete, (e) -> {
            Term t = e.getOriginal();
            cascadeDelete(t.getMisCode(), t.getSisTermId(), e.getCascadeDelete(), e.getTestDelete());
        });
    }

    /**
     * Copy a course from one term to another
     *
     * @param misCode      MIS Code
     * @param sisTermId    SIS Term ID
     * @param sisCourseId  SIS Course ID
     * @param newSisTermId New SIS Term ID
     * @return New Course
     */
    public Course copy(String misCode, String sisTermId, String sisCourseId, String newSisTermId) {
        final Course record = get(misCode, sisTermId, sisCourseId);

        Course newCourse = deepCopy(record);
        newCourse.setSisTermId(newSisTermId);

        // clear dates so they default from the term
        newCourse.setStart(null);
        newCourse.setEnd(null);

        add(newCourse);

        return newCourse;
    }
}
