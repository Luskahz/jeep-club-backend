package com.jeepclub.backend.storage.core.domain.policy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Compares complete snapshots only. Findings do not authorize physical deletion. */
public final class Reconciliation {
    public record Location(String provider, String namespace, String key) implements Comparable<Location> {
        public Location { Objects.requireNonNull(provider); Objects.requireNonNull(namespace); Objects.requireNonNull(key); }
        @Override public int compareTo(Location other) {
            int result = provider.compareTo(other.provider);
            if (result == 0) result = namespace.compareTo(other.namespace);
            return result == 0 ? key.compareTo(other.key) : result;
        }
        @Override public String toString() { return "Location[redacted]"; }
    }
    public record Report(List<Location> missing, List<Location> uncatalogued, List<Location> sharedCatalogLocations) {}
    public Report compare(Map<Location, Integer> catalogCounts, Set<Location> physical, boolean completeCatalog, boolean completeInventory) {
        Objects.requireNonNull(catalogCounts); Objects.requireNonNull(physical);
        if (!completeCatalog || !completeInventory) throw new IllegalArgumentException("Complete snapshots required");
        if (catalogCounts.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 1) || physical.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Invalid inventory");
        var missing = new TreeSet<>(catalogCounts.keySet()); missing.removeAll(physical);
        var unknown = new TreeSet<>(physical); unknown.removeAll(catalogCounts.keySet());
        var duplicates = new TreeSet<Location>(); catalogCounts.forEach((k, v) -> { if (v > 1) duplicates.add(k); });
        return new Report(List.copyOf(missing), List.copyOf(unknown), List.copyOf(duplicates));
    }
}
