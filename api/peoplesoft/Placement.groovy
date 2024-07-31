package api.peoplesoft

import org.springframework.core.env.Environment

/**
 * <h1>Placement (Deprecated)</h1>
 * <summary>Staged into PS, the placement data from the CCCAssess system</summary>
 * @Deprecated replaced by StudentPlacement class per updated MMPS specs
 */
@Deprecated
class Placement {

    //****** Populated by injection in Groovy Service Implementation ****
    Environment environment

    void post(String misCode, PlacementTransaction placementTrans) {
        throw new UnsupportedOperationException("Use StudentPlacement endpoint instead")
    }
}
