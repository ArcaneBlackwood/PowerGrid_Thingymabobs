package dev.thingymabobs.config.properties;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;

public class IntProp extends ASubProp {
	public ConfigBase.ConfigInt value = null, min = null, max = null;
	public int valueDefault, minDefault, maxDefault;
	public boolean hasMinMax = false;
	public String comment;
	protected boolean unloaded = false;

	public IntProp(int value) {
		Thingymabobs.LOGGER.info("IntProp "+this);
		this.valueDefault = value;
	}
	public IntProp(int value, int min, int max) {
		this.valueDefault = value;
		this.minDefault = min;
		this.maxDefault = max;
		hasMinMax = true;
	}
	public int get() {
		if (unloaded) return valueDefault;
		return value.get();
	}
	public int min() {
		if (unloaded) return minDefault;
		return min.get();
	}
	public int max() {
		if (unloaded) return maxDefault;
		return max.get();
	}

	@Override
	public void onLoad() {
		unloaded = false;
	}
	@Override
	public Class<?> getType() {
		return IntProp.class;
	}
	@Override
	public void register(String id, CProperties.Builder builder) {
		String valueId = hasMinMax ? id+"_default" : id;
		if (comment != null && !comment.isEmpty())
			value = builder.i(valueDefault, valueId, comment);
		else
			value = builder.i(valueDefault, valueId);
		if (!hasMinMax) return;
		min = builder.i(minDefault, id+"_min");
		max = builder.i(maxDefault, id+"_max");
	}
}