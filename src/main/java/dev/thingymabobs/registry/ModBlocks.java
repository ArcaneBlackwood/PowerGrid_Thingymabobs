package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.blocks.switches.LVSwitchDPDTBlock;
import dev.thingymabobs.blocks.switches.LVSwitchDPSTBlock;
import dev.thingymabobs.blocks.switches.LVSwitchSPDTBlock;
import dev.thingymabobs.blocks.switches.LVSwitchTPSTBlock;
import dev.thingymabobs.blocks.switches.MVSwitchDPDTBlock;
import dev.thingymabobs.blocks.switches.MVSwitchDPSTBlock;
import dev.thingymabobs.blocks.switches.MVSwitchSPDTBlock;
import dev.thingymabobs.blocks.switches.MVSwitchTPSTBlock;
import dev.thingymabobs.blocks.switches.SwitchBlock;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.blocks.LavaLamp;
import dev.thingymabobs.blocks.PowerShunt;
import dev.thingymabobs.blocks.PowerShuntEntity;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBattery;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryArray;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryBlock;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryBlockCT;
import dev.thingymabobs.blocks.battery.PotatoBatteryArray;
import dev.thingymabobs.blocks.battery.PotatoBatteryArrayEntity;
import dev.thingymabobs.blocks.battery.PotatoBatteryBlock;
import dev.thingymabobs.blocks.battery.PotatoBatteryBlockCT;
import dev.thingymabobs.blocks.battery.PotatoBatteryBlockEntity;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryEntity;
import dev.thingymabobs.blocks.electricfurnace.ElectricFurnace;
import dev.thingymabobs.blocks.electricfurnace.ElectricFurnaceConfig;
import dev.thingymabobs.blocks.electricfurnace.ElectricFurnaceEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.thingymabobs.blocks.Zoey.PlasmaGlobe.PlasmaGlobe;

public final class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS =
		DeferredRegister.createBlocks(Thingymabobs.MOD_ID);

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

	public static final DeferredBlock<Block> POISONOUS_POTATO_BATTERY = BLOCKS.registerBlock(
		"poisonous_potato_battery", PoisonousPotatoBattery::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_WART));
	public static final DeferredBlock<Block> POTATO_BATTERY_ARRAY = BLOCKS.registerBlock(
		"potato_battery_array", PotatoBatteryArray::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> POISONOUS_POTATO_BATTERY_ARRAY = BLOCKS.registerBlock(
		"poisonous_potato_battery_array", PoisonousPotatoBatteryArray::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> POTATO_BATTERY_BLOCK = BLOCKS.registerBlock(
		"potato_battery_block", PotatoBatteryBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> POISONOUS_POTATO_BATTERY_BLOCK = BLOCKS.registerBlock(
		"poisonous_potato_battery_block", PoisonousPotatoBatteryBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).requiresCorrectToolForDrops());

	public static final DeferredBlock<Block> POWER_SHUNT = BLOCKS.registerBlock(
		"power_shunt", PowerShunt::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> LAVA_LAMP = BLOCKS.registerBlock(
		"lava_lamp", LavaLamp::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).requiresCorrectToolForDrops());
	public static final DeferredBlock<Block> ELECTRIC_FURNACE = BLOCKS.registerBlock(
		"electric_furnace", ElectricFurnace::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).requiresCorrectToolForDrops());

	public static final DeferredBlock<Block> PLASMA_GLOBE = BLOCKS.registerBlock(
		"plasma_globe", PlasmaGlobe::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE).requiresCorrectToolForDrops());
	

	public static void register(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);

		CProperties.register(LV_SWITCH_DPDT.getId())
			.registerResistance(0.15f)
			.registerThermal(1f, 37.4f*2)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 320f)
			.complete(LVSwitchDPDTBlock::configUpdated);
		CProperties.register(LV_SWITCH_SPDT.getId())
			.registerResistance(0.15f)
			.registerThermal(0.5f, 37.4f)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 320f)
			.complete(LVSwitchSPDTBlock::configUpdated);
		CProperties.register(LV_SWITCH_TPST.getId())
			.registerResistance(0.15f)
			.registerThermal(1.5f, 37.4f*3)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE,320f)
			.complete(LVSwitchTPSTBlock::configUpdated);
		CProperties.register(LV_SWITCH_DPST.getId())
			.registerResistance(0.15f)
			.registerThermal(1f, 37.4f*2)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 320f)
			.complete(LVSwitchDPSTBlock::configUpdated);


		CProperties.register(MV_SWITCH_DPDT.getId())
			.registerResistance(0.05f)
			.registerThermal(2f, 51.2f*2)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 640f)
			.complete(MVSwitchDPDTBlock::configUpdated);
		CProperties.register(MV_SWITCH_SPDT.getId())
			.registerResistance(0.05f)
			.registerThermal(1f, 51.2f)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 640f)
			.complete(MVSwitchSPDTBlock::configUpdated);
		CProperties.register(MV_SWITCH_TPST.getId())
			.registerResistance(0.05f)
			.registerThermal(3f, 51.2f*3)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 640f)
			.complete(MVSwitchTPSTBlock::configUpdated);
		CProperties.register(MV_SWITCH_DPST.getId())
			.registerResistance(0.05f)
			.registerThermal(2f, 51.2f*2)
			.registerFloat(SwitchBlock.CONFIG_MAX_VOLTAGE, 640f)
			.complete(MVSwitchDPSTBlock::configUpdated);


		CProperties.register(POISONOUS_POTATO_BATTERY.getId())
			.registerThermal(100f, 0.04f)
			.registerFloat(PoisonousPotatoBattery.CONFIG_RECHARGE, 0.009f)
			.registerBattery(2.88f, 2.88f,
				0.8f, 1.5f, 300, 10000, 1f)
			.complete(PoisonousPotatoBatteryEntity::configUpdated);

		CProperties.register(POTATO_BATTERY_ARRAY.getId())
			.registerThermal(100f, 0.04f)
			.registerBattery(0.864f, 0.864f,
				0.9f, 1.6f, 110, 10000, 1f)
			.complete(PotatoBatteryArrayEntity::configUpdatedPotato);
		CProperties.register(POISONOUS_POTATO_BATTERY_ARRAY.getId())
			.registerThermal(100f, 0.04f)
			.registerFloat(PoisonousPotatoBattery.CONFIG_RECHARGE, 0.009f)
			.registerBattery(8.64f, 8.64f,
				1.4f, 2.6f, 75, 10000, 1.2f)
			.complete(PotatoBatteryArrayEntity::configUpdatedPoison);

		CProperties.register(POTATO_BATTERY_BLOCK.getId())
			.registerThermal(100f, 0.08f)
			.registerBattery(2.6f, 2.6f,
				1.0f, 1.8f, 35, 10000, 1.2f)
			.complete(PotatoBatteryBlockEntity::configUpdatedPotato);
		CProperties.register(POISONOUS_POTATO_BATTERY_BLOCK.getId())
			.registerThermal(100f, 0.08f)
			.registerFloat(PoisonousPotatoBattery.CONFIG_RECHARGE, 0.036f)
			.registerBattery(25f, 25f,
				1.7f, 3.0f, 25, 10000, 1.4f)
			.complete(PotatoBatteryBlockEntity::configUpdatedPoison);

		
		CProperties.register(POWER_SHUNT.getId())
			.registerThermal(25f, 1000f, 300f, 500f)
			.registerInt(PowerShuntEntity.CONFIG_RES_PRECISION, 4)
			.registerInt(PowerShuntEntity.CONFIG_RES_STEPS, 36)
			.complete(PowerShuntEntity::configUpdated);
		float furnaceTemp = 2000f, furnaceTempSt = furnaceTemp / 13;
		CProperties.register(ELECTRIC_FURNACE.getId())
			.registerResistance(240*240 / 5000f)
			.registerThermal(600f, 5000f, furnaceTemp, furnaceTempSt * 18)
			.register("ef", new ElectricFurnaceConfig(
				16, 10, 10/200f,
				furnaceTempSt*1, furnaceTempSt*3, furnaceTempSt*5, furnaceTempSt*10,
				2.5f, 2f/400f, 20*20f,
				500f, 3.5f))
			.complete(ElectricFurnaceEntity::configUpdated);

        BlockMovementChecks.registerAttachedCheck((BlockState state, Level world, BlockPos pos, Direction direction) -> {
			var block = state.getBlock();
            if (!(block instanceof PotatoBatteryBlock) && !(block instanceof PoisonousPotatoBatteryBlock))
                return CheckResult.PASS;
            return ConnectivityHandler.isConnected(world, pos, pos.relative(direction)) ? CheckResult.SUCCESS : CheckResult.PASS;
        });
	}

	public static void postRegister() {
		CreateRegistrate.connectedTextures(PotatoBatteryBlockCT::new).accept(POTATO_BATTERY_BLOCK.get());
		CreateRegistrate.connectedTextures(PoisonousPotatoBatteryBlockCT::new).accept(POISONOUS_POTATO_BATTERY_BLOCK.get());
	}
}