package dev.thingymabobs.mixin.unit;

import org.patryk3211.powergrid.circuits.circuitboard.BakedCircuit;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import net.minecraft.network.FriendlyByteBuf;

@Mixin(CircuitBoardBlockEntity.class)
public class CircuitBoardBlockEntityMixin {
	@Shadow
    private BakedCircuit baked;
	
	@Inject(
		method = "writeToSync",
		at = @At("RETURN"),
		require = 1
	)
	private void thingymabobs$writeToSync(
		FriendlyByteBuf buffer,
		CallbackInfo cir
	) {
		if (baked == null) return;
        for (var placed : baked.tickedComponents) {
            if (!(placed.component instanceof ISynchronizedComponent sync)) continue;
			sync.writeToSync(placed, buffer);
        }
	}
	@Inject(
		method = "readFromSync",
		at = @At("RETURN")
	)
	private void thingymabobs$readFromSync(
		FriendlyByteBuf buffer,
		CallbackInfo cir
	) {
		if (baked == null) return;
        for (var placed : baked.tickedComponents) {
            if (!(placed.component instanceof ISynchronizedComponent sync)) continue;
			sync.readFromSync(placed, buffer);
        }
	}
}