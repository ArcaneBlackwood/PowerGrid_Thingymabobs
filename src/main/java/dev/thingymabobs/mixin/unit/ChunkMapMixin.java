package dev.thingymabobs.mixin.unit;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import dev.thingymabobs.mixin.ChunkMapExt;
import dev.thingymabobs.util.CombinedSpliterator;
import dev.thingymabobs.util.SingleSpliterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapExt {
	@Accessor(value = "entityMap")
	public abstract Int2ObjectMap<?> getEntityMap();

	@Shadow
	public abstract boolean isChunkTracked(ServerPlayer player, int x, int z);


	@Mixin(targets = {"net.minecraft.server.level.ChunkMap$TrackedEntity"})
	private interface TrackedEntityMixin {
		@Accessor(value = "seenBy")
		Set<ServerPlayerConnection> getSeenBy();
	}
	

	@Override
	public Stream<ServerPlayerConnection> trackingStream(ServerLevel level, BlockPos blockPos) {
		ChunkPos pos = new ChunkPos(blockPos);
		return level.players().stream().filter((player) ->
				isChunkTracked(player, pos.x, pos.z))
			.map((player) -> (ServerPlayerConnection)player.connection);
	}
	@Override
	public Stream<ServerPlayerConnection> trackingStream(BlockEntity tracking) {
		ServerLevel level = ((ServerLevel)tracking.getLevel());
		if (level == null) return Stream.empty();
		ChunkPos pos = new ChunkPos(tracking.getBlockPos());
		return level.players().stream().filter((player) ->
				isChunkTracked(player, pos.x, pos.z))
			.map((player) -> (ServerPlayerConnection)player.connection);
	}
	@Override
	public Stream<ServerPlayerConnection> trackingStream(Entity tracking) {
		var trackedEntity = (TrackedEntityMixin)getEntityMap().get(tracking.getId());
		if (trackedEntity == null) return Stream.empty();
		return trackedEntity.getSeenBy().stream();
	}
	/**
	 * Also includes the player themselves.  If this is unwanted, use {@link #trackingStream(Entity)}
	 */
	@Override
	public Stream<ServerPlayerConnection> trackingStream(ServerPlayer tracking) {
		var trackedEntity = (TrackedEntityMixin)getEntityMap().get(tracking.getId());
		if (trackedEntity == null) return Stream.empty();
		return StreamSupport.stream(CombinedSpliterator.of(
			SingleSpliterator.of(tracking.connection),
			trackedEntity.getSeenBy().spliterator()
		), false);
	}
}
