package dev.thingymabobs.mixin;

public interface ThermalExt {
	public boolean isRemoved();
	public boolean isBlown();
	public void addEffector(ThermalEffector effector);
	public boolean removeEffector(ThermalEffector effector);
	public float getAmbientTemperature();
	public float getTemperature();
	public void setTemperature(float temp);
	public float getDefaultMass();
	public float getMass();
	public void setMass(float mass);
	public float getDefaultDissipation();
	public float getDissipation();
	public void setDissipation(float factor);
	public Object getCustomData();
	public void setCustomData(Object customData);
}
