package com.feb.moregrid.blocks.electricfurnace;

import com.feb.moregrid.blocks.PowerShunt;
import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModDataComponents;
import com.feb.moregrid.util.IDirectionSocketElectric;
import com.mojang.datafixers.util.Unit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;

import java.util.List;

public class ElectricFurnace extends HorizontalElectricBlock implements IBE<ElectricFurnaceEntity>, IDirectionSocketElectric, IHaveElectricProperties {
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 2);
	public static final BooleanProperty BUTTON = BooleanProperty.create("button");
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");
	public static final BooleanProperty BLOWN = PowerShunt.BLOWN;

	private static final TerminalBoundingBox SOCKET_LEFT =
		new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 14, 1, 8, 15, 7, 14); //withOrigin(8f, 1.5f, 6f);
	private static final TerminalBoundingBox SOCKET_TOP =
		new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 5, 16, 8, 11, 17, 14);
	private static final TerminalBoundingBox SOCKET_RIGHT = 
		new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 1, 1, 8, 2, 7, 14);
	
	private static final TerminalBoundingBox[][] sockets = new TerminalBoundingBox[4][3];
	static {
		for (int face = 0; face < 4; face++) for (int rot = 0; rot < 3; rot++) {
			//weird face rotation fix due to inconsistency between Direction and Rotation (North = rotate none)
			sockets[face < 2 ? face + 2 : face - 2][rot] = (switch(rot) {
				case 0 -> SOCKET_LEFT;
				case 1 -> SOCKET_TOP;
				default -> SOCKET_RIGHT;
			}).rotateAroundY(Rotation.values()[face]);
		}
	}
    

	private static final VoxelShape SHAPE_NS = box(2, 0, 0, 14, 16, 16);
	private static final VoxelShape SHAPE_EW = box(0, 0, 2, 16, 16, 14);

	public ElectricFurnace(Properties settings) {
		super(settings);
		registerDefaultState(defaultBlockState()
			.setValue(BLOWN, false)
			.setValue(ROTATION, 0)
			.setValue(BUTTON, false)
			.setValue(POWERED, false));
	}
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(HORIZONTAL_FACING).getAxis() == Axis.Z ? SHAPE_NS : SHAPE_EW;
    }
    @Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BLOWN, ROTATION, BUTTON, POWERED);
	}
    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Power.max(stack, player, tooltip);
    }
	@Override
	protected RenderShape getRenderShape(BlockState p_60550_) {
		return RenderShape.MODEL;
	}


	@Override
	public ITerminalPlacement socket(BlockState state) {
		return sockets[state.getValue(HORIZONTAL_FACING).get2DDataValue()]
			[state.getValue(ROTATION)];
	}
	@Override
	public Direction socketFacing(BlockState current) {
		Direction facing = switch (current.getValue(ROTATION)) {
			case 0 -> Direction.WEST;
			case 1 -> Direction.DOWN;
			default -> Direction.EAST;
		};
		if (facing.getAxis() != Axis.Y)
			facing = switch (current.getValue(HORIZONTAL_FACING)) {
				case EAST -> facing.getClockWise();
				case SOUTH -> facing.getOpposite();
				case WEST -> facing.getCounterClockWise();
				default -> facing;
			};
		return facing;
	}
	@Override
	public boolean socketValid(BlockState current, BlockState madeWith) {
		return current.getValue(HORIZONTAL_FACING) == madeWith.getValue(HORIZONTAL_FACING)
			&& current.getValue(ROTATION) == madeWith.getValue(ROTATION);
	}


	@Override
	public Class<ElectricFurnaceEntity> getBlockEntityClass() {
		return ElectricFurnaceEntity.class;
	}
	@Override
	public BlockEntityType<? extends ElectricFurnaceEntity> getBlockEntityType() {
		return ModBlockEntities.ELECTRIC_FURNACE.get();
	}
    

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
			return be.useItemOn(player, hitResult, hand, stack) ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        });
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> {
            return be.useItemOn(player, hitResult, InteractionHand.MAIN_HAND, ItemStack.EMPTY) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        });
    }


    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var stacks = super.getDrops(state, builder);
        for(var stack : stacks) {
            if(stack.is(this.asItem())) {
                if (state.getValue(BLOWN).booleanValue())
                    stack.set(ModDataComponents.BLOWN.get(), Unit.INSTANCE);
            }
        }
        return stacks;
    }
	@Override
	public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
		var state = super.getStateForPlacement(ctx);
		if(state == null) return null;
        state = state.setValue(BLOWN, ctx.getItemInHand().has(ModDataComponents.BLOWN.get()));
		return state;
	}
	
    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        if (targetedFace.getAxis() == Axis.Y)
			return rotate(state, Rotation.CLOCKWISE_90);
		Direction facing = state.getValue(HORIZONTAL_FACING);
		if (targetedFace.getAxis() == facing.getAxis())
			return state.setValue(ROTATION, (state.getValue(ROTATION) + 1) % 3) ;
		return mirror(state, targetedFace.getAxis() == Axis.Z ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT);
    }
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
		int rotation = state.getValue(ROTATION);
		return super.mirror(state, mirrorIn)
			.setValue(ROTATION, rotation == 1 ? 1 : 2 - rotation);
    }
}
