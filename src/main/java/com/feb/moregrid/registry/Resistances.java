package com.feb.moregrid.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.config.ResistanceValues;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class Resistances implements ResistanceValues.Provider {
	public static final Resistances INSTANCE = new Resistances();
    private final Map<ResourceLocation, Resistance> resistances = new HashMap<>();

	@Override
	public @Nullable DoubleSupplier get(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = resistances.get(id);
        return entry == null ? null : entry::get;
	}

	@Override
	public @Nullable DoubleSupplier get(Block block, String suffix) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block)
                .withSuffix("." + suffix);
		var entry = resistances.get(id);
        return entry == null ? null : entry::get;
	}
	
	public static void register(ResourceLocation id, double resistance) {
		INSTANCE.resistances.put(id, new Resistance(resistance));
	}
	public static void register(ResourceLocation id, String suffix, double resistance) {
		INSTANCE.resistances.put(id.withSuffix("." + suffix), new Resistance(resistance));
	}

	private Resistances() {}

	private static class Resistance {
		private double value;
		public double get() {
			return value;
		}
		public Resistance(double value) {
			this.value = value;
		}
	}
}
