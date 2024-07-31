package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.MockCondition;
import com.ccctc.adaptor.model.Person;
import com.ccctc.adaptor.model.PrimaryKey;
import com.ccctc.adaptor.model.placement.PlacementTransaction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Conditional(MockCondition.class)
public class PlacementDB extends BaseMockDataDB<PlacementDB.MockPlacementTransaction> {

    private PersonDB personDB;

    @Autowired
    public PlacementDB(PersonDB personDB) throws Exception {
        super("Placements.vm", PlacementDB.MockPlacementTransaction.class);
        this.personDB = personDB;
    }

    public MockPlacementTransaction get(int id) {
        return super.get(Arrays.asList(id));
    }

    public MockPlacementTransaction delete(int id, boolean cascade) {
        return super.delete(Arrays.asList(id), cascade);
    }

    private void beforeAddOrUpdate(EventDetail<MockPlacementTransaction> eventDetail) {
        MockPlacementTransaction replacement = eventDetail.getReplacement();
        Person p = personDB.validate(replacement.getPlacementTransaction().getMisCode(), replacement.getSisPersonId(), replacement.placementTransaction.getCccid());

        replacement.setSisPersonId(p.getSisPersonId());
        replacement.getPlacementTransaction().setCccid(p.getCccid());
    }

    private void cascadeDelete(String misCode, String sisPersonId, Boolean cascadeDelete, Boolean testDelete) {
        List<List<Object>> keysToDelete = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("sisPersonId", sisPersonId);
        List<MockPlacementTransaction> records = super.find(map);

        for(MockPlacementTransaction r : records) {
            if (r.getPlacementTransaction().getMisCode().equals(misCode))
                keysToDelete.add(getPrimaryKey(r));
        }

        deleteMany(keysToDelete, cascadeDelete, testDelete);
    }

    private void cascadeUpdateFromPerson(Person person) {
        String misCode = person.getMisCode();
        String sisPersonId = person.getSisPersonId();
        String cccId = person.getCccid();

        // cascade update of CCC ID
        database.forEach((k, v) -> {
            if (ObjectUtils.equals(misCode, v.getPlacementTransaction().getMisCode()) && ObjectUtils.equals(sisPersonId, v.getSisPersonId())) {
                v.placementTransaction.setCccid(cccId);
            }
        });
    }

    @Override
    void registerHooks() {
        addHook(EventType.beforeAdd, this::beforeAddOrUpdate);
        addHook(EventType.beforeUpdate, this::beforeAddOrUpdate);

        // cascade update from person
        personDB.addHook(EventType.afterUpdate, (e) -> cascadeUpdateFromPerson(e.getReplacement()));

        // cascade delete from person
        personDB.addHook(EventType.beforeDelete, (e) -> {
            Person p = e.getOriginal();
            cascadeDelete(p.getMisCode(), p.getSisPersonId(), e.getCascadeDelete(), e.getTestDelete());
        });
    }

    @ToString
    @Getter
    @Setter
    @EqualsAndHashCode(exclude = "id")
    @PrimaryKey("id")
    @ApiModel
    public static class MockPlacementTransaction implements Serializable {
        private static AtomicInteger keyCounter = new AtomicInteger(1);

        @ApiModelProperty(value = "auto-generated primary key", example = "1")
        private final int id;

        @ApiModelProperty(value = "SIS Person ID", example = "111")
        private String sisPersonId;

        @ApiModelProperty(value = "Placement transaction")
        private PlacementTransaction placementTransaction;

        private MockPlacementTransaction() {
            this.id = keyCounter.getAndIncrement();
        }

        public MockPlacementTransaction(String sisPersonId, @NonNull PlacementTransaction placementTransaction) {
            this();
            this.sisPersonId = sisPersonId;
            this.placementTransaction = placementTransaction;
        }
    }
}
