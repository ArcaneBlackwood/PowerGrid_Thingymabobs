package dev.thingymabobs.component.thermal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.IRenderableUIComponent;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.util.ComponentUtils;
import dev.thingymabobs.util.ThermalElectricWire;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ThermalRTDComponent extends Component implements IRenderableUIComponent {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3,3, null, null)
		.addPad(0, 0, 0)
		.addPad(0, 2, 1)
		.withItem().withOutline().withArrow(Orientation.RIGHT).build();
	private static final ComponentFootprint FOOTPRINT_DOWN = new ComponentFootprint.Builder(
			3,3, null, null)
		.addPad(1, 0, 0)
		.addPad(1, 2, 1)
		.withItem().withOutline().build();
	private static final ComponentFootprint FOOTPRINT_UP = new ComponentFootprint.Builder(
			3,3, null, null)
		.addPad(1, 0, 0)
		.addPad(1, 2, 1)
		.withItem().withOutline().build();
	private static final ComponentFootprint[] FOOTPRINT_ROTS = new ComponentFootprint[4];
	private static final ComponentFootprint[] FOOTPRINT_DOWN_ROTS = new ComponentFootprint[2];
	private static final ComponentFootprint[] FOOTPRINT_UP_ROTS = new ComponentFootprint[2];
	static {
		for (int i=0; i<4; i++) {
			FOOTPRINT_ROTS[i] = FOOTPRINT.rotated(Orientation.values()[i]);
		}
		for (int i=0; i<2; i++) {
			FOOTPRINT_DOWN_ROTS[i] = FOOTPRINT_DOWN.rotated(Orientation.values()[i]);
			FOOTPRINT_UP_ROTS[i] = FOOTPRINT_UP.rotated(Orientation.values()[i]);
		}
	}

	public static final int lazyTickRate = 40;


	public static final String CONFIG_RESISTANCE = "base_resistance";
	public static final String CONFIG_TEMPERATURE = "base_temperature";
	public static final String CONFIG_BETA = "beta";
	protected static CProperties.PropDevice CONFIG = null;
	protected static float RESISTANCE_TEMP = 25f;
	protected static float RESISTANCE_MIN = 10f;
	protected static float BETA;
	public static void configUpdated(CProperties.PropDevice prop) {
		CONFIG = prop;
		RESISTANCE_TEMP = prop.getFloat(CONFIG_TEMPERATURE).get();
		RESISTANCE_MIN = prop.getResistance(CProperties.MIN).get();
		BETA = prop.getFloat(CONFIG_BETA).get();
		POWER.markDirty();
	}


   	public static final EnumProperty<Variant> VARIANT = new EnumProperty<>(Thingymabobs.MOD_ID, "variant",
		Variant.class).hidden().cast();
	public static final EnumProperty<Connection> CONNECTION = new EnumProperty<>(Thingymabobs.MOD_ID, "rtd.connect",
		Connection.class).hidden().cast();
   	public static final EnumProperty<Orientation> ORIENTATION = Orientation.PROPERTY; //Shadow property

	public static final DynamicFloatProperty RESISTANCE = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "rtd.resistance", () -> CONFIG.getFloat(CONFIG_RESISTANCE)).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());


	

	public ThermalRTDComponent() {
		super(null);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(RESISTANCE, POWER, VARIANT, CONNECTION, ORIENTATION);
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
			.addHeatSource(state.wire);
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
			updateConnections(placed, state);
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
		placed.set(ORIENTATION, placed.get(VARIANT).rotation);
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
			newState = Connection.EXTERNAL;
			state.monitorBlock = null;
			if (!placed.getWorld().isLoaded(state.monitorBlockPos)) break EXIT_EARLY;
			BlockEntity be = placed.getWorld().getBlockEntity(state.monitorBlockPos);
			if (be == null) break EXIT_EARLY;
			var thermal = BlockEntityBehaviour.get(be, AThermalBehaviour.TYPE);
			if (thermal == null) break EXIT_EARLY;
			state.monitorBlock = thermal;
			newState = Connection.EXTERNAL_ACTIVE;
		}
		if (newState != null && newState != placed.get(CONNECTION)) {
			placed.set(CONNECTION, newState);
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
		return Math.max(resistance + BETA * (temp - RESISTANCE_TEMP), RESISTANCE_MIN); 
	}




	protected void updateConnections(@NotNull PlacedComponent placed, State state) {
		Connection connect = Connection.NONE;
		state.monitoring.clear();
		state.monitorBlockPos = null;
		Variant variant = placed.get(VARIANT);
		connect = updateConnectionExternal(placed, state, variant);
		if (connect == Connection.NONE)
			connect = updateConnectionInternal(placed, state, variant);
		placed.set(CONNECTION, connect);
		placed.set(VARIANT, variant);
		placed.set(ORIENTATION, placed.get(VARIANT).rotation);
		placed.notifyClients(VARIANT);
		placed.notifyClients(CONNECTION);
	}
	protected Connection updateConnectionExternal(@NotNull PlacedComponent placed, State state, Variant variant) {
		if (variant.facing != null && !ComponentUtils.isOnEdge(placed, variant.facing)) return Connection.NONE;
		Direction direction = ComponentUtils.getGlobalDirection(placed, variant.direction);
		@Nullable AThermalBehaviour thermal = ComponentUtils.getThermalExternal(placed, direction);
		state.monitorBlock = thermal;
		state.monitorBlockPos = placed.getPos().relative(direction);
		return thermal == null ? Connection.EXTERNAL : Connection.EXTERNAL_ACTIVE;
	}
	protected Connection updateConnectionInternal(@NotNull PlacedComponent placed, State state, Variant variant) {
		if (variant.facing == null) return Connection.NONE;
		Stream<PlacedComponent> search = ComponentUtils.getAllOtherBoardComponents(placed);
		if (search == null) return Connection.NONE;
		Connection connect = Connection.NONE;
		for (var iter = search.iterator(); iter.hasNext(); ) {
			PlacedComponent other = iter.next();
			Connection con = isTouching(placed, variant.facing, other);
			if (con == Connection.NONE) continue;

			List<ThermalUnit> thermals = ComponentUtils.getThermalUnits(other);
			if (thermals.isEmpty()) continue;

			connect = Connection.INTERNAL;
			state.monitoring.addAll(thermals);
		}
		return connect;
	}


	public static Connection isTouching(PlacedComponent placed, Orientation facing, PlacedComponent other) {
		if (ComponentUtils.isTouchingInDirection(placed, facing, other))
			return Connection.INTERNAL;
		return Connection.NONE;
	}



	@OnlyIn(Dist.CLIENT)
	@Override
	public void renderUI(GuiGraphics ctx, ComponentFootprint that, int x, int y, boolean hovering) {
		boolean isDown = ArrayUtils.contains(FOOTPRINT_DOWN_ROTS, that);
		boolean isUp = ArrayUtils.contains(FOOTPRINT_UP_ROTS, that);
		if (!isDown && !isUp) return;
      	PoseStack ms = ctx.pose();

		float offX = 0.5f, offY = 0.5f;
		if (isDown) {
			if (that == FOOTPRINT_DOWN_ROTS[0]) offX = 1f;
			else offY = 0f;
		} else if (isUp) {
			if (that == FOOTPRINT_UP_ROTS[0]) offX = 1f;
			else offY = 0f;
		}

		ms.translate(
			(float)x + that.getWidth() * offX,
			(float)y + that.getHeight() * offY,
			0.0F);
		ms.scale(0.25F, 0.25F, 1.0F);
		ms.translate(-3.5f, -3.5f, 25.0f);

		if (isDown) {
			ctx.blit(ModModels.ARROWS_INOUT, 0, 0, 8, 0, 7, 7, 16, 16);
		} else if (isUp) {
			ctx.blit(ModModels.ARROWS_INOUT, 0, 0, 0, 0, 7, 7, 16, 16);
		}
	}
	@Override
	public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
		Variant variant = placed.get(VARIANT);
		if (counterClockwise) variant = variant.previous();
		else variant = variant.next();
		placed.set(VARIANT, variant);
		placed.set(ORIENTATION, placed.get(VARIANT).rotation);
		return true;
	}

	
	@Override
	public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
		if (placed == null) return FOOTPRINT;
		Variant variant = placed.get(VARIANT);
		return (switch (variant) {
			case RIGHT, DOWN, LEFT, UP -> FOOTPRINT_ROTS;
			case UP_V, UP_H -> FOOTPRINT_UP_ROTS;
			case DOWN_V, DOWN_H -> FOOTPRINT_DOWN_ROTS;
		})[variant.rotation.ordinal()];
	}
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		boolean external = placed.get(CONNECTION) == Connection.EXTERNAL_ACTIVE;
		return switch (placed.get(VARIANT)) {
			case RIGHT, DOWN, LEFT, UP -> external ? ModModels.RTD_EX : ModModels.RTD;
			case UP_V, UP_H -> external ? ModModels.RTD_UP_EX : ModModels.RTD_UP;
			case DOWN_V, DOWN_H -> external ? ModModels.RTD_DOWN_EX : ModModels.RTD_DOWN;
		};
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.RTD, ModModels.RTD_EX, ModModels.RTD_DOWN, ModModels.RTD_DOWN_EX, ModModels.RTD_UP, ModModels.RTD_UP_EX);
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
		NONE, INTERNAL, EXTERNAL, EXTERNAL_ACTIVE;
	}
	public enum Variant {
		RIGHT(Direction.EAST, Orientation.RIGHT),
		DOWN(Direction.SOUTH, Orientation.DOWN),
		LEFT(Direction.WEST, Orientation.LEFT),
		UP(Direction.NORTH, Orientation.UP),
		DOWN_V(Direction.DOWN, null, Orientation.RIGHT),
		DOWN_H(Direction.DOWN, null, Orientation.UP),
		UP_V(Direction.UP, null, Orientation.RIGHT),
		UP_H(Direction.UP, null, Orientation.UP);
		public static final Variant[] VALUES = values();
		public static final int SIZE = VALUES.length;
		public final Direction direction;
		public final Orientation facing;
		public final Orientation rotation;
		private Variant(Direction direction, Orientation facing) {
			this.direction = direction;
			this.facing = facing;
			this.rotation = facing;
		}
		private Variant(Direction direction, Orientation facing, Orientation rotation) {
			this.direction = direction;
			this.facing = facing;
			this.rotation = rotation;
		}

		public Variant next() {
			int index = ordinal() + 1;
			if (index >= SIZE) index = 0;
			return VALUES[index];
		}
		public Variant previous() {
			int index = ordinal();
			if (index <= 0) index = SIZE;
			return VALUES[index-1];
		}
	}
}
