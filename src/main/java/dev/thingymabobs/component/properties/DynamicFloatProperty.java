package dev.thingymabobs.component.properties;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import com.mojang.datafixers.util.Pair;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.FloatProp;
import dev.thingymabobs.util.MetricScale;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;

public class DynamicFloatProperty extends FloatProperty {
	private final Supplier<Float> defaultValueSupplier, minSupplier, maxSupplier;
	private Float defaultValue, min, max;
	protected boolean useMetrics = false;

	public DynamicFloatProperty(String namespace, String name, Supplier<Float> defaultValue, Supplier<Float> min, Supplier<Float> max) {
		super(namespace, name, 0, 0, 0);
		this.defaultValueSupplier = defaultValue;
		this.minSupplier = min;
		this.maxSupplier = max;
	}
	public DynamicFloatProperty(String namespace, String name, Supplier<FloatProp> prop) {
		super(namespace, name, 0, 0, 0);
		this.defaultValueSupplier = () -> prop.get().get();
		this.minSupplier = () -> prop.get().min();
		this.maxSupplier = () -> prop.get().max();
	}
	public DynamicFloatProperty useMetrics() {
		useMetrics = true;
		return this;
	}
	public void markDirty() {
		defaultValue = null;
		min = null;
		max = null;
	}


	@Override
	public float limit(float value) {
		fetchCache();
		if (value < min) {
			return min;
		} else {
			return value > max ? max : value;
		}
	}
	@Override
	public Float parse(String str) throws NumberFormatException {
		if (!useMetrics) return this.limit(Float.parseFloat(str));
		Pair<Double, MetricScale> parsed = MetricScale.parse(str);
		return this.limit(parsed.getFirst().floatValue());
	}
	@Override
	public String toString(Float value) {
		String string;
		if (useMetrics) {
			MetricScale scale = MetricScale.getScaleOf(value);
			string = MetricScale.formatDefault(value / scale.getScale(), scale);
		} else
			string = Float.toString(value);
		if (string.length() > 20) {
			Thingymabobs.LOGGER.error("Failed to convert property to string. ", 
				new IllegalStateException("Value too long: '"+string+"'"));
			return "0";
		}
		return string;
	}
	@Override
	public Float defaultValue() {
		fetchCache();
		return this.defaultValue;
	}

	@Override
	public Float read(HolderLookup.Provider registries, @Nullable Tag element) {
		if (element == null || element.getId() != 5) {
			return defaultValue();
		} else {
			float value = ((FloatTag)element).getAsFloat();
			return this.limit(value);
		}
	}
	@Override
	public Tag write(HolderLookup.Provider registries, Float value) {
		return FloatTag.valueOf(value);
	}


	protected void fetchCache() {
		if (defaultValue == null) defaultValue = defaultValueSupplier.get();
		if (min == null) min = minSupplier.get();
		if (max == null) max = maxSupplier.get();
	}
}
