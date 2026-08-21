package com.feb.moregrid.blocks;

import com.feb.moregrid.client.CustomModelItemRenderer;
import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModDataComponents;
import com.mojang.datafixers.util.Unit;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;

import java.util.List;

public class PowerShunt extends SurfaceElectricBlock implements IBE<PowerShuntEntity>, IHaveElectricProperties, CustomModelItemRenderer.Provider {
	public static final BooleanProperty BLOWN = BooleanProperty.create("blown");
	private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
		new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 3, 0,  9.5, 6, 2),
		new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 3, 14, 9.5, 6, 16)
	};

	private static final VoxelShape SHAPE1 = box(4, 0, 2, 12, 8, 14);
	private static final VoxelShape SHAPE2 = box(2, 0, 4, 14, 8, 12);

	public PowerShunt(Properties settings) {
		super(settings);
		registerDefaultState(defaultBlockState().setValue(BLOWN, false));
		setTerminalCollection(surfaceTerminals(this, TERMINALS, SHAPE1, SHAPE2));
	}

    protected static final PartialModel MODEL = CustomModelItemRenderer.generateModel("block/shunt_v");
    protected static final PartialModel MODEL_BLOWN = CustomModelItemRenderer.generateModel("block/shunt_blown_v");
    @Override
    public PartialModel getModel(ItemStack stack) {
        return stack.has(ModDataComponents.BLOWN.get()) ? MODEL_BLOWN : MODEL;
    }

    @Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BLOWN);
	}

	@Override
	public Class<PowerShuntEntity> getBlockEntityClass() {
		return PowerShuntEntity.class;
	}

	@Override
	public BlockEntityType<? extends PowerShuntEntity> getBlockEntityType() {
		return ModBlockEntities.POWER_SHUNT.get();
	}

	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		Power.max(stack, player, tooltip);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
            boolean hasItems = stack.is(AllItems.BRASS_SHEET) && stack.getCount() >= 1;
            if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
                if(!player.isCreative() || !player.isShiftKeyDown()) {
                    stack.shrink(1);
                }
                be.resetState();
                if(player.isCreative() && player.isShiftKeyDown())
                    return ItemInteractionResult.CONSUME;
                else
                    return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, be -> {
            if (player.isShiftKeyDown() && player.isCreative()) {
                be.resetState();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
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
		return state.cycle(ALONG_FIRST_AXIS);
	}
}
