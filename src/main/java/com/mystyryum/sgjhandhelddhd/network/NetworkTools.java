package com.mystyryum.sgjhandhelddhd.network;

import com.mystyryum.sgjhandhelddhd.SGJHandheldDHD;
import com.mystyryum.sgjhandhelddhd.SGJHandheldDHDClient;
import com.mystyryum.sgjhandhelddhd.database.GataBase;
import com.mystyryum.sgjhandhelddhd.database.GateObject;
import com.mystyryum.sgjhandhelddhd.database.GateObjectPacket;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ================================================================
 *  NetworkTools — Abstract Foundation for NeoForge Networking
 * ================================================================
 *
 * This class provides a framework for:
 *  • Declaring packet types
 *  • Connecting packet types to their codecs (encode/decode logic)
 *  • Automatically registering packets with NeoForge
 *  • Receiving packets through a unified abstract callback
 *  • Sending packets to client or server using helper methods
 * This allows large mods to centralize and STANDARDIZE their
 * networking behavior, while keeping all specific logic in subclasses.
 */
public abstract class NetworkTools {

    /**
     * A local map storing all packet types + their codecs.
     *
     * IMPORTANT:
     *  • This does NOT register packets with NeoForge immediately.
     *  • It ONLY stores them until the registration event occurs.
     *  • NeoForge requires registration during the
     *    RegisterPayloadHandlersEvent.
     *
     * Keys:   CustomPacketPayload.Type<?>   → identifies the packet
     * Values: StreamCodec<Buf, Packet>      → how to serialize it
     */
    protected static final Map<CustomPacketPayload.Type<?>,
            StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>>
            codecRegistry =
            new ConcurrentHashMap<>();


    // ---------------------------------------------------------------------
    //  REGISTER LOCAL PACKET DEFINITIONS
    // ---------------------------------------------------------------------

    /**
     * Adds a packet type + codec pair to the registry.
     *
     * @param type  Unique NeoForge packet identifier
     * @param codec Encoder/decoder for the packet data
     * @param <T>   Packet class extending CustomPacketPayload
     *
     * NOTE:
     *  This does NOT register with NeoForge yet, only stores it.
     */
    protected static <T extends CustomPacketPayload> void registerPacket(
            CustomPacketPayload.Type<T> type,
            StreamCodec<FriendlyByteBuf, T> codec
    ) {
        codecRegistry.put(type, codec);
    }


    // ---------------------------------------------------------------------
    //  REGISTRATION EVENT ENTRY POINT
    // ---------------------------------------------------------------------

    /**
     * Called during NeoForge's networking registration phase.
     *
     * NeoForge supplies a PayloadRegistrar, which we must use to
     * actually register all our stored packets.
     *
     * @param event NeoForge registration event
     */

    @SubscribeEvent
    public void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {

        // Version "1" — matches your networking configuration
        PayloadRegistrar registrar = event.registrar("1");

        // Register each stored packet+codec pair
        for (var entry : codecRegistry.entrySet()) {
            registerPayloadHelper(registrar, entry.getKey(), entry.getValue());
        }
    }


    // ---------------------------------------------------------------------
    //  INTERNAL: PER-PACKET REGISTRATION
    // ---------------------------------------------------------------------

    /**
     * Registers a single packet type with NeoForge.
     *
     * Most of this method is handling Java generics safely.
     *
     * @param registrar NeoForge registrar
     * @param type      Packet identifier
     * @param codec     Packet codec
     * @param <T>       Packet class
     */
    @SuppressWarnings("unchecked")
    private <T extends CustomPacketPayload> void registerPayloadHelper(
            PayloadRegistrar registrar,
            CustomPacketPayload.Type<?> type,
            StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> codec
    ) {
        // Safe generic casts to satisfy the type system
        CustomPacketPayload.Type<T> castType =
                (CustomPacketPayload.Type<T>) type;

        StreamCodec<FriendlyByteBuf, T> castCodec =
                (StreamCodec<FriendlyByteBuf, T>) codec;

        /**
         * The CALLBACK when a packet arrives (client OR server side).
         * NeoForge gives us:
         *  • payload → the decoded packet object
         *  • context → information about who sent it
         * We forward both into our abstract method so subclasses
         * can implement custom logic.
         */
        IPayloadHandler<T> handler = this::onPayloadReceived;

        // Bidirectional = can be sent both directions
        registrar.playBidirectional(castType, castCodec, handler);
    }


    // ---------------------------------------------------------------------
    //  ABSTRACT HANDLER IMPLEMENTED BY SUBCLASSES
    // ---------------------------------------------------------------------

    /**
     * This method is called whenever NeoForge receives a packet of a type
     * registered by this NetworkTools instance.
     *
     * Subclasses override this to implement the behavior for the packet.
     *
     * @param payload The packet object
     * @param context Provides access to:
     *                • context.player() — who sent it
     *                • context.isClientSide()
     *                • context.isServerSide()
     *                • context.enqueueWork(Runnable)
     */
    protected abstract void onPayloadReceived(
            CustomPacketPayload payload,
            IPayloadContext context
    );

    protected abstract <T> T returnOnPayloadRecieved(
            CustomPacketPayload payload,
            IPayloadContext context
    );


    // ---------------------------------------------------------------------
    //  SEND HELPERS — Convenience API
    // ---------------------------------------------------------------------

    /**
     * Sends packet to the server.
     */
    protected static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /**
     * Sends packet to one specific client.
     */
    protected static void sendToClient(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static class GPI {
        GateObject target;
        GateObject updateTarget;
        String type;
        Boolean Admin;

        public GPI(GateObject target, GateObject updateTarget, String type, Boolean Admin) {
            this.target = target;
            this.updateTarget = updateTarget;
            this.type = type;
            this.Admin = Admin;
        }

        public void serialize(FriendlyByteBuf buf) {
            buf.writeUtf(type);
            buf.writeBoolean(Admin);

            // Write flag: does target exist?
            buf.writeBoolean(target != null);
            if (target != null)
                target.serialize(buf);

            buf.writeBoolean(updateTarget != null);
            if (updateTarget != null)
                updateTarget.serialize(buf);
        }

        public static GPI deserialize(FriendlyByteBuf buf) {

            String type = buf.readUtf();
            Boolean Admin = buf.readBoolean();

            GateObject target = null;
            if (buf.readBoolean()) {
                target = GateObject.deserialize(buf);
            }

            GateObject updateTarget = null;
            if (buf.readBoolean()) {
                updateTarget = GateObject.deserialize(buf);
            }

            return new GPI(target, updateTarget, type, Admin);
        }
    }


    // =====================================================================
    //  SUBCLASSES — Actual Mod Logic
    // =====================================================================


    /**
     * Handles packets related to sending Gate Object lists to clients.
     */
    public static class GateSends extends NetworkTools {

        /**
         * Processes packets sent to the server or client.
         */
        @Override
        protected void onPayloadReceived(CustomPacketPayload payload,
                                         IPayloadContext context) {

            // We only care about GateObjectPacket
            if (payload instanceof GateObjectPacket gatePacket) {

                // Ensure the sender is a server-side player
                if ((context.flow().isClientbound())) {


                    // Extract list of gates from the packet
                    List<GateObject> gates = gatePacket.getGateObjects();

                    // Store the Gate data in client static data for use
                    SGJHandheldDHDClient.ClientGateCache.setGates(gates);

                }
            }
        }

        @Override
        protected <T> T returnOnPayloadRecieved(CustomPacketPayload payload, IPayloadContext context) {
            return null;
        }

        /**
         * Helper method used when a player logs in and must receive
         * their list of gates from the server.
         *
         * @param player Player being updated
         * @param id     Player UUID used for filtering gate data
         */
        public static void LoginUpdate(ServerPlayer player, UUID id) {
            List<GateObject> filteredGates = GataBase.getFilteredGates(id);
            GateObjectPacket pkt = new GateObjectPacket(filteredGates);

            NetworkTools.sendToClient(pkt, player);
        }
    }


    /**
     * ================================================================
     *  GatabaseRecieve — Serverbound Gate Modification Handler
     * ================================================================
     *
     * This class handles GPIPacket messages sent *from the client to the server*.
     * These packets represent a player's or the Server's intention to:
     *
     *     • Add a new gate
     *     • Edit an existing gate
     *     • Remove an existing gate
     *
     * Each GPIPacket contains a GPI structure describing:
     *     - target gate
     *     - updated gate (if applicable)
     *     - operation type ("Add", "Edit", "Remove")
     *
     * SECURITY MODEL:
     *  • The server validates the player UUID using context.player().
     *  • If the target gate is system-owned (creator == ADMIN_UUID OR default gate),
     *    the server overrides the UUID → ADMIN_UUID.
     *  • This prevents normal players from altering system/admin gates.
     *
     * This class runs ONLY on the server side.
     */
    public static class GatabaseRecieve extends NetworkTools {

        /**
         * Called automatically when NeoForge delivers a GPIPacket to the server.
         *
         * @param payload The decoded incoming packet.
         * @param context Networking context, provides:
         *                - player sending the packet
         *                - packet flow direction
         *                - thread-safe work enqueuing
         */
        @Override
        protected void onPayloadReceived(CustomPacketPayload payload, IPayloadContext context) {

            // Only handle packets of our expected type
            if (payload instanceof GPIPacket pkt) {

                // Ensure this packet came from CLIENT → SERVER
                if (context.flow().isServerbound()) {

                    GPI gpi = pkt.getGPI();                 // Extract packet data
                    UUID id = context.player().getUUID();   // UUID of sending player

                    // SAFETY FILTER:
                    // If the gate is admin-created OR a default gate,
                    // TODO: potentially ad an admin check
                    // perform the operation under ADMIN authority.
                    if (gpi.target.getCreator().equals(GataBase.ADMIN_UUID) ||
                            gpi.target.isDefaultGate() ||
                            gpi.Admin) {

                        id = GataBase.ADMIN_UUID;
                    }

                    // SELECT OPERATION BASED ON PACKET TYPE
                    switch (gpi.type) {
                        case "Add" -> {
                            GataBase.addGate(gpi.target, id);
                        }
                        case "Edit" -> {
                            GataBase.editGate(gpi.target, gpi.updateTarget, id);
                        }
                        case "Remove" -> {
                            GataBase.removeGate(gpi.target, id);
                        }
                        default -> {
                            SGJHandheldDHD.LOGGER.error("Unknown GPI packet type: {}", gpi.type);
                        }
                    }
                }
            }
        }

        /**
         * Currently unused — included only because the abstract superclass requires it.
         * Always returns null.
         */
        @Override
        protected <T> T returnOnPayloadRecieved(CustomPacketPayload payload, IPayloadContext context) {
            return null;
        }

        // ======================================================================
        //  SENDING HELPERS — Used client-side to send gate changes to server
        // ======================================================================

        /**
         * Sends an "Edit" packet to the server.
         */
        public static void sendEditUpdateToServer(GateObject target, GateObject updatedTarget, Boolean admin) {

            GPI stagedInfo = new GPI(target, updatedTarget, "Edit", admin);
            GPIPacket pkt = new GPIPacket(stagedInfo);

            NetworkTools.sendToServer(pkt);
        }

        /**
         * Sends an "Add" packet to the server.
         */
        public static void sendAddUpdateToServer(GateObject target, Boolean admin) {

            GPI stagedInfo = new GPI(target, null, "Add", admin);
            GPIPacket pkt = new GPIPacket(stagedInfo);

            NetworkTools.sendToServer(pkt);
        }

        /**
         * Sends a "Remove" packet to the server.
         */
        public static void sendRemoveUpdateToServer(GateObject target, Boolean admin) {

            GPI stagedInfo = new GPI(target, null, "Remove", admin);
            GPIPacket pkt = new GPIPacket(stagedInfo);

            NetworkTools.sendToServer(pkt);
        }
    }



    /**
     * Handles packets related to dialing a gate (currently empty).
     *
     * Extend this class later with your actual dial-handling logic.
     */
    public static class DialSends extends NetworkTools {

        @Override
        protected void onPayloadReceived(CustomPacketPayload payload,
                                         IPayloadContext context) {
            // TODO: implement dial logic
        }

        @Override
        protected <T> T returnOnPayloadRecieved(CustomPacketPayload payload, IPayloadContext context) {
            return null;
        }
    }


    public static class GateSend extends NetworkTools {

        @Override
        protected void onPayloadReceived(CustomPacketPayload payload, IPayloadContext context) {

        }

        @Override
        protected <T> T returnOnPayloadRecieved(CustomPacketPayload payload, IPayloadContext context) {
            return null;
        }
    }
}
