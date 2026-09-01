package com.feb.moregrid.blocks.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;

import com.feb.moregrid.client.CustomModelItemRenderer;
import com.feb.moregrid.registry.ModDataComponents;
import com.feb.moregrid.registry.ModModels;
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
    @Override
    public PartialModel getModel(ItemStack stack) {
        return stack.has(ModDataComponents.BAKED.get()) ? ModModels.PBA_MODEL_BAKED : ModModels.PBA_MODEL;
    }
    @Override
    public Item getUsedItem() {
        return Items.BONE_MEAL;
    }
    @Override
    public Item getReplaceItem() {
        return Items.POTATO;
    }
    @Override
    public Item getBakedItem() {
        return Items.BAKED_POTATO;
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
