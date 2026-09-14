package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.utility.Unit;

public class SmallDiodeComponent extends VerticallyOrientableComponent {
	public static final float K = 1.380649e-23f;
	public static final float Q = 1.602176634e-19f;
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3,1, Thingymabobs.MOD_ID + ".component.small_diode", null)
		.addPad(0, 0, 0, "Cathode -", "C-")
		.addPad(2, 0, 1, "Anode +", "A+")
		.withItem().withOutline().build();
	private static final ComponentFootprint VERTICAL_FOOTPRINT = new ComponentFootprint.Builder(
			2,1, Thingymabobs.MOD_ID + ".component.small_diode", null)
		.addPad(0, 0, 0, "+Cathode", "+C")
		.addPad(1, 0, 1, "-Anode", "-A")
		.withItem().withOutline().build();
	
	protected static CProperties.Prop CONFIG = null;
	protected static Config CONFIG_DIODE = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		CONFIG_DIODE = prop.get(Config.class, Config.KEY);
		FORWARD_VOLTAGE.markDirty();
		BREAKDOWN_VOLTAGE.markDirty();
		RESISTANCE.markDirty();
		REVERSE_LEAKAGE.markDirty();
	}

	public static final LazyConstantProperty FORWARD_VOLTAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "forward_voltage",
		() -> Unit.VOLTAGE.formatWithPrefixes(CONFIG_DIODE.getForwardVoltage(0, CONFIG.getResistance().get(), 22)).string());
	public static final LazyConstantProperty BREAKDOWN_VOLTAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "breakdown_voltage",
		() -> Unit.VOLTAGE.formatWithPrefixes(CONFIG_DIODE.getBreakdown()).string());
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());
	public static final LazyConstantProperty REVERSE_LEAKAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "reverse_leakage",
		() -> Unit.CURRENT.formatWithPrefixes(CONFIG_DIODE.getReverseLeakage(22)).string());
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());


	public SmallDiodeComponent() {
		super(FOOTPRINT, VERTICAL_FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(FORWARD_VOLTAGE, BREAKDOWN_VOLTAGE, RESISTANCE,
			REVERSE_LEAKAGE, POWER);
	}

	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var pnJunctionWire = CONFIG_DIODE.createJunction(
			CONFIG.getResistance().get(), builder.terminalNode(1), builder.terminalNode(0));
		builder.add(pnJunctionWire);
		CONFIG.getThermal().apply(thermals)
			.withTemperatureCallback(pnJunctionWire::setTemperatureCelsius)
			.addHeatSource(pnJunctionWire);
	}
	
	public static class Config extends ASubProp {
		public static final String KEY = "do";
		public ConfigBase.ConfigFloat idealityFactor = null,
			breakdownVoltage = null, breakdownSaturationCurrent = null, reverseSaturationCurrent = null;
		public float idealityFactorDef,
			breakdownVoltageDef, breakdownSaturationCurrentDef, reverseSaturationCurrentDef;

		public Config(float idealityFactor,
			float breakdownVoltage, float breakdownSaturationCurrent, float reverseSaturationCurrent) {
			this.idealityFactorDef = idealityFactor;
			this.breakdownVoltageDef = breakdownVoltage;
			this.breakdownSaturationCurrentDef = breakdownSaturationCurrent;
			this.reverseSaturationCurrentDef = reverseSaturationCurrent;
		}

		@Override
		public Class<?> getType() {
			return Config.class;
		}
		public float getBreakdown() {
			return breakdownVoltage.getF();
		}
		public float getForwardVoltage(float current, float resistance, float temperature) {
			float temperatureK = temperature + 273.15f;
			float referenceTemperature = 295.15f;
			float thermalVoltage = K * temperatureK / Q;

			float temperatureRatio = temperatureK / referenceTemperature;
			float idealityFactor = this.idealityFactor.getF();
			float saturationCurrent = (float)(reverseSaturationCurrent.getF()
				* Math.pow(temperatureRatio, 3.0f / idealityFactor)
				* Math.exp(-(Q * 1.12f / K / temperatureK / idealityFactor)
					* (1.0f - temperatureRatio)));

			return idealityFactor * thermalVoltage
				* (float)Math.log1p(current / saturationCurrent)
				+ current * resistance;
		}
		public float getReverseLeakage(float temperature) {
			float temperatureK = temperature + 273.15f;
			float referenceTemperature = 295.15f;
			float temperatureRatio = temperatureK / referenceTemperature;
			float idealityFactor = this.idealityFactor.getF();

			return (float)(reverseSaturationCurrent.getF()
				* Math.pow(temperatureRatio, 3.0f / idealityFactor)
				* Math.exp(-(Q * 1.12f / K / temperatureK / idealityFactor)
					* (1.0f - temperatureRatio)));
		}
		@Override
		public void register(String id, CProperties.Builder builder) {
			idealityFactor = builder.f(idealityFactorDef, 0f, id+"_idealityFactor");
			breakdownVoltage = builder.f(breakdownVoltageDef, 0f, id+"_breakdown_voltage");
			breakdownSaturationCurrent = builder.f(breakdownSaturationCurrentDef, 0f, id+"_breakdown_saturation_current");
			reverseSaturationCurrent = builder.f(reverseSaturationCurrentDef, 0f, id+"_reverse_saturation_current");
		}
		public PNJunctionWire createJunction(float resistance, IElectricNode node1, IElectricNode node2) {
			return new PNJunctionWire(
				reverseSaturationCurrent.get(), resistance, 22,
				idealityFactor.get(), breakdownVoltage.get(), breakdownSaturationCurrent.get(),
				node1, node2);
		}
	}
}