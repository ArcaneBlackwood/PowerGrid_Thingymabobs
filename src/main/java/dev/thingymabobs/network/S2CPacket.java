package dev.thingymabobs.network;

import net.minecraft.network.FriendlyByteBuf;

public interface S2CPacket {
	void write(FriendlyByteBuf buf);
	void read(FriendlyByteBuf buf);
}