package dev.thingymabobs.registry.network;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import dev.thingymabobs.mixin.ChunkMapExt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract class PacketTargets {
    public boolean isS2C() {
        return true;
    }
	public abstract boolean dispatch(Type<BufferPayload> type, FriendlyByteBuf data);



	// ######## CLIENT TO SERVER
	@OnlyIn(Dist.CLIENT)
	public static PacketTargets toServer() {
		return new PacketTargets() {
            @Override
            public boolean isS2C() {
                return false;
            }
			@Override
			public boolean dispatch(Type<BufferPayload> type, FriendlyByteBuf data) {
				var connection = Minecraft.getInstance().getConnection();
				if (connection == null) return false;
				BufferPayload payload = new BufferPayload(type, data);
				connection.send(new ServerboundCustomPayloadPacket(payload));
				return true;
			}
		};
	}




	// ######## SERVER TO CLIENT
	public static PacketTargets toClient(ServerPlayer player) {
		return new PacketTargets() {
			@Override
			public boolean dispatch(Type<BufferPayload> type, FriendlyByteBuf data) {
				BufferPayload payload = new BufferPayload(type, data);
				player.connection.send(new ClientboundCustomPayloadPacket(payload));
				return true;
			}
		};
	}
	public static PacketTargets toClientAll() {
		return new PacketTargets() {
			@Override
			public boolean dispatch(Type<BufferPayload> type, FriendlyByteBuf data) {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				if (server == null) return false;
				List<ServerPlayer> players = server.getPlayerList().getPlayers();
				BufferPayload payload = new BufferPayload(type, data);
				ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);

				for (ServerPlayer player : players) {
					var connection = player.connection;
					if (connection == null) continue;
					connection.send(packet);
				}
				return true;
			}
		};
	}




	// ######## SERVER TO TRACKING CLIENTS
	public static PacketTargets toTracking(ServerLevel level, BlockPos pos) {
		return new Tracking() {
			@Override
			protected Stream<ServerPlayerConnection> getStream() {
				ChunkMapExt chunkMap = ((ChunkMapExt)level.getChunkSource().chunkMap);
				return chunkMap.trackingStream(level, pos);
			}
		};
	}
	public static PacketTargets toTracking(BlockEntity be) {
		return new Tracking() {
			@Override
			protected Stream<ServerPlayerConnection> getStream() {
				ServerLevel level = (ServerLevel)be.getLevel();
				if (level == null) return null;
				ChunkMapExt chunkMap = ((ChunkMapExt)level.getChunkSource().chunkMap);
				return chunkMap.trackingStream(be);
			}
		};
	}
	public static PacketTargets toTracking(Entity entity) {
		return new Tracking() {
			@Override
			protected Stream<ServerPlayerConnection> getStream() {
				ServerLevel level = (ServerLevel)entity.level();
				if (level == null) return null;
				ChunkMapExt chunkMap = ((ChunkMapExt)level.getChunkSource().chunkMap);
				return chunkMap.trackingStream(entity);
			}
		};
	}
	public static PacketTargets toTracking(ServerPlayer player) {
		return new Tracking() {
			@Override
			protected Stream<ServerPlayerConnection> getStream() {
				ServerLevel level = (ServerLevel)player.level();
				if (level == null) return null;
				ChunkMapExt chunkMap = ((ChunkMapExt)level.getChunkSource().chunkMap);
				return chunkMap.trackingStream(player);
			}
		};
	}


	private static abstract class Tracking extends PacketTargets {
		@Override
		public boolean dispatch(Type<BufferPayload> type, FriendlyByteBuf data) {
				BufferPayload payload = new BufferPayload(type, data);
				ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
				Stream<ServerPlayerConnection> stream = getStream();
				if (stream == null) return false;
				Iterator<ServerPlayerConnection> iter = stream.iterator();
				while (iter.hasNext())iter.next().send(packet);
				return true;
		}
		protected abstract Stream<ServerPlayerConnection> getStream();
	}
}
