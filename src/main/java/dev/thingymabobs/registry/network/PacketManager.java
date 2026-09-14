package dev.thingymabobs.registry.network;

import java.util.function.Supplier;
import org.patryk3211.powergrid.PowerGrid;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.blocks.lavalamp.LavaLampGlobS2CPacket;
import dev.thingymabobs.util.interaction.InteractionPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PacketManager {
	public static final int VERSION = 1;

	private static PacketCollection<C2SPacket> C2S_PACKETS = new PacketCollection<>();
	private static PacketCollection<BiPacket> BI_PACKETS = new PacketCollection<>(
		packet(InteractionPacket.INSTANCE)
	);
	private static PacketCollection<S2CPacket> S2C_PACKETS = new PacketCollection<>(
		packet(LavaLampGlobS2CPacket.class, LavaLampGlobS2CPacket::new),
		packet(S2CVersionPacket.INSTANCE)
	);




	//	#### SERVER & SHARED PACKET REGISTERING
	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterPayloadHandlersEvent event) ->
			PacketManager.registerPayloadHandlersServer(event.registrar(PowerGrid.MOD_ID).optional()));
		NeoForge.EVENT_BUS.addListener(PacketManager::onServerPlayerConnect);
		
	}
	public static void registerPayloadHandlersServer(PayloadRegistrar registrar) {
		registrar.playToServer(
			C2SPacket.TYPE, BufferPayload.codec(C2SPacket.TYPE),
			(payload, context) -> {
				try {
					if (!(context.player() instanceof ServerPlayer player)) return;

					FriendlyByteBuf buf = payload.data();
					int packetID = buf.readVarInt();
					C2SPacket packet = C2S_PACKETS.get(packetID);
					if (packet == null) {
						Thingymabobs.LOGGER.warn("Failed to read C2S packet: "+packetID);
						return;
					}
					packet.read(buf, player);
				} finally {
					payload.release();
				}
			});
		registrar.playBidirectional(
			BiPacket.TYPE, BufferPayload.codec(BiPacket.TYPE),
			(payload, context) -> {
				try {
					FriendlyByteBuf buf = payload.data();
					int packetID = buf.readVarInt();
					BiPacket packet = BI_PACKETS.get(packetID);
					if (packet == null) {
						Thingymabobs.LOGGER.warn("Failed to read Bi packet: "+packetID);
						return;
					}
					if (context.player() instanceof ServerPlayer player) {
						packet.readC2S(buf, player);
					} else if (context instanceof ClientPayloadContext)
						packet.readS2C(buf);
					else
						Thingymabobs.LOGGER.warn("Failed to determine direction of BiPacket, context "+context);

				} finally {
					payload.release();
				}
			});
		registrar.playToClient(
			S2CPacket.TYPE,
			BufferPayload.codec(S2CPacket.TYPE),
			(payload, context) -> {
				try {
					FriendlyByteBuf buf = payload.data();
					int packetID = buf.readVarInt();
					S2CPacket packet = S2C_PACKETS.get(packetID);
					if (packet == null) {
						Thingymabobs.LOGGER.warn("Failed to read S2C packet: "+packetID);
						return;
					}
					packet.read(buf);
				} finally {
					payload.release();
				}
			});
	}
	public static void onServerPlayerConnect(PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		new S2CVersionPacket().dispatch(PacketTargets.toClient(player));
	}


	//	##	HELPERS
	@SuppressWarnings("unchecked")
	private static <K, T extends K> PacketCollection.Entry<K> packet(Class<T> type, Supplier<T> constructor) {
		return new PacketCollection.Entry<K>((Class<K>)type, (Supplier<K>)constructor);
	}
	private static <K, T extends K> PacketCollection.Entry<K> packet(T instance) {
		return new PacketCollection.Entry<K>((K)instance);
	}
	protected static FriendlyByteBuf newPacketBuffer(C2SPacket that) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeVarInt(C2S_PACKETS.get(that));
		return buf;
	}
	protected static FriendlyByteBuf newPacketBuffer(BiPacket that) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeVarInt(BI_PACKETS.get(that));
		return buf;
	}
	protected static FriendlyByteBuf newPacketBuffer(S2CPacket that) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeVarInt(S2C_PACKETS.get(that));
		return buf;
	}
}
