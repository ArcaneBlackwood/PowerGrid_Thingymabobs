package dev.thingymabobs.mixin.unit.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingMixin {
	@Invoker
	void invokeRelease();
}
