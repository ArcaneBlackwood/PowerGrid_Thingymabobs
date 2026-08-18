package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;

public class SmallResistorComponent extends VerticallyOrientableComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3,1, "component." + MoreGrid.MOD_ID + ".small_resistor", null)
            .addPad(0, 0, 0)
            .addPad(2, 0, 1)
            .withItem().withOutline().build();
    private static final ComponentFootprint VERTICAL_FOOTPRINT = new ComponentFootprint.Builder(
				2,1, "component." + MoreGrid.MOD_ID + ".small_resistor", null)
            .addPad(0, 0, 0)
            .addPad(1, 0, 1)
            .withItem().withOutline().build();

    public static final FloatProperty RESISTANCE = new FloatProperty(MoreGrid.MOD_ID, "resistor_value", 10000f, 10f, 100_000_000f);

    public SmallResistorComponent() {
        super(FOOTPRINT, VERTICAL_FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RESISTANCE, power(1));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));
        thermals.builder()
                .setThermalMass(0.05f)
                .setMaxPower(1, 125f)
                .addHeatSource(wire);
    }
}
