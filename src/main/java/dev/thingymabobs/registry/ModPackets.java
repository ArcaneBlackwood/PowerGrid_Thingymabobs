package dev.thingymabobs.registry;

import org.patryk3211.powergrid.network.PacketSet;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.blocks.lavalamp.LavaLampGlobS2CPacket;
import dev.thingymabobs.util.interaction.InteractionPacketC2S;
import dev.thingymabobs.util.interaction.InteractionPacketS2C;

public final class ModPackets {
	public static final PacketSet PACKETS = PacketSet.builder(Thingymabobs.MOD_ID, 2)
				.s2c(LavaLampGlobS2CPacket.class, LavaLampGlobS2CPacket::new)
				.c2s(InteractionPacketC2S.class, InteractionPacketC2S::new)
				.s2c(InteractionPacketS2C.class, InteractionPacketS2C::new)
				.build();
}
