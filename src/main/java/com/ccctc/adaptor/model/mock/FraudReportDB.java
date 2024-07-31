package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.model.fraud.FraudReport;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
@Conditional(MockCondition.class)
public class FraudReportDB extends BaseMockDataDB<FraudReport> {

    private CollegeDB collegeDB;
    protected Long currentMaxId;

    @Autowired
    public FraudReportDB(CollegeDB collegeDB) throws Exception {
        super("FraudReports.vm", FraudReport.class);
        this.collegeDB = collegeDB;
        this.currentMaxId = 1L;
    }

    public Long getMaxId() {
        return this.currentMaxId;
    }

    public FraudReport getById(String misCode, long sisFraudReportId) {
        return super.get(Arrays.asList(misCode, sisFraudReportId));
    }

    public FraudReport deleteById(String misCode, long sisFraudReportId) {
        return super.delete(Arrays.asList(misCode, sisFraudReportId), false);
    }

    public void deleteFraudReport(Map<String,Object> deleteFilter) {
        List<FraudReport> results = find(deleteFilter);
        if( results.size() < 1 ) {
            throw new EntityNotFoundException("No fraud report found.");
        }
        super.deleteMany(deleteFilter, true, false);
    }

    public List<FraudReport> getMatching(String misCode, long appId, String cccId) {
        Map<String, Object> criteria = new HashMap<String, Object>();
        criteria.put("misCode", misCode);
        if(appId > 0l) {
            criteria.put("appId", appId);
        }
        if(StringUtils.isNotEmpty(cccId)) {
            criteria.put("cccId", cccId);
        }
        return super.find(criteria);
    }

    // MIS Code validation (college must be valid)
    private void checkParameters(EventDetail<FraudReport> eventDetail) {
        Map parameters = eventDetail.getParameters();
        String toCollegeId = (String) parameters.get("misCode");
        collegeDB.validate(toCollegeId);
    }

    private void beforeAdd(EventDetail<FraudReport> eventDetail) {
        String toCollegeId = eventDetail.getReplacement().getMisCode();
        String fromCollegeId = eventDetail.getReplacement().getReportedByMisCode();
        collegeDB.validate(toCollegeId);
        collegeDB.validate(fromCollegeId);
    }

    private void afterAdd(EventDetail<FraudReport> eventDetail) {
        Long newId = eventDetail.getReplacement().getSisFraudReportId();
        if(this.currentMaxId < newId) {
            this.currentMaxId = newId;
        }
    }

    @Override
    void registerHooks() {
        addHook(EventType.checkParameters, this::checkParameters);
        addHook(EventType.beforeAdd, this::beforeAdd);
        addHook(EventType.afterAdd, this::afterAdd);
    }
}