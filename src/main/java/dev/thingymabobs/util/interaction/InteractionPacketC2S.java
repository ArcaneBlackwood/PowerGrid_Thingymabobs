package dev.thingymabobs.util.interaction;

import org.patryk3211.powergrid.network.C2SPacket;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.util.interaction.InteractionHandler.Type;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.server.level.ServerPlayer;

public class InteractionPacketC2S implements C2SPacket {
	InteractLocation location;
	String key;

	public InteractionPacketC2S(InteractionHandler handler) {
		key = handler.getKey();
		location = handler.location;
		Thingymabobs.LOGGER.info("  Client Write key "+key);
	}
	public InteractionPacketC2S() {
		key = null;
		location = null;
		Thingymabobs.LOGGER.info("  Client Write null");
	}

	public InteractionPacketC2S(FriendlyByteBuf buf) {
		int keySize = buf.readByte();
		if (keySize < 0) {
			key = null;
			Thingymabobs.LOGGER.info("  Server read null ");
			return;
		}
		key = Utf8String.read(buf, keySize);

		Type type = InteractionHandler.TYPES.get(key);
		if (type == null) {
			Thingymabobs.LOGGER.warn("Failed to fetch interaction type for packet key '"+key+"'");
			return;
		}
		location = type.reader().read(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		if (location == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			location.write(buf);
		}
	}

	@Override
	public void handle(ServerPlayer player) {//InteractionHandler
		if (location == null) {
			InteractionHandler.clearActive(player);
		} else {
			Thingymabobs.LOGGER.info("  Server read location "+location);
			Type type = InteractionHandler.TYPES.get(key);
			InteractionHandler.setActive(player, location, type.constructor());
		}
	}
}