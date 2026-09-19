package dev.thingymabobs.component.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

public abstract class VariantComponent extends Component {
	public static final IntProperty VARIANT = new IntProperty(
		Thingymabobs.MOD_ID, "variant", 0, 0, 128).hidden().cast();

	private final Variant[] variants;

	public VariantComponent(Variant... variants) {
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
		if(placed == null) return null;
		int variant = placed.get(VARIANT);
		if (variant >= variants.length) {
			variant = variants.length-1;
			placed.set(VARIANT, variant);
		}
		return variants[variant].footprint;
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		if(placed == null) return null;
		int variant = placed.get(VARIANT);
		if (variant >= variants.length) {
			variant = variants.length-1;
			placed.set(VARIANT, variant);
		}
		var id = ComponentRegistry.getId(this);
		String suffix = variants[variant].suffix;
		return suffix == null ? id : id.withSuffix(suffix);
	}
	public String getVariantSuffix(@NotNull PlacedComponent placed) {
		return variants[placed.get(VARIANT)].suffix;
	}

	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		ObjectOpenHashSet<String> suffixes = new ObjectOpenHashSet<>(variants.length);
		for (Variant v : variants)
			suffixes.add(v.suffix);
		var id = ComponentRegistry.getId(this);
		List<ResourceLocation> models = new ArrayList<>(suffixes.size());
		for (String suffix : suffixes)
			models.add(suffix == null ? id : id.withSuffix(suffix));
		return models;
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

	public static class Variant {
		public final ComponentFootprint footprint;
		public final String suffix;
		public Variant(ComponentFootprint footprint, String suffix) {
			this.footprint = footprint;
			this.suffix = suffix;
		}
		public Variant(ComponentFootprint footprint) {
			this.footprint = footprint;
			this.suffix = null;
		}
	}
}