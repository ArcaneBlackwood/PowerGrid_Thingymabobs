package dev.thingymabobs.mixin.unit;

import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.wire.powercord.SocketEndpoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import dev.thingymabobs.util.IDirectionSocketElectric;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Mixin(SocketEndpoint.class)
public abstract class SocketEndpointMixin {
	@Shadow
   	private BlockPos pos;
	@Shadow
	public abstract @Nullable ElectricBehaviour getElectricBehaviour(Level world);
	//@Shadow
	//public abstract @Nullable ISocketElectric getSocketBlock(Level world);
	

	@Overwrite
	public @NotNull Direction getFacing(Level world) {
		BlockState state = world.getBlockState(this.pos);
		if (state.getBlock() instanceof IDirectionSocketElectric dirSock)
			return dirSock.socketFacing(state);
		if (state.hasProperty(BlockStateProperties.FACING)) {
			return (Direction)state.getValue(BlockStateProperties.FACING);
		} else {
			return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
		}
	}

	protected BlockState madeWith = null;

	@Overwrite
	public boolean isValid(Level world) {
		if (!world.hasChunk(SectionPos.blockToSectionCoord(this.pos.getX()), SectionPos.blockToSectionCoord(this.pos.getZ()))) {
			return false;
		} else {
			ElectricBehaviour behaviour = this.getElectricBehaviour(world);
			if (behaviour == null) {
				return false;
			} else {
				if (!behaviour.hasTerminal(0) || !behaviour.hasTerminal(1)) return false;
				BlockState state = world.getBlockState(this.pos);
				if (!(state.getBlock() instanceof IDirectionSocketElectric dirSock)) return true; 
				if (madeWith == null) {
					madeWith = state;
					return true;
				}
				return dirSock.socketValid(state, madeWith);
			}
		}
   	}

	/*@Overwrite
	public @NotNull Vec3 getExactPosition(Level world) {
		ISocketElectric socketed = this.getSocketBlock(world);
		if (socketed == null) {
			return Vec3.atCenterOf(this.pos);
		} else {
			ITerminalPlacement placement = socketed.socket(world.getBlockState(this.pos));
			Vec3 origin = placement.getOrigin();
			return (new Vec3((double)this.pos.getX() + origin.x, (double)this.pos.getY() + origin.y, (double)this.pos.getZ() + origin.z)).relative(this.getFacing(world), (double)-0.1875F);
		}
	}*/
}
