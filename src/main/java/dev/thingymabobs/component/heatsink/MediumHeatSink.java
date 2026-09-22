package dev.thingymabobs.component.heatsink;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.registry.ModModels;
import net.minecraft.resources.ResourceLocation;

public class MediumHeatSink extends AHeatSink {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3, 8, null, Thingymabobs.MOD_ID + ".heatsink")
		.addPadSharedText(1, 1, 0, "0")
		.addPadSharedText(1, 6, 0, "1")
		.withItem().withOutline().withArrow(Orientation.RIGHT).build();

	public MediumHeatSink() {
		super(FOOTPRINT);
	}
	
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return ModModels.HEAT_MEDIUM;
		boolean externalConnected = state.isExternal && placed.get(CONNECTION) == Connection.EXTERNAL_ACTIVE.ordinal();
		return externalConnected ? ModModels.HEAT_MEDIUM_EXT : ModModels.HEAT_MEDIUM;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.HEAT_MEDIUM, ModModels.HEAT_MEDIUM_EXT);
	}

	@Override
	public int getHeight() {
		return 4;
	}
}
