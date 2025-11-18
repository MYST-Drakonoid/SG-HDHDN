package com.mystyryum.sgjhandhelddhd.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class MiscMethods {

    /**
     * Retrieves a player's name from their UUID.
     * <p>
     * This method first checks if the player is currently online.
     * If not, it attempts to retrieve their cached profile from the server’s profile cache
     * (which stores information about players who have joined before).
     * <p>
     * If no profile is found in either source, "Unknown Player" is returned.
     *
     * <p><b>Usage Notes:</b></p>
     * <ul>
     *     <li>This will always succeed for online players.</li>
     *     <li>For offline players, this only works if they have joined the server before
     *         and are still present in the profile cache.</li>

     * </ul>
     *
     * @param server the active {@link net.minecraft.server.MinecraftServer} instance.
     * @param uuid   the {@link java.util.UUID} of the player.
     * @return the player’s username if found; otherwise returns "Unknown Player".
     */
    public static String getPlayerNameFromUUID(MinecraftServer server, UUID uuid) {
        // Try to find an online player first (always accurate)
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }

        // Try to fetch from the cached profiles (players who joined before)
        GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
        if (profile != null) {
            return profile.getName();
        }

        // If nothing was found, return a placeholder
        return "Unknown Player";
    }


}
