package dev.thingymabobs.mixin;

import it.unimi.dsi.fastutil.floats.FloatConsumer;

public interface LinkBehaviourExt {
    public void setTransformer(LevelTransformer transformer);
    public LevelTransformer getTransform();
    public void setRecieveCallback(FloatConsumer callback);
	public void setReceivedStrength(float networkPower);
}
