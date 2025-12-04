package com.mystyryum.sgjhandhelddhd.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network packet wrapper for sending a DIP (Dial Instruction Packet)
 * from client → server.
 *
 * <p>This packet contains:
 *     • Chevron address (int[])
 *     • Dialing type (enum)
 *     • UUID of the player's handheld DHD
 *
 * The actual logic is stored in {@link NetworkTools.DIP};
 * this class simply handles encoding/decoding for network transfer.
 */
public class DIPPacket implements CustomPacketPayload {

    /** The actual dialing instruction being transported. */
    private final NetworkTools.DIP dip;

    /**
     * Constructs a new DIPPacket.
     *
     * @param dip The dialing instruction being wrapped for network transport.
     */
    public DIPPacket(NetworkTools.DIP dip) {
        this.dip = dip;
    }

    // -------------------------------------------------------------------------
    // PACKET TYPE IDENTIFIER
    // -------------------------------------------------------------------------

    /**
     * Unique packet identifier for NeoForge networking.
     * The ResourceLocation MUST match what you use during registration.
     */
    public static final Type<DIPPacket> TYPE =
            new Type<>(ResourceLocation.parse("sgjhandhelddhd:dip_packet"));

    // -------------------------------------------------------------------------
    // ENCODE / DECODE
    // -------------------------------------------------------------------------

    /**
     * Encodes this packet into the given buffer.
     *
     * Order MUST match DIP.deserialize().
     *
     * @param buf     The buffer to write into.
     * @param packet  The DIPPacket being serialized.
     */
    public static void encode(FriendlyByteBuf buf, DIPPacket packet) {
        packet.dip.serialize(buf);
    }

    /**
     * Decodes a DIPPacket from the given buffer.
     * Must match the order defined in DIP.serialize().
     *
     * @param buf Buffer containing packet bytes.
     * @return A reconstructed DIPPacket instance.
     */
    public static DIPPacket decode(FriendlyByteBuf buf) {
        NetworkTools.DIP dip = NetworkTools.DIP.deserialize(buf);
        return new DIPPacket(dip);
    }

    // -------------------------------------------------------------------------
    // PACKET TYPE REGISTRATION HOOK
    // -------------------------------------------------------------------------

    /**
     * Returns the registered packet type used by NeoForge.
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // -------------------------------------------------------------------------
    // STREAM CODEC
    // -------------------------------------------------------------------------

    /**
     * Stream codec for NeoForge 1.21.x packet registration.
     *
     * The method reference order MUST be:
     *     encode(buf, value)
     *     decode(buf)
     *
     * This is the correct signature:
     *     StreamCodec<FriendlyByteBuf, DIPPacket>
     */
    public static final StreamCodec<FriendlyByteBuf, DIPPacket> STREAM_CODEC =
            StreamCodec.of(DIPPacket::encode, DIPPacket::decode);

    // -------------------------------------------------------------------------
    // ACCESSOR
    // -------------------------------------------------------------------------

    /**
     * @return The contained Dial Instruction Packet (DIP).
     */
    public NetworkTools.DIP getDIP() {
        return dip;
    }
}
