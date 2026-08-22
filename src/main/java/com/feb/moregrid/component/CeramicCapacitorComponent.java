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
import org.patryk3211.powergrid.electricity.sim.special.CRSeriesWire;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;

public class CeramicCapacitorComponent extends OrientableComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3, 1, "component." + MoreGrid.MOD_ID + ".ceramic_capacitor", null)
			.addPad(0, 0, 0)
			.addPad(2, 0, 1)
			.withItem().withOutline().build();

    public static final FloatProperty CAPACITANCE = new FloatProperty(MoreGrid.MOD_ID, "capacitor_value", 0.1f, 1e-8f, 10.0f);
    private static final ChargeProperty CHARGE = new ChargeProperty(MoreGrid.MOD_ID, "charge");

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
        var capacitorWire = new CRSeriesWire(placed.get(CAPACITANCE) / 1000, 0.01f, builder.terminalNode(0), builder.terminalNode(1));
        capacitorWire.setVoltage(placed.get(CHARGE));
        builder.add(capacitorWire);
        placed.add(capacitorWire);
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
