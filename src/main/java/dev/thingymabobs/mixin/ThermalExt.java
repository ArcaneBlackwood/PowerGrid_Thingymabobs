package dev.thingymabobs.mixin;

public interface ThermalExt {
	public float getDefaultMass();
	public float getMass();
	public void setMass(float mass);
	public float getDefaultDissipation();
	public float getDissipation();
	public void setDissipation(float factor);
	public Object getCustomData();
	public void setCustomData(Object customData);
}
