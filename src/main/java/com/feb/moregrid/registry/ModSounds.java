package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.client.SoundScape;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
	private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
		 DeferredRegister.create(Registries.SOUND_EVENT, MoreGrid.MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> BUZZER =
		SOUND_EVENTS.register("buzzer", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "buzzer")
			)
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> POTATO_ELECTROCUTE =
		SOUND_EVENTS.register("potato_electrocute", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "potato_electrocute"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_OPEN =
		SOUND_EVENTS.register("electric_furnace.open", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.open"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_CLOSE =
		SOUND_EVENTS.register("electric_furnace.close", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.close"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_ON =
		SOUND_EVENTS.register("electric_furnace.on", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.on"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_OFF =
		SOUND_EVENTS.register("electric_furnace.off", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.off"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_FAN_START =
		SOUND_EVENTS.register("electric_furnace.fan_start", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.fan_start"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_FAN_LOOP =
		SOUND_EVENTS.register("electric_furnace.fan_loop", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.fan_loop"))
		);
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FURNACE_FAN_STOP =
		SOUND_EVENTS.register("electric_furnace.fan_stop", () ->
			SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "electric_furnace.fan_stop"))
		);

	
    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}