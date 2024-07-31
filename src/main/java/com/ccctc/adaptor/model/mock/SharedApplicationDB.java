package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.apply.SharedApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@Conditional(MockCondition.class)
public class SharedApplicationDB extends BaseMockDataDB<SharedApplication> {

    private CollegeDB collegeDB;

    @Autowired
    public SharedApplicationDB(CollegeDB collegeDB) throws Exception {
        super("SharedApplication.vm", SharedApplication.class);
        this.collegeDB = collegeDB;
    }

    public SharedApplication get(String misCode, long id) {
        return super.get(Arrays.asList(misCode, id));
    }

    public SharedApplication update(String misCode, long id, SharedApplication application) {
        return super.update(Arrays.asList(misCode, id), application);
    }

    public SharedApplication patch(String misCode, long id, Map application) {
        return super.patch(Arrays.asList(misCode, id), application);
    }

    public SharedApplication delete(String misCode, long id, boolean cascade) {
        return super.delete(Arrays.asList(misCode, id), cascade);
    }

    // MIS Code validation (college must be valid)
    private void checkParameters(EventDetail<SharedApplication> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String teachMisCode = (String) parameters.get("misCode");
        collegeDB.validate(teachMisCode);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
    }
}