package dev.thingymabobs.component.base;

import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public abstract class ABuzzerComponent extends OrientableComponent implements IGoggleLabel {
	public static final String CONFIG_SOUND_PITCH = "sound_pitch";
	public static final String CONFIG_PITCH = "pitch";
	public static final String CONFIG_VOLUME0 = "volume_min_power";
	public static final String CONFIG_VOLUME1 = "volume_max_power";
	public ABuzzerComponent(ComponentFootprint footprint) {
		super(footprint);
	}
	public boolean hasAudioSource = false;

	protected static float SOUND_PITCH_INV;
	
	abstract public float getVolume(PlacedComponent placed);
	abstract public float getPitch(PlacedComponent placed);
}
