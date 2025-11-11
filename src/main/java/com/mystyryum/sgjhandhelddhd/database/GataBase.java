package com.mystyryum.sgjhandhelddhd.database;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mystyryum.sgjhandhelddhd.Config;
import mcp.client.Start;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.mystyryum.sgjhandhelddhd.database.GateObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.data.*;
import net.povstalec.sgjourney.common.sgjourney.Galaxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GataBase {

// =====================
// GataBase Utility Fields
// =====================

    /**
     * Temporary list used to store GateObjects that are being added but not yet persisted to JSON.
     * Acts as a staging area before save.
     */
    private List<GateObject> appendingList = new ArrayList<>();

    /** Constant representing the admin UUID. Used for gates with no specific owner. */
    private static final UUID ADMIN_UUID = new UUID(0L, 0L);

    /** Indicates whether a save operation is currently in progress. */
    private boolean isSaving = false;

    /** Flag to indicate a save request occurred while another save was in progress. */
    private boolean saveQueued = false;

    /** File used as the main persistent database for gates. */
    private File mainDatabaseFile = new File("plugins/SGHDHD/GataBase.json");

    /** Backup file used in case the main database becomes corrupted. */
    private File backupDatabaseFile = new File("plugins/SGHDHD/backup.json");

    /** Lock used to ensure thread safety across all database operations. */
    private final Lock databaseLock = new ReentrantLock();

    /** Dirty flag indicating whether there are unsaved changes in the database. */
    private boolean isDirty;

    /** Logger instance for structured logging (replaces System.out calls). */
    private static final Logger LOGGER = LoggerFactory.getLogger(GataBase.class);


    //=== Constructor ===//
    public GataBase(File mainFile, File backupFile) {
        // initializing data structures

        this.mainDatabaseFile = mainFile;
        this.backupDatabaseFile = backupFile;
        this.isDirty = false;


        ensureDatabaseIntegrity();

    }

    // === Public Methods ===
    /**
     * Adds a new GateObject to the database.
     * Performs uniqueness checks for name and address before adding.
     *
     * @param gate            The GateObject to add
     * @param offendingPlayer The UUID of the player attempting the add; if null, defaults to ADMIN_UUID
     * @return A message indicating success or the type of uniqueness conflict
     */
    public String addGate(GateObject gate, UUID offendingPlayer) {
        // Check for duplicate name or address
        int checkVal = gateUniquenessCheck(gate, null);

        switch (checkVal) {
            case 1:
                LOGGER.warn("Attempted to add a gate with duplicate name: {}", gate.getName());
                return "A Gate with this name already exists!!";
            case 2:
                LOGGER.warn("Attempted to add a gate with duplicate address: {}", Arrays.toString(gate.getChevrons()));
                return "A Gate with this address already exists!!";
            case 3:
                LOGGER.warn("Attempted to add a gate with duplicate name and address: {}", gate.getName());
                return "A Gate with this name and address already exists!!";
            default:
                break;
        }

        // If no player provided, assign the gate to ADMIN
        if (offendingPlayer == null) {
            offendingPlayer = ADMIN_UUID;
            LOGGER.info("No player provided for gate '{}'; assigning ADMIN_UUID as creator.", gate.getName());
        }

        gate.setCreator(offendingPlayer);

        // Add gate to the in-memory queue for saving
        appendingList.add(gate);
        LOGGER.info("Gate '{}' added to appending list for saving.", gate.getName());

        // Mark database as dirty to trigger save
        setDirty(true, true, null, null);

        return "Add Successful";
    }

    /**
     * Attempts to remove a gate from the database.
     * Only the creator or an admin (ADMIN_UUID) can delete the gate.
     *
     * @param target The gate object to remove
     * @param offendingPlayer The UUID of the player attempting deletion
     * @return A status message indicating success or failure
     */
    public String removeGate(GateObject target, UUID offendingPlayer) {
        // Check if the player has permission to delete: either they created it or are admin
        boolean canDelete = target.getCreator().equals(offendingPlayer) || target.getCreator().equals(ADMIN_UUID);

        if (canDelete) {
            // Log the deletion attempt for auditing/debugging purposes
            LOGGER.warn("Player {} deleting gate '{}'", offendingPlayer, target.getName());

            // Mark the database as dirty to trigger a save and remove the gate
            setDirty(true, false, target, null);

            // Return success message
            return "Removal successful";
        } else {
            // Log unauthorized deletion attempt
            LOGGER.error("Player {} attempted to delete gate '{}' without permission", offendingPlayer, target.getName());

            // Return failure message
            return "Removal unsuccessful";
        }
    }


    /**
     * Attempts to edit an existing GateObject in the database.
     * Only the creator or an admin (ADMIN_UUID) can perform edits.
     *
     * @param target          The gate to be edited
     * @param updatedTarget   The gate containing the updated data
     * @param offendingPlayer The UUID of the player attempting the edit
     * @return A message indicating the result of the edit operation
     */
    public String editGate(GateObject target, GateObject updatedTarget, UUID offendingPlayer) {

        // Check if the player has permission to edit (creator or admin)
        if (!target.getCreator().equals(offendingPlayer) && !target.getCreator().equals(ADMIN_UUID)) {
            return "You do not have permission to edit";
        }

        LOGGER.info("Player {} is attempting to edit gate '{}'", offendingPlayer, target.getName());

        // Load the current list of gates from the JSON database
        Gson gson = new Gson();
        List<GateObject> gates;
        try (FileReader reader = new FileReader(mainDatabaseFile)) {
            Type listType = new TypeToken<List<GateObject>>() {}.getType();
            gates = gson.fromJson(reader, listType);
        } catch (IOException e) {
            LOGGER.error("Failed to read database file", e);
            gates = new ArrayList<>();
        }

        if (gates == null) {
            gates = new ArrayList<>();
        }

        // Remove the target gate to perform a uniqueness check without counting itself
        Iterator<GateObject> iterator = gates.iterator();
        while (iterator.hasNext()) {
            GateObject gate = iterator.next();
            if (gate.getName().equals(target.getName())) {
                iterator.remove();
            }
        }

        // Check if the updated gate conflicts with existing gates
        int result = gateUniquenessCheck(updatedTarget, gates);

        switch (result) {
            case 0:
                // No conflicts: mark database as dirty for saving
                setDirty(true, false, target, updatedTarget);
                LOGGER.info("Gate '{}' successfully edited by player {}", target.getName(), offendingPlayer);
                return "Gate edited successfully";
            case 1:
                return "Edit contains duplicate name";
            case 2:
                return "Edit contains duplicate address";
            case 3:
                return "Edit contains duplicate names and addresses";
            default:
                return "Error editing gate";
        }
    }

    /**
     * Marks the database as dirty (having unsaved changes) and optionally triggers a save.
     *
     * @param dirty          true if there are unsaved changes
     * @param save           true to trigger an immediate save
     * @param target         the gate being edited/removed (can be null for additions)
     * @param updatedTarget  the new gate data for edits (can be null)
     */
    public void setDirty(boolean dirty, boolean save, GateObject target, GateObject updatedTarget) {
        this.isDirty = dirty; // Update dirty flag

        if (dirty) {
            LOGGER.info("[GataBase] Database marked as dirty. Save triggered: {}", save);
            onDirty(save, target, updatedTarget); // Start the save process if necessary
        }
    }



    //===private/helper methods===//

    /**
     * Triggered when the database is marked as dirty (has unsaved changes).
     * Starts the save process if no save is currently in progress.
     * If a save is already running, queues another save to run afterward.
     *
     * @param save          true if this change is a new addition to be saved
     * @param target        the original GateObject being modified (for edits/removals)
     * @param updatedTarget the updated GateObject (for edits)
     */
    private synchronized void onDirty(boolean save, GateObject target, GateObject updatedTarget) {
        if (isSaving) {
            // A save is already in progress, queue this change for the next save
            LOGGER.info("Save queue started. Another save will run after current save completes.");
            saveQueued = true;
            return;
        }

        // No save in progress, proceed immediately
        LOGGER.info("Database marked dirty. Initiating save process...");
        saveData(save, target, updatedTarget);
    }


    /**
     * Saves the current Gatabase state to disk.
     *
     * @param save          true if adding new gates, false if editing/removing
     * @param target        the original gate (used for edit/remove operations)
     * @param updatedTarget the updated gate (used for edit/replace operations)
     */
    private void saveData(boolean save, GateObject target, GateObject updatedTarget) {
        isSaving = true;

        // Acquire lock to prevent concurrent access to database file
        databaseLock.lock();
        try {
            do {
                // Take a snapshot of pending gates to save.
                // This avoids concurrent modification of appendingList during the save.
                List<GateObject> procList = new ArrayList<>(appendingList);

                // Reset save queue flag
                saveQueued = false;

                LOGGER.info("Saving Gatabase...");

                Gson gson = new Gson();
                List<GateObject> gates;

                // === Read current database from JSON file ===
                try (FileReader reader = new FileReader(mainDatabaseFile)) {
                    Type listType = new TypeToken<List<GateObject>>() {}.getType();
                    gates = gson.fromJson(reader, listType);
                } catch (IOException e) {
                    LOGGER.error("Failed to read Gatabase file, starting with empty list", e);
                    gates = new ArrayList<>();
                }

                // Ensure we have a non-null list
                if (gates == null) {
                    gates = new ArrayList<>();
                }

                // === Perform save operation ===
                if (save) {
                    // Add all new gates from the pending list
                    gates.addAll(procList);
                } else {
                    // Remove the target gate (for edit/remove)
                    if (target != null) {
                        gates.removeIf(g -> g.getName().equals(target.getName()));
                    }

                    // Add updated gate (only for edit operation)
                    if (updatedTarget != null) {
                        gates.add(updatedTarget);
                    }
                }

                // === Write updated list back to disk ===
                try (FileWriter writer = new FileWriter(mainDatabaseFile)) {
                    gson.toJson(gates, writer);
                } catch (IOException e) {
                    LOGGER.error("Failed to write Gatabase file", e);
                }

                // Remove processed gates from appending list
                appendingList.removeAll(procList);

            } while (saveQueued); // Repeat if a new save was queued during this save

        } finally {
            // Always release resources and reset flags
            LOGGER.info("Save Complete");
            isSaving = false;
            databaseLock.unlock();
        }
    }



    /**
     * Ensures that the main and backup database files exist.
     * Creates missing files and parent directories if necessary.
     * Logs creation and errors using the LOGGER.
     */
    private void ensureDatabaseIntegrity() {
        // Ensure main database file exists
        if (!mainDatabaseFile.exists()) {
            try {
                // Create parent directories if needed
                if (mainDatabaseFile.getParentFile() != null) {
                    mainDatabaseFile.getParentFile().mkdirs();
                }

                // Create the main file
                mainDatabaseFile.createNewFile();
                LOGGER.info("[GataBase] Created new main database file: {}", mainDatabaseFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("[GataBase] Failed to create main database file: {}", mainDatabaseFile.getAbsolutePath(), e);
            }
        }

        // Ensure backup database file exists
        if (!backupDatabaseFile.exists()) {
            try {
                // Create parent directories if needed
                if (backupDatabaseFile.getParentFile() != null) {
                    backupDatabaseFile.getParentFile().mkdirs();
                }

                // Create the backup file
                backupDatabaseFile.createNewFile();
                LOGGER.info("[GataBase] Created new backup database file: {}", backupDatabaseFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("[GataBase] Failed to create backup database file: {}", backupDatabaseFile.getAbsolutePath(), e);
            }
        }
    }


    /**
     * Checks whether a newGate conflicts with existing gates in name or address.
     *
     * @param newGate The gate to check for uniqueness.
     * @param ingates Optional list of gates to check against. If null, the database file is read.
     * @return conflictCount Returns 1 if name conflict, 2 if address conflict, 3 if both.
     */
    private int gateUniquenessCheck(GateObject newGate, List<GateObject> ingates) {
        databaseLock.lock(); // Ensure thread safety for database access
        try {
            List<GateObject> gates;
            int conflictCount = 0;

            // Load gates from database if no list is provided
            if (ingates == null) {
                Gson gson = new Gson();
                try (FileReader reader = new FileReader(mainDatabaseFile)) {
                    Type listType = new TypeToken<List<GateObject>>() {}.getType();
                    gates = gson.fromJson(reader, listType);
                } catch (IOException e) {
                    LOGGER.error("[GataBase] Failed to read main database file during uniqueness check", e);
                    gates = new ArrayList<>();
                }

                if (gates == null) {
                    gates = new ArrayList<>();
                }
            } else {
                gates = ingates;
            }

            // Check for conflicts
            for (GateObject gate : gates) {
                if (gate.getName().equalsIgnoreCase(newGate.getName())) {
                    LOGGER.warn("A Gate with this name already exists: {}", newGate.getName());
                    conflictCount++;
                }
                if (Arrays.equals(gate.getChevrons(), newGate.getChevrons())) {
                    LOGGER.warn("A Gate with this address already exists: {}", Arrays.toString(newGate.getChevrons()));
                    conflictCount += 2; // Using 2 to indicate address conflict
                }
            }

            return conflictCount; // 1=name conflict, 2=address conflict, 3=both
        } finally {
            databaseLock.unlock();
        }
    }

    //unfinished
    /**
     * Adds the initial spawn gate to the database.
     * Ensures that the spawn dimension always has a gate registered.
     *
     * @param server The MinecraftServer instance used to retrieve universe info
     */
    private void addInitialSpawnGate(MinecraftServer server) {

        // Get the default spawn dimension string from config
        String defaultDim = Config.DEFAULTSPAWNDIMENSION.get();

        // Convert the dimension string into a ResourceKey for the level
        ResourceKey<Level> spawnDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(defaultDim));
        String prettyName = spawnDim.location().getPath();

        // Retrieve universe information from SGJourney
        Universe universeInfo = Universe.get(server);
        LOGGER.info("Grabbing SGJourney level info for spawn dimension '{}'", defaultDim);

        // Get galaxy info associated with this dimension
        Galaxy.Serializable galaxySerializable = universeInfo.getGalaxyFromDimension(spawnDim);
        ResourceKey<Galaxy> galaxyKey = galaxySerializable.getKey();
        ResourceLocation galaxyId = galaxyKey.location();
        LOGGER.info("Obtained spawn galaxy ID: {}", galaxyId);

        // Get the address of the spawn dimension within its galaxy
        int[] spawnAddress = universeInfo.getAddressInGalaxyFromDimension(galaxyId, spawnDim).toArray();
        LOGGER.info("Obtained address for spawn dimension: {}", Arrays.toString(spawnAddress));

        // Construct the GateObject representing the spawn gate
        GateObject spawnGate = new GateObject(
                prettyName,        // Name
                prettyName,        // Dimension display name
                true,              // Public gate
                spawnAddress,      // Gate address
                false,             // Has iris
                false,             // Defensive gate
                new ArrayList<>(), // Whitelist
                new ArrayList<>(), // Blacklist
                true,              // Default gate
                ADMIN_UUID         // Creator UUID (admin)
        );

        // Add the gate to the database
        addGate(spawnGate, null);
        LOGGER.info("Spawn gate '{}' has been registered in GataBase.", spawnGate.getName());
    }

    //getters

    public List<GateObject> getFilteredGates(UUID offendingPlayer, String keyword) {
        Gson gson = new Gson();
        List<GateObject> gates;
        try (FileReader reader = new FileReader(mainDatabaseFile)) {
            Type listType = new TypeToken<List<GateObject>>() {}.getType();
            gates = gson.fromJson(reader, listType);
        } catch (IOException e) {
            LOGGER.error("Failed to read database file", e);
            gates = new ArrayList<>();
        }

        if ((gates == null) || gates.isEmpty()) {
            return null;
        } else {
//            if (keyword != null) {
//                Iterator<GateObject> iterator = gates.iterator();
//                while (iterator.hasNext()) {
//                    GateObject gate = iterator.next();
//                    if (!gate.getName().toLowerCase().contains(keyword.toLowerCase())) {
//                        iterator.remove();
//                    }
//                }
//            }
            if (offendingPlayer != ADMIN_UUID) {
                Iterator<GateObject> iterator = gates.iterator();
                while (iterator.hasNext()) {
                    GateObject gate = iterator.next();
                    if (!gate.getCreator().equals(offendingPlayer)) {
                        iterator.remove();
                    }
                }



                return gates;
            } else {
                return gates;
            }
        }

    }


}



