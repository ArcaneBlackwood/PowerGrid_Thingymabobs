package dev.thingymabobs.mixin.unit;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.spongepowered.asm.mixin.Mixin;
import dev.thingymabobs.mixin.PlacedComponentExt;

@Mixin(PlacedComponent.class)
public abstract class PlacedComponentMixin implements PlacedComponentExt {
	private List<ThermalUnit> thermals = new ArrayList<>();
	@Override
	public List<ThermalUnit> getThermalUnits() {
		return thermals;
	}
}