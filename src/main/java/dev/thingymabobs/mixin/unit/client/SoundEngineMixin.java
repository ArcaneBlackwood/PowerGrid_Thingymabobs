package dev.thingymabobs.mixin.unit.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.thingymabobs.mixin.SoundEngineExt;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineExt {
	@Shadow
   	private SoundBufferLibrary soundBuffers;
	@Shadow
	private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
	@Shadow
	protected abstract float calculateVolume(SoundInstance soundInstance);
	@Shadow
	protected abstract float calculatePitch(SoundInstance soundInstance);


	@Override
	public CompletableFuture<SoundBuffer> getSoundBuffer(Sound sound) {
		return soundBuffers.getCompleteBuffer(sound.getLocation());
	}


	@Override
	public void refreshSound(SoundInstance soundInstance) {
		ChannelAccess.ChannelHandle channel =
			this.instanceToChannel.get(soundInstance);

		if (channel == null) {
			return;
		}

		float volume = this.calculateVolume(soundInstance);
		float pitch = this.calculatePitch(soundInstance);

		Vec3 position = new Vec3(
			soundInstance.getX(),
			soundInstance.getY(),
			soundInstance.getZ()
		);

		channel.execute(source -> {
			source.setVolume(volume);
			source.setPitch(pitch);
			source.setSelfPosition(position);
		});
	}
}
