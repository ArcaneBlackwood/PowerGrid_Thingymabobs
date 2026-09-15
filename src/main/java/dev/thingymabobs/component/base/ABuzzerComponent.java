package dev.thingymabobs.component.base;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import dev.thingymabobs.client.BuzzerSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class ABuzzerComponent extends OrientableComponent implements IGoggleLabel {
	public static final String CONFIG_SOUND_PITCH = "sound_pitch";
	public static final String CONFIG_PITCH = "pitch";
	public static final String CONFIG_VOLUME0 = "volume_min_power";
	public static final String CONFIG_VOLUME1 = "volume_max_power";
	public ABuzzerComponent(ComponentFootprint footprint) {
		super(footprint);
	}

	protected static float SOUND_PITCH_INV;
	
	abstract public float getVolume(PlacedComponent placed);
	abstract public float getPitch(PlacedComponent placed);

	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		super.stateUpdated(placed);
	}
	@OnlyIn(Dist.CLIENT)
	protected void tickClient(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof AState state)) return;
		state.volume = getVolume(placed);
		if (state.soundInstance != null) {
			state.soundInstance.forceStop();
			state.soundInstance = null;
		}
	}

	public static class AState {
		public ElectricWire buzzerWire;
		public float volume = 0, pitch = 0;
		@OnlyIn(Dist.CLIENT)
		public BuzzerSoundInstance soundInstance = null;
	}
}
