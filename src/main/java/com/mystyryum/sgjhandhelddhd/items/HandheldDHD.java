package com.mystyryum.sgjhandhelddhd.items;

import com.mystyryum.sgjhandhelddhd.SGJHandheldDHD;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class HandheldDHD {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SGJHandheldDHD.MOD_ID);

    public static final DeferredItem<Item> HDHD = ITEMS.register("hdhd",
            () -> new Item(new Item.Properties().stacksTo(1)));





    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}