package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.Course;
import com.ccctc.adaptor.model.CrosslistingDetail;
import com.ccctc.adaptor.model.Section;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Component
@Conditional(MockCondition.class)
public class SectionDB extends BaseMockDataDB<Section> {

    private CollegeDB collegeDB;
    private TermDB termDB;
    private CourseDB courseDB;

    @Autowired
    public SectionDB(CollegeDB collegeDB, TermDB termDB, CourseDB courseDB) throws Exception {
        super("Sections.vm", Section.class);
        this.collegeDB = collegeDB;
        this.termDB = termDB;
        this.courseDB = courseDB;
    }

    public Section get(String misCode, String sisTermId, String sisSectionId) {
        return super.get(Arrays.asList(misCode, sisTermId, sisSectionId));
    }

    public Section update(String misCode, String sisTermId, String sisSectionId, Section section) {
        return super.update(Arrays.asList(misCode, sisTermId, sisSectionId), section);
    }

    public Section patch(String misCode, String sisTermId, String sisSectionId, Map section) {
        return super.patch(Arrays.asList(misCode, sisTermId, sisSectionId), section);
    }

    public Section delete(String misCode, String sisTermId, String sisSectionId, boolean cascade) {
        return super.delete(Arrays.asList(misCode, sisTermId, sisSectionId), cascade);
    }

    public Section validate(String misCode, String sisTermId, String sisSectionId) {
        try {
            return get(misCode, sisTermId, sisSectionId);
        } catch (EntityNotFoundException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound,
                    "Section not found: " + misCode + " " + sisTermId + " " + sisSectionId);
        }
    }

    private void afterAddAndUpdate(EventDetail<Section> eventDetail) {
        Section replacement = eventDetail.getReplacement();

        // clean up units so both min and max are populated
        if (replacement.getMaximumUnits() == null && replacement.getMinimumUnits() != null)
            replacement.setMaximumUnits(replacement.getMinimumUnits());
        if (replacement.getMinimumUnits() == null && replacement.getMaximumUnits() != null)
            replacement.setMinimumUnits(replacement.getMaximumUnits());

        // update values from course
        Course course = courseDB.get(replacement.getMisCode(), replacement.getSisTermId(), replacement.getSisCourseId());

        if (replacement.getTitle() == null) replacement.setTitle(course.getTitle());
        if (replacement.getMinimumUnits() == null) replacement.setMinimumUnits(course.getMinimumUnits());
        if (replacement.getMaximumUnits() == null) replacement.setMaximumUnits(course.getMaximumUnits());

        // set start and end dates to be the same as the course if left blank
        if (replacement.getStart() == null) replacement.setStart(course.getStart());
        if (replacement.getEnd() == null) replacement.setEnd(course.getEnd());

        // remove time component
        replacement.setStart(MockUtils.removeTime(replacement.getStart()));
        replacement.setEnd(MockUtils.removeTime(replacement.getEnd()));

        // verify start and end dates
        if (replacement.getStart().after(course.getEnd()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section start date cannot be after course end date");
        if (replacement.getEnd().before(course.getStart()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section end date cannot be before course start date");
        if (replacement.getStart().after(replacement.getEnd()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section start date cannot be after section end date");

        // verify units
        if (replacement.getMinimumUnits() != null && course.getMinimumUnits() != null) {
            if (replacement.getMinimumUnits() > replacement.getMaximumUnits())
                throw new InvalidRequestException("Minimum units cannot be greater than maximum units");

            if (replacement.getMinimumUnits() > course.getMaximumUnits() || replacement.getMaximumUnits() < course.getMinimumUnits())
                throw new InvalidRequestException("Section units out of range of course");
        }
    }

    private void checkParameters(EventDetail<Section> eventDetail) {
        Map parameters = eventDetail.getParameters();

        String misCode = (String) parameters.get("misCode");
        String sisTermId = (String) parameters.get("sisTermId");
        String sisCourseId = (String) parameters.get("sisCourseId");

        if (misCode != null)
            collegeDB.validate(misCode);

        if (misCode != null && sisTermId != null)
            termDB.validate(misCode, sisTermId);

        if (misCode != null && sisTermId != null && sisCourseId != null)
            courseDB.validate(misCode, sisTermId, sisCourseId);
    }

    private void beforeAdd(EventDetail<Section> eventDetail) {
        Section replacement = eventDetail.getReplacement();

        if (replacement.getSisCourseId() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sisCourseId is a required field");

        if (replacement.getCrosslistingDetail() != null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "crosslistingDetail cannot be added during section creation. Create section first, then crosslist.");

        courseDB.validate(replacement.getMisCode(), replacement.getSisTermId(), replacement.getSisCourseId());
    }

    private void beforeUpdate(EventDetail<Section> eventDetail) {
        Section original = eventDetail.getOriginal();
        Section replacement = eventDetail.getReplacement();

        if (replacement.getSisCourseId() != null && !ObjectUtils.equals(original.getSisCourseId(), replacement.getSisCourseId()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "sisCourseId cannot be changed");

        if (!ObjectUtils.equals(original.getCrosslistingDetail(), replacement.getCrosslistingDetail()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "crosslistingDetail cannot be changed by a section update. Delete the crosslisting then create a new one.");

        replacement.setSisCourseId(original.getSisCourseId());
    }

    private void cascadeDelete(String misCode, String sisTermId, String sisCourseId, Boolean cascadeDelete, Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);
        map.put("sisTermId", sisTermId);
        map.put("sisCourseId", sisCourseId);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.afterAdd, this::afterAddAndUpdate);
        addHook(EventType.afterUpdate, this::afterAddAndUpdate);
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.beforeAdd, this::beforeAdd);
        addHook(EventType.beforeUpdate, this::beforeUpdate);

        // cascade delete from course
        courseDB.addHook(EventType.beforeDelete, (e) -> {
            Course c = e.getOriginal();
            cascadeDelete(c.getMisCode(), c.getSisTermId(), c.getSisCourseId(), e.getCascadeDelete(), e.getTestDelete());
        });
    }

    /**
     * Copy a section from one term to another
     *
     * @param misCode      MIS Code
     * @param sisTermId    SIS Term ID
     * @param sisSectionId SIS Section ID
     * @param newSisTermId New SIS Term ID
     * @return New Section
     */
    public Section copy(String misCode, String sisTermId, String sisSectionId, String newSisTermId) {
        final Section record = get(misCode, sisTermId, sisSectionId);

        Section newSection = deepCopy(record);
        newSection.setSisTermId(newSisTermId);

        // clear so they default from term
        newSection.setStart(null);
        newSection.setEnd(null);

        add(newSection);

        return newSection;
    }

    /***
     * Create a crosslisting given a primary section and list of secondary sections
     *
     * @param misCode MIS Code
     * @param sisTermId SIS Term ID
     * @param primarySisSectionId Primary SIS Section ID
     * @param secondarySisSectionIds Secondary SIS Section ID(s)
     * @return
     */
    public CrosslistingDetail createCrosslisting(String misCode, String sisTermId, String primarySisSectionId, List<String> secondarySisSectionIds) {
        List<Section> sections = new ArrayList<>();

        sections.add(crosslistSectionValidate(misCode, sisTermId, primarySisSectionId));

        for (String secondary : secondarySisSectionIds)
            sections.add(crosslistSectionValidate(misCode, sisTermId, secondary));

        for (Section section : sections) {
            if (section.getCrosslistingDetail() != null)
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section " + section.getSisSectionId() + " is already crosslisted");
        }

        sections = sections.stream().distinct().collect(toList());

        if (sections.size() <= 1)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "A crosslisting must have two or more sections");

        CrosslistingDetail crosslistingDetail = new CrosslistingDetail();
        crosslistingDetail.setName("XL" + sisTermId + primarySisSectionId); // arbitrary unique name in the term
        crosslistingDetail.setSisTermId(sisTermId);
        crosslistingDetail.setPrimarySisSectionId(primarySisSectionId);
        crosslistingDetail.setSisSectionIds(sections.stream().map(i -> i.getSisSectionId()).collect(toList()));

        for (Section section : sections) {
            section.setCrosslistingDetail(crosslistingDetail);
        }

        return crosslistingDetail;
    }

    /**
     * Delete an entire crosslisting given any section in the crosslisting
     *
     * @param misCode      MIS Code
     * @param sisTermId    SIS Term ID
     * @param sisSectionId SIS Section ID of any section in the crosslisting
     * @return
     */
    public CrosslistingDetail deleteCrosslisting(String misCode, String sisTermId, String sisSectionId) {
        Section section = validate(misCode, sisTermId, sisSectionId);
        CrosslistingDetail result = null;

        if (section.getCrosslistingDetail() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Section is not crosslisted");

        List<Section> sections = section.getCrosslistingDetail().getSisSectionIds()
                .stream().map(i -> crosslistSectionValidate(misCode, sisTermId, i)).collect(toList());

        if (sections.size() > 0)
            result = sections.get(0).getCrosslistingDetail();

        for (Section s : sections) {
            s.setCrosslistingDetail(null);
        }

        return result;
    }

    /**
     * Get the original record from the database (not a copy) for manipulation when we crosslist
     *
     * @param misCode      MIS Code
     * @param sisTermId    SIS Term ID
     * @param sisSectionId SIS Section ID
     * @return Section
     */
    private Section crosslistSectionValidate(String misCode, String sisTermId, String sisSectionId) {
        Section section = database.get(Arrays.asList((Object) misCode, sisTermId, sisSectionId));

        if (section == null) {
            throw new InvalidRequestException(InvalidRequestException.Errors.sectionNotFound,
                    "Section not found: " + misCode + " " + sisTermId + " " + sisSectionId);
        }

        return section;
    }
}