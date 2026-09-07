package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;

public class ShuntComponent extends OrientableComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				6,3, "component." + Thingymabobs.MOD_ID + ".small_resistor", null)
            .addPad(1, 1, 0)
            .addPad(4, 1, 1)
            .withItem().withOutline().build();

    public static final FloatProperty RESISTANCE = new FloatProperty(Thingymabobs.MOD_ID, "resistor_value", 0.01f, 0.0001f, 0.1f);

    public ShuntComponent() {
        super(FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RESISTANCE, power(50));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));
        thermals.builder()
                .setThermalMass(5f)
                .setDissipationFactor(100)
                .setOverheatTemperature(200f)
                .addHeatSource(wire);
    }
}
