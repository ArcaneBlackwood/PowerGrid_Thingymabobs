package dev.thingymabobs.config.properties;

import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;

public class Resistance extends ASubProp {
	public String comment;
	public ConfigBase.ConfigFloat value = null;
	public float defaultValue;
	protected boolean unloaded = false;
	public float get() {
		if (unloaded) return defaultValue;
		return value.getF();
	}
	public Resistance(float value) {
		this.defaultValue = value;
	}
	public Resistance(float value, String comment) {
		this.defaultValue = value;
		this.comment = comment;
	}
	@Override
	public void onLoad() {
		unloaded = false;
	}
	@Override
	public Class<?> getType() {
		return Resistance.class;
	}
	@Override
	public void register(String id, CProperties.Builder builder) {
		if (comment != null && !comment.isEmpty())
			value = builder.f(defaultValue, 0, id, comment);
		else
			value = builder.f(defaultValue, 0, id);
	}
}