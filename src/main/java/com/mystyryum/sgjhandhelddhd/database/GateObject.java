package com.mystyryum.sgjhandhelddhd.database;


import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

    /**
     * Represents a Stargate and its configuration.
     */
    public class GateObject {

        /** The gate's display name */
        private String name;

        /** The dimension where this gate is placed (e.g. "minecraft:overworld") */
        private ResourceKey<Level> dimension;

        /** True if the gate is public, false if private */
        private boolean isPublic;

        /** The chevron sequence as an array of integers */
        private int[] chevrons;

        /** True if this gate has an iris (a protective shield) */
        private boolean hasIris;

        /** True if this gate operates in defensive mode */
        private boolean isDefensive;

        /** List of player UUIDs allowed on private gates */
        private List<UUID> whitelist;

        /** List of player UUIDs blocked on defensive gates */
        private List<UUID> blacklist;

        /** True if this is a default server-added dimension gate */
        private boolean isDefaultGate;

        private UUID creator;

        private boolean admin;

        // --- Constructors ---


        public GateObject(String name, ResourceKey<Level> dimension, boolean isPublic, int[] chevrons,
                          boolean hasIris, boolean isDefensive,
                          List<UUID> whitelist, List<UUID> blacklist, boolean isDefaultGate, UUID creator, boolean admin) {
            this.name = name;
            this.dimension = dimension;
            this.isPublic = isPublic;
            this.chevrons = chevrons;
            this.hasIris = hasIris;
            this.isDefensive = isDefensive;
            this.whitelist = whitelist;
            this.blacklist = blacklist;
            this.isDefaultGate = isDefaultGate;
            this.creator = creator;
            this.admin = admin;
        }

        // --- Getters and Setters ---

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public ResourceKey<Level> getDimension() { return dimension; }
        public void setDimension(ResourceKey<Level> dimension) { this.dimension = dimension; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean aPublic) { isPublic = aPublic; }

        public int[] getChevrons() { return chevrons; }
        public void setChevrons(int[] chevrons) { this.chevrons = chevrons; }

        public boolean hasIris() { return hasIris; }
        public void setHasIris(boolean hasIris) { this.hasIris = hasIris; }

        public boolean isDefensive() { return isDefensive; }
        public void setDefensive(boolean defensive) { isDefensive = defensive; }

        public List<UUID> getWhitelist() { return whitelist; }
        public void setWhitelist(List<UUID> whitelist) { this.whitelist = whitelist; }

        public List<UUID> getBlacklist() { return blacklist; }
        public void setBlacklist(List<UUID> blacklist) { this.blacklist = blacklist; }

        public boolean isDefaultGate() { return isDefaultGate; }
        public void setDefaultGate(boolean dimensionGate) { isDefaultGate = dimensionGate; }

        public UUID getCreator() {return creator; }
        public void setCreator(UUID creator) {this.creator = creator; }

        public boolean getAdmin() { return admin; }
        public void setAdmin(boolean aadmin) { admin = aadmin; }

        // --- Utility ---

        @Override
        public String toString() {
            return "Gate{" +
                    "name='" + name + '\'' +
                    ", dimension='" + dimension + '\'' +
                    ", isPublic=" + isPublic +
                    ", hasIris=" + hasIris +
                    ", isDefensive=" + isDefensive +
                    ", isDefaultGate=" + isDefaultGate +
                    ", Creator="       + creator +
                    ", Admin Only ="   + admin +
                    '}';
        }


    }

