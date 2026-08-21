package com.feb.moregrid.blocks;

import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModBlocks;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.battery.CustomConnectivityHandler;

public class PotatoBatteryBlockItem extends BlockItem {
    public static final Function<Item.Properties, BlockItem> CONSTRUCTOR = 
        new Function<Item.Properties, BlockItem>() {
            @Override
            public BlockItem apply(Item.Properties props) {
                return new PotatoBatteryBlockItem(ModBlocks.POTATO_BATTERY_BLOCK.get(), props);
            }
        };
    
    public PotatoBatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        InteractionResult initialResult = super.place(ctx);
        if (!initialResult.consumesAction())
            return initialResult;
        tryMultiPlace(ctx);
        return initialResult;
    }

    private void tryMultiPlace(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null)
            return;
        if (player.isShiftKeyDown())
            return;
        Direction face = ctx.getClickedFace();
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = world.getBlockState(placedOnPos);
        if (!(placedOnState.getBlock() instanceof PotatoBatteryBlock)) return;
        BlockEntity be = world.getBlockEntity(placedOnPos);
        if (!(be instanceof PotatoBatteryBlockEntity)) return;
        if (!BatteryBlock.isBattery(placedOnState)) return;
        if (SymmetryWandItem.presentInHotbar(player)) return;
        
        PotatoBatteryBlockEntity batteryAt = CustomConnectivityHandler.partAt(ModBlockEntities.POTATO_BATTERY_BLOCK.get(),
                ((PotatoBatteryBlockEntity) be).getSpec(), world, placedOnPos);
        if (batteryAt == null) return;
        PotatoBatteryBlockEntity controllerBE = (PotatoBatteryBlockEntity)batteryAt.getControllerBE();
        if (controllerBE == null) return;

        int width = controllerBE.getWidth();
        if (width == 1) return;

        int batteriesToPlace = 0;
        BlockPos startPos = face == Direction.DOWN ? controllerBE.getBlockPos()
                .below()
                : controllerBE.getBlockPos()
                .above(controllerBE.getHeight());

        if (startPos.getY() != pos.getY()) return;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = world.getBlockState(offsetPos);
                if (BatteryBlock.isBattery(blockState))
                    continue;
                if (!blockState.canBeReplaced())
                    return;
                batteriesToPlace++;
            }
        }

        if (!player.isCreative() && stack.getCount() < batteriesToPlace) return;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = world.getBlockState(offsetPos);
                if (BatteryBlock.isBattery(blockState))
                    continue;
                BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                super.place(context);
            }
        }
    }
}
