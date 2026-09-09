package dev.thingymabobs.blocks.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.battery.AbstractBatteryBlock;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModBlockEntities;

public class PoisonousPotatoBattery extends AbstractBatteryBlock<PoisonousPotatoBatteryEntity> {
    public static final String CONFIG_RECHARGE = "recharge_power";

    public static float getRecharge(CProperties.Prop prop) {
        return prop.getFloat(CONFIG_RECHARGE).get();
    }

    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty BAKED = PotatoBatteryBlock.BAKED;

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 7, 3, 4.5, 9, 5.5, 6)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 7, 3, 10, 9, 5.5, 11.5)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_NORTH = box(6, 0, 5, 10, 3, 11);

    public PoisonousPotatoBattery(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BAKED, false));
        setTerminalCollection(HorizontalElectricBlock.horizontalNorthTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, BAKED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ? ctx.getHorizontalDirection() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(HORIZONTAL_FACING, player.getClockWise());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return canSupportCenter(world, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BatterySpec getSpec() {
        return PoisonousPotatoBatteryEntity.SPEC;
    }

    @Override
    public Class<PoisonousPotatoBatteryEntity> getBlockEntityClass() {
        return PoisonousPotatoBatteryEntity.class;
    }

    @Override
    public BlockEntityType<? extends PoisonousPotatoBatteryEntity> getBlockEntityType() {
        return ModBlockEntities.POTATO_BATTERY.get();
    }
}
