package api.colleague

import com.ccctc.adaptor.model.placement.PlacementTransaction
import com.ccctc.adaptor.util.ClassMap
import groovy.transform.CompileStatic
import org.springframework.core.env.Environment

/**
 * <h1>Placement (Deprecated)</h1>
 * <summary>Staged into colleague, the placement data from the CCCAssess system</summary>
 * @Deprecated replaced by StudentPlacement class per updated MMPS specs
 */
@CompileStatic
@Deprecated
class Placement {

    void colleagueInit(String misCode, Environment environment, ClassMap services) {
        throw new UnsupportedOperationException("Use StudentPlacement endpoint instead")
    }

    void post(String misCode, PlacementTransaction placementTrans) {
        throw new UnsupportedOperationException("Use StudentPlacement endpoint instead")
    }
}
