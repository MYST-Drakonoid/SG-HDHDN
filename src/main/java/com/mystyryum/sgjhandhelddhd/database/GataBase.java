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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.povstalec.sgjourney.common.data.*;
import net.povstalec.sgjourney.common.sgjourney.Address;
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
    private static List<GateObject> appendingList = new ArrayList<>();

    /** Constant representing the admin UUID. Used for gates with no specific owner. */
    private static final UUID ADMIN_UUID = new UUID(0L, 0L);

    /** Indicates whether a save operation is currently in progress. */
    private static boolean isSaving = false;

    /** Flag to indicate a save request occurred while another save was in progress. */
    private static boolean saveQueued = false;

    /** File used as the main persistent database for gates. */
    private static File mainDatabaseFile = new File("plugins/SGHDHD/GataBase.json");

    /** Backup file used in case the main database becomes corrupted. */
    private static File backupDatabaseFile = new File("plugins/SGHDHD/backup.json");

    /** Lock used to ensure thread safety across all database operations. */
    private static final Lock databaseLock = new ReentrantLock();

//    /** Dirty flag indicating whether there are unsaved changes in the database. */
//    private static boolean isDirty;

    /** Logger instance for structured logging (replaces System.out calls). */
    private static final Logger LOGGER = LoggerFactory.getLogger(GataBase.class);


    //=== Constructor ===//
    public GataBase(File mainFile, File backupFile) {
        // initializing data structures

        mainDatabaseFile = mainFile;
        backupDatabaseFile = backupFile;



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
    public static String addGate(GateObject gate, UUID offendingPlayer) {
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


        //adding default gates to the Registry
        if (gate.isDefaultGate() && (!gate.getAdmin())) {
            DefaultGateManager.addDimension(gate.getDimension());
        }


        //Sending ADD event to the server
        NeoForge.EVENT_BUS.post(new GatabaseChangedEvent(
                null,
                gate,
                GatabaseChangedEvent.ChangeType.ADD
        ));

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
    public static String removeGate(GateObject target, UUID offendingPlayer) {
        // Check if the player has permission to delete: either they created it or are admin
        boolean canDelete = target.getCreator().equals(offendingPlayer) || target.getCreator().equals(ADMIN_UUID);

        if (canDelete) {
            // Log the deletion attempt for auditing/debugging purposes
            LOGGER.warn("Player {} deleting gate '{}'", offendingPlayer, target.getName());

            // Mark the database as dirty to trigger a save and remove the gate
            setDirty(true, false, target, null);

            //removes Default gates from list if removing Default gate any gate created by an admin will be dismissed
            if (target.isDefaultGate() && (!target.getAdmin())) {
                DefaultGateManager.removeDimension(target.getDimension());
            }


            // sending REMOVE event to the server
            NeoForge.EVENT_BUS.post(new GatabaseChangedEvent(
                    target,
                    null,
                    GatabaseChangedEvent.ChangeType.REMOVE
            ));

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
    public static String editGate(GateObject target, GateObject updatedTarget, UUID offendingPlayer) {

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

        // === Update Default Gate Registry (in-memory tracking) ===
        //
        // Case 1: A gate was promoted to default (was not default before, now is).
        // Case 2: A gate was demoted or removed as default (was default before, now isn't).
        //
        // Admin-owned gates are excluded from tracking since they are system-level.
        if (!target.isDefaultGate() && updatedTarget.isDefaultGate() && !updatedTarget.getAdmin()) {
            // Promote — new default gate detected, register its dimension
            DefaultGateManager.addDimension(updatedTarget.getDimension());

        } else if (target.isDefaultGate() && !updatedTarget.isDefaultGate() && !updatedTarget.getAdmin()) {
            // Demote — gate lost default status, unregister its dimension
            DefaultGateManager.removeDimension(updatedTarget.getDimension());
        }

        switch (result) {
            case 0:
                // No conflicts: mark database as dirty for saving
                setDirty(true, false, target, updatedTarget);
                LOGGER.info("Gate '{}' successfully edited by player {}", target.getName(), offendingPlayer);

                //sending update event to the server
                NeoForge.EVENT_BUS.post(new GatabaseChangedEvent(
                        target,
                        updatedTarget,
                        GatabaseChangedEvent.ChangeType.UPDATE
                ));

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
     * Creates a backup of the main Gatabase file.
     * <p>
     * This method safely copies the main database file to the specified backup location.
     * The operation is thread-safe — it locks the database during the copy to prevent corruption.
     *
     * @param mainFile   the source file to back up (e.g., main Gatabase JSON)
     * @param backupFile the destination backup file (e.g., timestamped backup)
     * @return 1 if the backup succeeded, 0 if it failed
     */
    public static int startBackup(File mainFile, File backupFile) {
        databaseLock.lock(); // Ensure no save/load occurs during backup
        try {
            // Ensure both files exist or are accessible
            if (!mainFile.exists()) {
                LOGGER.warn("Backup failed: main database file not found at {}", mainFile.getAbsolutePath());
                return 0;
            }

            // Copy file (replacing if necessary)
            Files.copy(mainFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Database successfully backed up to {}", backupFile.getAbsolutePath());
            return 1;

        } catch (IOException e) {
            LOGGER.error("Backup operation failed: {}", e.toString());
            return 0;

        } finally {
            databaseLock.unlock(); // Always release the lock, even if copy fails
            LOGGER.debug("Backup operation completed (lock released).");
        }
    }

    /**
     * Fired whenever the Gatabase (server-side gate database) undergoes a change.
     *
     * This event is intended for server → client synchronization logic.
     * - ADD:    A new gate was inserted into the database.
     * - REMOVE: An existing gate was removed.
     * - UPDATE: A gate's data was modified.
     *
     * The event provides:
     *  - The original gate (if any)
     *  - The updated gate (if any)
     *  - The type of change that occurred
     *
     * Notes:
     *  - For ADD events, targetGate is usually null and updatedTarget is the new gate.
     *  - For REMOVE events, updatedTarget is usually null.
     *  - For UPDATE events, both values are non-null.
     *
     * This event is posted on the NeoForge EVENT_BUS and must be subscribed to
     * using @SubscribeEvent on any handler class.
     */
    public static class GatabaseChangedEvent extends Event {

        /**
         * The original gate involved in this change.
         * For UPDATE: the pre-change gate data.
         * For REMOVE: the gate being removed.
         * For ADD: usually null.
         */
        private final GateObject targetGate;

        /**
         * The resulting gate involved in this change.
         * For UPDATE: the post-change gate data.
         * For ADD: the newly added gate.
         * For REMOVE: usually null.
         */
        private final GateObject updatedTarget;

        /**
         * The type of modification that occurred.
         */
        private final ChangeType type;

        /**
         * Describes the category of database modification.
         */
        public enum ChangeType {
            /** A new gate has been added to the database. */
            ADD,

            /** A gate has been fully removed from the database. */
            REMOVE,

            /** An existing gate's data has been modified. */
            UPDATE
        }

        /**
         * Constructs a new database change event.
         *
         * @param targetGate1
         *      The original gate involved in the change (may be null).
         *
         * @param updatedTarget1
         *      The resulting gate after the change (may be null).
         *
         * @param type1
         *      The type of modification that occurred.
         */
        public GatabaseChangedEvent(GateObject targetGate1,
                                    GateObject updatedTarget1,
                                    ChangeType type1) {

            this.targetGate = targetGate1;
            this.updatedTarget = updatedTarget1;
            this.type = type1;
        }

        /**
         * Returns the original gate involved in the change.
         * May be null depending on ChangeType.
         */
        public GateObject getTargetGate() {
            return targetGate;
        }

        /**
         * Returns the resulting updated gate.
         * May be null depending on ChangeType.
         */
        public GateObject getUpdatedTarget() {
            return updatedTarget;
        }

        /**
         * Returns the type of modification that took place (ADD, REMOVE, UPDATE).
         */
        public ChangeType getType() {
            return type;
        }
    }



    /**
     * Handles automatic database backups at regular intervals.
     * <p>
     * Starts after the server fully initializes and stops cleanly on shutdown.
     * The task runs on a background daemon thread, ensuring it doesn't block the main server thread.
     */
    public static class AutoBackupTimer {

        private static Timer timer;
        private static final int minutesBetweenBackups = Config.MINBETWEENBACKUPS.get();

        /**
         * Called when the server has fully started.
         * Begins scheduling periodic backups.
         */
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            LOGGER.info("[AutoBackupTimer] Server started — initializing automatic backups...");

            timer = new Timer("GatabaseAutoBackupTimer", true); // Daemon thread (won’t prevent shutdown)

            // Delay startup by 1 minute (allow server and world to stabilize)
            long delay = 60_000L; // 1 minute
            // Period between backups (in milliseconds)
            long period = minutesBetweenBackups * 60_000L;

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    doAutoBackup();
                }
            }, delay, period);

            LOGGER.info("[AutoBackupTimer] Automatic backups scheduled every {} minutes.", minutesBetweenBackups);
        }

        /**
         * Called before the server shuts down.
         * Cancels any pending backup tasks and performs one final save.
         */
        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            LOGGER.info("[AutoBackupTimer] Server stopping — finalizing backups...");

            if (timer != null) {
                timer.cancel();
                LOGGER.debug("[AutoBackupTimer] Backup timer canceled.");
            }

            // Perform one last backup before exit
            doFinalSave();
        }

        /**
         * Performs a scheduled (incremental) backup.
         */
        private static void doAutoBackup() {
            LOGGER.info("[AutoBackupTimer] Performing scheduled backup...");

            // Example: reference your database files
            File main = mainDatabaseFile;
            File backup = backupDatabaseFile;

            int result = startBackup(main, backup);

            if (result == 1) {
                LOGGER.info("[AutoBackupTimer] Backup successful.");
            } else {
                LOGGER.warn("[AutoBackupTimer] Backup failed.");
            }
        }



        /**
         * Performs a final backup on shutdown.
         */
        private static void doFinalSave() {
            LOGGER.info("[AutoBackupTimer] Performing final backup before shutdown...");

            File main = mainDatabaseFile;
            File backup = backupDatabaseFile;

            int result = startBackup(main, backup);

            if (result == 1) {
                LOGGER.info("[AutoBackupTimer] Final backup successful.");
            } else {
                LOGGER.error("[AutoBackupTimer] Final backup failed!");
            }
        }
    }



    public static String noGateExistRemoval(GateObject gate) {
        String prettyName = gate.getDimension().location().getPath();
        LOGGER.error("Gate Provided does not exist DIMENSION: {}, Removing gate from list.", prettyName);
        removeGate(gate, ADMIN_UUID);
        return "Gate Removed";
    }


    //===private/helper methods===//


    /**
     * Marks the database as dirty (having unsaved changes) and optionally triggers a save.
     *
     * @param dirty          true if there are unsaved changes
     * @param save           true to trigger an immediate save
     * @param target         the gate being edited/removed (can be null for additions)
     * @param updatedTarget  the new gate data for edits (can be null)
     */
    private static void setDirty(boolean dirty, boolean save, GateObject target, GateObject updatedTarget) {


        if (dirty) {
            LOGGER.info("[GataBase] Database marked as dirty. Save triggered: {}", save);
            onDirty(save, target, updatedTarget); // Start the save process if necessary
        }
    }

    /**
     * Triggered when the database is marked as dirty (has unsaved changes).
     * Starts the save process if no save is currently in progress.
     * If a save is already running, queues another save to run afterward.
     *
     * @param save          true if this change is a new addition to be saved
     * @param target        the original GateObject being modified (for edits/removals)
     * @param updatedTarget the updated GateObject (for edits)
     */
    private static synchronized void onDirty(boolean save, GateObject target, GateObject updatedTarget) {
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
    private static void saveData(boolean save, GateObject target, GateObject updatedTarget) {
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
    private static void ensureDatabaseIntegrity() {
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
    private static int gateUniquenessCheck(GateObject newGate, List<GateObject> ingates) {
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

    /**
     * Adds the initial spawn gate to the database.
     * Ensures that the spawn dimension always has a gate registered.
     *
     * @param server The MinecraftServer instance used to retrieve universe info
     */
    private static void addInitialSpawnGate(MinecraftServer server) {
        int[] spawnAddress;
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

        Address.Immutable item = universeInfo.getAddressInGalaxyFromDimension(galaxyId, spawnDim);
        // Get the address of the spawn dimension within its galaxy

        try {
            spawnAddress = item.toArray();
            LOGGER.info("Obtained address for spawn dimension: {}", Arrays.toString(spawnAddress));
        } catch (NullPointerException e){
            LOGGER.error("Dimension provided in Config has no Galaxy. ERROR REPORT: '{}'", e.getMessage());

            spawnAddress = null;
            return;
        }

        // Construct the GateObject representing the spawn gate
        GateObject spawnGate = new GateObject(
                prettyName,        // Name
                spawnDim,          // Dimension display name
                true,              // Public gate
                spawnAddress,      // Gate address
                false,             // Has iris
                false,             // Defensive gate
                new ArrayList<>(), // Whitelist
                new ArrayList<>(), // Blacklist
                true,              // Default gate
                ADMIN_UUID,        // Creator UUID (admin)
                false              // not an Admin made Gate
        );

        // Add the gate to the database
        addGate(spawnGate, null);
        LOGGER.info("Spawn gate '{}' has been registered in GataBase.", spawnGate.getName());
    }

    //getters

    /**
     * Returns a list of gates owned by the specified player.
     * Admins see all gates.
     *
     * @param offendingPlayer the UUID of the player
     * @return list of GateObjects owned by the player, or all if admin; never null
     */
    public static List<GateObject> getFilteredGates(UUID offendingPlayer) {
        List<GateObject> gates = new ArrayList<>();

        // Read all gates from the database
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(mainDatabaseFile)) {
            Type listType = new TypeToken<List<GateObject>>() {}.getType();
            gates = gson.fromJson(reader, listType);
        } catch (IOException e) {
            LOGGER.error("Failed to read database file", e);
        }

        if (gates == null) gates = new ArrayList<>();

        // If not admin, filter gates to only those created by the player
        if (!offendingPlayer.equals(ADMIN_UUID)) {
            gates.removeIf(gate -> !gate.getCreator().equals(offendingPlayer));
        }

        return gates;
    }

    /**
     * Performs one-time setup tasks when the server starts.
     * Checks if a default spawn gate exists, and if not, creates one.
     */
    public static void firstTimeTasks(ServerStartingEvent event) {
        // Retrieve the Minecraft server instance from the startup event
        MinecraftServer server = event.getServer();

        // Load all gates associated with the admin UUID (system-owned gates)
        List<GateObject> adminGates = getFilteredGates(ADMIN_UUID);

        // Check whether any of these gates are marked as the default spawn gate
        boolean spawnGateExists = adminGates != null &&
                adminGates.stream().anyMatch(GateObject::isDefaultGate);

        // If a default spawn gate already exists, skip initialization
        if (spawnGateExists) {
            LOGGER.info("[FirstTimeTasks] Default spawn gate already exists — skipping initialization.");
        }
        // Otherwise, create a new default spawn gate
        else {
            LOGGER.info("[FirstTimeTasks] No default spawn gate found — creating initial spawn gate...");
            addInitialSpawnGate(server);
            LOGGER.info("[FirstTimeTasks] Initial spawn gate successfully created.");
        }


        //checking if files are correct
        ensureDatabaseIntegrity();
    }

    /**
     * Retrieves a list of dimensions that currently have a default gate.
     *
     * <p>This function safely reads the database file and checks each gate entry.
     * If a gate is marked as a default gate and its dimension matches its name path,
     * that dimension name is added to the result list.</p>
     *
     * @return a list of dimension IDs (ResourceKey<Level>) that have a default gate registered
     */
    public static List<ResourceKey<Level>> getDimensionList() {
        List<ResourceKey<Level>> dimensions = new ArrayList<>();
        databaseLock.lock(); // Prevent concurrent read/write access

        try {
            Gson gson = new Gson();
            List<GateObject> gates;

            // === Load all gates from JSON database ===
            try (FileReader reader = new FileReader(mainDatabaseFile)) {
                Type listType = new TypeToken<List<GateObject>>() {}.getType();
                gates = gson.fromJson(reader, listType);
            } catch (IOException e) {
                LOGGER.error("[GataBase] Failed to read main database file while gathering dimension list", e);
                gates = new ArrayList<>();
            }

            // Handle empty or null database gracefully
            if (gates == null || gates.isEmpty()) {
                return dimensions; // Return empty list instead of null
            }

            // === Filter for default gates and collect their dimension names ===
            for (GateObject gate : gates) {
                try {
                    ResourceKey<Level> dimKey = gate.getDimension();


                    // Only include dimensions with a properly flagged default gate
                    if (gate.isDefaultGate() && (!gate.getAdmin()) &&
                            dimKey.location().getPath().equalsIgnoreCase(gate.getName())) {
                        dimensions.add(gate.getDimension());
                    }
                } catch (Exception parseError) {
                    LOGGER.warn("Skipping invalid gate dimension entry: {}", gate.getDimension());
                }
            }

        } finally {
            databaseLock.unlock();
        }

        return dimensions;
    }



}



