package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
	public static final DeferredRegister.DataComponents DATA_COMPONENTS =
		DeferredRegister.createDataComponents(
			Registries.DATA_COMPONENT_TYPE,
			Thingymabobs.MOD_ID
		);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>>  BLOWN =
		DATA_COMPONENTS.registerComponentType("blown",
			builder -> builder.persistent(Codec.unit(Unit.INSTANCE)));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>>  BAKED =
		DATA_COMPONENTS.registerComponentType("baked",
			builder -> builder.persistent(Codec.unit(Unit.INSTANCE)));

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>>  ENERGY =
		DATA_COMPONENTS.registerComponentType("energy",
			builder -> builder.persistent(Codec.DOUBLE));
}