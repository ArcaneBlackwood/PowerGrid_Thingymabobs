package dev.thingymabobs.config.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
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
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public final class CProperties extends ConfigBase implements ResistanceValues.Provider, ThermalValues.Provider {
	public static final String VOLTAGE = "voltage";
	public static final String POWER = "power";
	public static final String CURRENT = "current";
	public static final String MIN = "minimum";
	public static final String MAX = "maximum";

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
		Prop entry = PROPERTIES.get(id);
		if (!(entry instanceof PropDevice device))
			return null;
		Resistance res = device.getResistance();
		if (res == null)
			return null;
		return res::get;
	}
	@Override
	public @Nullable DoubleSupplier get(Block block, String suffix) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		Prop entry = PROPERTIES.get(id);
		if (!(entry instanceof PropDevice device))
			return null;
		Resistance res = device.getResistance(suffix);
		if (res == null)
			return null;
		return res::get;
	}
	@Override
	public @Nullable DoubleSupplier getMass(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		Prop entry = PROPERTIES.get(id);
		if (!(entry instanceof PropDevice device))
			return null;
		Thermal therm = device.getThermal();
		if (therm == null)
			return null;
		return therm::getMass;
	}
	@Override
	public @Nullable DoubleSupplier getPower(Block block) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
		Prop entry = PROPERTIES.get(id);
		if (!(entry instanceof PropDevice device))
			return null;
		Thermal therm = device.getThermal();
		if (therm == null)
			return null;
		return therm::getPower;
	}


	private static String toPath(ResourceLocation id) {
		String path = id.getPath();
		int idDot = path.lastIndexOf(".");
		return idDot == -1 ? path : path.substring(idDot + 1);
	}
	public static PropDevice register(int depth, String path, ResourceLocation id) {
		INSTANCE.depth = depth;
		PropDevice prop = INSTANCE.new PropDevice(path);
		PROPERTIES.put(id, prop);
		return prop;
	}
	public static PropDevice register(ResourceLocation id) {
		INSTANCE.depth = 0;
		PropDevice prop = INSTANCE.new PropDevice(toPath(id));
		PROPERTIES.put(id, prop);
		return prop;
	}
	public static PropDevice register(ResourceLocation id, String suffix) {
		INSTANCE.depth = 0;
		PropDevice prop = INSTANCE.new PropDevice(toPath(id));
		PROPERTIES.put(id.withSuffix("." + suffix), prop);
		return prop;
	}
	public static ASubProp register(int depth, ResourceLocation id, ASubProp prop) {
		INSTANCE.depth = depth;
		prop.setup(toPath(id), INSTANCE.builder);
		PROPERTIES.put(id, prop);
		return prop;
	}
	public static ASubProp register(ResourceLocation id, ASubProp prop) {
		INSTANCE.depth = 0;
		prop.setup(toPath(id), INSTANCE.builder);
		PROPERTIES.put(id, prop);
		return prop;
	}
	public static ASubProp register(ResourceLocation id, String suffix, ASubProp prop) {
		INSTANCE.depth = 0;
		prop.setup(toPath(id), INSTANCE.builder);
		PROPERTIES.put(id.withSuffix("." + suffix), prop);
		return prop;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		for (Prop prop : PROPERTIES.values()) prop.onLoad();
	}
	@Override
	public void onReload() {
		super.onReload();
		for (Prop prop : PROPERTIES.values()) prop.onLoad();
	}

	public static abstract class Prop {
		public abstract void onLoad();
	}
	public class PropDevice extends Prop {
		public final Map<String, ASubProp> props = new HashMap<>();
		private Consumer<PropDevice> loadCallback = null;
		private String id;

		private static final String KEY_RESISTANCE = "resistance";
		private static final String KEY_THERMAL = "thermal";
		private static final String KEY_BATTERY_SPEC = "battery";


		protected PropDevice(String id) {
			this.id = id;
		}

		@Override
		public void onLoad() {
			for (var entry : props.entrySet())
				entry.getValue().onLoad();
			if (loadCallback != null)
				loadCallback.accept(this);
		}
		public PropDevice complete() {
			builder.group(id);
			for (var entry : props.entrySet())
				entry.getValue().register(entry.getKey(), builder);
			return this;
		}
		public PropDevice complete(Consumer<PropDevice> loadCallback) {
			this.loadCallback = loadCallback;
			complete();
			return this;
		}
		public PropDevice onLoad(Consumer<PropDevice> loadCallback) {
			this.loadCallback = loadCallback;
			return this;
		}

		public PropDevice alsoForId(ResourceLocation id) {
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

		public PropDevice mutateProperties(Consumer<Map<String, ASubProp>> callback) {
			callback.accept(props);
			return this;
		}

		public PropDevice register(String name, ASubProp prop) {
			ASubProp prevProp = props.put(name, prop);
			if (prevProp != null)
				Thingymabobs.LOGGER.warn("Replacing previous config property in '"+id+"'' with: "+prop.toString());
			return this;
		}
		public PropDevice registerResistance(float resistance) {
			props.put(KEY_RESISTANCE, new Resistance(resistance));
			return this;
		}
		public PropDevice registerResistance(String name, float resistance) {
			props.put(KEY_RESISTANCE + "_" + name, new Resistance(resistance));
			return this;
		}
		public PropDevice registerResistance(String name, float resistance, String comment) {
			props.put(KEY_RESISTANCE + "_" + name, new Resistance(resistance, comment));
			return this;
		}
		public PropDevice registerThermal(float mass, float power) {
			props.put(KEY_THERMAL, new Thermal(mass, power, TEMP_MAX_DEFAULT, OVERHEAT_DEFAULT));
			return this;
		}
		public PropDevice registerThermal(float mass, float power, float temp, float overheat) {
			props.put(KEY_THERMAL, new Thermal(mass, power, temp, overheat));
			return this;
		}
		public PropDevice registerThermal(String name, float mass, float power) {
			props.put(KEY_THERMAL + "_" + name, new Thermal(mass, power, TEMP_MAX_DEFAULT, OVERHEAT_DEFAULT));
			return this;
		}
		public PropDevice registerThermal(String name, float mass, float power, float temp, float overheat) {
			props.put(KEY_THERMAL + "_" + name, new Thermal(mass, power, temp, overheat));
			return this;
		}
		public PropDevice registerBattery(float max, float initial, float voltMin,
				float voltMax, float resMin, float resMax, float resCurve) {
			props.put(KEY_BATTERY_SPEC, new BatterySpec(max, initial, voltMin, voltMax, resMin, resMax, resCurve));
			return this;
		}
		public PropDevice registerBattery(String name, float max, float initial, float voltMin,
				float voltMax, float resMin, float resMax, float resCurve) {
			props.put(KEY_BATTERY_SPEC + "_" + name,
					new BatterySpec(max, initial, voltMin, voltMax, resMin, resMax, resCurve));
			return this;
		}
		public PropDevice registerFloat(String name, float value) {
			props.put(name, new FloatProp(value));
			return this;
		}
		public PropDevice registerFloat(String name, float value, float min, float max) {
			props.put(name, new FloatProp(value, min, max));
			return this;
		}
		public PropDevice registerFloat(String name, float value, String comment) {
			FloatProp custom = new FloatProp(value);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public PropDevice registerFloat(String name, float value, float min, float max, String comment) {
			FloatProp custom = new FloatProp(value, min, max);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public PropDevice registerInt(String name, int value) {
			props.put(name, new IntProp(value));
			return this;
		}
		public PropDevice registerInt(String name, int value, String comment) {
			IntProp custom = new IntProp(value);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}
		public PropDevice registerInt(String name, int value, int min, int max) {
			IntProp custom = new IntProp(value, min, max);
			props.put(name, custom);
			return this;
		}
		public PropDevice registerInt(String name, int value, int min, int max, String comment) {
			IntProp custom = new IntProp(value, min, max);
			custom.comment = comment;
			props.put(name, custom);
			return this;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			boolean notFirst = false;
			sb.append("PropDevice[");
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

	public static abstract class ASubProp extends Prop {
		private Consumer<ASubProp> loadCallback = null;
		private String id;
		private Builder builder;
		public abstract Class<?> getType();

		@Override
		public void onLoad() {
			if (loadCallback != null)
				loadCallback.accept(this);
		}
		protected void setup(String id, Builder builder) {
			this.id = id;
			this.builder = builder;
		}
		public Prop complete() {
			builder.group(id);
			register(id, builder);
			return this;
		}
		public Prop complete(Consumer<ASubProp> loadCallback) {
			this.loadCallback = loadCallback;
			complete();
			return this;
		}
		public Prop onLoad(Consumer<ASubProp> loadCallback) {
			this.loadCallback = loadCallback;
			return this;
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
		public <T extends Object> ConfigArray<T> array(Supplier<List<? extends T>> defaultSupplier, T defaultItem, String name, String... comment) {
			return new ConfigArray<>(name, defaultSupplier, defaultItem, comment);
		}
		public void group(String name, String... comment) {
			new ConfigGroup(name, depth + 1, comment);
		}
		public void clearGroup() {
			new ConfigGroupClear(1);
		}
	}

	public class ConfigArray<T extends Object> extends CValue<List<? extends T>, ConfigValue<List<? extends T>>> {
		public ConfigArray(String name, Supplier<List<? extends T>> defaultSupplier, T defaultItem, String... comment) {
			super(name, builder -> builder.<T>defineListAllowEmpty(
				name, defaultSupplier,
				() -> defaultItem, (Object object) -> true
			), comment);
		}
	}

	public class ConfigGroupClear  extends CValue<Boolean, BooleanValue> {
		private final int groupDepth;

		public ConfigGroupClear(int depth) {
			super(null, builder -> null);
			groupDepth = depth;
		}

		@Override
		public void register(ModConfigSpec.Builder builder) {
			if (depth > groupDepth)
				builder.pop(depth - groupDepth);
			depth = groupDepth;
		}
	}
}
