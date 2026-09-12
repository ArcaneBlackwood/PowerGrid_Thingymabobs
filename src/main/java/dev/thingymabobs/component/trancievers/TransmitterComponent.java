package dev.thingymabobs.component.trancievers;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import dev.thingymabobs.registry.ModModels;
import com.google.common.collect.ImmutableCollection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class TransmitterComponent extends ATrancieverComponent {
	public TransmitterComponent() {
		super();
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		properties.add(LABEL, PROP_TRANSMIT_CURRENT_FULL, PROP_TRANSMIT_RESISTANCE);
		super.addProperties(properties);
	}
	@Override
	protected boolean isTransmitter() {
		return true;
	}


	@Override
	protected void updateState(PlacedComponent placed, ServerState state, float currentSignal) {
		float transmitValue = Math.abs((float)state.signalWire.current() / TRANSMIT_CURRENT_FULL * 16f);
		setTransmission(state, transmitValue);
	}
	@Override
	protected float getSignalResistance(PlacedComponent placed, float signalValue) {
		return TRANSMIT_RESISTANCE;
	}


	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return ModModels.TRANS_BASE;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.TRANS_BASE);
	}
	@Override
	protected PartialModel getRenderModel(PlacedComponent placed) {
		return ModModels.TRANS_ANTENNA;
	}
}
