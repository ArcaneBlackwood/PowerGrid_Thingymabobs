package dev.thingymabobs.component;

import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.util.CombinedElectricWire;
import net.minecraft.util.Mth;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.MirrorableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.special.RelaySwitchWire;
import org.patryk3211.powergrid.utility.Unit;

public class DuelCoilRelay extends MirrorableComponent {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			4, 3, null, Thingymabobs.MOD_ID + ".component.relay")
		.addPadSharedText(0, 0, 0, "ca", "ca.short")
		.addPadSharedText(0, 2, 1, "ca", "ca.short")
		.addPadSharedText(1, 0, 2, "cb", "cb.short")
		.addPadSharedText(1, 2, 3, "cb", "cb.short")
		.addPadSharedText(3, 0, 4, "nc", "nc.short")
		.addPadSharedText(3, 1, 5, "cc", "cc.short")
		.addPadSharedText(3, 2, 6, "no", "no.short")
		.withItem().withOutline().build();
		
	public static final String CONFIG_THRESHOLD = "threshold_voltage";
	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		THRESHOLD_VOLTAGE.markDirty();
	}


    public static final DynamicFloatProperty THRESHOLD_VOLTAGE = new DynamicFloatProperty(
		PowerGrid.MOD_ID, "relay_threshold", () -> CONFIG.getFloat(CONFIG_THRESHOLD)).useMetrics();
    public static final CalculatedProperty<Float> THRESHOLD_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "relay_current",
            c -> CONFIG.getThermal("coil").getPower() / c.get(THRESHOLD_VOLTAGE),
            v -> Unit.CURRENT.formatWithPrefixes(v).string());
    public static final BooleanProperty STATE = new BooleanProperty(PowerGrid.MOD_ID, "relay_state").hidden().cast();
    public static final BooleanProperty POLARIZED = new BooleanProperty(PowerGrid.MOD_ID, "relay_polarized");
	public static final LazyConstantProperty CURRENT = new LazyConstantProperty(
		PowerGrid.MOD_ID, "current",
		() -> Unit.CURRENT.formatWithPrefixes(
            Mth.sqrt(CONFIG.getThermal("switch").getPower() / CONFIG.getResistance("switch").get())
        ).string());

    public DuelCoilRelay() {
        super(FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(THRESHOLD_VOLTAGE, STATE, THRESHOLD_CURRENT, POLARIZED, CURRENT);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        // Target power is 1.2 W
		float voltage = placed.get(THRESHOLD_VOLTAGE);
        var onCurrent = CONFIG.getThermal("coil").getPower() / voltage;
        var offCurrent = onCurrent * ModdedConfigs.server().electricity.holdingCurrentPercent.getF();

        var resistance = voltage / onCurrent;
        var coilWireA = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));
        var coilWireB = builder.connect(resistance, builder.terminalNode(2), builder.terminalNode(3));
        ElectricWire combinedWire = new CombinedElectricWire(List.of(coilWireA, coilWireB));

        final var switchResistance = CONFIG.getResistance("switch").get();
        var state = placed.get(STATE);
        var polarized = placed.get(POLARIZED);
        RelaySwitchWire wireSwitchNO = new RelaySwitchWire(
			switchResistance, builder.terminalNode(4), builder.terminalNode(5), 
			state, combinedWire, onCurrent, offCurrent, true, polarized);
        SwitchedWire wireSwitchNC = builder.connectSwitch(switchResistance, builder.terminalNode(6), builder.terminalNode(5), !state);

        builder.add(wireSwitchNO);
        builder.add(wireSwitchNC);
        placed.add(wireSwitchNO);
        placed.add(wireSwitchNC);

        CONFIG.getThermal("coil").apply(thermals)
                .addHeatSource(coilWireA).addHeatSource(coilWireB);
        CONFIG.getThermal("switch").apply(thermals)
                .addHeatSource(wireSwitchNO).addHeatSource(wireSwitchNC);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.size() != 2)
            return true;

        var NO = (RelaySwitchWire) placed.wires.get(0);
        if(NO.wasSwitched()) {
            boolean state = NO.getState();
			///TODO: Custom sound
            placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(
                    world, placed.getPos(),
                    0.75f,
                    state ? 1.9f : 2.0f
            ));
            placed.set(STATE, state);
            ((SwitchedWire)placed.wires.get(1)).setState(!state);
        }

        return true;
    }
}
