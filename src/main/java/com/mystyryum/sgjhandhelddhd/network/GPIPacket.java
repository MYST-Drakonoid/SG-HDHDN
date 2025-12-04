package com.mystyryum.sgjhandhelddhd.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * ============================================================================
 * GPIPacket  —  A network packet used to send Stargate Edit/Add/Remove actions
 * ============================================================================
 *
 * This packet is a wrapper around a {@link NetworkTools.GPI} object, which
 * contains:
 *   • target gate (may be null)
 *   • updated gate (may be null)
 *   • action type ("ADD", "EDIT", "REMOVE")
 *
 * The packet is sent from client → server to request a database modification.
 *
 * IMPORTANT:
 *  - NeoForge requires:
 *      encode(FriendlyByteBuf, Packet)
 *      decode(FriendlyByteBuf)
 *  - This class supplies a StreamCodec so the networking system knows how to
 *    serialize and deserialize the packet.
 */
public class GPIPacket implements CustomPacketPayload {

    /**
     * The payload data being transmitted — a structured gate-change request.
     */
    private final NetworkTools.GPI gpi;

    /**
     * Constructs the packet wrapper using the provided GPI data object.
     *
     * @param gpi A GPI object describing the requested gate operation.
     */
    public GPIPacket(NetworkTools.GPI gpi) {
        this.gpi = gpi;
    }

    /**
     * Required NeoForge packet type identifier.
     * This must be globally unique to your mod.
     */
    public static final Type<GPIPacket> TYPE =
            new Type<>(ResourceLocation.parse("sgjhandhelddhd:gpi_packet"));

    /**
     * Serializes the packet into the network buffer.
     *
     * NOTE: NeoForge requires the signature (buf, value) for encode().
     *
     * @param buf     The buffer to write into
     * @param packet  The GPIPacket instance being serialized
     */
    public static void encode(FriendlyByteBuf buf, GPIPacket packet) {
        // Delegate serialization to GPI.serialize()
        packet.gpi.serialize(buf);
    }

    /**
     * Deserializes a GPI object from the buffer and wraps it in a packet.
     *
     * @param buf The buffer containing encoded packet data
     * @return A reconstructed GPIPacket
     */
    public static GPIPacket decode(FriendlyByteBuf buf) {
        NetworkTools.GPI gpi = NetworkTools.GPI.deserialize(buf);
        return new GPIPacket(gpi);
    }

    /**
     * Returns the NeoForge packet type identifier.
     *
     * @return the TYPE constant for this packet
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * StreamCodec defining how NeoForge serializes & deserializes this packet.
     *
     * StreamCodec.of() requires:
     *   (buf, packet) -> void   → encoder
     *   (buf) -> packet         → decoder
     *
     * Our encode() method matches the reversed signature, so we use:
     *    StreamCodec.of(GPIPacket::encode, GPIPacket::decode)
     */
    public static final StreamCodec<FriendlyByteBuf, GPIPacket> STREAM_CODEC =
            StreamCodec.of(GPIPacket::encode, GPIPacket::decode);

    /**
     * @return The internal GPI object stored in this packet
     */
    public NetworkTools.GPI getGPI() {
        return gpi;
    }
}
