package dev.thingymabobs.mixin.unit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.thingymabobs.registry.ModAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.PhantomSpawner;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
	@ModifyExpressionValue(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/stats/ServerStatsCounter;getValue(Lnet/minecraft/stats/Stat;)I"
		),
		require = 1
	)
	private int modifyTimeSinceRest(
		int value,
		@Local ServerPlayer serverPlayer
	) {
		return value - ModAttachments.PhantomSleepReduction.get(serverPlayer);
	}
}