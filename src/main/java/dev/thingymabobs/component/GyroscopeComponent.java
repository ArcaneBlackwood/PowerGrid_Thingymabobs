package dev.thingymabobs.component;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder.IEmitter;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;
import com.google.common.collect.ImmutableCollection.Builder;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.AVertMirrorComponent;
import dev.thingymabobs.component.base.CouplingWireProxy;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.component.trancievers.DirectionalRecieverComponent;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.util.SableUtils;
import dev.thingymabobs.util.SableUtils.PoseMotion;
import dev.thingymabobs.util.TMath;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GyroscopeComponent extends AVertMirrorComponent {
	protected static final ComponentFootprint FOOTPRINT_H = new ComponentFootprint.Builder(
			6,4, "component." + Thingymabobs.MOD_ID + ".gyroscope", null)
		.addPad(0, 1, 0, "Motor", "M")
		.addPad(5, 2, 1, "Motor", "M")
		.addPad(2, 2, 2, "Signal +", "S+")
		.addPad(3, 1, 3, "Signal -", "S-")
		.withItem().withOutline().withArrow().build();
	protected static final ComponentFootprint FOOTPRINT_V = new ComponentFootprint.Builder(
			6,4, "component." + Thingymabobs.MOD_ID + ".gyroscope", null)
		.addPad(0, 0, 0, "Motor", "M")
		.addPad(0, 3, 1, "Motor", "M")
		.addPad(3, 1, 2, "Signal +", "S+")
		.addPad(4, 2, 3, "Signal -", "S-")
		.withItem().withOutline().build();

	public static final String CONFIG_MOTOR = "motor";
	public static final String CONFIG_MOTOR_POWER = "motor_power_min";
	public static final String CONFIG_SENSITIVITY = "sensitivity";
	public static final String CONFIG_FALLOFF = "falloff_smoothing";
	public static final String CONFIG_PARTICLES = "particle_spawn_rate";
	protected static CProperties.Prop CONFIG = null;
	public static float VOLTAGE, MOTOR_CURRENT_MIN, PARTICLE_SPAWN_RATE;
	protected static final TMath.SoftMax SOFT_MAX = new TMath.SoftMax();
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		float motorPower = prop.getFloat(CONFIG_MOTOR_POWER).get();
		float sigalPower = prop.getThermal().getPower() - motorPower;
		VOLTAGE = Mth.sqrt(sigalPower * prop.getResistance().get());
		MOTOR_CURRENT_MIN = Mth.sqrt(motorPower / prop.getResistance(CONFIG_MOTOR).get());
		PARTICLE_SPAWN_RATE = prop.getFloat(CONFIG_PARTICLES).get();
		SOFT_MAX.setSmooth(prop.getFloat(CONFIG_FALLOFF).get());
		SENSITIVITY.markDirty();
		POWER_PROP.markDirty();
		VOLTAGE_PROP.markDirty();
		MOTOR_POWER_PROP.markDirty();
		MOTOR_CURRENT_PROP.markDirty();
	}
		
	public static final DynamicFloatProperty SENSITIVITY = new DynamicFloatProperty( //v / m/ss
		Thingymabobs.MOD_ID, "gyroscope.sensitivity", () -> CONFIG.getFloat(CONFIG_SENSITIVITY)).useMetrics();
	public static final LazyConstantProperty POWER_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "gyroscope.power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());
	public static final LazyConstantProperty VOLTAGE_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "gyroscope.voltage_max",
		() -> Unit.VOLTAGE.formatWithPrefixes(VOLTAGE).string());
	public static final LazyConstantProperty MOTOR_POWER_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "gyroscope.motor_power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getFloat(CONFIG_MOTOR_POWER).get()).string());
	public static final LazyConstantProperty MOTOR_CURRENT_PROP = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "gyroscope.motor_current_min",
		() -> Unit.CURRENT.formatWithPrefixes(MOTOR_CURRENT_MIN).string());



	public GyroscopeComponent() {
		super(FOOTPRINT_V, FOOTPRINT_H);
	}
	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(SENSITIVITY, POWER_PROP, VOLTAGE_PROP, MOTOR_POWER_PROP, MOTOR_CURRENT_PROP);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull IEmitter thermals) {
		State state = new State();
		state.source = builder.addInternalNode(
			VoltageSourceCoupling.class, builder.terminalNode(2), builder.terminalNode(3), CONFIG.getResistance().get());
		state.source.setVoltage(0);
		state.source.setResistance(CONFIG.getResistance().get());
		state.motor = builder.connect(CONFIG.getResistance(CONFIG_MOTOR).get(), 
			builder.terminalNode(0), builder.terminalNode(1));
		placed.customData = state;
		CONFIG.getThermal().apply(thermals)
			.addHeatSource(state.motor)
			.addHeatSource(new CouplingWireProxy(state.source));
	}
	

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return true;
		float rot = tickMotion(placed);
		if (!state.source.isConverged()) return true;
		double current = state.source.getCurrent();
		if (!Double.isFinite(current)) return true;

		state.source.setVoltage(SOFT_MAX.compute(rot * placed.get(SENSITIVITY), VOLTAGE));
		return true;
	}
	protected float tickMotion(PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return 0;
		float motorCurrent = Math.abs((float)state.motor.current()) / MOTOR_CURRENT_MIN;
		motorCurrent = 8*(motorCurrent-1)+1;
		if (motorCurrent < 0) return 0;
		if (motorCurrent > 1) motorCurrent = 1;
		if (placed.isClient()) playEffect(placed, motorCurrent * PARTICLE_SPAWN_RATE); 
		PoseMotion motion = SableUtils.getPoseMotion(placed.getWorld().getBlockEntity(placed.getPos()));
		if (motion == null) return 0;
		float rot = motion.getAngularVelocity(motion.getDirectionGlobal(getDirection(placed)));
		return rot * motorCurrent;
	}
	@OnlyIn(Dist.CLIENT)
	protected void playEffect(PlacedComponent placed, float rate) {
		Level world = placed.getWorld();
		RandomSource rand = world.random;
		if (rand.nextFloat() > rate) return;
		Vec3 spawnLoc = getCenterPos(placed, 6, rand.nextFloat() * 2-1, rand.nextFloat() * 2-1);
		final float s = 0.04f;
		world.addParticle(ParticleTypes.EFFECT, spawnLoc.x, spawnLoc.y, spawnLoc.z, 
			(rand.nextFloat()-0.5f)*s, (rand.nextFloat()-0.5f)*s, (rand.nextFloat()-0.5f)*s);
	}
	public static @NotNull Vec3 getCenterPos(PlacedComponent placed, float offset, float x, float y) {
		ComponentFootprint fp = placed.footprint();
		Vec3 pos = new Vec3((placed.x + fp.getWidth()*0.5F + x) * 0.0625f, 0.125F + offset * 0.0625f, (placed.y + fp.getHeight()*0.5F + y) * 0.0625f);
		BlockPos blockPos = placed.getPos();
		BlockState state = placed.getWorld().getBlockState(blockPos);
		pos = VecHelper.rotateCentered(pos, CircuitBoardBlock.getAngleX(state), Axis.X);
		pos = VecHelper.rotateCentered(pos, CircuitBoardBlock.getAngleY(state), Axis.Y);
		return pos.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	public Direction getDirection(PlacedComponent placed) {
		return DirectionalRecieverComponent.getFacing(placed);
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
		return component.get(VERTICAL) ? Thingymabobs.asResource("gyroscope_vertical")
			: Thingymabobs.asResource("gyroscope");
	}

	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			Thingymabobs.asResource("gyroscope_vertical"),
			Thingymabobs.asResource("gyroscope")
		);
	}

	protected static class State {
		public VoltageSourceCoupling source;
		public ElectricWire motor;
	}
}
