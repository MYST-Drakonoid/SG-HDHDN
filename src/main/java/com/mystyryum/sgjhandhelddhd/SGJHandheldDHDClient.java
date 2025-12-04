package com.mystyryum.sgjhandhelddhd;

import com.mystyryum.sgjhandhelddhd.database.GateObject;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SGJHandheldDHD.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = SGJHandheldDHD.MOD_ID, value = Dist.CLIENT)
public class SGJHandheldDHDClient {
    public SGJHandheldDHDClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        SGJHandheldDHD.LOGGER.info("HELLO FROM CLIENT SETUP");
        SGJHandheldDHD.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }


    public static class ClientGateCache {

        private static List<GateObject> gateList = new ArrayList<>();

        public static void setGates(List<GateObject> gates) {
            gateList = gates;
        }

        public static List<GateObject> getGates() {
            return gateList;
        }

        public static void clear() {
            gateList.clear();
        }
    }
}
