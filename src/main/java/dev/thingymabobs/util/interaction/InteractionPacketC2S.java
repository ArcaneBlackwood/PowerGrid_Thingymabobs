package dev.thingymabobs.util.interaction;

import java.util.function.Consumer;

import org.patryk3211.powergrid.network.C2SPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.server.level.ServerPlayer;

public class InteractionPacketC2S implements C2SPacket {
	String key;
	BlockPos pos;

    public InteractionPacketC2S(InteractionHandler interact) {
		this.key = interact.getKey();
		this.pos = interact.pos;
    }
	public InteractionPacketC2S() {
		this.key = null;
    }

    public InteractionPacketC2S(FriendlyByteBuf buf) {
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
		if (key == null) {
			buf.writeByte(-1);
		} else {
			buf.writeByte(key.length());
			Utf8String.write(buf, key, 64);
			buf.writeBlockPos(pos);
		}
	}

	@Override
	public void handle(ServerPlayer player) {//InteractionHandler
		if (key == null) {
			InteractionHandler.clearActive(player);
		} else {
			InteractionHandler.setActive(player, key, pos);
		}
	}
}