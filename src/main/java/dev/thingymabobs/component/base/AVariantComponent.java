package dev.thingymabobs.component.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

public abstract class AVariantComponent extends Component {
	protected final Variant[] variants;

	public AVariantComponent(Variant... variants) {
		super(null);
		this.variants = variants;
		if (variants.length < 1)
			throw new IllegalArgumentException("Must have atleast one variant.");
	}

	public abstract int getVariantIndex(@Nullable PlacedComponent placed);

	@Override
	public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
		int variant = getVariantIndex(placed);
		return variants[variant].footprint;
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		var id = ComponentRegistry.getId(this);
		String suffix = variants[getVariantIndex(placed)].suffix;
		return suffix == null ? id : id.withSuffix(suffix);
	}
	public String getVariantSuffix(@NotNull PlacedComponent placed) {
		return variants[getVariantIndex(placed)].suffix;
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