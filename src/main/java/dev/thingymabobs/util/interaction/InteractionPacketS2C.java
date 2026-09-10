package dev.thingymabobs.util.interaction;

import java.util.UUID;
import org.patryk3211.powergrid.network.S2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.world.entity.player.Player;

public class InteractionPacketS2C implements S2CPacket {
	UUID player;
	String key;
	BlockPos pos;

    public InteractionPacketS2C(Player player, InteractionHandler interact) {
		this.player = player.getUUID();
		this.key = interact.getKey();
		this.pos = interact.pos;
    }
	public InteractionPacketS2C(Player player) {
		this.player = player.getUUID();
		this.key = null;
    }

    public InteractionPacketS2C(FriendlyByteBuf buf) {
		player = buf.readUUID();
        int keySize = buf.readByte();
		if (keySize < 0) {
			key = null;
		} else {
			key = Utf8String.read(buf, keySize);
			pos = buf.readBlockPos();
		}
    }

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(player);
		if (key == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			buf.writeBlockPos(pos);
		}
	}

	@Override
	public void handle(Minecraft mc) {//InteractionHandler
		Player remote = mc.level.getPlayerByUUID(player);
		if (remote == null) return;
		if (key == null) {
			InteractionHandler.clearActive(remote);
		} else {
			InteractionHandler.setActive(remote, key, pos);
		}
	}
}