package com.mystyryum.sgjhandhelddhd.items;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class HandheldHelperMethods {


    public record GateCommandPayload(BlockPos gatePos, String command) {

        // Encode the payload into the FriendlyByteBuf
        public static void encode(GateCommandPayload msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.gatePos());   // send where the gate is
            buf.writeUtf(msg.command());        // send what action to perform
        }

        // Decode it back from the buffer
        public static GateCommandPayload decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            String cmd = buf.readUtf();
            return new GateCommandPayload(pos, cmd);
        }
    }
}
