package dev.thingymabobs.mixin.unit;

import java.util.function.IntConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.mixin.LevelTransformer;
import dev.thingymabobs.mixin.LinkBehaviourExt;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import com.simibubi.create.content.redstone.link.LinkBehaviour;

@Mixin(LinkBehaviour.class)
public abstract class LinkBehaviourMixin implements LinkBehaviourExt {
	@Unique
	public LevelTransformer transformer;
	@Override
	public void setTransformer(LevelTransformer transformer) {
		this.transformer = transformer;
	}
	@Override
	public LevelTransformer getTransform() {
		return transformer;
	}

	@Unique
	public FloatConsumer signalCallbackFloat;
	@Override
	public void setRecieveCallback(FloatConsumer callback) {
		signalCallbackFloat = callback;
	}
	@Override
	public void setReceivedStrength(float networkPower) {
		if (signalCallbackFloat == null) return;
		signalCallbackFloat.accept(networkPower);
	}

	@Shadow
	private IntConsumer signalCallback;
	@Inject(at = @At("HEAD"), cancellable = true, method = "setReceivedStrength", require = 1)
	public void thingymabobs$setReceivedStrength(int networkPower, CallbackInfo ci) {
		setReceivedStrength(networkPower);
		if (signalCallback == null) ci.cancel();
	}
}
