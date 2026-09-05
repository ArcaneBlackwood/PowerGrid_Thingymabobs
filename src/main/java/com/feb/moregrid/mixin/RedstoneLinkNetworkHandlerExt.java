package com.feb.moregrid.mixin;

import java.util.Set;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;

import net.minecraft.world.level.LevelAccessor;

public interface RedstoneLinkNetworkHandlerExt {
	public void tick();
	public static class Network {
		public Set<IRedstoneLinkable> links;
		public LevelAccessor world;
		public Network(Set<IRedstoneLinkable> links, LevelAccessor world) {
			this.links = links;
			this.world = world;
		}
	}
}
