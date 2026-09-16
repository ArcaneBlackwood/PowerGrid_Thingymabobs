package dev.thingymabobs.registry.network;

import dev.thingymabobs.Thingymabobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface BiPacket extends IPacket {
	public static final Type<BufferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thingymabobs.MOD_ID, "bi"));

	boolean writeC2S(FriendlyByteBuf buf);
	@OnlyIn(Dist.CLIENT)
	boolean writeS2C(FriendlyByteBuf buf);
	boolean readC2S(FriendlyByteBuf buf, ServerPlayer from);
	@OnlyIn(Dist.CLIENT)
	boolean readS2C(FriendlyByteBuf buf);

	
	@Override
	default boolean dispatch(PacketTargets targets) {
		FriendlyByteBuf buf = PacketManager.newPacketBuffer(this);
		if (targets.isS2C())
			writeS2C(buf);
		else
			writeC2S(buf);
		return targets.dispatch(TYPE, buf);
	}
}
