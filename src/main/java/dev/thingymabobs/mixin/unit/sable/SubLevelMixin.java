package dev.thingymabobs.mixin.unit.sable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.thingymabobs.mixin.SubLevelExt;
import dev.thingymabobs.util.SableUtils;

@Mixin(SubLevel.class)
public abstract class SubLevelMixin implements SubLevelExt {
    @Inject(at = @At("HEAD"), method = "tick", require = 1)
	private void thingymabobs$tick(CallbackInfo ci) {
		SableUtils.tickSubLevel((SubLevelAccess)(Object)this);
	}
    @Inject(at = @At("HEAD"), method = "markRemoved", require = 1)
	private void thingymabobs$markRemoved(CallbackInfo ci) {
		SableUtils.onRemoveSubLevel((SubLevelAccess)(Object)this);
	}
    @Shadow
    private boolean isRemoved;
    @Override
    public boolean isRemoved() {
        return isRemoved;
    }
}