package dev.thingymabobs.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;
import com.google.common.collect.ImmutableCollection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.util.ComponentUtils;
import dev.thingymabobs.util.TMath;
import dev.thingymabobs.util.ThermalElectricWire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ThermistorComponent extends OrientableComponent {
	private static final ComponentFootprint FOOTPRINT_FACE = new ComponentFootprint.Builder(
			1,2, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(0, 1, 1)
		.withItem().withOutline().withArrow(Orientation.RIGHT).build();
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			1,2, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(0, 1, 1)
		.withItem().withOutline().build();

	public static final int lazyTickRate = 40;

	public static final String CONFIG_RESISTANCE = "base_resistance";
	public static final String CONFIG_TEMPERATURE = "base_temperature";
	public static final String CONFIG_BETA = "beta";
	public static final float TEMP_C2K = 273.15f;
	protected static CProperties.Prop CONFIG = null;
	protected static float RESISTANCE_TEMP = 298.15f;
	protected static float BETA;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		RESISTANCE_TEMP = prop.getFloat(CONFIG_TEMPERATURE).get() + TEMP_C2K;
		BETA = prop.getFloat(CONFIG_BETA).get();
		POWER.markDirty();
	}

	public static final BooleanProperty FRONT_ONLY = new BooleanProperty(Thingymabobs.MOD_ID, "thermistor.front_only", false).hidden().cast();
	public static final IntProperty CONNECTION = new IntProperty(Thingymabobs.MOD_ID, "thermistor.connect", 0, 0, 4).hidden().cast();

	public static final DynamicFloatProperty RESISTANCE = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "resistor_temp_value", () -> CONFIG.getFloat(CONFIG_RESISTANCE)).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());


	

	public ThermistorComponent() {
		super(null);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(RESISTANCE, POWER, FRONT_ONLY, CONNECTION);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var therm = CONFIG.getThermal();
		State state = new State();

		placed.customData = state;
		state.wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));
		state.dissipation = ThermalBehaviour.dissipationFactor(therm.getPower(), therm.getTemp());
		state.mass = therm.getMass();
		state.thermal = new ThermalElectricWire(state.dissipation);

		therm.apply(thermals)
			.addHeatSource(state.wire)
			.addHeatSource(state.thermal);
	}



	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!placed.isClient())
			return tickServer(placed);
		return false;
	}
	public boolean tickServer(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return false;
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) return false;

		if (state.firstTick) {
			if (!updateConnections(placed, state)) return true;
            state.temperature = state.ambient = ThermalBehaviour.getAmbientTemperature(board.getLevel(), placed.getPos());
			state.firstTick = false;
		}

		float tempExt = getExternalTemperature(placed, state);
		state.thermal.setTemperature(state.ambient, tempExt);

		updateThermals(state, (float)(state.thermal.power() + state.wire.power()));

		double resistance = calcResistance(
			placed.get(RESISTANCE), state.temperature);
		state.wire.setResistance(resistance);
		
		return true;
	}
	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		if (!placed.isClient()) return;
		modelChanged(placed.getPos());
	}



	protected float getExternalTemperature(@NotNull PlacedComponent placed, State state) {
		if (state.monitorBlockPos == null) {
			double temp = 0;
			if (state.monitoring.size() == 0) return state.ambient;
			for (ThermalUnit thermal : state.monitoring) {
				temp += thermal.getTemperature();
			}
			return (float)(temp / state.monitoring.size());
		}

		Connection newState = null;
		EXIT_EARLY: if (state.monitorBlock == null || state.monitorBlock.blockEntity.isRemoved()) {
			if (placed.get(CONNECTION) != Connection.NONE.toProp())
				newState = Connection.NONE;
			state.monitorBlock = null;
			if (!placed.getWorld().isLoaded(state.monitorBlockPos)) break EXIT_EARLY;
			BlockEntity be = placed.getWorld().getBlockEntity(state.monitorBlockPos);
			if (be == null) break EXIT_EARLY;
			var thermal = BlockEntityBehaviour.get(be, AThermalBehaviour.TYPE);
			if (thermal == null) break EXIT_EARLY;
			state.monitorBlock = thermal;
			newState = Connection.EXTERNAL;
		}
		if (newState != null) {
			placed.set(CONNECTION, newState.toProp());
			placed.notifyClients(CONNECTION);
		}
		return state.monitorBlock == null ? state.ambient : state.monitorBlock.getTemperature();
	}
	//Custom thermals just for resistance tracking.  Purpose is to shadow main theremal, but without extra dissipation factor of fans
	protected void updateThermals(State state, float sourcePower) {
         float power = -state.dissipation * (state.temperature - state.ambient)
		 	+ sourcePower;
         state.temperature += power / 20.0F / state.mass;
         if (!Float.isFinite(state.temperature))
            state.temperature = state.ambient;
         if (state.temperature < state.ambient)
            state.temperature = state.ambient;
	}
	public static double calcResistance(float resistance, float temp) {
		//Math.exp
		return resistance * TMath.fastPow2(BETA * (1.0f / (temp + TEMP_C2K) - 1.0f / RESISTANCE_TEMP));
	}




	protected boolean updateConnections(@NotNull PlacedComponent placed, State state) {
		Connection connect = Connection.NONE;
		state.monitoring.clear();
		state.monitorBlockPos = null;
		if (placed.get(FRONT_ONLY))
			connect = updateConnectionExternal(placed, state);
		if (connect == Connection.NONE)
			connect = updateConnectionInternal(placed, state);
		placed.set(CONNECTION, connect.toProp());
		placed.notifyClients(CONNECTION);
		placed.notifyClients(FRONT_ONLY);
		placed.notifyClients(ORIENTATION);
		return connect != Connection.INTERNAL;
	}
	protected Connection updateConnectionExternal(@NotNull PlacedComponent placed, State state) {
		if (!ComponentUtils.isOnEdge(placed)) return Connection.NONE;
		Direction direction = ComponentUtils.getGlobalFacing(placed);
		@Nullable AThermalBehaviour thermal = ComponentUtils.getThermalExternal(placed, direction);
		state.monitorBlock = thermal;
		state.monitorBlockPos = placed.getPos().relative(direction);
		return Connection.EXTERNAL;
	}
	protected Connection updateConnectionInternal(@NotNull PlacedComponent placed, State state) {
		Stream<PlacedComponent> search = ComponentUtils.getAllOtherBoardComponents(placed);
		if (search == null) return Connection.INTERNAL;
		Connection connect = Connection.NONE;
		for (var iter = search.iterator(); iter.hasNext(); ) {
			PlacedComponent other = iter.next();
			Connection con = isTouching(placed, other);
			if (con == Connection.NONE) continue;

			List<ThermalUnit> thermals = ComponentUtils.getThermalUnits(other);
			if (thermals.isEmpty()) continue;

			connect = connect.combine(con);
			state.monitoring.addAll(thermals);
		}
		return connect;
	}


	public static Connection isTouching(PlacedComponent placed, PlacedComponent other) {
		Orientation facing = placed.get(ORIENTATION);
		if (ComponentUtils.isTouchingInDirection(placed, facing, other))
			return Connection.FRONT;
		if (placed.get(FRONT_ONLY)) return Connection.NONE;
		if (ComponentUtils.isTouchingInDirection(placed, facing.getOpposite(), other))
			return Connection.FRONT;
		return Connection.NONE;
	}



	@Override
	public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
		Orientation orientation = (Orientation)placed.get(Orientation.PROPERTY);
		boolean frontOnly = placed.get(FRONT_ONLY);
		if (frontOnly) {
			frontOnly = orientation != (counterClockwise ? Orientation.RIGHT : Orientation.UP);
			if (frontOnly)
				orientation = counterClockwise ? orientation.getCounterClockwise() : orientation.getClockwise();
			else
				orientation = counterClockwise ? Orientation.DOWN : Orientation.RIGHT;
		} else {
			frontOnly = orientation == (counterClockwise ? Orientation.RIGHT : Orientation.DOWN);
			if (!frontOnly)
				orientation = counterClockwise ? Orientation.RIGHT : Orientation.DOWN;
			else
				orientation = counterClockwise ? Orientation.UP : Orientation.RIGHT;
		}
		placed.set(Orientation.PROPERTY, orientation);
		placed.set(FRONT_ONLY, frontOnly);
		return true;
	}

	
	@Override
	public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
		if (placed==null) return FOOTPRINT;
		return (placed.get(FRONT_ONLY) ? FOOTPRINT_FACE : FOOTPRINT).rotated((Orientation)placed.get(ORIENTATION));
	}
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return switch (Connection.fromProp(placed.get(CONNECTION))) {
			case NONE, INTERNAL -> ModModels.THI;
			case FRONT -> ModModels.THI_F; 
			case BACK -> ModModels.THI_B;
			case BOTH -> ModModels.THI_FB;
			case EXTERNAL -> ModModels.THI_EX;
		};
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			ModModels.THI, ModModels.THI_F, ModModels.THI_FB, ModModels.THI_B, ModModels.THI_EX
		);
	}


	protected static class State {
		boolean firstTick = true;
		ArrayList<ThermalUnit> monitoring = new ArrayList<ThermalUnit>();
		AThermalBehaviour monitorBlock = null;
		BlockPos monitorBlockPos = null;

		ThermalElectricWire thermal;
		ElectricWire wire;

		float temperature, mass, dissipation, ambient = 25f;
	}
	protected static enum Connection {
		NONE, FRONT, BACK, BOTH, EXTERNAL,   INTERNAL;
		public Connection combine(Connection that) {
			if (this==EXTERNAL || that==EXTERNAL) return EXTERNAL;
			if (that==INTERNAL || this==that) return this;
			boolean isFront = this==FRONT || this==BOTH || that==FRONT || that==BOTH;
			boolean isBack = this==BACK || this==BOTH || that==BACK || that==BOTH;
			if (isFront == isBack)
				return isFront ? BOTH : NONE;
			else
				return isFront ? FRONT : BACK;
		}
		public static Connection fromProp(int index) {
			return values()[index];
		}
		public int toProp() {
			if (this==INTERNAL) return 0;
			return ordinal();
		}
	}
}
