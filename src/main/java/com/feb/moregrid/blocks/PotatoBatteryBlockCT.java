package com.feb.moregrid.blocks;

import com.feb.moregrid.MoreGrid;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PotatoBatteryBlockCT extends ConnectedTextureBehaviour.Base {
        private static final CTSpriteShiftEntry TOP = CTSpriteShifter.getCT(
                AllCTTypes.RECTANGLE,
                MoreGrid.asResource("block/battery/potato_battery_top"),
                MoreGrid.asResource("block/battery/potato_battery_top_connected")
	);
	private static final CTSpriteShiftEntry BOTTOM = CTSpriteShifter.getCT(
                AllCTTypes.RECTANGLE,
                MoreGrid.asResource("block/battery/potato_battery_bottom"),
                MoreGrid.asResource("block/battery/potato_battery_bottom_connected")
	);
	private static final CTSpriteShiftEntry SIDES = CTSpriteShifter.getCT(
                AllCTTypes.RECTANGLE,
                MoreGrid.asResource("block/battery/potato_battery_side"),
                MoreGrid.asResource("block/battery/potato_battery_side_connected")
	);

	private static final CTSpriteShiftEntry SIDES_BAKED = CTSpriteShifter.getCT(
                AllCTTypes.RECTANGLE,
                MoreGrid.asResource("block/battery/baked_potato_battery_side"),
                MoreGrid.asResource("block/battery/baked_potato_battery_side_connected")
	);

	@Override
	public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		if(direction == Direction.UP)
                        return TOP;
		else if (direction == Direction.DOWN)
                        return BOTTOM;
		return state.getValue(APotatoBatteryArray.BAKED).booleanValue() ? SIDES_BAKED : SIDES;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
		return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos);
	}
}
