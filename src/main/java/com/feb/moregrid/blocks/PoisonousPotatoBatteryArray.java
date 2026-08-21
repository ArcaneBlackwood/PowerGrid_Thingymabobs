package com.feb.moregrid.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;

public class PoisonousPotatoBatteryArray extends APotatoBatteryArray {
    public static final BatterySpec BATTERY_SPEC = new SimpleBatterySpec(
		8.64f,
		8.64f,
		e -> 1.2f * e + 1.4f,
		e -> (float) Math.exp(7f - 8.5f * e) + 75
    );

    public PoisonousPotatoBatteryArray(Properties settings) {
        super(settings);
    }

    @Override
    public BatterySpec getSpec() {
        return BATTERY_SPEC;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUse(level, pos, be -> {
            if(be instanceof PotatoBatteryArrayEntity block) {
				if (player.isCreative() && player.isShiftKeyDown()) {
                	block.setEnergy(block.getCapacity());
					level.setBlockAndUpdate(pos, state.setValue(BAKED, false));
					if (!level.isClientSide) be.resetThermals();
					return InteractionResult.SUCCESS;
				}
            }
            return InteractionResult.FAIL;
        });
    }
	
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
            if(be instanceof PotatoBatteryArrayEntity block) {
				boolean hasItems = stack.is(Items.POISONOUS_POTATO) && stack.getCount() >= 8;
				if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
					if(!player.isCreative() || !player.isShiftKeyDown()) {
						double usage = block.getEnergy() / block.getCapacity();
						boolean baked = state.getValue(BAKED).booleanValue();
						int usedPotatos = baked ? 8 : (int)(8.9999 - 8*usage);
						int processedPotatos = (int)(8.4999 - 8*usage);
						stack.shrink(usedPotatos);
						ItemStack potatos = null;
						if (!baked && processedPotatos > 0)
							potatos = new ItemStack(Items.BONE_MEAL, processedPotatos);
						if (potatos != null)
							if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
					}
                	block.setEnergy(block.getCapacity());
					level.setBlockAndUpdate(pos, state.setValue(BAKED, false));
					if (!level.isClientSide) be.resetThermals();
					if(player.isCreative() && player.isShiftKeyDown())
						return ItemInteractionResult.CONSUME;
					else
						return ItemInteractionResult.SUCCESS;
				}
            }
            return ItemInteractionResult.FAIL;
        });
    }

}
