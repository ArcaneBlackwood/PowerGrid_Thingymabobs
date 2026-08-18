package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.blocks.LVSwitchDPDTBlock;
import com.feb.moregrid.blocks.LVSwitchDPSTBlock;
import com.feb.moregrid.blocks.LVSwitchSPDTBlock;
import com.feb.moregrid.blocks.LVSwitchTPSTBlock;
import com.feb.moregrid.blocks.MVSwitchDPDTBlock;
import com.feb.moregrid.blocks.MVSwitchDPSTBlock;
import com.feb.moregrid.blocks.MVSwitchSPDTBlock;
import com.feb.moregrid.blocks.MVSwitchTPSTBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS =
		DeferredRegister.createBlocks(MoreGrid.MOD_ID);

	public static final DeferredBlock<Block> LV_SWITCH_DPDT = BLOCKS.registerBlock(
		"lv_switch_dpdt", LVSwitchDPDTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> LV_SWITCH_SPDT = BLOCKS.registerBlock(
		"lv_switch_spdt", LVSwitchSPDTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> LV_SWITCH_TPST = BLOCKS.registerBlock(
		"lv_switch_tpst", LVSwitchTPSTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> LV_SWITCH_DPST = BLOCKS.registerBlock(
		"lv_switch_dpst", LVSwitchDPSTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());

	public static final DeferredBlock<Block> MV_SWITCH_DPDT = BLOCKS.registerBlock(
		"mv_switch_dpdt", MVSwitchDPDTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> MV_SWITCH_SPDT = BLOCKS.registerBlock(
		"mv_switch_spdt", MVSwitchSPDTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> MV_SWITCH_TPST = BLOCKS.registerBlock(
		"mv_switch_tpst", MVSwitchTPSTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> MV_SWITCH_DPST = BLOCKS.registerBlock(
		"mv_switch_dpst", MVSwitchDPSTBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	
	public static void register(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
		Resistances.register(LV_SWITCH_DPDT.getId(), 0.15);
		Thermals.register(LV_SWITCH_DPDT.getId(), 1, 37.4*2);
		Resistances.register(LV_SWITCH_SPDT.getId(), 0.15);
		Thermals.register(LV_SWITCH_SPDT.getId(), 0.5, 37.4);
		Resistances.register(LV_SWITCH_TPST.getId(), 0.15);
		Thermals.register(LV_SWITCH_TPST.getId(), 1.5, 37.4*3);
		Resistances.register(LV_SWITCH_DPST.getId(), 0.15);
		Thermals.register(LV_SWITCH_DPST.getId(), 1, 37.4*2);

		Resistances.register(MV_SWITCH_DPDT.getId(), 0.05);
		Thermals.register(MV_SWITCH_DPDT.getId(), 2, 51.2*2);
		Resistances.register(MV_SWITCH_SPDT.getId(), 0.05);
		Thermals.register(MV_SWITCH_SPDT.getId(), 1, 51.2);
		Resistances.register(MV_SWITCH_TPST.getId(), 0.05);
		Thermals.register(MV_SWITCH_TPST.getId(), 3, 51.2*3);
		Resistances.register(MV_SWITCH_DPST.getId(), 0.05);
		Thermals.register(MV_SWITCH_DPST.getId(), 2, 51.2*2);
	}
}