package com.mystyryum.sgjhandhelddhd.database;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Manages the in-memory list of dimensions that have a default gate.
 * <p>
 * This allows blocks or other systems to quickly check for default gates
 * without reading the database each time. Thread-safe via synchronizedSet.
 */
public class DefaultGateManager {

    /** Thread-safe set of dimensions with a default gate */
    private static final Set<ResourceKey<Level>> DEFAULT_GATE_DIMENSIONS =
            Collections.synchronizedSet(new HashSet<>(GataBase.getDimensionList()));

    /**
     * Registers a dimension as having a default gate.
     *
     * @param dimensionKey the dimension to add
     */
    public static void addDimension(ResourceKey<Level> dimensionKey) {
        DEFAULT_GATE_DIMENSIONS.add(dimensionKey);
    }

    /**
     * Unregisters a dimension from having a default gate.
     *
     * @param dimensionKey the dimension to remove
     */
    public static void removeDimension(ResourceKey<Level> dimensionKey) {
        DEFAULT_GATE_DIMENSIONS.remove(dimensionKey);
    }

    /**
     * Checks if a dimension currently has a default gate.
     *
     * @param dimensionKey the dimension to check
     * @return true if the dimension has a default gate, false otherwise
     */
    public static boolean hasDefaultGate(ResourceKey<Level> dimensionKey) {
        return DEFAULT_GATE_DIMENSIONS.contains(dimensionKey);
    }

    /**
     * Returns a read-only view of all dimensions with default gates.
     *
     * @return an unmodifiable set of all default gate dimensions
     */
    public static Set<ResourceKey<Level>> getAllDefaultDimensions() {
        return Collections.unmodifiableSet(DEFAULT_GATE_DIMENSIONS);
    }

    /**
     * Clears all registered default gate dimensions.
     * <p>
     * Useful for resetting the manager during a reload or server shutdown.
     */
    public static void clear() {
        DEFAULT_GATE_DIMENSIONS.clear();
    }
}
