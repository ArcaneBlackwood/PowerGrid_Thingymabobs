package dev.thingymabobs.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.network.CustomPayloadWrapper;
import org.patryk3211.powergrid.network.PacketSet;
import org.patryk3211.powergrid.network.S2CPacket;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.packets.LavaLampGlobS2CPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPackets {
	public static final PacketSet PACKETS = PacketSet.builder(Thingymabobs.MOD_ID, 1)
            .s2c(LavaLampGlobS2CPacket.class, LavaLampGlobS2CPacket::new)
            .build();


    public static Collection<ServerPlayer> getPlayersTracking(BlockEntity be) {
        ChunkMap chunkMap = ((ServerLevel)be.getLevel()).getChunkSource().chunkMap;
        List<ServerPlayer> players = chunkMap.getPlayers(new ChunkPos(be.getBlockPos()), false);

        if (players == null) return Collections.emptySet();
        return players;
    }

    public static void sendToClientsTracking(S2CPacket packet, BlockEntity be) {
        for (ServerPlayer player : getPlayersTracking(be)) {
            PACKETS.sendTo(player, packet);
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PowerGrid.MOD_ID)
            .optional();
        registrar.playToServer(
            CustomPayloadWrapper.type(PACKETS.c2sPacket),
            CustomPayloadWrapper.codec(PACKETS.c2sPacket),
            (payload, context) -> {
                try {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        PACKETS.handleC2SPacket(serverPlayer, payload.data());
                    }
                } finally {
                    payload.release();
                }
            });
        registrar.playToClient(
            CustomPayloadWrapper.type(PACKETS.s2cPacket),
            CustomPayloadWrapper.codec(PACKETS.s2cPacket),
            (payload, context) -> {
                try {
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    PACKETS.handleS2CPacket(mc, payload.data());
                } finally {
                    payload.release();
                }
            });
    }
}
