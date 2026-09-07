package dev.thingymabobs.blocks.switches;

import dev.thingymabobs.registry.ModBlockEntities;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
public abstract class SwitchBlock extends ElectricBlock implements IBE<SwitchBlockEntity>, IHaveElectricProperties {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;
    public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 2);

    protected float maxVoltage = 200f;
    protected boolean isButton = false;
	protected SwitchStates switchStates;
	//If the zero state is triggered with shift right click
	protected boolean zeroStateShift = false;
	protected int terminalCount;

    public SwitchBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(STATE, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATE, FACING, ROTATION);
    }

    public boolean isButton() {
        return isButton;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var terminal = terminalAt(state, hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
        if(terminal != null)
            return InteractionResult.PASS;
        if(!player.isShiftKeyDown()) {
            if(!AllItems.WRENCH.isIn(player.getItemInHand(hand))) {
                int switchState = state.getValue(STATE);
				boolean overflow = switchState+1 >= switchStates.states.length;
				final int nextSwitchState = overflow ? 1 : switchState+1;
				world.setBlockAndUpdate(pos, state.setValue(STATE, nextSwitchState));
				if(!world.isClientSide)
					withBlockEntityDo(world, pos, be -> be.setState(nextSwitchState));
				useSound(world, pos, overflow);
                return InteractionResult.SUCCESS;
            } else if (isButton) {
                if(!world.isClientSide)
                    withBlockEntityDo(world, pos, be -> {
                        be.setNormallyClosed(!be.isNormallyClosed());
                        be.setState(0);
                    });
                return InteractionResult.SUCCESS;
            }
        } else if (zeroStateShift && !AllItems.WRENCH.isIn(player.getItemInHand(hand))) {
			world.setBlockAndUpdate(pos, state.setValue(STATE, 0));
			if(!world.isClientSide)
				withBlockEntityDo(world, pos, be -> be.setState(0));
			useSound(world, pos, true);
			return InteractionResult.SUCCESS;
		}
        return InteractionResult.PASS;
    }

    abstract public void useSound(Level world, BlockPos pos, boolean open);

    @Override
    public Class<SwitchBlockEntity> getBlockEntityClass() {
        return SwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SwitchBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SWITCH.get();
    }

    public float getMaxVoltage() {
        return maxVoltage;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        Current.max(stack, player, tooltip);
        Voltage.max(maxVoltage, player, tooltip);
    }

    @Nullable
    public static Component overlayText(Player player) {
		if (!Minecraft.getInstance().level.isClientSide) return null;
        if(!AllItems.WRENCH.isIn(player.getItemInHand(InteractionHand.MAIN_HAND)))
            return null;
        HitResult hit = Minecraft.getInstance().hitResult;
        if(!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS)
            return null;
        var state = Minecraft.getInstance().level.getBlockState(blockHit.getBlockPos());
        if(!(state.getBlock() instanceof SwitchBlock switchBlock) || !switchBlock.isButton())
            return null;
        boolean nc = switchBlock.getBlockEntityOptional(Minecraft.getInstance().level, blockHit.getBlockPos())
                .map(SwitchBlockEntity::isNormallyClosed).orElse(false);
        return Lang.translate("gui.button_overlay.mode")
                .add(Lang.translate("gui.button_overlay." + (nc ? "nc" : "no"))
                        .style(ChatFormatting.BLUE))
                .style(ChatFormatting.GRAY)
                .component();
    }

    public static BlockStateTerminalCollection switchDownTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape downShape) {
        var shapers = new VoxelShaper[] {
                VoxelShaper.forDirectional(downShape, Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.EAST), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.SOUTH), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.WEST), Direction.DOWN)
        };
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals,
                        terminal -> {
                            var facing = state.getValue(FACING);
                            terminal = switch(facing) {
                                case DOWN -> terminal;
                                case UP -> terminal.rotateAroundX(180);
                                case EAST -> terminal.rotateAroundZ(-90);
                                case WEST -> terminal.rotateAroundZ(90);
                                case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                                case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                            };
                            int rotation = state.getValue(ROTATION);
                            if(facing == Direction.SOUTH) {
                                terminal = terminal.rotate(facing.getAxis(), -(90 * rotation - 90));
                            } else if(facing == Direction.EAST) {
                                terminal = terminal.rotate(facing.getAxis(), 180 - (90 * rotation - 90));
                            } else {
                                terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            }
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    int rotation = state.getValue(ROTATION);
                    if(facing.getAxis() == Direction.Axis.Y)
                        rotation = (rotation + 1) % 4;
                    return shapers[rotation].get(facing);
                })
                .build();
    }


    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        int rotation = 0;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            rotation = player.get2DDataValue() + 3;
        }

        if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
            rotation += 3;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ROTATION, rotation % 4);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
            return originalState.cycle(ROTATION);
        }
        return super.getRotatedBlockState(originalState, targetedFace);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

	public static class ShaperUtils extends VoxelShaper {
		private static final Function<Direction, Vec3> DIRECTION_VALUES = direction ->
				new Vec3(direction == Direction.UP ? 0.0 : (Direction.Plane.VERTICAL.test(direction) ? 180 : 90), -VoxelShaper.horizontalAngleFromDirection(direction), 0.0);

		public static VoxelShape rotate(VoxelShape shape, Direction from, Direction to) {
			return rotate(shape, from, to, DIRECTION_VALUES);
		}
	}
}
