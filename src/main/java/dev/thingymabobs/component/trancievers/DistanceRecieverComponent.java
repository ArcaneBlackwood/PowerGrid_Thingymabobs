package dev.thingymabobs.component.trancievers;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.mixin.LinkBehaviourExt;
import dev.thingymabobs.registry.ModModels;
import com.google.common.collect.ImmutableCollection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DistanceRecieverComponent extends ATrancieverComponent {
	public static final float MAX_DIST = 128;
    public static final FloatProperty PROP_RECIEVE_DISTANCE =
		new FloatProperty(Thingymabobs.MOD_ID, "tranciever.recieve_resistance_max", 32, 0, MAX_DIST);


	public DistanceRecieverComponent() {
		super();
	}
    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(PROP_RECIEVE_RESISTANCE_MIN, PROP_RECIEVE_RESISTANCE_MAX, PROP_RECIEVE_DISTANCE);
    }
	@Override
	protected boolean isTransmitter() {
		return false;
	}
	@Override
	protected boolean setupLink(PlacedComponent placed, State state) {
		boolean result = super.setupLink(placed, state);
		if (placed.customData == null) return result;
		State state2 = (State)placed.customData;
		if(state2.link == null || state2.link.link == null) return true;
		((LinkBehaviourExt)state2.link.link).setTransformer((level, self, other) -> {
			float distanceMax = placed.get(PROP_RECIEVE_DISTANCE);
			float distance = (float)self.getLocation().getCenter().subtract(other.getLocation().getCenter()).length();
			return Math.round(level * Mth.clamp(distance / distanceMax, 0, 1));
		});
		return result;
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
