package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.apply.CCPromiseGrant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class CCPromiseGrantDB extends BaseMockDataDB<CCPromiseGrant> {

    private CollegeDB collegeDB;

    @Autowired
    public CCPromiseGrantDB(CollegeDB collegeDB) throws Exception {
        super("CCPromiseGrant.vm", CCPromiseGrant.class);
        this.collegeDB = collegeDB;
    }

    public CCPromiseGrant get(String misCode, long id) {
        return super.get(Arrays.asList(misCode, id));
    }

    public CCPromiseGrant update(String misCode, long id, CCPromiseGrant ccPromiseGrant) {
        return super.update(Arrays.asList(misCode, id), ccPromiseGrant);
    }

    public CCPromiseGrant patch(String misCode, long id, Map ccPromiseGrant) {
        return super.patch(Arrays.asList(misCode, id), ccPromiseGrant);
    }

    public CCPromiseGrant delete(String misCode, long id, boolean cascade) {
        return super.delete(Arrays.asList(misCode, id), cascade);
    }

    // MIS Code validation (college must be valid)
    private void checkParameters(EventDetail<CCPromiseGrant> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String collegeId = (String) parameters.get("collegeId");
        collegeDB.validate(collegeId);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
    }
}