package com.mystyryum.sgjhandhelddhd.database;

import com.mystyryum.sgjhandhelddhd.database.GateObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GateObjectPacket implements CustomPacketPayload {

    private final List<GateObject> gateObjects;

    // Constructor
    public GateObjectPacket(List<GateObject> gateObjects) {
        this.gateObjects = gateObjects;
    }

    public List<GateObject> getGateObjects() {
        return gateObjects;
    }

    // Required packet identifier using a ResourceLocation
    public static final Type<GateObjectPacket> TYPE =
            new Type<>(ResourceLocation.parse("sgjhandhelddhd:gate_object_packet"));

    // ---- ENCODER ----
    public static void encode(GateObjectPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.gateObjects.size());

        // serialize each gate
        for (GateObject gate : packet.gateObjects) {
            gate.serialize(buf);
        }
    }

    // ---- DECODER ----
    public static GateObjectPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();

        List<GateObject> gates = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            // IMPORTANT: This must be a *static* method
            gates.add(GateObject.deserialize(buf));
        }

        return new GateObjectPacket(gates);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE; // MUST NOT BE NULL
    }
}
