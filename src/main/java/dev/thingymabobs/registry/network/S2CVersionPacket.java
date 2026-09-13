package dev.thingymabobs.registry.network;

import dev.thingymabobs.registry.ModLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class S2CVersionPacket implements S2CPacket {
	public static final S2CVersionPacket INSTANCE = new S2CVersionPacket();

	@Override
	public boolean write(FriendlyByteBuf buf) {
		buf.writeVarInt(PacketManager.VERSION);
		return true;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean read(FriendlyByteBuf buf) {
		if (buf.readVarInt() == PacketManager.VERSION) return true;
		Minecraft.getInstance().disconnect(
			new DisconnectedScreen(
				new TitleScreen(), CommonComponents.CONNECT_FAILED, 
				ModLang.translateDirect("gui.disconnect.packet_version")
		));
		return true;
	}
}
