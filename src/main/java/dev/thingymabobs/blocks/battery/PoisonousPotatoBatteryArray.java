package dev.thingymabobs.blocks.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import dev.thingymabobs.client.CustomModelItemRenderer;
import dev.thingymabobs.registry.ModDataComponents;
import dev.thingymabobs.registry.ModModels;
import com.mojang.datafixers.util.Unit;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class PoisonousPotatoBatteryArray extends APotatoBatteryArray implements CustomModelItemRenderer.Provider {
    public PoisonousPotatoBatteryArray(Properties settings) {
        super(settings);
    }
    @Override
    public PartialModel getModel(ItemStack stack) {
        return stack.has(ModDataComponents.BAKED.get()) ? ModModels.PPBA_MODEL_BAKED : ModModels.PPBA_MODEL;
    }
    @Override
    public BatterySpec getSpec() {
        return PotatoBatteryArrayEntity.SPEC_POISON;
    }
    @Override
    public Item getUsedItem() {
        return Items.BONE_MEAL;
    }
    @Override
    public Item getReplaceItem() {
        return Items.POISONOUS_POTATO;
    }
    @Override
    public Item getBakedItem() {
        return null;
    }
	
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
            if (!be.use(player, hand, stack, hitResult)) return ItemInteractionResult.FAIL;

            if(player.isCreative() && player.isShiftKeyDown())
                return ItemInteractionResult.CONSUME;
            else
                return ItemInteractionResult.SUCCESS;
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
