package dev.thingymabobs.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector2ic;
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
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
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
import net.minecraft.resources.ResourceLocation;
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

		if (state.monitoring.size() == 0) return true;
		double temp = 0;
		for (ThermalUnit thermal : state.monitoring) {
			temp += thermal.getTemperature();
		}
		temp /= state.monitoring.size();

		state.thermal.setTemperature(state.ambient, (float)temp);
		updateThermals(state, (float)(state.thermal.power() + state.wire.power()));
		double resistance = calcResistance(
			placed.get(RESISTANCE), state.temperature);
		state.wire.setResistance(resistance);
		//Thingymabobs.LOGGER.info("Thermistor res: "+resistance+", temp: "+state.temperature+", temp other: "+temp);
		
		return true;
	}
	@OnlyIn(Dist.CLIENT)
	public boolean tickClient(@NotNull PlacedComponent placed) {
		int stateHash = getStateHash(placed);
		StateClient state;
		if (placed.customData instanceof StateClient state2) {
			state = state2;
			if (stateHash == state.lastStateHash) return true;
		} else {
			state = new StateClient();
			placed.customData = state;
		}
		state.lastStateHash = stateHash;
		updateConnectionsClient(placed);
		modelChanged(placed.getPos());
		return true;
	}



	public boolean updateConnections(@NotNull PlacedComponent placed) {
		if (placed.isClient())
			return updateConnectionsClient(placed);
		else
			return updateConnectionsServer(placed);
	}
	///TODO: Add side checking.  If on edge and facing out, try get thermals of adjasent block
	protected boolean updateConnectionsServer(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return false;
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) return false;

		state.monitoring.clear();
		Connection connect = Connection.NONE;
		Stream<PlacedComponent> search = board.getComponentsStream();
		for (var iter = search.iterator(); iter.hasNext(); ) {
			PlacedComponent other = iter.next();
			Connection con = effectsThermalReading(placed, other);
			if (con == Connection.NONE) continue;
			List<ThermalUnit> thermals = getThermalUnits(other);
			if (thermals.isEmpty()) continue;
			state.monitoring.addAll(thermals);
			connect = connect.combine(con);
		}
		if (connect == Connection.BACK) {
			placed.set(ORIENTATION, placed.get(ORIENTATION).getOpposite());
		}
		placed.set(CONNECTION, connect.toProp());
		return true;
	}
	protected boolean updateConnectionsClient(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return false;
		if (placed.get(CONNECTION) == Connection.EXTERNAL.ordinal()) {
			state.tall = true;
			return true;
		}
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board)) return false;

		Connection connect = Connection.NONE;
		state.tall = true;
		Stream<PlacedComponent> search = board.getComponentsStream();
		for (var iter = search.iterator(); iter.hasNext(); ) {
			PlacedComponent other = iter.next();
			Connection con = effectsThermalReading(placed, other);
			if (con == Connection.NONE) continue;
			List<ThermalUnit> thermals = getThermalUnits(other);
			if (thermals.isEmpty()) continue;
			connect = connect.combine(con);
			int height = getComponentHeight(other);
			if (height < 2) state.tall = false;
		}
		if (connect == Connection.BACK) {
			placed.set(ORIENTATION, placed.get(ORIENTATION).getOpposite());
		}
		placed.set(CONNECTION, connect.toProp());
		return true;
	}
	//Custom thermals just for resistance tracking.  Purpose is to shadow main theremal, but without extra dissipation factor
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
	public static int getStateHash(PlacedComponent placed) {
		if (!(placed.customData instanceof StateClient state)) return -10;
		return placed.get(ORIENTATION).ordinal() | (state.tall ? 1<<2 : 0) | (placed.get(CONNECTION) << 3);
	}
	public static double calcResistance(float resistance, float temp) {
		return resistance * TMath.fastLog2(BETA * (1.0f / (temp + 273.15f)) - 1.0f / RESISTANCE_TEMP);
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
		ArrayList<ThermalUnit> monitoring = new ArrayList<ThermalUnit>();
		boolean firstTick = true;
		ThermalElectricWire thermal;
		ElectricWire wire;
		float ambient = 25f;
		boolean tall = true;
		Connection connect;
		float temperature, mass, dissipation;
	}
	protected static class StateClient {
		int lastStateHash = -1;
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
