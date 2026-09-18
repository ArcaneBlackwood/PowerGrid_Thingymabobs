package dev.thingymabobs.mixin;

import java.util.List;

import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;

public interface PlacedComponentExt {
	public List<ThermalUnit> getThermalUnits();
}
