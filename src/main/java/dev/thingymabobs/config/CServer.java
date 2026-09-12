package dev.thingymabobs.config;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.CProperties;
import net.createmod.catnip.config.ConfigBase;

public class CServer extends ConfigBase {
	public static final int CONFIG_VERSION = 1;
	public final ConfigInt version = i(CONFIG_VERSION, "configVersion", Comments.version);

	public final CProperties properties = nested(0, CProperties::getInstance, Comments.properties);


	@Override
	public String getName() {
		return "server";
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if(!isUpToDate())
			Thingymabobs.LOGGER.warn("Detected outdated configs, consider resetting your server configs if you experience issues.");
	}

	@Override
	public void onReload() {
		super.onReload();
		Thingymabobs.LOGGER.warn("Server config reloaded, this can cause unexpected behaviour if done during gameplay!");
	}

	public boolean isUpToDate() {
		if(version.get() < 0)
			return true;
		return version.get() >= CONFIG_VERSION;
	}

	private static class Comments {
		public static final String properties = "Propreties for all components and devices";
		public static final String version = "Config version check, values below 0 will disable config version checker";
	}
}
