package dev.thingymabobs.component.base;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import net.minecraft.resources.ResourceLocation;

public abstract class VariantOrientableComponent extends OrientableComponent {
	public static final IntProperty VARIANT = new IntProperty(
		Thingymabobs.MOD_ID, "variant", 0, 0, 128).hidden().cast();

	private final Variant[] variants;

	public VariantOrientableComponent(Variant... variants) {
		super(null);
		this.variants = variants;
		if (variants.length < 1)
			throw new IllegalArgumentException("Must have atleast one variant.");
	}

	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(VARIANT);
	}

	@Override
	public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
		int variant = placed.get(VARIANT);
		if(placed != null && variant < variants.length) {
			return variants[variant].footprint.rotated(placed.get(ORIENTATION));
		}
		return variants[0].footprint.rotated(placed.get(ORIENTATION));
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		var id = ComponentRegistry.getId(this);
		String suffix = variants[placed.get(VARIANT)].suffix;
		return suffix == null ? id : id.withSuffix(suffix);
	}
	public String getVariantSuffix(@NotNull PlacedComponent placed) {
		return variants[placed.get(VARIANT)].suffix;
	}

	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		var id = ComponentRegistry.getId(this);
		return List.of(id, id.withSuffix("_vertical"));
	}

	@Override
	public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
		super.rotate(placed, counterClockwise);
		Orientation orient = placed.get(ORIENTATION);
		int index = placed.get(VARIANT);
		if(counterClockwise) {
			if (orient == Orientation.RIGHT) return true; //Only switch on full rotation
			index++;
			if (index >= variants.length) index = 0;
		} else {
			if (orient == Orientation.DOWN) return true; //Only switch on full rotation
			if (index == 0) index = variants.length;
			index--;
		}
		placed.set(VARIANT, index);
		return true;
	}

	public static record Variant(ComponentFootprint footprint, String suffix) { }
}