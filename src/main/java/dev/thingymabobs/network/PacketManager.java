package dev.thingymabobs.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;

public class PacketManager {
	public static void register(IEventBus modBus) {

	}


	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {

	}
}
