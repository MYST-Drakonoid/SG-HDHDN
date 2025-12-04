package com.mystyryum.sgjhandhelddhd;


import com.mystyryum.sgjhandhelddhd.blocks.EditingBlock;
import com.mystyryum.sgjhandhelddhd.blocks.GateSecBlock;
import com.mystyryum.sgjhandhelddhd.database.DefaultGateManager;
import com.mystyryum.sgjhandhelddhd.database.GataBase;
import com.mystyryum.sgjhandhelddhd.database.GateObject;
import com.mystyryum.sgjhandhelddhd.database.GateObjectPacket;
import com.mystyryum.sgjhandhelddhd.items.HandheldDHD;
import com.mystyryum.sgjhandhelddhd.network.NetworkTools;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.List;
import java.util.UUID;

import static net.povstalec.sgjourney.StargateJourney.MODID;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SGJHandheldDHD.MOD_ID)
public class SGJHandheldDHD {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "sgjhandhelddhd";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String Mod_Name = "Stargate Handheld DHD";

    public SGJHandheldDHD(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(GataBase.GatabaseChangedEvent.class);
        NeoForge.EVENT_BUS.register(this);



        // registering my mods stuff



        HandheldDHD.register(modEventBus);
        EditingBlock.register(modEventBus);
        GateSecBlock.register(modEventBus);


        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {


    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HandheldDHD.HDHD);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(EditingBlock.EDITING_BLOCK);
        }


        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(GateSecBlock.GATE_SEC_BLOCK);
        }
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GataBase.firstTimeTasks(event);


    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        LOGGER.warn("Server has stopped! Gatabase Backup started");
        DefaultGateManager.clear();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        UUID id = player.getUUID();
        NetworkTools.GateSends.LoginUpdate(player, id);
    }
}


