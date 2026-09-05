package com.feb.moregrid.client;

import net.minecraft.world.phys.Vec3;

public interface SoundScapeSource {
	public Vec3 getSoundScapePos();
	public boolean isSoundScapeValid();
	public float getSoundScapeVolume();
	public boolean getSoundScapeState();
}
