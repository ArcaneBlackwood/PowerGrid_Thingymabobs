package com.feb.moregrid.mixin.unit;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.feb.moregrid.mixin.SoundBufferExt;
import com.mojang.blaze3d.audio.SoundBuffer;

@Mixin(SoundBuffer.class)
public abstract class SoundBufferMixin implements SoundBufferExt {
	@Shadow
   	private ByteBuffer data;
	@Shadow
   	private AudioFormat format;
	
	@Override
	public float getDuration() {
		return data.capacity() / (format.getSampleRate() * format.getFrameSize());
	}
}
