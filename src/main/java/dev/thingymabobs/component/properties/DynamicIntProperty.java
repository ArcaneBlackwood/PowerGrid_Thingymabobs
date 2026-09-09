package dev.thingymabobs.component.properties;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import dev.thingymabobs.config.properties.IntProp;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

public class DynamicIntProperty extends IntProperty {
	private final Supplier<Integer> defaultValueSupplier, minSupplier, maxSupplier;
	private Integer defaultValue, min, max;

	public DynamicIntProperty(String namespace, String name, Supplier<Integer> defaultValue, Supplier<Integer> min, Supplier<Integer> max) {
		super(namespace, name, 0, 0, 0);
		this.defaultValueSupplier = defaultValue;
		this.minSupplier = min;
		this.maxSupplier = max;
	}
	public DynamicIntProperty(String namespace, String name, Supplier<IntProp> prop) {
		super(namespace, name, 0, 0, 0);
		this.defaultValueSupplier = () -> prop.get().get();
		this.minSupplier = () -> prop.get().min();
		this.maxSupplier = () -> prop.get().max();
	}

	public void markDirty() {
		defaultValue = null;
		min = null;
		max = null;
	}


	protected int limit(int value) {
		fetchCache();
		if (value < min) {
			return min;
		} else {
			return value > max ? max : value;
		}
	}
	@Override
	public Integer parse(String str) throws NumberFormatException {
		int value = Integer.parseInt(str);
		return this.limit(value);
	}
	@Override
	public String toString(Integer value) {
		return Integer.toString(value);
	}
	@Override
	public Integer defaultValue() {
		fetchCache();
		return this.defaultValue;
	}
	@Override
	public Integer read(HolderLookup.Provider registries, @Nullable Tag element) {
		if (element == null || element.getId() != 5) {
			return defaultValue();
		} else {
			int value = ((IntTag)element).getAsInt();
			return this.limit(value);
		}
	}
	@Override
	public Tag write(HolderLookup.Provider registries, Integer value) {
		return IntTag.valueOf(value);
	}


	protected void fetchCache() {
		if (defaultValue == null) defaultValue = defaultValueSupplier.get();
		if (min == null) min = minSupplier.get();
		if (max == null) max = maxSupplier.get();
	}
}
