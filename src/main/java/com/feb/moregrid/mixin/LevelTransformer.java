package com.feb.moregrid.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;

public interface LevelTransformer {
	public int transform(int level, IRedstoneLinkable self, IRedstoneLinkable other);
}