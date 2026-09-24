package dev.thingymabobs.component.thermal;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import dev.thingymabobs.registry.ModModels;
import net.minecraft.resources.ResourceLocation;

public class SmallHeatSink extends AHeatSink {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			2, 3, null, null)
		.addPad(1, 1, 0)
		.withItem().withOutline().withArrow(Orientation.RIGHT).build();

	public SmallHeatSink() {
		super(FOOTPRINT);
	}
	
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return ModModels.HEAT_SMALL;
		boolean externalConnected = state.isExternal && placed.get(CONNECTION) == Connection.EXTERNAL_ACTIVE.ordinal();
		return externalConnected ? ModModels.HEAT_SMALL_EXT : ModModels.HEAT_SMALL;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.HEAT_SMALL, ModModels.HEAT_SMALL_EXT);
	}

	@Override
	public int getHeight() {
		return 2;
	}
}
