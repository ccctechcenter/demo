package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.College;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class CollegeDB extends BaseMockDataDB<College> {

    public CollegeDB() throws Exception {
        super("Colleges.vm", College.class);
    }

    public College get(String misCode) {
        return super.get(Collections.singletonList(misCode));
    }

    public College update(String misCode, College college) {
        return super.update(Collections.singletonList(misCode), college);
    }

    public College patch(String misCode, Map college) {
        return super.patch(Collections.singletonList(misCode), college);
    }

    public College delete(String misCode, boolean cascade) {
        return super.delete(Collections.singletonList(misCode), cascade);
    }

    public College validate(String misCode) {
        try {
            return get(misCode);
        } catch (EntityNotFoundException e) {
            throw new InvalidRequestException(InvalidRequestException.Errors.collegeNotFound,
                    "College not found: " + misCode);
        }
    }

    private void beforeAddOrUpdate(EventDetail<College> eventDetail) {
        College replacement = eventDetail.getReplacement();

        if (!replacement.getMisCode().matches("[0-9A-Z]{3}"))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "MIS Code must be 3 characters (numbers and uppercase letters only)");

        if (replacement.getDistrictMisCode() == null || !replacement.getDistrictMisCode().matches("[0-9A-Z]{3}"))
            throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "District MIS Code must be 3 characters (numbers and uppercase letters only)");
    }

    @Override
    void registerHooks() {
        addHook(EventType.beforeAdd, this::beforeAddOrUpdate);
        addHook(EventType.beforeUpdate, this::beforeAddOrUpdate);
    }
}
