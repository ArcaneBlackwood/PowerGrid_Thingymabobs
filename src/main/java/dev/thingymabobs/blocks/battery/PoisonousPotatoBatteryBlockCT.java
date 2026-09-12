package dev.thingymabobs.blocks.battery;

import dev.thingymabobs.Thingymabobs;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PoisonousPotatoBatteryBlockCT extends ConnectedTextureBehaviour.Base {
		private static final CTSpriteShiftEntry TOP = CTSpriteShifter.getCT(
				AllCTTypes.RECTANGLE,
				Thingymabobs.asResource("block/battery/potato_battery_top"),
				Thingymabobs.asResource("block/battery/potato_battery_top_connected")
	);
	private static final CTSpriteShiftEntry BOTTOM = CTSpriteShifter.getCT(
				AllCTTypes.RECTANGLE,
				Thingymabobs.asResource("block/battery/potato_battery_bottom"),
				Thingymabobs.asResource("block/battery/potato_battery_bottom_connected")
	);
	private static final CTSpriteShiftEntry SIDES = CTSpriteShifter.getCT(
				AllCTTypes.RECTANGLE,
				Thingymabobs.asResource("block/battery/poisonous_potato_battery_side"),
				Thingymabobs.asResource("block/battery/poisonous_potato_battery_side_connected")
	);

	private static final CTSpriteShiftEntry SIDES_BAKED = CTSpriteShifter.getCT(
				AllCTTypes.RECTANGLE,
				Thingymabobs.asResource("block/battery/baked_poisonous_potato_battery_side"),
				Thingymabobs.asResource("block/battery/baked_poisonous_potato_battery_side_connected")
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
