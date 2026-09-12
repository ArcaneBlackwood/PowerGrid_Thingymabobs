package dev.thingymabobs.util.interaction;

import java.util.UUID;
import org.patryk3211.powergrid.network.S2CPacket;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.util.interaction.InteractionHandler.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.world.entity.player.Player;

public class InteractionPacketS2C implements S2CPacket {
	UUID player;
	InteractLocation location;
	String key;

	public InteractionPacketS2C(Player player, InteractionHandler handler) {
		this.player = player.getUUID();
		key = handler.getKey();
		location = handler.location;
		Thingymabobs.LOGGER.info("  Server Write key "+key);
	}
	public InteractionPacketS2C(Player player) {
		this.player = player.getUUID();
		key = null;
		location = null;
		Thingymabobs.LOGGER.info("  Server Write null");
	}

	public InteractionPacketS2C(FriendlyByteBuf buf) {
		player = buf.readUUID();
		int keySize = buf.readByte();
		if (keySize < 0) {
			key = null;
			Thingymabobs.LOGGER.info("  Client read null ");
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
		buf.writeUUID(player);
		if (location == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			location.write(buf);
		}
	}

	@Override
	public void handle(Minecraft mc) {//InteractionHandler
		Player remote = mc.level.getPlayerByUUID(player);
		if (remote == null) return;
		if (location == null) {
			InteractionHandler.clearActive(remote);
		} else {
			location.setWorld(mc.level);
			Thingymabobs.LOGGER.info("  Client read location "+location);
			Type type = InteractionHandler.TYPES.get(key);
			InteractionHandler.setActive(remote, location, type.constructor());
		}
	}
}