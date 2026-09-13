package dev.thingymabobs.util.interaction;

import java.util.UUID;
import dev.thingymabobs.registry.network.BiPacket;
import dev.thingymabobs.registry.network.PacketTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class InteractionPacket implements BiPacket {
	public static final InteractionPacket INSTANCE = new InteractionPacket();
	UUID player;
	InteractLocation location;
	String key;

	public InteractionPacket() {}

	public void sendToClient(ServerPlayer player, InteractionHandler handler) {
		this.player = player.getUUID();
		key = handler.getKey();
		location = handler.location;
		//Thingymabobs.LOGGER.info("  Server Write key "+key);
		dispatch(PacketTargets.toClient(player));
	}
	public void sendToClient(ServerPlayer player) {
		this.player = player.getUUID();
		key = null;
		location = null;
		//Thingymabobs.LOGGER.info("  Server Write null");
		dispatch(PacketTargets.toClient(player));
	}

	@OnlyIn(Dist.CLIENT)
	public void sendToServer(InteractionHandler handler) {
		key = handler.getKey();
		location = handler.location;
		//Thingymabobs.LOGGER.info("  Client Write key "+key);
		dispatch(PacketTargets.toServer());
	}
	@OnlyIn(Dist.CLIENT)
	public void sendToServer() {
		key = null;
		location = null;
		dispatch(PacketTargets.toServer());
	}


	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean writeC2S(FriendlyByteBuf buf) {
		if (location == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			location.write(buf);
		}
		return true;
	}
	@Override
	public boolean writeS2C(FriendlyByteBuf buf) {
		buf.writeUUID(player);
		if (location == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			location.write(buf);
		}
		return true;
	}
	@Override
	public boolean readC2S(FriendlyByteBuf buf, ServerPlayer player) {
		int keySize = buf.readByte();
		if (keySize < 0) {
			key = null;
			//Thingymabobs.LOGGER.info("  Server read null ");
			return false;
		}
		key = Utf8String.read(buf, keySize);

		InteractionManager.Type type = InteractionManager.TYPES.get(key);
		if (type == null) {
			//Thingymabobs.LOGGER.warn("Failed to fetch interaction type for packet key '"+key+"'");
			return false;
		}
		location = type.reader().read(buf);
		
		if (location == null) {
			InteractionManager.clearActive(player);
		} else {
			//Thingymabobs.LOGGER.info("  Server read location "+location);
			InteractionManager.setActive(player, location, type.constructor());
		}
		return true;
	}
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean readS2C(FriendlyByteBuf buf) {
		player = buf.readUUID();
		int keySize = buf.readByte();
		if (keySize < 0) {
			key = null;
			//Thingymabobs.LOGGER.info("  Client read null ");
			return false;
		}
		key = Utf8String.read(buf, keySize);

		InteractionManager.Type type = InteractionManager.TYPES.get(key);
		if (type == null) {
			//Thingymabobs.LOGGER.warn("Failed to fetch interaction type for packet key '"+key+"'");
			return false;
		}

		location = type.reader().read(buf);

		ClientLevel world = Minecraft.getInstance().level;
		Player remote = world.getPlayerByUUID(player);
		if (remote == null) return false;

		if (location == null) {
			InteractionManager.clearActive(remote);
		} else {
			location.setWorld(world);
			InteractionManager.setActive(remote, location, type.constructor());
		}
		return true;
	}
}