package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.utility.Unit;

public class SmallDiodeComponent extends VerticallyOrientableComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3,1, "component." + MoreGrid.MOD_ID + ".small_diode", null)
            .addPad(0, 0, 0, "Cathode", "C")
            .addPad(2, 0, 1, "Anode", "A")
            .withItem().withOutline().build();
    private static final ComponentFootprint VERTICAL_FOOTPRINT = new ComponentFootprint.Builder(
				2,1, "component." + MoreGrid.MOD_ID + ".small_diode", null)
            .addPad(0, 0, 0, "Cathode", "C")
            .addPad(1, 0, 1, "Anode", "A")
            .withItem().withOutline().build();

    public static final ConstantProperty BREAKDOWN_VOLTAGE = new ConstantProperty(MoreGrid.MOD_ID, "diode_vb", Unit.VOLTAGE.format(1000));

    public SmallDiodeComponent() {
        super(FOOTPRINT, VERTICAL_FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(BREAKDOWN_VOLTAGE, power(25));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        // Variation of 1N4007 diode model
        var pnJunctionWire = new PNJunctionWire(5.47e-9, 0.075f,22, 1.783,
                500, 1e-6,
                builder.terminalNode(1), builder.terminalNode(0));
        builder.add(pnJunctionWire);
        thermals.builder()
                .setMaxPower(1, 175)
                .setOverheatTemperature(175)
                .setThermalMass(0.05f)
                .withTemperatureCallback(pnJunctionWire::setTemperatureCelsius)
                .addHeatSource(pnJunctionWire);
    }
}