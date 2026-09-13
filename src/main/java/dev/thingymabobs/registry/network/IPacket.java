package dev.thingymabobs.registry.network;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IPacket {
	boolean dispatch(PacketTargets targets);

	/**
	 * Dispatch to all other server players tracking this.
	 * Only supports Players, Entities, BlockEntities
	 */
	default boolean dispatchTracking() {
		if (this instanceof BlockEntity target)
			return dispatch(PacketTargets.toTracking(target));
		else if (this instanceof Entity target)
			return dispatch(PacketTargets.toTracking(target));
		return false;
	}
}
