package dev.thingymabobs.registry.capabilities;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public interface ICapability {
	public void apply(RegisterCapabilitiesEvent event);
}
