package com.mystyryum.sgjhandhelddhd.gui.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class EditingBlockScreenHandler extends AbstractContainerMenu {

    private final UUID playerUUID;

    // Constructor
    public EditingBlockScreenHandler(int syncID, UUID playerUUID) {
        super(null, syncID); // MenuType can be null for now
        this.playerUUID = playerUUID;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No inventory to move items from, so just return empty
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Always allow the screen to be open
        return true;
    }

    // Getter to access the player's UUID from other methods
    public UUID getPlayerUUID() {
        return playerUUID;
    }
}