package dev.thingymabobs.client;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.sounds.SoundSource;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import dev.thingymabobs.component.base.ABuzzerComponent;
import dev.thingymabobs.registry.ModSounds;

@OnlyIn(Dist.CLIENT)
public class BuzzerSoundInstance extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
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
		if(placed.customData instanceof ABuzzerComponent.AState state)
			state.soundInstance = this;
	}

	public boolean canStartSilent() {
		return true;
	}

	public void forceStop() {
		cleanup();
	}

	@Override
	public void tick() {
		if (checkInvalid()) {
			cleanup();
			return;
		}
		if(!(placed.customData instanceof ABuzzerComponent.AState state)) {
			cleanup();
			return;
		}
		state.soundInstance = this;
		this.volume = state.volume;
		if (this.volume < 0.0001f) {
			if (lastVolumeTicks <= 0) this.stop();
			else lastVolumeTicks--;
		} else {
			lastVolumeTicks = 200;
			this.pitch = state.pitch;
		}
	}
	private void cleanup() {
		stop();
		this.volume = 0;
		if (placed.customData instanceof ABuzzerComponent.AState state) {
			if (state.soundInstance == this)
				state.soundInstance = null;
		}
	}
	private boolean checkInvalid() {
		if (isStopped()) return true;
		if (placed == null || placed.destroyed) return true;
		Level world = placed.getWorld();
		if (world == null) return true;
		BlockEntity be = placed.getWorld().getBlockEntity(placed.getPos());
		if (be == null || be.isRemoved()) return true;
		return false;
	}
}
