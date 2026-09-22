package dev.thingymabobs.config.properties;

import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;

public class FloatProp extends ASubProp {
	public ConfigBase.ConfigFloat value = null, min = null, max = null;
	public float valueDefault, minDefault, maxDefault;
	public boolean hasMinMax = false;
	public String comment;
	protected boolean unloaded = false;

	public FloatProp(float value) {
		this.valueDefault = value;
	}
	public FloatProp(float value, float min, float max) {
		this.valueDefault = value;
		this.minDefault = min;
		this.maxDefault = max;
		hasMinMax = true;
	}
	public float get() {
		if (unloaded) return valueDefault;
		return value.getF();
	}
	public float min() {
		if (unloaded) return minDefault;
		return min.getF();
	}
	public float max() {
		if (unloaded) return maxDefault;
		return max.getF();
	}

	@Override
	public void onLoad() {
		unloaded = false;
		super.onLoad();
	}
	@Override
	public Class<?> getType() {
		return FloatProp.class;
	}
	@Override
	public void register(String id, CProperties.Builder builder) {
		String valueId = hasMinMax ? id+"_default" : id;
		if (comment != null && !comment.isEmpty())
			value = builder.f(valueDefault, 0, valueId, comment);
		else
			value = builder.f(valueDefault, 0, valueId);
		if (!hasMinMax) return;
		min = builder.f(minDefault, 0, id+"_min");
		max = builder.f(maxDefault, 0, id+"_max");
	}
}