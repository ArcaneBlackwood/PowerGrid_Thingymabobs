package dev.thingymabobs.registry;

import java.util.function.Supplier;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.blocks.PowerShuntEntity;
import dev.thingymabobs.blocks.battery.PotatoBatteryArrayEntity;
import dev.thingymabobs.blocks.battery.PotatoBatteryBlockEntity;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryEntity;
import dev.thingymabobs.blocks.electricfurnace.ElectricFurnaceEntity;
import dev.thingymabobs.blocks.electricfurnace.ElectricFurnaceRenderer;
import dev.thingymabobs.blocks.lavalamp.LavaLampEntity;
import dev.thingymabobs.blocks.lavalamp.LavaLampRenderer;
import dev.thingymabobs.blocks.switches.SwitchBlockEntity;
import dev.thingymabobs.registry.capabilities.ICapability;
import dev.thingymabobs.registry.capabilities.ItemCapability;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.thingymabobs.blocks.Zoey.PlasmaGlobe.PlasmaGlobeEntity;

public final class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thingymabobs.MOD_ID);

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

	public static final Supplier<BlockEntityType<PoisonousPotatoBatteryEntity>> POTATO_BATTERY = 
	BLOCK_ENTITY_TYPES.register("potato_battery", () -> 
		BlockEntityType.Builder.of(PoisonousPotatoBatteryEntity::new,
			ModBlocks.POISONOUS_POTATO_BATTERY.get())
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

	public static final Supplier<BlockEntityType<PowerShuntEntity>> POWER_SHUNT = 
	BLOCK_ENTITY_TYPES.register("power_shunt", () -> 
		BlockEntityType.Builder.of(PowerShuntEntity::new,
			ModBlocks.POWER_SHUNT.get())
		.build(null));

	public static final Supplier<BlockEntityType<LavaLampEntity>> LAVA_LAMP = 
	BLOCK_ENTITY_TYPES.register("lava_lamp", () -> 
		BlockEntityType.Builder.of(LavaLampEntity::new,
			ModBlocks.LAVA_LAMP.get())
		.build(null));


	public static final Supplier<BlockEntityType<PlasmaGlobeEntity>> PLASMA_GLOBE = 
	BLOCK_ENTITY_TYPES.register("plasma_globe", () -> 
		BlockEntityType.Builder.of(PlasmaGlobeEntity::new,
			ModBlocks.PLASMA_GLOBE.get())
		.build(null));


	public static final Supplier<BlockEntityType<ElectricFurnaceEntity>> ELECTRIC_FURNACE = 
	BLOCK_ENTITY_TYPES.register("electric_furnace", () -> 
		BlockEntityType.Builder.of(ElectricFurnaceEntity::new,
			ModBlocks.ELECTRIC_FURNACE.get())
		.build(null));


	public static final ICapability[] HAS_CAPABILITIES = new ICapability[] {
		new ItemCapability<ElectricFurnaceEntity>(ELECTRIC_FURNACE)};


	public static void register(IEventBus modBus) {
		BLOCK_ENTITY_TYPES.register(modBus);
		modBus.addListener(ModBlockEntities::registerCapabilities);
	}
	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		modBus.addListener(ModBlockEntities::registerClientExtensions);
	}
	@OnlyIn(Dist.CLIENT)
	private static void registerCapabilities(RegisterCapabilitiesEvent event)  {
		for (ICapability capability : HAS_CAPABILITIES) capability.apply(event);
	}
	@OnlyIn(Dist.CLIENT)
	private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(LAVA_LAMP.get(), LavaLampRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ELECTRIC_FURNACE.get(), ElectricFurnaceRenderer::new);
	}

}
