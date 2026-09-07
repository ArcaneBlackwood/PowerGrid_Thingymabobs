package dev.thingymabobs.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.config.ThermalValues;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class Thermals implements ThermalValues.Provider {
	public static final Thermals INSTANCE = new Thermals();
    private final Map<ResourceLocation, Thermal> thermals = new HashMap<>();

	@Override
	public @Nullable DoubleSupplier getMass(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = thermals.get(id);
        return entry == null ? null : entry::getMass;
	}

	@Override
	public @Nullable DoubleSupplier getPower(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = thermals.get(id);
        return entry == null ? null : entry::getPower;
	}
	
	public static void register(ResourceLocation id, double mass, double power) {
		INSTANCE.thermals.put(id, new Thermal(mass, power));
	}


	private static class Thermal {
		private double mass, power;
		public double getMass() {
			return mass;
		}
		public double getPower() {
			return power;
		}
		protected Thermal(double mass, double power) {
			this.mass = mass;
			this.power = power;
		}
	}
}
