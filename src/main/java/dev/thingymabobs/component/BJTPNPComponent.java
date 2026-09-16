package dev.thingymabobs.component;

import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.BJTWire;

public class BJTPNPComponent extends OrientableComponent {
	protected static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3,1, Thingymabobs.MOD_ID + ".component.bjt_pnp", null)
		.addPad(0, 0, 0, "Collector", "C")
		.addPad(1, 0, 1, "Base", "B")
		.addPad(2, 0, 2, "Emitter", "E")
		.withItem().withOutline().build();


	public BJTPNPComponent() {
		super(FOOTPRINT);
	}

	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(BJTNPNComponent.GAIN, BJTNPNComponent.POWER, BJTNPNComponent.RESISTANCE);
	}

	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var wire = new BJTWire(
			builder.terminalNode(0), // Collector
			builder.terminalNode(1), // Base
			builder.terminalNode(2), // Emitter
			5.47e-12, placed.get(BJTNPNComponent.GAIN),
			BJTNPNComponent.CONFIG.getResistance().get(), true
		);
		builder.add(wire);
		placed.add(wire);

		BJTNPNComponent.CONFIG.getThermal().apply(thermals)
			.addHeatSource(wire);
	}
}
