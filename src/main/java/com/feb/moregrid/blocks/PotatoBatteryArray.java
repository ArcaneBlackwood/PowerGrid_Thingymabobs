package com.feb.moregrid.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;

import com.feb.moregrid.client.CustomModelItemRenderer;
import com.feb.moregrid.registry.ModDataComponents;
import com.mojang.datafixers.util.Unit;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class PotatoBatteryArray extends APotatoBatteryArray implements CustomModelItemRenderer.Provider {
    public static final BatterySpec BATTERY_SPEC = new SimpleBatterySpec(
		0.864f,
		0.864f,
		e -> 0.7f * e + 0.9f,
		e -> (float) Math.exp(9f - 8f * e) + 110
    );

    public PotatoBatteryArray(Properties settings) {
        super(settings);
    }

    @Override
    public BatterySpec getSpec() {
        return BATTERY_SPEC;
    }

    protected static final PartialModel MODEL = CustomModelItemRenderer.generateModel("block/battery/potato_battery_array_v");
    protected static final PartialModel MODEL_BAKED = CustomModelItemRenderer.generateModel("block/battery/baked_potato_battery_array_v");
    @Override
    public PartialModel getModel(ItemStack stack) {
        return stack.has(ModDataComponents.BAKED.get()) ? MODEL_BAKED : MODEL;
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
                    Component.translatable("moregrid.message.potato_battery.replace_required"),
                    true
            );
            return InteractionResult.FAIL;
        });
    }
	
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
					double usage = be.getEnergy() / be.getCapacity();
            boolean baked = state.getValue(BAKED).booleanValue();
            int usedPotatos = baked ? 8 : (int)(8.9999 - 8*usage);
			boolean hasItems = stack.is(Items.POTATO) && stack.getCount() >= usedPotatos;
			if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
				if(!player.isCreative() || !player.isShiftKeyDown()) {
					int processedPotatos = (int)(8.4999 - 8*usage);
					stack.shrink(usedPotatos);
					ItemStack potatos = null;
					if (baked)
						potatos = new ItemStack(Items.BAKED_POTATO, 8);
					else if (processedPotatos > 0)
						potatos = new ItemStack(Items.BONE_MEAL, processedPotatos);
					if (potatos != null)
						if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
				}
				be.resetState();
                if (be.getLevel() != null) {
                    be.getLevel().playSound(
                        null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
                }
				if(player.isCreative() && player.isShiftKeyDown())
					return ItemInteractionResult.CONSUME;
				else
					return ItemInteractionResult.SUCCESS;
			}
            player.displayClientMessage(
                    Component.translatable("moregrid.message.potato_battery.replace_required"),
                    true
            );
            return ItemInteractionResult.FAIL;
        });
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var stacks = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(be instanceof PotatoBatteryBlockEntity battery) {
            for(var stack : stacks) {
                if(stack.is(this.asItem())) {
                    stack.set(ModDataComponents.ENERGY, battery.getIndividualEnergy());
                    if (state.getValue(BAKED).booleanValue())
                        stack.set(ModDataComponents.BLOWN.get(), Unit.INSTANCE);
                }
            }
        }
        return stacks;
    }
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (itemStack.has(ModDataComponents.ENERGY)) {
            double energy = itemStack.get(ModDataComponents.ENERGY);
            this.withBlockEntityDo(world, pos, (be) -> {
                be.setEnergy(energy);
            });
        }
    }
	@Override
	public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
		var state = super.getStateForPlacement(ctx);
		if(state == null) return null;
        state.setValue(BAKED, ctx.getItemInHand().has(ModDataComponents.BAKED.get()));
		return state;
	}
}
