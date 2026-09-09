package dev.thingymabobs.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;

public interface LevelTransformer {
	public float transform(float level, IRedstoneLinkable self, IRedstoneLinkable other);
}