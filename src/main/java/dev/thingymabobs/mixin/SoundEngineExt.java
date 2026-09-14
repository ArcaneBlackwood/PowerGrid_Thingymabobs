package dev.thingymabobs.mixin;

import java.util.concurrent.CompletableFuture;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SoundEngineExt {
	public CompletableFuture<SoundBuffer> getSoundBuffer(Sound sound);
	public void refreshSound(SoundInstance soundInstance);
}
