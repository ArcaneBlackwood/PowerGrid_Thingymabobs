package dev.thingymabobs.component;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder.IEmitter;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.trancievers.DirectionalRecieverComponent;
import dev.thingymabobs.util.SableUtils;
import dev.thingymabobs.util.SableUtils.PoseMotion;
import com.google.common.collect.ImmutableCollection.Builder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class AccelerometerComponent extends AVertMirrorComponent {
    protected static final ComponentFootprint FOOTPRINT_H = new ComponentFootprint.Builder(
				5,3, "component." + Thingymabobs.MOD_ID + ".accelerometer", null)
            .addPad(4, 0, 0)
            .addPad(4, 2, 1)
            .withItem().withOutline().build();
    protected static final ComponentFootprint FOOTPRINT_V = new ComponentFootprint.Builder(
				4,3, "component." + Thingymabobs.MOD_ID + ".accelerometer", null)
            .addPad(2, 0, 0)
            .addPad(2, 2, 1)
            .withItem().withOutline().build();

	public static final float POWER = 5;
	public static final float VOLTAGE = 50;
	public static final float RESISTANCE = VOLTAGE*VOLTAGE/POWER;
		
	public static final FloatProperty SENSITIVITY = new FloatProperty( //v / m/ss
		Thingymabobs.MOD_ID, "sensitivity", 1f, 0f, 10f);
	public static final ConstantProperty POWER_PROP = new ConstantProperty(
		Thingymabobs.MOD_ID, "power",
		Unit.POWER.formatWithPrefixes(POWER).component()
	);
	public static final ConstantProperty VOLTAGE_PROP = new ConstantProperty(
		Thingymabobs.MOD_ID, "max_voltage",
		Unit.VOLTAGE.formatWithPrefixes(VOLTAGE).component()
	);

	public AccelerometerComponent() {
		super(FOOTPRINT_V, FOOTPRINT_H);
	}
	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(SENSITIVITY, POWER_PROP, VOLTAGE_PROP);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull IEmitter thermals) {
        VoltageSourceCoupling source = builder.addInternalNode(
                VoltageSourceCoupling.class,
                builder.terminalNode(0),
                builder.terminalNode(1),
                RESISTANCE
        );
        source.setVoltage(0);
        source.setResistance(RESISTANCE);
        placed.customData = source;
		thermals.builder()
			.setDissipationFactor(ThermalBehaviour.dissipationFactor(POWER, 100f))
			.setThermalMass(0.2f)
			.addHeatSource(new CouplingWireProxy(source));
	}
	

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		float accel = tickMotion(placed);
        if (!(placed.customData instanceof VoltageSourceCoupling source)) return true;
        if (source == null || !source.isConverged()) return true;
        double current = source.getCurrent();
        if (!Double.isFinite(current)) return true;
		source.setVoltage(accel * placed.get(SENSITIVITY));

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
