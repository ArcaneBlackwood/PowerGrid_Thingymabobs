package dev.thingymabobs.registry.network;

import dev.thingymabobs.Thingymabobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface C2SPacket extends IPacket {
    public static final Type<BufferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thingymabobs.MOD_ID, "c2s"));

	@OnlyIn(Dist.CLIENT)
	boolean write(FriendlyByteBuf buf);
	boolean read(FriendlyByteBuf buf, ServerPlayer from);
	
	@Override
	default boolean dispatch(PacketTargets targets) {
		if (targets.isS2C()) throw new IllegalCallerException("Trying to dispatch a C2S packet with a S2C target");
		FriendlyByteBuf buf = PacketManager.newPacketBuffer(this);
		write(buf);
		return targets.dispatch(TYPE, buf);
	}
}
