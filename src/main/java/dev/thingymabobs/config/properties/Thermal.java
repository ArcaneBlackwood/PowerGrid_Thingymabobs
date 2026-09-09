package dev.thingymabobs.config.properties;

import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;

public class Thermal extends ASubProp  {
	private ConfigBase.ConfigFloat mass = null, power = null, tempMax = null, overheat = null;
	private float massDefault, powerDefault, tempMaxDefault, overheatDefault;
	protected boolean unloaded = false;
	protected Thermal(float mass, float power, float tempMax, float overheat) {
		this.massDefault = mass;
		this.powerDefault = power;
		this.tempMaxDefault = tempMax;
		this.overheatDefault = overheat;
	}
	public Thermal setTempMax(float tempMax) {
		this.tempMaxDefault = tempMax;
		return this;
	}
	public Thermal setOverheat(float overheat) {
		this.overheatDefault = overheat;
		return this;
	}

	public float getMass() {
		if (unloaded) return massDefault;
		return mass.getF();
	}
	public float getPower() {
		if (unloaded) return powerDefault;
		return power.getF();
	}
	public float getTemp() {
		if (unloaded) return tempMaxDefault;
		return tempMax.getF();
	}
	public float getOverheat() {
		if (unloaded) return overheatDefault;
		return overheat.getF();
	}
	@Override
	public void onLoad() {
		unloaded = false;
	}
	@Override
	public Class<?> getType() {
		return Thermal.class;
	}
	@Override
	public void register(String id, CProperties.Builder builder) {
		mass = builder.f(massDefault, 0f, id+"_mass");
		power = builder.f(powerDefault, 0f, id+"_power");
		tempMax = builder.f(tempMaxDefault, 0f, id+"_tempMax");
		overheat = builder.f(overheatDefault, 0f, id+"_overheat");
	}
	public ThermalBehaviour createBehaviour(SmartBlockEntity be) {
		return ThermalBehaviour.simple(be,  getMass(), getPower() / (getTemp() - 22.0F), getOverheat());
	}
	public ThermalBuilder apply(ThermalBuilder.IEmitter thermals) {
        return thermals.builder()
			.setMaxPower(getPower(), getTemp())
			.setThermalMass(getMass()).setOverheatTemperature(getOverheat());
	}

	protected CProperties.Prop parent = null;
	public CProperties.Prop build() {
		CProperties.Prop last = parent;
		parent = null;
		return last;
	}
}