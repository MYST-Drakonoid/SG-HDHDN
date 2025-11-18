package com.mystyryum.sgjhandhelddhd.gui.block;

import com.mystyryum.sgjhandhelddhd.SGJHandheldDHD;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EditingBlockScreen extends AbstractContainerScreen<EditingBlockScreenHandler> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(SGJHandheldDHD.MOD_ID, "textures/gui/editing_block.png");

    public EditingBlockScreen(EditingBlockScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.imageWidth = 208;  // 176 + 32
        this.imageHeight = 198; // 166 + 32
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
