package com.mystyryum.sgjhandhelddhd.blocks;

import com.mystyryum.sgjhandhelddhd.SGJHandheldDHD;
import com.mystyryum.sgjhandhelddhd.items.HandheldDHD;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class GateSecBlock {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(SGJHandheldDHD.MOD_ID);

    public static final DeferredBlock<Block> GATE_SEC_BLOCK = registerBlock("gate_sec_block",
            () -> new stateblocks(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            ));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static  <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        // HandheldDHD will handle item registry of blocks
        HandheldDHD.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
