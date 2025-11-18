package com.mystyryum.sgjhandhelddhd.network;

import com.mystyryum.sgjhandhelddhd.database.GataBase;
import com.mystyryum.sgjhandhelddhd.database.GateObject;
import com.mystyryum.sgjhandhelddhd.database.GateObjectPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for managing NeoForge network packets.
 * <p>
 * Provides methods to register custom packet types and codecs,
 * handle incoming payloads, and send packets to server or clients.
 */
public abstract class NetworkTools {

    /**
     * Local registry mapping packet types to their codecs.
     */
    protected final Map<CustomPacketPayload.Type<?>, StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> codecRegistry = new ConcurrentHashMap<>();

    /**
     * Registers a custom packet type and its codec locally.
     * <p>
     * The actual registration with NeoForge occurs during the
     * {@link RegisterPayloadHandlersEvent}.
     *
     * @param type  The custom packet type.
     * @param codec The codec used to encode/decode the packet.
     * @param <T>   The packet type extending {@link CustomPacketPayload}.
     */
    protected <T extends CustomPacketPayload> void registerPacket(
            CustomPacketPayload.Type<T> type,
            StreamCodec<FriendlyByteBuf, T> codec
    ) {
        codecRegistry.put(type, codec);
    }

    /**
     * Handles the {@link RegisterPayloadHandlersEvent} to register
     * all locally stored codecs with NeoForge.
     *
     * @param event The event providing access to the {@link PayloadRegistrar}.
     */
    public void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        for (Map.Entry<CustomPacketPayload.Type<?>, StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> entry : codecRegistry.entrySet()) {
            registerPayloadHelper(registrar, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Internal helper to register a single packet type with the NeoForge network.
     * <p>
     * Handles generic type casting safely for payload registration.
     *
     * @param registrar The {@link PayloadRegistrar} instance.
     * @param type      The packet type to register.
     * @param codec     The codec used to encode/decode the packet.
     * @param <T>       The packet type extending {@link CustomPacketPayload}.
     */
    @SuppressWarnings("unchecked")
    private <T extends CustomPacketPayload> void registerPayloadHelper(
            PayloadRegistrar registrar,
            CustomPacketPayload.Type<?> type,
            StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> codec
    ) {
        CustomPacketPayload.Type<T> castType = (CustomPacketPayload.Type<T>) type;
        StreamCodec<FriendlyByteBuf, T> castCodec = (StreamCodec<FriendlyByteBuf, T>) codec;

        IPayloadHandler<T> handler = (payload, context) -> onPayloadReceived(payload);

        registrar.playBidirectional(castType, castCodec, handler);
    }

    /**
     * Callback method invoked when a custom packet payload is received.
     * <p>
     * Subclasses must implement this to handle packet logic.
     *
     * @param payload The received {@link CustomPacketPayload}.
     */
    protected abstract void onPayloadReceived(CustomPacketPayload payload);

    /**
     * Sends a packet to the server.
     *
     * @param payload The {@link CustomPacketPayload} to send.
     */
    protected static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /**
     * Sends a packet to a specific client.
     *
     * @param payload The {@link CustomPacketPayload} to send.
     * @param player  The target {@link ServerPlayer}.
     */
    protected static void sendToClient(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }


    public static class GateSends extends NetworkTools {

        @Override
        protected void onPayloadReceived(CustomPacketPayload payload) {
            if (payload instanceof GateObjectPacket gatePacket) {
                IPayloadHandler<CustomPacketPayload> handler = (payload1, context) -> {
                    Player player = context.player();

                    if (player == null) return;

                    List<GateObject> gates = gatePacket.getGateObjects();

                    List<String> serializedGates = gates.stream().map(GateObject::serializeToString).toList();

                    player.getPersistentData().putString("Player_gates", String.join(";", serializedGates));
                };
            }
        }
        public static void LoginUpdate(ServerPlayer player, UUID id) {
            List<GateObject> filteredGates = GataBase.getFilteredGates(id);
            GateObjectPacket pkt = new GateObjectPacket(filteredGates);

            NetworkTools.sendToClient(pkt, player);
        }
    }
}



