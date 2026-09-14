package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.utility.Unit;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;

public class ShuntComponent extends OrientableComponent {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			6,3, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(1, 1, 0)
		.addPad(4, 1, 1)
		.withItem().withOutline().build();

	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		RESISTANCE.markDirty();
		POWER.markDirty();
	}

	public static final DynamicFloatProperty RESISTANCE = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "resistor_value", () -> CONFIG.getFloat("resistance")).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());

	public ShuntComponent() {
		super(FOOTPRINT);
	}

	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(RESISTANCE, POWER);
	}

	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));
		CONFIG.getThermal().apply(thermals)
			.addHeatSource(wire);
	}
}
