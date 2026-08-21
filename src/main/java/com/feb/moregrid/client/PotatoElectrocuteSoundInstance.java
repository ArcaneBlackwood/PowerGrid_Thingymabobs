package com.feb.moregrid.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import com.feb.moregrid.registry.ModSounds;

public class PotatoElectrocuteSoundInstance extends AbstractTickableSoundInstance {
	private final ISoundSource block;
	private int lastVolumeTicks = 1;

    public PotatoElectrocuteSoundInstance(ISoundSource block) {
        super(ModSounds.POTATO_ELECTROCUTE.get(), SoundSource.BLOCKS, block.getRandom());
        this.block = block;
        var pos = block.getPosition();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
		this.pitch = 1.0f;
		block.getVolume();
    }

	public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
		if (block == null) {
			stop();
			return;
		}
		if(block.isRemoved()) {
			stop();
		} else {
			this.volume = block.getVolume();
			if (this.volume < 0.0001f) {
				if (lastVolumeTicks <= 0) this.stop();
				else lastVolumeTicks--;
			} else
				lastVolumeTicks = 200;
		}
		if (isStopped()) block.onStop();
    }
}
