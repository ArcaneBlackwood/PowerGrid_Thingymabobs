package dev.thingymabobs.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public class DistLocal<T> {
	@OnlyIn(Dist.CLIENT)
	private T client;
	private T server;

	public DistLocal() { }
	public DistLocal(Builder<T> constructor) {
		server = constructor.construct(false);
		if (FMLLoader.getDist() == Dist.CLIENT)
			constructClient(constructor);
	}

	public T get(LevelAccessor level) {
		if (!level.isClientSide()) return server;
		if (FMLLoader.getDist() == Dist.CLIENT)
			return getClient();
		throw new IllegalStateException("Tried to fetch client DistLocal on server");
	}
	public T get(Player player) {
		if (!player.level().isClientSide) return server;
		if (FMLLoader.getDist() == Dist.CLIENT)
			return getClient();
		throw new IllegalStateException("Tried to fetch client DistLocal on server");
	}
	public T getServer() {
		return server;
	}
	@OnlyIn(Dist.CLIENT)
	public T getClient() {
		return client;
	}

	@OnlyIn(Dist.CLIENT)
	private void constructClient(Builder<T> constructor) {
		client = constructor.construct(true);
	}

	public static interface Builder<T> {
		public T construct(boolean isClient);
	}
}
