package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.config.properties.CProperties;

import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.CRSeriesWire;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;

public class CeramicCapacitorComponent extends OrientableComponent {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3, 1, Thingymabobs.MOD_ID + ".component.ceramic_capacitor", null)
		.addPad(0, 0, 0)
		.addPad(2, 0, 1)
		.withItem().withOutline().build();

	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		CAPACITANCE.markDirty();
	}

	public static final DynamicFloatProperty CAPACITANCE = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "capacitor_value", () -> CONFIG.getFloat("capacitance")).useMetrics();
	private static final ChargeProperty CHARGE = new ChargeProperty(Thingymabobs.MOD_ID, "charge");


	public CeramicCapacitorComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(CAPACITANCE, CHARGE);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var capacitorWire = new CRSeriesWire(placed.get(CAPACITANCE),
			CONFIG.getResistance().get(), builder.terminalNode(0), builder.terminalNode(1));
		capacitorWire.setVoltage(placed.get(CHARGE));
		builder.add(capacitorWire);
		placed.add(capacitorWire);
		CONFIG.getThermal().apply(thermals)
			.addHeatSource(capacitorWire);
	}


	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if(!placed.wires.isEmpty()) {
			// Make charge persistent
			placed.set(CHARGE, (float) ((CRSeriesWire) placed.wires.get(0)).capacitorVoltage());
		}
		return true;
	}
	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		if(placed.wires.isEmpty())
			return;
		var wire = (CRSeriesWire) placed.wires.get(0);
		wire.setVoltage(placed.get(CHARGE));
	}


	private static class ChargeProperty extends FloatProperty {
		public ChargeProperty(String namespace, String name) {
			super(namespace, name, 0, 0, 0);
		}

		@Override
		public Float parse(String str) throws NumberFormatException {
			// Not allowed
			return 0.0f;
		}

		@Override
		public boolean isHidden() {
			return true;
		}

		@Override
		public boolean isUnsafe() {
			return true;
		}

		@Override
		protected float limit(float value) {
			return value;
		}
	}
}
