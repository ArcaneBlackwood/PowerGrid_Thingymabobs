package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.VariantOrientableComponent;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.PlacedComponentExt;
import dev.thingymabobs.util.TMath;
import dev.thingymabobs.util.ThermalElectricWire;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;

public class ThermistorComponent extends VariantOrientableComponent {
	private static final ComponentFootprint FOOTPRINT_FACE = new ComponentFootprint.Builder(
			2,1, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(1, 0, 1)
		.withItem().withOutline().withArrow(Orientation.UP).build();
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			2,1, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(1, 0, 1)
		.withItem().withOutline().build();

	public static final String CONFIG_BETA = "beta";
	protected static CProperties.Prop CONFIG = null;
	public static final float RESISTANCE_TEMP = 298.15f;
	protected static float BETA;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		BETA = prop.getFloat(CONFIG_BETA).get();
		POWER.markDirty();
	}

	public static final DynamicFloatProperty RESISTANCE = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "resistor_temp_value", () -> CONFIG.getFloat("resistance")).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());


	public ThermistorComponent() {
		super(new Variant(FOOTPRINT, ""), new Variant(FOOTPRINT_FACE, "_face"));
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(RESISTANCE, POWER);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));

		var therm = CONFIG.getThermal();
		therm.apply(thermals)
			.addHeatSource(wire);

		State state = new State();
		placed.customData = state;
		state.wire = wire;
		state.thermal = new ThermalElectricWire(
			ThermalBehaviour.dissipationFactor(therm.getPower(), therm.getTemp()));
	}

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (placed.isClient() || !(placed.customData instanceof State state)) return false;
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) return false;
		if (state.ambient < -900)
            state.ambient = ThermalBehaviour.getAmbientTemperature(board.getLevel(), placed.getPos());

		if (state.lazyTick-- == 0) {
			state.lazyTick = 20;
			state.monitoring.clear();
			Stream<PlacedComponent> search = board.getComponentsStream();
			search.forEach((other) -> {
				if (!effectsThermalReading(placed, other) || !(other instanceof PlacedComponentExt otherExt)) return;
				state.monitoring.addAll(otherExt.getThermalUnits());
			});
			Thingymabobs.LOGGER.info("Thermistor monitoring: "+state.monitoring);
		}

		if (state.monitoring.size() == 0) return true;
		double temp = 0;
		for (ThermalUnit thermal : state.monitoring) {
			temp += thermal.getTemperature();
		}
		temp /= state.monitoring.size();

		state.thermal.setTemperature(state.ambient, (float)temp);
		state.wire.setResistance(calcResistance(
			placed.get(RESISTANCE), (float)temp));
		
		return true;
	}

	public static double calcResistance(float resistance, float temp) {
		return resistance * TMath.fastLog2(BETA * (1.0f / (temp + 273.15f)) - 1.0f / RESISTANCE_TEMP);
	}
	public static boolean effectsThermalReading(PlacedComponent placed, PlacedComponent other) {
		if (other==placed) return true;
		Orientation facing = placed.get(ORIENTATION);
		boolean isDirectional = placed.get(VARIANT) == 1;
		Vector2ic norm = getNorm(facing);
		if (other.intersects(placed.x+norm.x(), placed.y+norm.y(),
				placed.footprint().getWidth(), placed.footprint().getHeight()))
			return true;
		if (isDirectional) return false;
		if (other.intersects(placed.x-norm.x(), placed.y-norm.y(),
				placed.footprint().getWidth(), placed.footprint().getHeight()))
			return true;
		return false;
	}
	private static final Vector2ic UP = new Vector2i(0,1), DOWN = new Vector2i(0,-1),
		RIGHT = new Vector2i(1,0), LEFT = new Vector2i(-1,0);
	public static Vector2ic getNorm(Orientation facing) {
		return switch (facing) {
			case UP -> UP;
			case DOWN -> DOWN;
			case LEFT -> LEFT;
			case RIGHT -> RIGHT;
		};
	}

	protected static class State {
		ArrayList<ThermalUnit> monitoring = new ArrayList<ThermalUnit>();
		int lazyTick = 0;
		ThermalElectricWire thermal;
		ElectricWire wire;
		float ambient = -1000;
	}
}
