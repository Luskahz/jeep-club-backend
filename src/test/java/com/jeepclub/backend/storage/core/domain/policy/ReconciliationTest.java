package com.jeepclub.backend.storage.core.domain.policy;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class ReconciliationTest {
    @Test void reportsMissingAndUnknownWithoutAssumingDeletion() {
        var missing = new Reconciliation.Location("LOCAL", "images", "missing");
        var unknown = new Reconciliation.Location("LOCAL", "images", "unknown");
        var present = new Reconciliation.Location("LOCAL", "images", "present");
        var report = new Reconciliation().compare(Map.of(missing, 1, present, 2), Set.of(present, unknown), true, true);
        assertThat(report.missing()).containsExactly(missing);
        assertThat(report.uncatalogued()).containsExactly(unknown);
        assertThat(report.duplicateCatalogLocations()).containsExactly(present);
        assertThatThrownBy(() -> new Reconciliation().compare(Map.of(), Set.of(), true, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
