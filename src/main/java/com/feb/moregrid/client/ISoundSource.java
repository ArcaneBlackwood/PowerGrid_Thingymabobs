package com.feb.moregrid.client;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public interface ISoundSource {
	public Vec3 getPosition();
	//Should also set hasAudioSource
	public float getVolume();
	public float getPitch();
	public boolean isRemoved();
	public void onStop();
	public RandomSource getRandom();
}
