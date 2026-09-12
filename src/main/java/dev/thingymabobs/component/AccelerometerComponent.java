package dev.thingymabobs.component;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder.IEmitter;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.AVertMirrorComponent;
import dev.thingymabobs.component.base.CouplingWireProxy;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.component.trancievers.DirectionalRecieverComponent;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.util.SableUtils;
import dev.thingymabobs.util.TMath;
import dev.thingymabobs.util.SableUtils.PoseMotion;
import com.google.common.collect.ImmutableCollection.Builder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AccelerometerComponent extends AVertMirrorComponent {
	protected static final ComponentFootprint FOOTPRINT_H = new ComponentFootprint.Builder(
			5,3, "component." + Thingymabobs.MOD_ID + ".accelerometer", null)
		.addPad(4, 0, 0, "Signal +", "S+")
		.addPad(4, 2, 1, "Signal -", "S-")
		.withItem().withOutline().withArrow(Orientation.LEFT).build();
	protected static final ComponentFootprint FOOTPRINT_V = new ComponentFootprint.Builder(
			4,3, "component." + Thingymabobs.MOD_ID + ".accelerometer", null)
		.addPad(2, 0, 0, "Signal +", "S+")
		.addPad(2, 2, 1, "Signal -", "S-")
		.withItem().withOutline().build();
		
	public static final String CONFIG_SENSITIVITY = "sensitivity";
	public static final String CONFIG_FALLOFF = "falloff_smoothing";
	protected static CProperties.Prop CONFIG = null;
	public static float VOLTAGE;
	protected static final TMath.SoftMax SOFT_MAX = new TMath.SoftMax();
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		VOLTAGE = Mth.sqrt(prop.getThermal().getPower() * prop.getResistance().get());
		SOFT_MAX.setSmooth(prop.getFloat(CONFIG_FALLOFF).get());
		SENSITIVITY.markDirty();
		VOLTAGE_PROP.markDirty();
		POWER_PROP.markDirty();
	}
		
	public static final DynamicFloatProperty SENSITIVITY = new DynamicFloatProperty( //v / m/ss
		Thingymabobs.MOD_ID, "gyroscope.sensitivity", () -> CONFIG.getFloat(CONFIG_SENSITIVITY)).useMetrics();
	public static final LazyConstantProperty VOLTAGE_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "accelerometer.voltage_max",
		() -> Unit.POWER.formatWithPrefixes(VOLTAGE).string());
	public static final LazyConstantProperty POWER_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "accelerometer.power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());



	public AccelerometerComponent() {
		super(FOOTPRINT_V, FOOTPRINT_H);
	}
	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL, SENSITIVITY, VOLTAGE_PROP, POWER_PROP);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull IEmitter thermals) {
		VoltageSourceCoupling source = builder.addInternalNode(
			VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), CONFIG.getResistance().get());
		source.setVoltage(0);
		source.setResistance(CONFIG.getResistance().get());
		placed.customData = source;
		CONFIG.getThermal().apply(thermals)
			.addHeatSource(new CouplingWireProxy(source));
	}
	

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof VoltageSourceCoupling source)) return true;
		float accel = tickMotion(placed);
		if (source == null || !source.isConverged()) return true;
		double current = source.getCurrent();
		if (!Double.isFinite(current)) return true;
		//Thingymabobs.LOGGER.info("Accel: "+accel+", soft: "+SOFT_MAX.compute(accel));
		source.setVoltage(SOFT_MAX.compute(accel * placed.get(SENSITIVITY), VOLTAGE));

		return true;
	}
	protected static final Vector3fc GRAVITY = Direction.UP.step().mul(9.81f);
	protected float tickMotion(PlacedComponent placed) {
		PoseMotion motion = SableUtils.getPoseMotion(placed.getWorld().getBlockEntity(placed.getPos()));
		if (motion == null) return 0;
		Vector3f direc = motion.getDirectionGlobal(getDirection(placed));
		float accel = motion.getAcceleration(
			direc,
			motion.getPositionGlobal(placed.getExactPos().toVector3f()));
		return accel + direc.dot(GRAVITY);
	}

	public Direction getDirection(PlacedComponent placed) {
		return DirectionalRecieverComponent.getFacing(placed);
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
		return component.get(VERTICAL) ? Thingymabobs.asResource("accelerometer_vertical")
			: Thingymabobs.asResource("accelerometer");
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			Thingymabobs.asResource("accelerometer_vertical"),
			Thingymabobs.asResource("accelerometer")
		);
	}
}
