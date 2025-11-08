package com.mystyryum.sgjhandhelddhd.database;

import net.povstalec.sgjourney.common.data.Universe;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.mystyryum.sgjhandhelddhd.database.GateObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
//import com.mystyryum.sgjhandhelddhd.player.Player; // Hypothetical player class

public class GataBase {

    private Map<String, GateObject> gatesByName;    //ensures each gate has a unique name
    private Map<String, GateObject> gatesByAddress; //ensures each gate has a unique address

    //Files for Persistant Storage
    private File mainDatabaseFile = new File("plugins/SGHDHD/GataBase.json"); //Main file for database
    private File backupDatabaseFile = new File("plugins/SGHDHD/backup.json");   //backup in case of corruption

    //thread safety lock for all operations
    private final Lock databaseLock = new ReentrantLock();

    // Dirty Flag: true if there are unsaved changes
    private boolean isDirty;


    //=== Constructor ===//
    public GataBase(File mainFile, File backupFile) {
        // initializing data structures


        this.gatesByName = new HashMap<>();
        this.gatesByAddress = new HashMap<>();
        this.mainDatabaseFile = mainFile;
        this.backupDatabaseFile = backupFile;
        this.isDirty = false;

        ensureDatabaseIntegrity();
        addinitialOverworldGate();

    }

    // === Public Methods ===
    public void addGate(GateObject gate) {
        // later, this will add gates safely and validate uniqueness
    }

    public void removeGate(String name) {
        // later, this will remove gates safely
    }

    //===private/helper methods===//

    private void ensureDatabaseIntegrity() {
        if (!mainDatabaseFile.exists()) {
            try {
                mainDatabaseFile.getParentFile().mkdirs();
                mainDatabaseFile.createNewFile();
                System.out.println("[GataBase] Created new main database file.");
            } catch (IOException e) {
                System.err.println("[GataBase] Failed to create main database file!");
                e.printStackTrace();
            }
        }

        if (!backupDatabaseFile.exists()) {
            try {
                backupDatabaseFile.getParentFile().mkdirs();
                backupDatabaseFile.createNewFile();
                System.out.println("[GataBase] Created new backup database file.");
            } catch (IOException e) {
                System.err.println("[GataBase] Failed to create backup database file!");
                e.printStackTrace();
            }
        }
    }

    private void addinitialOverworldGate() {
        int[] overworldAddress = {27, 25, 4, 35, 10, 28};


    }
}
