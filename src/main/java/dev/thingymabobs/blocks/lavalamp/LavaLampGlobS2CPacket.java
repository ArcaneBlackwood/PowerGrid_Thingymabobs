package dev.thingymabobs.blocks.lavalamp;

import org.patryk3211.powergrid.network.S2CPacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LavaLampGlobS2CPacket implements S2CPacket {
	BlockPos pos;
	int waxTop, waxBottom;

	boolean movingUp;
	float speed;
	float size;
	int volume;
	Rotation rotation;
	float x, z;

	public LavaLampGlobS2CPacket(LavaLampEntity be, LavaLampEntity.Glob glob) {
		pos = be.getBlockPos();
		waxTop = be.getVisibleWaxRaw(true);
		waxBottom = be.getVisibleWaxRaw(false);
		
		movingUp = glob.movingUp;
		speed = glob.speed;
		size = glob.size;
		volume = glob.volume;
		rotation = glob.rotation;
		x = glob.x;
		z = glob.z;
	}

	public LavaLampGlobS2CPacket(FriendlyByteBuf buf) {
		pos = buf.readBlockPos();
		waxTop = buf.readInt();
		waxBottom = buf.readInt();
		movingUp = buf.readBoolean();
		speed = buf.readFloat();
		volume = buf.readInt();
		rotation = buf.readEnum(Rotation.class);
		x = buf.readFloat();
		z = buf.readFloat();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeInt(waxTop);
		buf.writeInt(waxBottom);
		buf.writeBoolean(movingUp);
		buf.writeFloat(speed);
		buf.writeInt(volume);
		buf.writeEnum(rotation);
		buf.writeFloat(x);
		buf.writeFloat(z);
	}

	@Override
	public void handle(Minecraft mc) {
		Level world = ClientSideAccess.world();
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof LavaLampEntity lavaLamp)) return;
		lavaLamp.onParticlePacket(
			LavaLampEntity.Glob.fromVolume(movingUp, speed, volume, rotation, x, z),
			waxTop, waxBottom);
	}
}
