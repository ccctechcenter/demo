package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.placement.StudentPlacementData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
@Component
@Conditional(MockCondition.class)
public class StudentPlacementDB extends BaseMockDataDB<StudentPlacementData> {

    private CollegeDB collegeDB;

    @Autowired
    public StudentPlacementDB(CollegeDB collegeDB) throws Exception {
        super("StudentPlacement.vm", StudentPlacementData.class);
        this.collegeDB = collegeDB;
    }

    public StudentPlacementData get(String miscode, String californiaCommunityCollegeId, String statewideStudentId) {
        return super.get(Arrays.asList(miscode, californiaCommunityCollegeId, statewideStudentId));
    }
    @Override
    public StudentPlacementData add(StudentPlacementData placementData) {
        return super.add(placementData);
    }

    private void checkParameters(EventDetail<StudentPlacementData> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String collegeId = (String) parameters.get("miscode");
        collegeDB.validate(collegeId);
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
    }
}

