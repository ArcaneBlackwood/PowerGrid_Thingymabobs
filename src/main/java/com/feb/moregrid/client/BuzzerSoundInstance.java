package com.feb.moregrid.client;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.feb.moregrid.component.ABuzzerComponent;
import com.feb.moregrid.registry.ModSounds;

@OnlyIn(Dist.CLIENT)
public class BuzzerSoundInstance extends AbstractTickableSoundInstance {
	private final PlacedComponent placed;
	private int lastVolumeTicks = 1;

    public BuzzerSoundInstance(PlacedComponent placed) {
        super(ModSounds.BUZZER.get(), SoundSource.BLOCKS, placed.getWorld().random);
        this.placed = placed;
        var pos = placed.getPos().getCenter();
        this.x = pos.x;// + placed.x / 16.0f;
        this.y = pos.y;
        this.z = pos.z;// + placed.y / 16.0f;
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
		if(placed.component instanceof ABuzzerComponent buzzer)
			buzzer.hasAudioSource = true;
    }

    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
		Level world = placed.getWorld();
		if(placed.component instanceof ABuzzerComponent buzzer) {
			BlockEntity blockEntity = world.getBlockEntity(placed.getPos());
			if(blockEntity == null || blockEntity.isRemoved()) {
				stop();
			} else {
				buzzer.hasAudioSource = true;
				this.volume = buzzer.getVolume(placed);
				if (this.volume < 0.0001f) {
					if (lastVolumeTicks <= 0) this.stop();
					else lastVolumeTicks--;
				} else
					lastVolumeTicks = 200;
				this.pitch = buzzer.getPitch(placed);
			}
			if (isStopped()) buzzer.hasAudioSource = false;
		}
    }
}
