package dev.thingymabobs.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.component.trancievers.ATrancieverComponent;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.PlacedComponentExt;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.util.BakedQuadEditor;
import dev.thingymabobs.util.TMath;
import dev.thingymabobs.util.ThermalElectricWire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

public class ThermistorComponent extends OrientableComponent {
	private static final ComponentFootprint FOOTPRINT_FACE = new ComponentFootprint.Builder(
			2,1, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(1, 0, 1)
		.withItem().withOutline().withArrow(Orientation.DOWN).build();
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			2,1, Thingymabobs.MOD_ID + ".component.small_resistor", null)
		.addPad(0, 0, 0)
		.addPad(1, 0, 1)
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
		if (placed.isClient())
			return tickClient(placed);
		else
			return tickServer(placed);
	}
	public boolean tickServer(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return false;
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) return false;

		if (state.firstTick) {
			if (!updateConnectionsServer(placed)) return true;
            state.temperature = state.ambient = ThermalBehaviour.getAmbientTemperature(board.getLevel(), placed.getPos());
			state.firstTick = false;
		}

		double temp = 0;

		Connection newState = null;
		IF: if (state.monitorBlockPos != null) {
			if (state.monitorBlock == null || state.monitorBlock.blockEntity.isRemoved()) {
				if (placed.get(CONNECTION) != Connection.NONE.toProp())
					newState = Connection.NONE;
				state.monitorBlock = null;
				if (!placed.getWorld().isLoaded(state.monitorBlockPos)) break IF;
				BlockEntity be = placed.getWorld().getBlockEntity(state.monitorBlockPos);
				if (be == null) break IF;
				var thermal = BlockEntityBehaviour.get(be, AThermalBehaviour.TYPE);
				if (thermal == null) break IF;
				state.monitorBlock = thermal;
				newState = Connection.EXTERNAL;
			}
			temp = state.monitorBlock.getTemperature();
		} else {
			if (state.monitoring.size() == 0) break IF;
			for (ThermalUnit thermal : state.monitoring) {
				temp += thermal.getTemperature();
			}
			temp /= state.monitoring.size();
		}
		if (newState != null) {
			placed.set(CONNECTION, newState.toProp());
			placed.notifyClients(CONNECTION);
		}

		state.thermal.setTemperature(state.ambient, (float)temp);
		updateThermals(state, (float)(state.thermal.power() + state.wire.power()));
		double resistance = calcResistance(
			placed.get(RESISTANCE), state.temperature);
		state.wire.setResistance(resistance);
		
		return true;
	}
	@OnlyIn(Dist.CLIENT)
	public boolean tickClient(@NotNull PlacedComponent placed) {
		StateClient state;
		if (placed.customData instanceof StateClient state2) {
			state = state2;
		} else {
			state = new StateClient();
			placed.customData = state;
		}

		if (state.firstTick) {
			if (!updateConnectionsClient(placed)) return true;
			state.firstTick = false;
			modelChanged(placed.getPos());
		}
		return false;
	}
	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		if (!placed.isClient()) return;
		modelChanged(placed.getPos());
	}




	public boolean updateConnections(@NotNull PlacedComponent placed) {
		if (placed.isClient())
			return updateConnectionsClient(placed);
		else
			return updateConnectionsServer(placed);
	}
	protected boolean updateConnectionsServer(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return false;
		state.monitoring.clear();

		boolean result = updateConnections(placed, (other, con, units) -> {
			state.monitoring.addAll(units);
		}, (pos, thermal) -> {
			state.monitorBlock = thermal;
			state.monitorBlockPos = pos;
		});
		placed.notifyClients(CONNECTION);
		placed.notifyClients(FRONT_ONLY);
		placed.notifyClients(ORIENTATION);
		return result;
	}
	protected boolean updateConnectionsClient(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return false;
		state.tall = true;

		return updateConnections(placed, (other, con, units) -> {
			int height = getComponentHeight(other);
			if (height < 3) state.tall = false;
		}, null);
	}
	@FunctionalInterface
	protected static interface OnComponent {
		public void run(PlacedComponent other, Connection con, List<ThermalUnit> units);
	}
	@FunctionalInterface
	protected static interface OnBlock {
		public void run(BlockPos pos, @Nullable AThermalBehaviour thermal);
	}
	protected boolean updateConnections(@NotNull PlacedComponent placed, OnComponent onComponent, OnBlock onBlock) {
		Connection connect = Connection.NONE;
		COMPLETE: {
			if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) {
				placed.set(CONNECTION, Connection.NONE.toProp());
				return false;
			}
			IF: if (placed.get(FRONT_ONLY)) {
				Orientation facing = placed.get(ORIENTATION);
				if (!(placed.x==0 && facing == Orientation.DOWN) && !(placed.x==15 && facing == Orientation.UP)
					&&!(placed.y==0 && facing == Orientation.RIGHT) && !(placed.y==15 && facing == Orientation.LEFT)) break IF;
				connect = Connection.EXTERNAL;
				BlockPos pos = placed.getPos().relative(getFacing(placed));
				
				@Nullable AThermalBehaviour thermal = null;
				if (placed.getWorld().isLoaded(pos)) {
					BlockEntity be = placed.getWorld().getBlockEntity(pos);
					if (be != null)
						thermal = BlockEntityBehaviour.get(be, AThermalBehaviour.TYPE);
				}
				if (onBlock != null)
					onBlock.run(pos, thermal);
				break COMPLETE;
			}

			Stream<PlacedComponent> search = board.getComponentsStream();
			for (var iter = search.iterator(); iter.hasNext(); ) {
				PlacedComponent other = iter.next();
				Connection con = effectsThermalReading(placed, other);
				if (con == Connection.NONE) continue;
				List<ThermalUnit> thermals = getThermalUnits(other);
				if (thermals.isEmpty()) continue;
				if (onComponent != null) onComponent.run(other, con, thermals);
				connect = connect.combine(con);
			}
			break COMPLETE;
		}
		if (connect == Connection.NONE) {
			placed.set(CONNECTION, Connection.NONE.toProp());
		} else if (connect == Connection.EXTERNAL) {
			placed.set(CONNECTION, Connection.EXTERNAL.toProp());
		} else {
			if (connect == Connection.BACK) {
				placed.set(ORIENTATION, placed.get(ORIENTATION).getOpposite());
			}
			placed.set(CONNECTION, connect.toProp());
		}
		return true;
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




	public static Direction getFacing(PlacedComponent placed) {
		Direction compFacing = switch (placed.get(ATrancieverComponent.ORIENTATION)) {
			case LEFT -> Direction.NORTH;
			case UP -> Direction.EAST;
			case RIGHT -> Direction.SOUTH;
			case DOWN -> Direction.WEST;
		};
		if (placed.getPos() == null) return compFacing;

		BlockState board = placed.getWorld().getBlockState(placed.getPos());
		Direction facing = board.getValue(CircuitBoardBlock.HORIZONTAL_FACING);


		Direction compFacingVertical = compFacing.getCounterClockWise(Axis.X);
		return switch (board.getValue(CircuitBoardBlock.ROTATION)) {
			case 0 -> switch(facing) {
				case NORTH -> compFacing;
				case EAST -> compFacing.getClockWise();
				case SOUTH -> compFacing.getOpposite();
				case WEST -> compFacing.getCounterClockWise();
				default -> null;
			};
			case 2 -> switch(facing) {
				case NORTH -> compFacing;
				case EAST -> compFacing.getCounterClockWise();
				case SOUTH -> compFacing.getOpposite();
				case WEST -> compFacing.getClockWise();
				default -> null;
			};
			case 1 -> switch(facing) {
				case NORTH -> compFacingVertical;
				case EAST -> compFacingVertical.getClockWise();
				case SOUTH -> compFacingVertical.getOpposite();
				case WEST -> compFacingVertical.getCounterClockWise();
				default -> null;
			};
			default -> null;
		};
	}
	@OnlyIn(Dist.CLIENT)
	public static int getComponentHeight(@NotNull PlacedComponent placed) {
		IF: if (placed.component instanceof IInteractableComponent interact) {
			VoxelShape shape = interact.getShape(placed);
			if (shape == null) break IF;
			AABB bounds = shape.bounds();
			return (int)((bounds.maxY - bounds.minY) * 16);
		}
		ResourceLocation modelId = placed.component.getModelId(placed);
		if (modelId == null) return 16;
        var manager = Minecraft.getInstance().getModelManager();
        BakedModel model = manager.getModel(ComponentModels.modelId(modelId));
		if (model == manager.getMissingModel()) return 16;
		List<BakedQuad> quads = model.getQuads(placed.getWorld().getBlockState(placed.getPos()), null, null, ModelData.EMPTY, RenderType.SOLID);
		int min = 16, max = 0;
		for (BakedQuad quad : quads) {
			BoundingBox bounds = new BakedQuadEditor(quad, DefaultVertexFormat.BLOCK).getBounds();
			if (bounds.minY() < min) min = bounds.minY();
			if (bounds.maxY() > max) max = bounds.maxY();
		}
		return min > max ? 16 : max-min;
	}
	public static List<ThermalUnit> getThermalUnits(@NotNull PlacedComponent placed) {
		if (!(placed instanceof PlacedComponentExt placedExt)) return null;
		return placedExt.getThermalUnits();
	}
	public static ThermalUnit getThermalUnit(@NotNull PlacedComponent placed) {
		if (!(placed instanceof PlacedComponentExt placedExt)) return null;
		List<ThermalUnit> units = placedExt.getThermalUnits();
		return units.isEmpty() ? null : units.getFirst();
	}
	public static Connection effectsThermalReading(PlacedComponent placed, PlacedComponent other) {
		if (other==placed) return Connection.NONE;
		Orientation variant = placed.get(ORIENTATION);
		Vector2ic norm = getNorm(variant);
		if (other.intersects(placed.x+norm.x(), placed.y+norm.y(),
				placed.footprint().getWidth(), placed.footprint().getHeight()))
			return Connection.FRONT;
		if (placed.get(FRONT_ONLY)) return Connection.NONE;
		if (other.intersects(placed.x-norm.x(), placed.y-norm.y(),
				placed.footprint().getWidth(), placed.footprint().getHeight()))
			return Connection.BACK;
		return Connection.NONE;
	}
	private static final Vector2ic UP = new Vector2i(1,0), DOWN = new Vector2i(-1,0),
		RIGHT = new Vector2i(0, 1), LEFT = new Vector2i(0,-1);
	public static Vector2ic getNorm(Orientation variant) {
		return switch (variant) {
			case RIGHT -> RIGHT;
			case DOWN -> DOWN;
			case LEFT -> LEFT;
			case UP -> UP;
		};
	}
	public static double calcResistance(float resistance, float temp) {
		//Math.exp
		return resistance * TMath.fastPow2(BETA * (1.0f / (temp + TEMP_C2K) - 1.0f / RESISTANCE_TEMP));
	}

	

	@Override
	public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
		if (placed==null) return FOOTPRINT;
		return (placed.get(FRONT_ONLY) ? FOOTPRINT_FACE : FOOTPRINT).rotated((Orientation)placed.get(ORIENTATION));
	}
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return ModModels.THI_TALL;
		return switch (Connection.fromProp(placed.get(CONNECTION))) {
			case NONE, INTERNAL -> (state.tall ? ModModels.THI_TALL : ModModels.THI_SHORT);
			case FRONT, BACK -> (state.tall ? ModModels.THI_TALL_C1 : ModModels.THI_SHORT_C1);
			case BOTH -> (state.tall ? ModModels.THI_TALL_C2 : ModModels.THI_SHORT_C2);
			case EXTERNAL -> ModModels.THI_TALL_EX;
		};
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			ModModels.THI_SHORT, ModModels.THI_SHORT_C1, ModModels.THI_SHORT_C2,
			ModModels.THI_TALL, ModModels.THI_TALL_C1, ModModels.THI_TALL_C2, ModModels.THI_TALL_EX
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
	protected static class StateClient {
		boolean firstTick = true;
		boolean tall = true;
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
