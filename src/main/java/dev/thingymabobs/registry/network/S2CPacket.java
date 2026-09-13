package dev.thingymabobs.registry.network;

import dev.thingymabobs.Thingymabobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface S2CPacket extends IPacket {
    public static final Type<BufferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thingymabobs.MOD_ID, "s2c"));
	
	public abstract boolean write(FriendlyByteBuf buf);
	@OnlyIn(Dist.CLIENT)
	public abstract boolean read(FriendlyByteBuf buf);
	
	@Override
	default boolean dispatch(PacketTargets targets) {
		if (!targets.isS2C()) throw new IllegalCallerException("Trying to dispatch a S2C packet with a C2S target");
		FriendlyByteBuf buf = PacketManager.newPacketBuffer();
		write(buf);
		return targets.dispatch(TYPE, buf);
	}
}