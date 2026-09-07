package dev.thingymabobs.util;

import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface IDirectionSocketElectric extends ISocketElectric {

	Direction socketFacing(BlockState state);
	boolean socketValid(BlockState state, BlockState madeWith);
}
