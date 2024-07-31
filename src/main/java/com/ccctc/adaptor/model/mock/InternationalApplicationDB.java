package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.apply.InternationalApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class InternationalApplicationDB extends BaseMockDataDB<InternationalApplication> {

    private CollegeDB collegeDB;

    @Autowired
    public InternationalApplicationDB(CollegeDB collegeDB) throws Exception {
        super("InternationalApplication.vm", InternationalApplication.class);
        this.collegeDB = collegeDB;
    }

    public InternationalApplication get(String misCode, long id) {
        return super.get(Arrays.asList(misCode, id));
    }

    public InternationalApplication update(String misCode, long id, InternationalApplication application) {
        return super.update(Arrays.asList(misCode, id),application);
    }

    public InternationalApplication patch(String misCode, long id, Map application) {
        return super.patch(Arrays.asList(misCode, id), application);
    }

    public InternationalApplication delete(String misCode, long id, boolean cascade) {
        return super.delete(Arrays.asList(misCode, id), cascade);
    }

    // MIS Code validation (college must be valid)
    private void checkParameters(EventDetail<InternationalApplication> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String collegeId = (String) parameters.get("collegeId");
        collegeDB.validate(collegeId);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
    }
}