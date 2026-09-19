package dev.thingymabobs.util;

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;

public class ThermalElectricWire extends AbstractElectricWire {
	protected float dissipation;
	protected double nextPower;

	public ThermalElectricWire(float dissipation) {
		super(null, null);
		this.dissipation = dissipation;
	}

	@Override
	public double conductance() {
		return 0;
	}

	@Override
	public double power() {
		return nextPower;
	}

	public void setTemperature(float ambientTemp, float target) {
		nextPower = dissipation*(target - ambientTemp);
	}
}
