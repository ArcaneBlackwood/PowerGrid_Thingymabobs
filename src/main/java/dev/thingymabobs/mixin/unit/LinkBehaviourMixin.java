package dev.thingymabobs.mixin.unit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.thingymabobs.mixin.LevelTransformer;
import dev.thingymabobs.mixin.LinkBehaviourExt;
import it.unimi.dsi.fastutil.floats.FloatConsumer;

import com.simibubi.create.content.redstone.link.LinkBehaviour;

@Mixin(LinkBehaviour.class)
public abstract class LinkBehaviourMixin implements LinkBehaviourExt {
	@Unique
    public LevelTransformer transformer;
	@Unique
	public FloatConsumer signalCallbackFloat;

	@Override
	public void setTransformer(LevelTransformer transformer) {
		this.transformer = transformer;
	}

    @Override
    public LevelTransformer getTransform() {
		return transformer;
	}
	@Override
	public void setRecieveCallback(FloatConsumer callback) {
		signalCallbackFloat = callback;
	}
	@Override
	public void setReceivedStrength(float networkPower) {
		if (signalCallbackFloat == null) return;
		signalCallbackFloat.accept(networkPower);
	}
}
