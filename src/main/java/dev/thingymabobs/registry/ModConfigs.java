package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.CServer;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.function.Supplier;

public final class ModConfigs {
	public enum Type {
		CLIENT, COMMON, SERVER
	}

	private static CServer server;
	//private static CCommon common;
	//private static CClient client;

	public static CServer server() {
		return server;
	}

	// public static CCommon common() {
	//	 return common;
	// }

	// public static CClient client() {
	//	 return client;
	// }

	private static <T extends ConfigBase> T register(Supplier<T> factory, ModConfig.Type type) {
		Pair<T, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
			T config = factory.get();
			config.registerAll(builder);
			return config;
		});

		T config = specPair.getLeft();
		config.specification = specPair.getRight();
		Thingymabobs.CONTAINER.registerConfig(type, config.specification);
		return config;
	}

	public static void register(IEventBus modBus) {
		server = register(CServer::new, ModConfig.Type.SERVER);
		// common = register(CCommon::new, ModConfig.Type.COMMON);
		// client = register(CClient::new, ModConfig.Type.CLIENT);
		modBus.addListener(ModConfigs::configLoad);
		modBus.addListener(ModConfigs::configReload);
	}

	@SubscribeEvent
	public static void configLoad(ModConfigEvent.Loading event) {
		IConfigSpec eventSpec = event.getConfig().getSpec();
		if (eventSpec == server.specification) server.onLoad();
		//if (eventSpec == common.specification) common.onLoad();
		//if (eventSpec == client.specification) client.onLoad();
	}

	@SubscribeEvent
	public static void configReload(ModConfigEvent.Reloading event) {
		IConfigSpec eventSpec = event.getConfig().getSpec();
		if (eventSpec == server.specification) server.onReload();
		//if (eventSpec == common.specification) common.onReload();
		//if (eventSpec == client.specification) client.onReload();
	}
}
