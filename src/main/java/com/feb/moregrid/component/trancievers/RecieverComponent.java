package com.feb.moregrid.component.trancievers;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import com.feb.moregrid.registry.ModModels;
import com.google.common.collect.ImmutableCollection;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RecieverComponent extends ATrancieverComponent {
	public RecieverComponent() {
		super();
	}
    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(PROP_RECIEVE_RESISTANCE_MIN, PROP_RECIEVE_RESISTANCE_MAX);
    }
	@Override
	protected boolean isTransmitter() {
		return false;
	}

	
	@Override
	protected void updateState(PlacedComponent placed, ServerState state, int currentSignal) {
		state.signalWire.setResistance(getSignalResistance(placed, currentSignal));
	}
	@Override
	protected float getSignalResistance(PlacedComponent placed, int signalValue) {
		return Mth.lerp(signalValue / 15f, placed.get(PROP_RECIEVE_RESISTANCE_MAX), placed.get(PROP_RECIEVE_RESISTANCE_MIN));
	}


	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return ModModels.RECV_BASE;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.RECV_BASE);
	}
	@Override
	protected PartialModel getRenderModel(PlacedComponent placed) {
		return ModModels.RECV_ANTENNA;
	}
}
