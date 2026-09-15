package dev.thingymabobs.component;

import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.BJTWire;
import org.patryk3211.powergrid.utility.Unit;

public class BJTNPNComponent extends OrientableComponent {
	protected static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3,1, Thingymabobs.MOD_ID + ".component.bjt_npn", null)
		.addPad(0, 0, 0, "Collector", "C")
		.addPad(1, 0, 1, "Base", "B")
		.addPad(2, 0, 2, "Emitter", "E")
		.withItem().withOutline().build();


	public static final String CONFIG_GAIN = "gain";
	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		GAIN.markDirty();
	}
 
	//resistance 0.1
	//public static final FloatProperty GAIN = new FloatProperty(PowerGrid.MOD_ID, "bjt_gain", 20, 5, 100);
	//.setThermalMass(0.01f).setMaxPower(20, 125);
	public static final DynamicFloatProperty GAIN = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "bjt.gain", () -> CONFIG.getFloat(CONFIG_GAIN)).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());

    public BJTNPNComponent() {
        super(FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(GAIN, POWER, RESISTANCE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new BJTWire(
			builder.terminalNode(0), // Collector
			builder.terminalNode(1), // Base
			builder.terminalNode(2), // Emitter
			5.47e-12, placed.get(GAIN),
			CONFIG.getResistance().get(), false
        );
        builder.add(wire);
        placed.add(wire);

		CONFIG.getThermal().apply(thermals)
			.addHeatSource(wire);
    }
}
