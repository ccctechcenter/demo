package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.Term;
import com.ccctc.adaptor.util.mock.MockUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Conditional(MockCondition.class)
public class TermDB extends BaseMockDataDB<Term> {

    private CollegeDB collegeDB;

    @Autowired
    public TermDB(CollegeDB collegeDB) throws Exception {
        super("Terms.vm", Term.class);
        this.collegeDB = collegeDB;
    }

    public Term get(String misCode, String sisTermId) {
        return super.get(Arrays.asList(misCode, sisTermId));
    }

    public Term update(String misCode, String sisTermId, Term term) {
        return super.update(Arrays.asList(misCode, sisTermId), term);
    }

    public Term patch(String misCode, String sisTermId, Map term) {
        return super.patch(Arrays.asList(misCode, sisTermId), term);
    }

    public Term delete(String misCode, String sisTermId, boolean cascade) {
        return super.delete(Arrays.asList(misCode, sisTermId), cascade);
    }

    public Term validate(String misCode, String sisTermId) {
        try {
            return get(misCode, sisTermId);
        } catch (EntityNotFoundException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.termNotFound,
                    "Term not found: " + misCode + " " + sisTermId);
        }
    }

    private void beforeAddOrUpdate(EventDetail<Term> eventDetail) {
        Term replacement = eventDetail.getReplacement();

        if (replacement.getStart() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Term start date cannot be null");
        if (replacement.getEnd() == null)
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Term end date cannot be null");

        replacement.setStart(MockUtils.removeTime(replacement.getStart()));
        replacement.setEnd(MockUtils.removeTime(replacement.getEnd()));

        if (replacement.getStart().after(replacement.getEnd()))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Term start date cannot be after term end date");
    }

    private void checkParameters(EventDetail<Term> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String misCode = (String) parameters.get("misCode");
        if (misCode != null)
            collegeDB.validate(misCode);
    }

    private void cascadeDelete(String misCode, Boolean cascadeDelete, Boolean testDelete) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);

        deleteMany(map, cascadeDelete, testDelete);
    }

    @Override
    void registerHooks() {
        addHook(EventType.beforeAdd, this::beforeAddOrUpdate);
        addHook(EventType.beforeUpdate, this::beforeAddOrUpdate);
        addHook(EventType.checkParameters, this::checkParameters);

        // add cascade delete hook for college
        collegeDB.addHook(EventType.beforeDelete, (e) ->
                cascadeDelete(e.getOriginal().getMisCode(), e.getCascadeDelete(), e.getTestDelete()));
    }

    /**
     * Find the "current term". This is defined as the first term that the ends on or after today's date.
     *
     * @param misCode MIS Code
     * @return Current term
     */
    public Term getCurrentTerm(String misCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("misCode", misCode);

        List<Term> all = super.find(map);

        Date today = new Date();

        Optional<Term> term = all.stream()
                .filter(i -> i.getEnd() != null && !i.getEnd().before(today))
                .sorted(Comparator.comparingLong(i -> i.getEnd().getTime()))
                .findFirst();

        if (term.isPresent())
            return term.get();

        throw new EntityNotFoundException("No current term found");
    }
}
