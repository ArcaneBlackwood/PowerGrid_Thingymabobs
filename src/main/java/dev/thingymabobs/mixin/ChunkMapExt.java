package dev.thingymabobs.mixin;

import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ChunkMapExt {
	public Stream<ServerPlayerConnection> trackingStream(ServerLevel level, BlockPos blockPos);
	public Stream<ServerPlayerConnection> trackingStream(BlockEntity tracking);
	public Stream<ServerPlayerConnection> trackingStream(Entity tracking);
	/**
	 * Also includes the player themselves.  If this is unwanted, use {@link #trackingStream(Entity)}
	 */
	public Stream<ServerPlayerConnection> trackingStream(ServerPlayer tracking);
}
