package dev.thingymabobs.mixin.unit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.thingymabobs.util.SableUtils;

import net.minecraft.world.level.Level;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(at = @At("HEAD"), method = "tickBlockEntities", require = 1)
	private void thingymabobs$tickBlockEntities(CallbackInfo ci) {
		if (SableUtils.isLoaded) SableUtils.tick();
	}
}