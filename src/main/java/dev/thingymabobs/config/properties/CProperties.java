package dev.thingymabobs.config.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.thingymabobs.Thingymabobs;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class CProperties extends ConfigBase implements ResistanceValues.Provider, ThermalValues.Provider {
	public static final String VOLTAGE = "voltage";
	public static final String POWER = "power";

	public static final CProperties INSTANCE = new CProperties();
	public static final float TEMP_MAX_DEFAULT = 150f;
	public static final float OVERHEAT_DEFAULT = 175f;
	private static Map<ResourceLocation, Prop> PROPERTIES = new HashMap<>();
	private final Builder builder = new Builder();

	private CProperties() {
		depth = 0;
	}
	public static CProperties getInstance() {
		return INSTANCE;
	}
	@Override
	public String getName() {
		return "properties";
	}


	public static @Nullable Prop get(SmartBlockEntity block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock());
		return PROPERTIES.get(id);
	}
	public static @Nullable Prop get(SmartBlockEntity block, String suffix) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock());
		return PROPERTIES.get(id.withSuffix("." + suffix));
	}
	public static @Nullable Prop get(PlacedComponent placed) {
		ResourceLocation id = ComponentRegistry.getId(placed.component);
		return PROPERTIES.get(id);
	}
	public static @Nullable Prop get(PlacedComponent placed, String suffix) {
		ResourceLocation id = ComponentRegistry.getId(placed.component);
		return PROPERTIES.get(id.withSuffix("." + suffix));
	}

	@Override
	public @Nullable DoubleSupplier get(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = PROPERTIES.get(id);
		if (entry == null)
			return null;
		Resistance res = entry.getResistance();
		if (res == null)
			return null;
		return res::get;
	}
	@Override
	public @Nullable DoubleSupplier get(Block block, String suffix) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = PROPERTIES.get(id);
		if (entry == null)
			return null;
		Resistance res = entry.getResistance(suffix);
		if (res == null)
			return null;
		return res::get;
	}
	@Override
	public @Nullable DoubleSupplier getMass(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = PROPERTIES.get(id);
		if (entry == null)
			return null;
		Thermal therm = entry.getThermal();
		if (therm == null)
			return null;
		return therm::getMass;
	}
	@Override
	public @Nullable DoubleSupplier getPower(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		var entry = PROPERTIES.get(id);
		if (entry == null)
			return null;
		Thermal therm = entry.getThermal();
		if (therm == null)
			return null;
		return therm::getPower;
	}


	public static Prop register(ResourceLocation id) {
		Prop prop = INSTANCE.new Prop(id.getPath());
		PROPERTIES.put(id, prop);
		return prop;
	}
	public static Prop register(ResourceLocation id, String suffix) {
		Prop prop = INSTANCE.new Prop(id.getPath());
		PROPERTIES.put(id.withSuffix("." + suffix), prop);
		return prop;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		for (Prop prop : PROPERTIES.values()) prop.onLoad();
	}

	public class Prop {
		public final Map<String, ASubProp> props = new HashMap<>();
		private Consumer<Prop> loadCallback = null;
		private String id;

		private static final String KEY_RESISTANCE = "resistance";
		private static final String KEY_THERMAL = "thermal";
		private static final String KEY_BATTERY_SPEC = "battery";


		protected Prop(String id) {
			int idDot = id.lastIndexOf(".");
			if (idDot != -1)
				id.substring(idDot + 1);
			this.id = id;
		}

		protected void onLoad() {
			for (var entry : props.entrySet())
				entry.getValue().onLoad();
			if (loadCallback != null)
				loadCallback.accept(this);
		}
		public Prop complete() {
			builder.group(id);
			for (var entry : props.entrySet())
				entry.getValue().register(entry.getKey(), builder);
			return this;
		}
		public Prop complete(Consumer<Prop> loadCallback) {
			this.loadCallback = loadCallback;
			complete();
			return this;
		}
		public Prop onLoad(Consumer<Prop> loadCallback) {
			this.loadCallback = loadCallback;
			return this;
		}

		public Prop alsoForId(ResourceLocation id) {
			PROPERTIES.put(id, this);
			return this;
		}


		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> type, String key) {
			ASubProp prop = props.get(key);
			if (prop == null || prop.getType() != type)
				return null;
			return (T) prop;
		}

		public Class<?> getType(String key) {
			return props.get(key).getType();
		}

		public Resistance getResistance() {
			return get(Resistance.class, KEY_RESISTANCE);
		}
		public Resistance getResistance(String name) {
			return get(Resistance.class, KEY_RESISTANCE + "_" + name);
		}
		public Thermal getThermal() {
			return get(Thermal.class, KEY_THERMAL);
		}
		public Thermal getThermal(String name) {
			return get(Thermal.class, KEY_THERMAL + "_" + name);
		}
		public BatterySpec getBattery() {
			return get(BatterySpec.class, KEY_BATTERY_SPEC);
		}
		public BatterySpec getBattery(String name) {
			return get(BatterySpec.class, KEY_BATTERY_SPEC + "_" + name);
		}
		public FloatProp getFloat(String name) {
			return get(FloatProp.class, name);
		}
		public IntProp getInt(String name) {
			return get(IntProp.class, name);
		}

		public Prop mutateProperties(Consumer<Map<String, ASubProp>> callback) {
			callback.accept(props);
			return this;
		}

		public Prop register(String name, ASubProp prop) {
			ASubProp prevProp = props.put(name, prop);
			if (prevProp != null)
				Thingymabobs.LOGGER.warn("Replacing previous config property in '"+id+"'' with: "+prop.toString());
			return this;
		}
		public Prop registerResistance(float resistance) {
			props.put(KEY_RESISTANCE, new Resistance(resistance));
			return this;
		}
		public Prop registerResistance(String name, float resistance) {
			props.put(KEY_RESISTANCE + "_" + name, new Resistance(resistance));
			return this;
		}
		public Prop registerResistance(String name, float resistance, String comment) {
			props.put(KEY_RESISTANCE + "_" + name, new Resistance(resistance, comment));
			return this;
		}
		public Prop registerThermal(float mass, float power) {
			props.put(KEY_THERMAL, new Thermal(mass, power, TEMP_MAX_DEFAULT, OVERHEAT_DEFAULT));
			return this;
		}
		public Prop registerThermal(float mass, float power, float temp, float overheat) {
			props.put(KEY_THERMAL, new Thermal(mass, power, temp, overheat));
			return this;
		}
		public Prop registerThermal(String name, float mass, float power) {
			props.put(KEY_THERMAL + "_" + name, new Thermal(mass, power, TEMP_MAX_DEFAULT, OVERHEAT_DEFAULT));
			return this;
		}
		public Prop registerThermal(String name, float mass, float power, float temp, float overheat) {
			props.put(KEY_THERMAL + "_" + name, new Thermal(mass, power, temp, overheat));
			return this;
		}
		public Prop registerBattery(float max, float initial, float voltMin,
				float voltMax, float resMin, float resMax, float resCurve) {
			props.put(KEY_BATTERY_SPEC, new BatterySpec(max, initial, voltMin, voltMax, resMin, resMax, resCurve));
			return this;
		}
		public Prop registerBattery(String name, float max, float initial, float voltMin,
				float voltMax, float resMin, float resMax, float resCurve) {
			props.put(KEY_BATTERY_SPEC + "_" + name,
					new BatterySpec(max, initial, voltMin, voltMax, resMin, resMax, resCurve));
			return this;
		}
		public Prop registerFloat(String name, float value) {
			props.put(name, new FloatProp(value));
			return this;
		}
		public Prop registerFloat(String name, float value, float min, float max) {
			props.put(name, new FloatProp(value, min, max));
			return this;
		}
		public Prop registerFloat(String name, float value, String comment) {
			FloatProp custom = new FloatProp(value);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public Prop registerFloat(String name, float value, float min, float max, String comment) {
			FloatProp custom = new FloatProp(value, min, max);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public Prop registerInt(String name, int value) {
			props.put(name, new IntProp(value));
			return this;
		}
		public Prop registerInt(String name, int value, String comment) {
			IntProp custom = new IntProp(value);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public Prop registerInt(String name, int value, int min, int max, String comment) {
			IntProp custom = new IntProp(value, min, max);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			boolean notFirst = false;
			sb.append("Prop[");
			for (var entry : props.entrySet()) {
				if (notFirst) sb.append(", ");
				notFirst = true;
				sb.append(entry.getKey()).append(": ");
				sb.append(entry.getValue().getType());
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public static abstract class ASubProp {
		public abstract Class<?> getType();

		public void onLoad() {
		}

		public abstract void register(String id, Builder builder);
	}

	public class Builder {
		protected Builder() { }
		public ConfigBool b(boolean current, String name, String... comment) {
			return new ConfigBool(name, current, comment);
		}
		public ConfigFloat f(float current, float min, float max, String name, String... comment) {
			return new ConfigFloat(name, current, min, max, comment);
		}
		public ConfigFloat f(float current, float min, String name, String... comment) {
			return f(current, min, Float.MAX_VALUE, name, comment);
		}
		protected ConfigInt i(int current, int min, int max, String name, String... comment) {
			return new ConfigInt(name, current, min, max, comment);
		}
		public ConfigInt i(int current, int min, String name, String... comment) {
			return i(current, min, Integer.MAX_VALUE, name, comment);
		}
		public ConfigInt i(int current, String name, String... comment) {
			return i(current, Integer.MIN_VALUE, Integer.MAX_VALUE, name, comment);
		}
		public <T extends Enum<T>> ConfigEnum<T> e(T defaultValue, String name, String... comment) {
			return new ConfigEnum<>(name, defaultValue, comment);
		}
		public ConfigGroup group(String name, String... comment) {
			return new ConfigGroup(name, depth + 1, comment);
		}
	}
}
