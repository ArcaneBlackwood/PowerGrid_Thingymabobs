package com.feb.moregrid.registry;

import java.util.function.Supplier;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.blocks.PotatoBatteryArrayEntity;
import com.feb.moregrid.blocks.PotatoBatteryBlockEntity;
import com.feb.moregrid.blocks.SwitchBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MoreGrid.MOD_ID);

	public static final Supplier<BlockEntityType<SwitchBlockEntity>> SWITCH = 
    BLOCK_ENTITY_TYPES.register("switch", () -> 
        BlockEntityType.Builder.of(SwitchBlockEntity::new,
			ModBlocks.LV_SWITCH_DPDT.get(),
			ModBlocks.LV_SWITCH_SPDT.get(),
			ModBlocks.LV_SWITCH_TPST.get(),
			ModBlocks.LV_SWITCH_DPST.get(),
			ModBlocks.MV_SWITCH_DPDT.get(),
			ModBlocks.MV_SWITCH_SPDT.get(),
			ModBlocks.MV_SWITCH_TPST.get(),
			ModBlocks.MV_SWITCH_DPST.get())
		.build(null));

	public static final Supplier<BlockEntityType<PotatoBatteryArrayEntity>> POTATO_BATTERY_ARRAY = 
    BLOCK_ENTITY_TYPES.register("potato_battery_array", () -> 
        BlockEntityType.Builder.of(PotatoBatteryArrayEntity::new,
			ModBlocks.POTATO_BATTERY_ARRAY.get(),
			ModBlocks.POISONOUS_POTATO_BATTERY_ARRAY.get())
		.build(null));

	public static final Supplier<BlockEntityType<PotatoBatteryBlockEntity>> POTATO_BATTERY_BLOCK = 
    BLOCK_ENTITY_TYPES.register("potato_battery_block", () -> 
        BlockEntityType.Builder.of(PotatoBatteryBlockEntity::new,
			ModBlocks.POTATO_BATTERY_BLOCK.get(),
			ModBlocks.POISONOUS_POTATO_BATTERY_BLOCK.get())
		.build(null));
}
