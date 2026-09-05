package com.feb.moregrid.blocks.electricfurnace;

import com.feb.moregrid.blocks.PowerShunt;
import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModDataComponents;
import com.feb.moregrid.registry.ModLang;
import com.feb.moregrid.util.IDirectionSocketElectric;
import com.mojang.datafixers.util.Unit;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;
import java.util.List;

public class ElectricFurnace extends HorizontalElectricBlock implements IBE<ElectricFurnaceEntity>, IDirectionSocketElectric, IHaveElectricProperties {
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 2);
	public static final BooleanProperty BUTTON = BooleanProperty.create("button");
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
    
	private static final VoxelShape[] SHAPE = new VoxelShape[4];
	private static final AABB[] SHAPE_BUTTON = new AABB[4];
	static {
		final int n = Direction.NORTH.get2DDataValue(), e = Direction.EAST.get2DDataValue(),
			s = Direction.SOUTH.get2DDataValue(), w = Direction.WEST.get2DDataValue();
		float B = 1/16f;
		SHAPE_BUTTON[n] = new AABB(10*B, 0*B, -1*B, 14*B, 4*B, 0*B );
		SHAPE_BUTTON[e] = new AABB(16*B, 0*B, 10*B, 17*B, 4*B, 14*B);
		SHAPE_BUTTON[s] = new AABB(2*B,  0*B, 16*B, 6*B,  4*B, 17*B);
		SHAPE_BUTTON[w] = new AABB(-1*B, 0*B, 2*B,  0*B,  4*B, 6*B );
		SHAPE[n] = Shapes.join(Shapes.create(SHAPE_BUTTON[n]), box(2, 0, 0, 14, 16, 16), BooleanOp.OR);
		SHAPE[e] = Shapes.join(Shapes.create(SHAPE_BUTTON[e]), box(0, 0, 2, 16, 16, 14), BooleanOp.OR);
		SHAPE[s] = Shapes.join(Shapes.create(SHAPE_BUTTON[s]), box(2, 0, 0, 14, 16, 16), BooleanOp.OR);
		SHAPE[w] = Shapes.join(Shapes.create(SHAPE_BUTTON[w]), box(0, 0, 2, 16, 16, 14), BooleanOp.OR);
	}

	public ElectricFurnace(Properties settings) {
		super(settings);
		registerDefaultState(defaultBlockState()
			.setValue(BLOWN, false)
			.setValue(ROTATION, 0)
			.setValue(BUTTON, false));
	}
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE[state.getValue(HORIZONTAL_FACING).get2DDataValue()];
    }
    public static AABB getButtonShape(BlockState state) {
        return SHAPE_BUTTON[state.getValue(HORIZONTAL_FACING).get2DDataValue()];
    }
    @Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BLOWN, ROTATION, BUTTON);
	}
    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        ModLang.translate("tooltip.temperature.max").style(ChatFormatting.GRAY).addTo(tooltip);
        ModLang.builder()
            .add(Component.nullToEmpty(" ")).add(ModLang.number(ElectricFurnaceEntity.OVERHEAT_TEMPERATURE))
            .add(Component.nullToEmpty(" ")).add(org.patryk3211.powergrid.utility.Unit.TEMPERATURE.get())
			.style(ChatFormatting.DARK_BLUE).addTo(tooltip);
		Power.max(ElectricFurnaceEntity.BLOW_POWER, player, tooltip);
		if (stack.getItem() instanceof BlockItem blockItem) {
			float resistance = ResistanceValues.get(blockItem.getBlock());
			Resistance.coil(resistance, player, tooltip);
			float voltage = Mth.sqrt(ElectricFurnaceEntity.MAX_POWER * resistance);
			ModLang.translate("tooltip.voltage.rated_for",
					ModLang.number(ElectricFurnaceEntity.MAX_TEMPERATURE).text(" ")
					.add(org.patryk3211.powergrid.utility.Unit.TEMPERATURE.get()).string())
				.style(ChatFormatting.GRAY).addTo(tooltip);
			ModLang.builder()
				.add(Component.nullToEmpty(" ")).add(ModLang.number(voltage))
				.add(Component.nullToEmpty(" ")).add(org.patryk3211.powergrid.utility.Unit.VOLTAGE.get())
				.style(ChatFormatting.DARK_AQUA).addTo(tooltip);
		}
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
			return be.useItemOn(player, hitResult, hand, stack) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        });
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> {
            return be.useItemOn(player, hitResult, InteractionHand.MAIN_HAND, ItemStack.EMPTY) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
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
