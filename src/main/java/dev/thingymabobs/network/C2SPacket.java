package dev.thingymabobs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface C2SPacket {
	void write(FriendlyByteBuf buf);
	void read(FriendlyByteBuf buf, ServerPlayer from);
}
