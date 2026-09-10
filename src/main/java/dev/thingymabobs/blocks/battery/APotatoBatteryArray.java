package dev.thingymabobs.blocks.battery;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.battery.AbstractBatteryBlock;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.utility.Lang;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModDataComponents;

@MethodsReturnNonnullByDefault
public abstract class APotatoBatteryArray extends AbstractBatteryBlock<PotatoBatteryArrayEntity> implements IHaveElectricProperties, IPotatoBattery {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;
    public static final BooleanProperty BAKED = BooleanProperty.create("baked");

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 1, 6, 6.5, 4, 9, 9.3)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 12, 6, 6.5, 15, 9, 9.3)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_NORTH = box(1, 0, 1, 15, 7, 15);

    public APotatoBatteryArray(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BAKED, false));
        setTerminalCollection(switchDownTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATION, BAKED);
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
    public Class<PotatoBatteryArrayEntity> getBlockEntityClass() {
        return PotatoBatteryArrayEntity.class;
    }
    @Override
    public BlockEntityType<? extends PotatoBatteryArrayEntity> getBlockEntityType() {
        return ModBlockEntities.POTATO_BATTERY_ARRAY.get();
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> {
            if (!be.use(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hitResult)) return InteractionResult.FAIL;

            return InteractionResult.SUCCESS;
        });
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUse(level, pos, be -> {
			if (player.isCreative() && player.isShiftKeyDown()) {
				be.resetState();
                if (be.getLevel() != null) {
                    be.getLevel().playSound(
                        null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
                }
				return InteractionResult.SUCCESS;
			}
            player.displayClientMessage(
                    Component.translatable("thingymabobs.message.poisonous_potato_battery.replace_required"),
                    true
            );
            return InteractionResult.FAIL;
        });
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

	public static class ShaperUtils extends VoxelShaper {
		private static final Function<Direction, Vec3> DIRECTION_VALUES = direction ->
				new Vec3(direction == Direction.UP ? 0.0 : (Direction.Plane.VERTICAL.test(direction) ? 180 : 90), -VoxelShaper.horizontalAngleFromDirection(direction), 0.0);

		public static VoxelShape rotate(VoxelShape shape, Direction from, Direction to) {
			return rotate(shape, from, to, DIRECTION_VALUES);
		}
	}
    
    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Voltage.max(getSpec().calculateVoltage(1), player, tooltip);
        Power.max(stack, player, tooltip);
        float charge;
        float maxCharge = getSpec().getMaxCharge();
        if(stack.has(ModDataComponents.ENERGY)) {
            charge = (float) (stack.get(ModDataComponents.ENERGY) / maxCharge);
        } else {
            charge = getSpec().getInitialCharge() / maxCharge;
        }
        Lang.translate("tooltip.charge.current")
                .style(ChatFormatting.GRAY).addTo(tooltip);
        Lang.builder()
                .add(Component.literal(" "))
                .add(Lang.numberConstant(charge * 100))
                .add(Component.literal("%"))
                .style(ChatFormatting.AQUA).addTo(tooltip);
        Lang.translate("tooltip.capacity")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);
        Lang.builder()
                .add(Component.literal(" "))
                .add(Lang.numberConstant(maxCharge / 3.6f))
                .add(Component.literal(" m"))
                .add(org.patryk3211.powergrid.utility.Unit.ENERGY.get())
                .style(ChatFormatting.GREEN)
                .addTo(tooltip);
    }
}
