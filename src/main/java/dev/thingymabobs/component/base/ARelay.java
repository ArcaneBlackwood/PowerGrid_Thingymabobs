package dev.thingymabobs.component.base;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.ARelay.State.Switch;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.util.CombinedElectricWire;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.MirrorableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.special.RelaySwitchWire;
import org.patryk3211.powergrid.utility.Unit;

public abstract class ARelay extends MirrorableComponent {
	public static final String CONFIG_THRESHOLD = "threshold_voltage";
	public static final String CONFIG_COIL= "coil";
	public static final String CONFIG_SWITCH = "switch";
	
	protected CProperties.Prop CONFIG = null;
	public void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		if (THRESHOLD_VOLTAGE == null)
			throw new IllegalStateException("Parameters not initialized.  Make sure to run initializeProperties in your addProperties.");
		THRESHOLD_VOLTAGE.markDirty();
		CURRENT.markDirty();
	}


	public DynamicFloatProperty THRESHOLD_VOLTAGE;
	public CalculatedProperty<Float> THRESHOLD_CURRENT;
	public LazyConstantProperty CURRENT;

	public static final BooleanProperty ENERGIZED = new BooleanProperty(Thingymabobs.MOD_ID, "energized").hidden().cast();
	public static final BooleanProperty POLARIZED = new BooleanProperty(PowerGrid.MOD_ID, "relay_polarized");
	public static final BooleanProperty NORMALLY_CLOSED = new BooleanProperty(Thingymabobs.MOD_ID, "relay.normally_closed");
	public static final BooleanProperty NORMALLY_CLOSEDA = new BooleanProperty(Thingymabobs.MOD_ID, "relay.normally_closed_a");
	public static final BooleanProperty NORMALLY_CLOSEDB = new BooleanProperty(Thingymabobs.MOD_ID, "relay.normally_closed_b");

	public ARelay(ComponentFootprint footprint) {
		super(footprint);
		
	}
	protected void initializeProperties() {
		THRESHOLD_VOLTAGE = new DynamicFloatProperty(
			PowerGrid.MOD_ID, "relay_threshold", () -> CONFIG.getFloat(CONFIG_THRESHOLD)).useMetrics();
		THRESHOLD_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "relay_current",
				c -> CONFIG.getThermal(CONFIG_COIL).getPower() / c.get(THRESHOLD_VOLTAGE),
				v -> Unit.CURRENT.formatWithPrefixes(v).string());
		CURRENT = new LazyConstantProperty(
			PowerGrid.MOD_ID, "current",
			() -> Unit.CURRENT.formatWithPrefixes(
				Mth.sqrt(CONFIG.getThermal(CONFIG_SWITCH).getPower() / CONFIG.getResistance(CONFIG_SWITCH).get())
			).string());
	}


	protected abstract void createSwitches(PlacedComponent placed, ComponentCircuitBuilder builder, ThermalBuilder.IEmitter thermals, SwitchBuilder switches);
	public abstract int getCoilCount(PlacedComponent placed);
	protected abstract void playSound(boolean energized, PlacedComponent placed);

	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		State state = new State();
		placed.customData = state;

		// ## Calculate constants
		final float voltage = placed.get(THRESHOLD_VOLTAGE);
		final float onCurrent = CONFIG.getThermal(CONFIG_COIL).getPower() / voltage;
		final float offCurrent = onCurrent * ModdedConfigs.server().electricity.holdingCurrentPercent.getF();
		final float resistance = voltage / onCurrent;
		final var switchResistance = CONFIG.getResistance(CONFIG_SWITCH).get();
		final boolean energized = placed.get(ENERGIZED);
		final boolean polarized = placed.get(POLARIZED);

		// ## Create coil wires
		ElectricWire combinedWire;
		int coilCount = getCoilCount(placed);
		if (coilCount < 1)
			throw new IllegalStateException("Relay requires atleast one coil from getCoilCount()");
		List<AbstractElectricWire> coils = new ArrayList<>(coilCount);
		if (coilCount == 1) {
			combinedWire = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));
			coils.add(combinedWire);
		} else {
			for (int i = 0, j = 0; i < coilCount; i++) {
				coils.add(builder.connect(resistance, builder.terminalNode(j++), builder.terminalNode(j++)));
			}
			combinedWire = new CombinedElectricWire(coils);
		}
		state.coil = combinedWire;

		// ## Create relay switches
		createSwitches(placed, builder, thermals, new SwitchBuilder() {
			@Override
			public void relay(int terminal0, int terminal1, boolean normallyClosed) {
				if (state.relay != null)
					throw new IllegalStateException("Relay already has a defined relay switch wire! "+state.relay);
				state.relay = new State.Relay(normallyClosed, new RelaySwitchWire(
					switchResistance, builder.terminalNode(terminal0), builder.terminalNode(terminal1), 
					energized != normallyClosed, combinedWire, onCurrent, offCurrent, normallyClosed, polarized));
			}
			@Override
			public void wire(int terminal0, int terminal1, boolean normallyClosed) {
				state.switches.add(new State.Switch(normallyClosed, builder.connectSwitch(
					switchResistance, builder.terminalNode(terminal0), builder.terminalNode(terminal1), energized != normallyClosed)));
			}
		});

		// ## Add relay switches to this & thermals
		if (state.relay == null)
			throw new IllegalStateException("Relay requires one relay wire in createSwitches()");
		ThermalBuilder thermalSwitch = CONFIG.getThermal(CONFIG_SWITCH).apply(thermals)
			.addHeatSource(state.relay.wire);
		builder.add(state.relay.wire);
		placed.add(state.relay.wire);

		for (Switch sw : state.switches) {
			builder.add(sw.wire);
			placed.add(sw.wire);
			thermalSwitch.addHeatSource(sw.wire);
		}

		ThermalBuilder thermalCoil = CONFIG.getThermal(CONFIG_COIL).apply(thermals, 1.5f);
		for (AbstractElectricWire coil : coils) {
			thermalCoil.addHeatSource(coil);
		}

	}


	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) {
			return false;
		}
		if(placed.wires.size() != state.switches.size()+1) {
			return false;
		}

		if(state.relay.wire.wasSwitched()) {
			boolean energized = state.relay.wire.getState() != state.relay.normallyClosed;
            Level world = placed.getWorld();
            if (!world.isClientSide) {
                playSound(energized, placed);
            }
			placed.set(ENERGIZED, energized);
			for (State.Switch sw : state.switches) {
				sw.wire.setState(energized != sw.normallyClosed);
			}
		}

		return true;
	}

	protected class State {
		/**
		 * Nested custom data
		 */
		AbstractElectricWire coil;
		Object data;
		Relay relay = null;
		List<Switch> switches = new ArrayList<>();
		public record Switch(boolean normallyClosed, SwitchedWire wire) {}
		public record Relay(boolean normallyClosed, RelaySwitchWire wire) {}
	}
	protected interface SwitchBuilder {
		public void relay(int terminal0, int terminal1, boolean normallyClosed);
		public void wire(int terminal0, int terminal1, boolean normallyClosed);
	}
}
