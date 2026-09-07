package dev.thingymabobs.mixin;

import java.util.concurrent.CompletableFuture;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;

public interface SoundEngineExt {
	public CompletableFuture<SoundBuffer> getSoundBuffer(Sound sound);
	public void refreshSound(SoundInstance soundInstance);
}
