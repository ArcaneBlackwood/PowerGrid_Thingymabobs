package dev.thingymabobs.component.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;

public abstract class VariantComponent extends AVariantComponent {
	public static final IntProperty VARIANT = new IntProperty(
		Thingymabobs.MOD_ID, "variant", 0, 0, 128).hidden().cast();
	

	public VariantComponent(Variant... variants) {
		super(variants);
	}
	
	@Override
	public int getVariantIndex(@Nullable PlacedComponent placed) {
		if (placed == null) return 0;
		int index = placed.get(VARIANT);
		if (index < variants.length)
			return index;
		return 0;
	}

	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(VARIANT);
	}

	@Override
	public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
		int index = placed.get(VARIANT);
		if(counterClockwise) {
			if (index <= 0) index = variants.length;
			index--;
		} else {
			index++;
			if (index >= variants.length) index = 0;
		}
		placed.set(VARIANT, index);
		return true;
	}
}