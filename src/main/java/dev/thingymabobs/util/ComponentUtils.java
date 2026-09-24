package dev.thingymabobs.util;

import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.thingymabobs.mixin.PlacedComponentExt;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class ComponentUtils {
	
	public static Direction getGlobalFacing(PlacedComponent placed) {
		Direction compFacing = switch (placed.get(Orientation.PROPERTY)) {
			case UP -> Direction.NORTH;
			case RIGHT -> Direction.EAST;
			case DOWN -> Direction.SOUTH;
			case LEFT -> Direction.WEST;
		};
		if (placed.getPos() == null) return compFacing;

		return getGlobalDirection(placed, compFacing);
	}
	public static Direction getGlobalFacingVertical(PlacedComponent placed) {
		boolean compVertical = placed.get(VerticallyOrientableComponent.VERTICAL);
		Direction compFacing = compVertical ?
			Direction.UP : switch (placed.get(Orientation.PROPERTY)) {
				case UP -> Direction.NORTH;
				case RIGHT -> Direction.EAST;
				case DOWN -> Direction.SOUTH;
				case LEFT -> Direction.WEST;
			};
		if (placed.getPos() == null) return compFacing;

		return getGlobalDirection(placed, compFacing);
	}
	public static Direction getGlobalDirection(PlacedComponent placed, Direction compFacing) {
		BlockState board = placed.getWorld().getBlockState(placed.getPos());
		Direction facing = board.getValue(CircuitBoardBlock.HORIZONTAL_FACING);

		Direction compFacingVertical = compFacing.getCounterClockWise(Axis.X);
		return switch (board.getValue(CircuitBoardBlock.ROTATION)) {
			case 0 -> compFacing.getAxis() == Axis.Y ? compFacing : switch(facing) {
				case NORTH -> compFacing;
				case EAST -> compFacing.getClockWise();
				case SOUTH -> compFacing.getOpposite();
				case WEST -> compFacing.getCounterClockWise();
				default -> null;
			};
			case 2 -> compFacing.getAxis() == Axis.Y ? compFacing : switch(facing) {
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


	public static Stream<PlacedComponent> getAllBoardComponents(@NotNull PlacedComponent placed) {
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board))
			return null;
		return board.getComponentsStream();
	}
	public static Stream<PlacedComponent> getAllOtherBoardComponents(@NotNull PlacedComponent placed) {
		if (!(placed.getWorld().getBlockEntity(placed.getPos()) instanceof CircuitBoardBlockEntity board))
			return null;
		return board.getComponentsStream().filter((other) -> other != placed);
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
	public static @Nullable AThermalBehaviour getThermalExternal(@NotNull PlacedComponent placed, Direction direction) {
		BlockPos pos = placed.getPos().relative(ComponentUtils.getGlobalDirection(placed, direction));
		return getThermalExternal(placed, pos);
	}
	public static @Nullable AThermalBehaviour getThermalExternal(@NotNull PlacedComponent placed, BlockPos pos) {
		AThermalBehaviour thermal = null;
		if (placed.getWorld().isLoaded(pos)) {
			BlockEntity be = placed.getWorld().getBlockEntity(pos);
			if (be != null)
				thermal = BlockEntityBehaviour.get(be, AThermalBehaviour.TYPE);
		}
		return thermal;
	}

	public static boolean isOnEdge(@NotNull PlacedComponent placed) {
		Orientation facing = placed.get(Orientation.PROPERTY);
		return isOnEdge(placed, facing);
	}
	public static boolean isOnEdge(@NotNull PlacedComponent placed, Orientation facing) {
		float w = placed.footprint().getWidth();
		float h = placed.footprint().getHeight();
		if (!(placed.x==0 && facing == Orientation.LEFT) && !(placed.x+w==16 && facing == Orientation.RIGHT)
			&&!(placed.y==0 && facing == Orientation.UP) && !(placed.y+h==16 && facing == Orientation.DOWN)) return false;
		return true;
	}
	public static boolean isTouchingInDirection(@NotNull PlacedComponent placed, Orientation facing, PlacedComponent other) {
		if (other == null) return false;
		Vector2ic norm = ComponentUtils.getLocalNorm(facing);
		if (other.intersects(placed.x+norm.x(), placed.y+norm.y(),
				placed.footprint().getWidth(), placed.footprint().getHeight()))
			return true;
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public static float getHeight(@NotNull PlacedComponent placed) {
		if (placed.component instanceof IInteractableComponent comp) {
			AABB bounds = comp.getShape(placed).bounds();
			return (float)(bounds.maxY - bounds.minY) * 16;
		}

		ResourceLocation modelId = placed.component.getModelId(placed);
		if (modelId == null) return 16;
        var manager = Minecraft.getInstance().getModelManager();
        BakedModel model = manager.getModel(ComponentModels.modelId(modelId));
		if (model == manager.getMissingModel()) return 16;
		List<BakedQuad> quads = model.getQuads(placed.getWorld().getBlockState(placed.getPos()), null, null, ModelData.EMPTY, RenderType.SOLID);
		double min = 16, max = 0;
		for (BakedQuad quad : quads) {
			AABB bounds = BakedQuadEditor.fromLinked(quad, DefaultVertexFormat.BLOCK).getBounds();
			if (bounds.minY < min) min = bounds.minY;
			if (bounds.maxY > max) max = bounds.maxY;
		}
		return (float)(min > max ? 16 : (max-min) * 16);
	}
	
	//private static final Vector2ic UP = new Vector2i(1,0), DOWN = new Vector2i(-1,0),
	//	RIGHT = new Vector2i(0, 1), LEFT = new Vector2i(0,-1);


	private static final Vector2ic UP = new Vector2i(0, -1), DOWN = new Vector2i(0, 1),
		RIGHT = new Vector2i(1, 0), LEFT = new Vector2i(-1, 0);
	public static Vector2ic getLocalNorm(Orientation variant) {
		return switch (variant) {
			case RIGHT -> RIGHT;
			case DOWN -> DOWN;
			case LEFT -> LEFT;
			case UP -> UP;
		};
	}
}
