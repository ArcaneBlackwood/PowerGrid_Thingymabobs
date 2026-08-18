package com.feb.moregrid.component;

import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public abstract class ABuzzerComponent extends OrientableComponent {
    public ABuzzerComponent(ComponentFootprint footprint) {
        super(footprint);
    }
	public boolean hasAudioSource = false;
	abstract public float getVolume(PlacedComponent placed);
	abstract public float getPitch(PlacedComponent placed);
}
