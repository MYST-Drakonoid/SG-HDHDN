package com.mystyryum.sgjhandhelddhd.database;


import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

    /**
     * Represents a Stargate and its configuration.
     */
    public class GateObject {

        /**
         * The gate's display name
         */
        private String name;

        /**
         * The dimension where this gate is placed (e.g. "minecraft:overworld")
         */
        private ResourceKey<Level> dimension;

        /**
         * True if the gate is public, false if private
         */
        private boolean isPublic;

        /**
         * The chevron sequence as an array of integers
         */
        private int[] chevrons;

        /**
         * True if this gate has an iris (a protective shield)
         */
        private boolean hasIris;

        /**
         * True if this gate operates in defensive mode
         */
        private boolean isDefensive;

        /**
         * List of player UUIDs allowed on private gates
         */
        private List<UUID> whitelist;

        /**
         * List of player UUIDs blocked on defensive gates
         */
        private List<UUID> blacklist;

        /**
         * True if this is a default server-added dimension gate
         */
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ResourceKey<Level> getDimension() {
            return dimension;
        }

        public void setDimension(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public void setPublic(boolean aPublic) {
            isPublic = aPublic;
        }

        public int[] getChevrons() {
            return chevrons;
        }

        public void setChevrons(int[] chevrons) {
            this.chevrons = chevrons;
        }

        public boolean hasIris() {
            return hasIris;
        }

        public void setHasIris(boolean hasIris) {
            this.hasIris = hasIris;
        }

        public boolean isDefensive() {
            return isDefensive;
        }

        public void setDefensive(boolean defensive) {
            isDefensive = defensive;
        }

        public List<UUID> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<UUID> whitelist) {
            this.whitelist = whitelist;
        }

        public List<UUID> getBlacklist() {
            return blacklist;
        }

        public void setBlacklist(List<UUID> blacklist) {
            this.blacklist = blacklist;
        }

        public boolean isDefaultGate() {
            return isDefaultGate;
        }

        public void setDefaultGate(boolean dimensionGate) {
            isDefaultGate = dimensionGate;
        }

        public UUID getCreator() {
            return creator;
        }

        public void setCreator(UUID creator) {
            this.creator = creator;
        }

        public boolean getAdmin() {
            return admin;
        }

        public void setAdmin(boolean aadmin) {
            admin = aadmin;
        }

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
                    ", Creator=" + creator +
                    ", Admin Only =" + admin +
                    '}';
        }


        public void serialize(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUtf(dimension.location().toString()); // serialize ResourceKey as string
            buf.writeBoolean(isPublic);

            buf.writeInt(chevrons.length);
            for (int c : chevrons) {
                buf.writeInt(c);
            }

            buf.writeBoolean(hasIris);
            buf.writeBoolean(isDefensive);

            buf.writeInt(whitelist.size());
            for (UUID id : whitelist) {
                buf.writeUUID(id);
            }

            buf.writeInt(blacklist.size());
            for (UUID id : blacklist) {
                buf.writeUUID(id);
            }

            buf.writeBoolean(isDefaultGate);
            buf.writeUUID(creator);
            buf.writeBoolean(admin);
        }

        public static GateObject deserialize(FriendlyByteBuf buf) {
            String name = buf.readUtf();

            String dimIdString = buf.readUtf();
            ResourceLocation dimLoc = ResourceLocation.parse(dimIdString);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimLoc);

            boolean isPublic = buf.readBoolean();
            int chevronLength = buf.readInt();
            int[] chevrons = new int[chevronLength];
            for (int i = 0; i < chevronLength; i++) {
                chevrons[i] = buf.readInt();
            }

            boolean hasIris = buf.readBoolean();
            boolean isDefensive = buf.readBoolean();

            int whitelistSize = buf.readInt();
            List<UUID> whitelist = new ArrayList<>();
            for (int i = 0; i < whitelistSize; i++) {
                whitelist.add(buf.readUUID());
            }

            int blacklistSize = buf.readInt();
            List<UUID> blacklist = new ArrayList<>();
            for (int i = 0; i < blacklistSize; i++) {
                blacklist.add(buf.readUUID());
            }

            boolean isDefaultGate = buf.readBoolean();
            UUID creator = buf.readUUID();
            boolean admin = buf.readBoolean();

            return new GateObject(name, dimension, isPublic, chevrons, hasIris, isDefensive,
                    whitelist, blacklist, isDefaultGate, creator, admin);

        }



        public String serializeToString() {
            List<String> whitelistTag = new ArrayList<>();
            for (UUID uuid : whitelist) {
                whitelistTag.add(uuid.toString());
            }

            List<String> blacklistTag = new ArrayList<>();
            for (UUID uuid : blacklist) {
                blacklistTag.add(uuid.toString());
            }

            String DimensionString = dimension.location().toString();

            return name + "|" + DimensionString + "|" + isPublic + "|" + Arrays.toString(chevrons) + "|" + hasIris + "|" + isDefensive + "|" +
                Arrays.toString(whitelistTag.toArray()) + "|" + Arrays.toString(blacklistTag.toArray()) + "|" + isDefaultGate + "|" +
                creator.toString() + "|" + admin;
        }

        public GateObject GateObject (String buffer) {
            String[] parts = buffer.split("\\|");

            //basic fields
            String name = parts[0];

            //dimension
            String DimensionString = parts[1];
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(DimensionString));

            boolean isPublic = Boolean.parseBoolean(parts[2]);

            //parsing chevrons
            String ChevronString = parts[3].replaceAll("[\\[\\]\\s]", "");
            String[] chevParts = ChevronString.isEmpty() ? new String[0] : ChevronString.split(",");
            int[] chevrons = new int[chevParts.length];
            for (int i = 0; i < chevParts.length; i++) {
                chevrons[i] = Integer.parseInt(chevParts[i]);
            }

            //more basic fields
            boolean hasIris = Boolean.parseBoolean(parts[4]);
            boolean isDefensive = Boolean.parseBoolean(parts[5]);

            //deserialize whitelist
            String whitelistString = parts[6].replaceAll("[\\[\\]\\s]", "");
            List<UUID> whitelist = new ArrayList<>();
            if (!whitelistString.isEmpty()) {
                for (String uuidstr : whitelistString.split(",")) {
                    whitelist.add(UUID.fromString(uuidstr));
                }
            }

            //deserialize blacklist
            String blacklistString = parts[7].replaceAll("[\\[\\]\\s]", "");
            List<UUID> blacklist = new ArrayList<>();
            if (!blacklistString.isEmpty()) {
                for (String uuidstr : blacklistString.split(",")) {
                    blacklist.add(UUID.fromString(uuidstr));
                }
            }

            //final basics

            boolean isDefault = Boolean.parseBoolean(parts[8]);
            UUID creator = UUID.fromString(parts[9]);
            boolean admin = Boolean.parseBoolean(parts[10]);


            GateObject gate = new GateObject(name, dimension, isPublic, chevrons, hasIris, isDefensive, whitelist, blacklist, isDefault, creator, admin);

            gate.setCreator(creator);

            return gate;


        }
    }

