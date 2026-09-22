package dev.thingymabobs.component.heatsink;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder.IEmitter;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import com.google.common.collect.ImmutableCollection.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.IDestroyComponent;
import dev.thingymabobs.mixin.ThermalEffector;
import dev.thingymabobs.mixin.ThermalExt;
import dev.thingymabobs.util.ComponentUtils;
import dev.thingymabobs.util.ThermalElectricWire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class AHeatSink extends OrientableComponent implements IRenderedComponent, IDestroyComponent {
	protected CProperties.PropDevice CONFIG = null;
	public void configUpdated(CProperties.PropDevice prop) {
		CONFIG = prop;
	}

	public static final IntProperty CONNECTION = new IntProperty(Thingymabobs.MOD_ID, "heatsync.connect", 0, 0, 3).hidden().cast();

	
	public AHeatSink(ComponentFootprint footprint) {
		super(footprint);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull IEmitter thermal) {
		State state = new State();
		placed.customData = state;
		var therm = CONFIG.getThermal();
		state.wire = new ThermalElectricWire(0);
		state.isExternal = ComponentUtils.isOnEdge(placed);
		therm.apply(thermal, state.isExternal ? HeatSinkConfig.getExternalMul() : 1f)
			.addHeatSource(state.wire);
	}
	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(CONNECTION);
	}

	public abstract int getHeight();
	


	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		StateServer state;
		if (placed.customData instanceof StateServer state2) state = state2;
		else if (placed.customData instanceof State stateCommon) {
			connectHeatsync(placed, stateCommon);
			if (placed.isClient()) {
				StateClient stateClient = new StateClient(stateCommon);
				placed.customData = stateClient;
				stateClient.units = null;
				updateThermalPadMesh(placed, stateClient);
				placed.notifyClients(CONNECTION);
				return false;
			} else {
				state = new StateServer(stateCommon);
				state.connections = null;
				state.effector = new ThermalSink(placed);
				for (var unit : state.units)
					unit.addEffector(state.effector);
				placed.customData = state;
			}
		} else return true;
		
		if (state.syncBlockPos != null) {
			if (!state.units.isEmpty() && state.units.getFirst().isRemoved()) state.units.clear();
			if (state.units.isEmpty()) {
				@Nullable AThermalBehaviour thermal = ComponentUtils.getThermalExternal(placed, state.syncBlockPos);
				if (thermal instanceof ThermalExt ext)
					state.units.add(ext);
			}
			int connect = (state.units.isEmpty() ? Connection.EXTERNAL : Connection.EXTERNAL_ACTIVE).ordinal();
			if (placed.get(CONNECTION) != connect) {
				placed.set(CONNECTION, connect);
				placed.notifyClients(CONNECTION);
			}
		}
		state.wire.setPower(state.nextPower);
		state.nextPower = 0;
		return true;
	}
	@Override
	public void onDestroy(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof StateServer state)) return;
		for (var unit : state.units)
			unit.removeEffector(state.effector);
	}


	public static class ThermalSink implements ThermalEffector {
		PlacedComponent placed;
		ThermalExt thisThermal;
		public ThermalSink(PlacedComponent placed) {
			thisThermal = (ThermalExt)ComponentUtils.getThermalUnit(placed);
			this.placed = placed;
		}
		@Override
		public boolean effect(ThermalExt unit) {
			if (!(placed.customData instanceof StateServer state)) return false;
			BlockEntity be = placed.getWorld().getBlockEntity(placed.getPos());
			if (!(be instanceof CircuitBoardBlockEntity board)) return false;
			if (board.getComponentsStream().noneMatch((other -> other==placed))) return false;
			if (unit.isBlown() || thisThermal.isBlown()) return true;

			float thisTemp = thisThermal.getTemperature();

			float conduction = (Float)unit.getCustomData();
			float powerTransfer = (unit.getTemperature() - thisTemp) * conduction;
			float unitTemp = unit.getTemperature() - powerTransfer / 20 / unit.getMass();
			unit.setTemperature(unitTemp);
			state.nextPower += powerTransfer;
			return true;
		}
	}



	protected boolean connectHeatsync(@NotNull PlacedComponent placed, State state) {
		Connection connect = Connection.NONE;
		state.units.clear();
		state.syncBlockPos = null;
		if (state.isExternal)
			connect = connectHeatsyncExternal(placed, state);
		else
			connect = connectHeatsyncInternal(placed, state);
		placed.set(CONNECTION, connect.toProp());
		placed.notifyClients(CONNECTION);
		placed.notifyClients(ORIENTATION);
		return connect != Connection.INTERNAL;
	}
	protected Connection connectHeatsyncExternal(@NotNull PlacedComponent placed, State state) {
		Direction direction = ComponentUtils.getGlobalFacing(placed);
		state.syncBlockPos = placed.getPos().relative(direction);
		@Nullable AThermalBehaviour thermal = ComponentUtils.getThermalExternal(placed, direction);
		if (thermal instanceof ThermalExt ext) {
			ResourceLocation id = BlockEntityType.getKey(thermal.blockEntity.getType());
			ext.setCustomData(HeatSinkConfig.get(id));
			state.units.add(ext);
			return Connection.EXTERNAL_ACTIVE;
		}
		return Connection.EXTERNAL;
	}
	protected Connection connectHeatsyncInternal(@NotNull PlacedComponent placed, State state) {
		Stream<PlacedComponent> search = ComponentUtils.getAllOtherBoardComponents(placed);
		if (search == null) return Connection.INTERNAL;
		int connections = 0;
		state.connections.clear();
		for (var iter = search.iterator(); iter.hasNext(); ) {
			PlacedComponent other = iter.next();
			boolean con = isTouching(placed, other);
			if (!con) continue;

			List<ThermalUnit> thermals = ComponentUtils.getThermalUnits(other);
			if (thermals.isEmpty()) continue;
			ResourceLocation id = ComponentRegistry.getId(other.component);
			for (ThermalUnit unit : thermals) {
				if (unit instanceof ThermalExt ext) {
					ext.setCustomData(HeatSinkConfig.get(id));
					state.units.add(ext);
				}
			}
			state.connections.add(other);
			connections++;
		}
		if (connections == 0) return Connection.NONE;
		return Connection.INTERNAL;
	}


	public void updateThermalPadMesh(@NotNull PlacedComponent placed) {
		if (!placed.isClient()) return;
		if (!(placed.customData instanceof StateClient state)) return;
		updateThermalPadMesh(placed, state);
	}
	@OnlyIn(Dist.CLIENT)
	protected void updateThermalPadMesh(@NotNull PlacedComponent placed, StateClient state) {
		if (state.syncBlockPos == null) {
			state.model = new HeatSinkPad();
			var orientation = placed.get(ORIENTATION);
			boolean horiz = orientation.isX();
			boolean axisNeg = orientation == Orientation.LEFT || orientation == Orientation.DOWN;
			float depth = footprint(placed).originalWidth;
			int heightMax = getHeight();
			int widthMax = footprint(placed).originalHeight;
			for (PlacedComponent other : state.connections) {
				float height = ComponentUtils.getHeight(other);
				if (height > heightMax) height = heightMax;
				ComponentFootprint footprint = other.footprint();
				int width = horiz ? footprint.getHeight() : footprint.getWidth();
				int offset = horiz ? other.y - placed.y: other.x - placed.x;
				if (offset < 0) {
					width += offset;
					offset = 0;
				}
				if (width+offset > widthMax)
					width = widthMax - offset;
				if (axisNeg)
					offset = widthMax - offset - width;
				state.model.addPad(depth, offset, 0, width, height);
			}
		} else {
			state.model = null;
		}
	}


	public static boolean isTouching(PlacedComponent placed, PlacedComponent other) {
		Orientation facing = placed.get(ORIENTATION);
		return ComponentUtils.isTouchingInDirection(placed, facing, other);
	}


	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CircuitBoardBlockEntity board, PlacedComponent placed, float partialTicks, PoseStack pose,
			MultiBufferSource buffer, int light, int overlay) {
		if (!(placed.customData instanceof StateClient state)) return;
		if (state.model == null) return;

		pose.pushPose();

		var orientation = placed.get(Orientation.PROPERTY);
		var footprint = placed.component.footprint(placed);
		switch (orientation) {
			case DOWN -> pose.translate(footprint.getOriginalHeight() / 16f, 0, 0);
			case LEFT -> pose.translate(footprint.getOriginalWidth() / 16f, 0, footprint.getOriginalHeight() / 16f);
			case UP -> pose.translate(0, 0, footprint.getOriginalWidth() / 16f);
			case RIGHT -> {}
		}
		pose.mulPose(Axis.YP.rotation(orientation.ordinal() * (float) Math.PI * 0.5f));
		state.model.render(pose, buffer, RenderType.SOLID, light, overlay);

		pose.popPose();
	}



	protected static class State {
		ThermalElectricWire wire;
		BlockPos syncBlockPos;
		ArrayList<ThermalExt> units = new ArrayList<>();
		ArrayList<PlacedComponent> connections = new ArrayList<>();
		boolean isExternal;
	}
	protected static class StateServer extends State {
		ThermalSink effector;
		float nextPower = 0;
		public StateServer(State s) {
			wire = s.wire;
			syncBlockPos = s.syncBlockPos;
			units = s.units;
			connections = s.connections;
			isExternal = s.isExternal;
		}
	}
	protected static class StateClient extends State {
		public StateClient(State s) {
			wire = s.wire;
			syncBlockPos = s.syncBlockPos;
			units = s.units;
			connections = s.connections;
			isExternal = s.isExternal;
		}
		HeatSinkPad model;
	}
	public static enum Connection {
		NONE, INTERNAL, EXTERNAL, EXTERNAL_ACTIVE;
		public static Connection fromProp(int index) {
			return values()[index];
		}
		public int toProp() {
			return ordinal();
		}
	}
}
