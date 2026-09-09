package dev.thingymabobs.component.properties;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public class LazyConstantProperty extends ComponentProperty<Byte> {
	private String cached = null;
	private final Supplier<String> callback;

	public LazyConstantProperty(String namespace, String name, Supplier<String> callback) {
		super(namespace, name);
		this.callback = callback;
	}

	@Override 
	public String toString(Byte value) {
		if (cached == null)
			cached = callback.get();
		return cached;
	}
	public void markDirty() {
		cached = null;
	}


	@Override 
	public Byte parse(String value) {
		return null;
	}
	@Override 
	public Byte read(HolderLookup.Provider registries, @Nullable Tag element) {
		return null;
	}
	@Override 
	public Tag write(HolderLookup.Provider registries, Byte value) {
		return null;
	}
	@Override 
	public Byte defaultValue() {
		return null;
	}
}