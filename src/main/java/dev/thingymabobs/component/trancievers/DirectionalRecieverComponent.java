package dev.thingymabobs.component.trancievers;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import dev.thingymabobs.mixin.LinkBehaviourExt;
import dev.thingymabobs.registry.ModModels;
import com.google.common.collect.ImmutableCollection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DirectionalRecieverComponent extends AVertTrancieverComponent {
	protected Vec3 facing;
	public DirectionalRecieverComponent() {
		super();
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		properties.add(LABEL, PROP_RECIEVE_RESISTANCE_MIN, PROP_RECIEVE_RESISTANCE_MAX);
		super.addProperties(properties);
	}
	@Override
	protected boolean isTransmitter() {
		return false;
	}

	
	@Override
	protected void updateState(PlacedComponent placed, ServerState state, float currentSignal) {
		state.signalWire.setResistance(getSignalResistance(placed, currentSignal));
	}
	@Override
	protected float getSignalResistance(PlacedComponent placed, float signalValue) {
		return Mth.lerp(signalValue / 15f, placed.get(PROP_RECIEVE_RESISTANCE_MAX), placed.get(PROP_RECIEVE_RESISTANCE_MIN));
	}

	@Override
	protected boolean setupLink(PlacedComponent placed, State state) {
		boolean result = super.setupLink(placed, state);
		if (placed.customData == null) return result;
		State state2 = (State)placed.customData;
		if(state2.link == null || state2.link.link == null) return true;
		facing =  Vec3.atLowerCornerOf(getFacing(placed).getNormal());
		((LinkBehaviourExt)state2.link.link).setTransformer((level, self, other) -> {
			float dot = (float)this.facing.dot(self.getLocation().getCenter().subtract(other.getLocation().getCenter()).normalize());
			return level * Mth.clamp(dot, 0, 1);
		});
		return result;
	}
	@Override
	public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
		boolean result = super.rotate(placed, counterClockwise);
		facing = Vec3.atLowerCornerOf(getFacing(placed).getNormal());
		return result;
	}
	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		super.stateUpdated(placed);
		if (placed.getPos() == null || !placed.getWorld().getBlockState(placed.getPos()).is(ModdedBlocks.CIRCUIT_BOARD)) return;
		facing = Vec3.atLowerCornerOf(getFacing(placed).getNormal());
	}
	public static Direction getFacing(PlacedComponent placed) {
		boolean compVertical = placed.get(AVertTrancieverComponent.VERTICAL);
		Direction compFacing = compVertical ?
			Direction.UP : switch (placed.get(ATrancieverComponent.ORIENTATION)) {
				case UP -> Direction.NORTH;
				case RIGHT -> Direction.EAST;
				case DOWN -> Direction.SOUTH;
				case LEFT -> Direction.WEST;
			};
		if (placed.getPos() == null) return compFacing;

		BlockState board = placed.getWorld().getBlockState(placed.getPos());
		Direction facing = board.getValue(CircuitBoardBlock.HORIZONTAL_FACING);


		Direction compFacingVertical = compFacing.getCounterClockWise(Axis.X);
		return switch (board.getValue(CircuitBoardBlock.ROTATION)) {
			case 0 -> compVertical ? compFacing : switch(facing) {
				case NORTH -> compFacing;
				case EAST -> compFacing.getClockWise();
				case SOUTH -> compVertical ? compFacing : compFacing.getOpposite();
				case WEST -> compFacing.getCounterClockWise();
				default -> null;
			};
			case 2 -> compVertical ? compFacing.getOpposite() : switch(facing) {
				case NORTH -> compFacing;
				case EAST -> compFacing.getCounterClockWise();
				case SOUTH -> compFacing.getOpposite();
				case WEST -> compFacing.getClockWise();
				default -> null;
			};
			case 1 -> compFacingVertical.getAxis() == Axis.Y ? compFacingVertical : switch(facing) {
				case NORTH -> compFacingVertical;
				case EAST -> compFacingVertical.getClockWise();
				case SOUTH -> compFacingVertical.getOpposite();
				case WEST -> compFacingVertical.getCounterClockWise();
				default -> null;
			};
			default -> null;
		};
	}


	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return placed.get(VERTICAL) ? ModModels.DIRECT_BASE_V : ModModels.DIRECT_BASE_H;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(ModModels.DIRECT_BASE_V, ModModels.DIRECT_BASE_H);
	}
	@Override
	protected PartialModel getRenderModel(PlacedComponent placed) {
		return placed.get(VERTICAL) ? ModModels.DIRECT_ANTENNA_V : ModModels.DIRECT_ANTENNA_H;
	}

}
