package dev.thingymabobs.config.properties;

import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.util.Mth;

public class BatterySpec extends ASubProp implements org.patryk3211.powergrid.electricity.battery.BatterySpec {

	public ConfigBase.ConfigFloat initial = null, max = null,
		voltMin = null, voltMax = null, resMin = null, resMax = null, resCurve = null;
	public float initialDefault, maxDefault,
		voltMinDefault, voltMaxDefault, resMinDefault, resMaxDefault, resCurveDefault;
	
	private boolean computed = false, unloaded = false;
	private float resA, resB, resC;
	
	public BatterySpec(float max, float initial, float voltMin, 
		float voltMax, float resMin, float resMax, float resCurve) {
		this.maxDefault = max;
		this.initialDefault = initial;
		this.voltMinDefault = voltMin;
		this.voltMaxDefault = voltMax;
		this.resMinDefault = resMin;
		this.resMaxDefault = resMax;
		this.resCurveDefault = resCurve;
	}
	public float getInitial() {
		if (unloaded) return initialDefault;
		return initial.getF();
	}
	public float getMax() {
		if (unloaded) return maxDefault;
		return max.getF();
	}
	public float getVoltMin() {
		if (unloaded) return voltMinDefault;
		return voltMin.getF();
	}
	public float getVoltMax() {
		if (unloaded) return voltMaxDefault;
		return voltMax.getF();
	}
	public float getResistMin() {
		if (unloaded) return resMinDefault;
		return resMin.getF();
	}
	public float getResistMax() {
		if (unloaded) return resMaxDefault;
		return resMax.getF();
	}
	public float getResistCurve() {
		if (unloaded) return resCurveDefault;
		return resCurve.getF();
	}
	@Override
	public void onLoad() {
		unloaded = false;
		precompute();
	}
	public void precompute() {
		float diff = getResistMax() - getResistMin();
		resB = (diff < 0 ? -1 : 1) * getResistCurve();
		float expA = diff / (1 - (float)Math.exp(-resB));
		resA = (float)Math.log(expA);
		resC = getResistMax() - expA;
		computed = true;
	}
	@Override
	public Class<?> getType() {
		return BatterySpec.class;
	}
	@Override
	public void register(String id, CProperties.Builder builder) {
		max = builder.f(maxDefault, 0, id+"_max_charge");
		initial = builder.f(initialDefault, 0, id+"_initial_charge");
		voltMin = builder.f(voltMinDefault, 0, id+"_voltatge_min");
		voltMax = builder.f(voltMaxDefault, 0, id+"_voltage_max");
		resMin = builder.f(resMinDefault, 0, id+"_resistance_min");
		resMax = builder.f(resMaxDefault, 0, id+"_resistance_max");
		resCurve = builder.f(resCurveDefault, 0, id+"_resistance_curve",
			"Value of zero represents a straight resistance curve.  Lead acids curve is about 2.7, and potato 0.77");
	}

	@Override
	public float calculateResistance(float charge) {
		if (computed == false) precompute();
		return (float)Math.exp(resA - resB * charge) + resC;
	}
	@Override
	public float calculateVoltage(float charge) {
		return Mth.lerp(charge, getVoltMin(), getVoltMax());
	}
	@Override
	public float getInitialCharge() {
		return getInitial();
	}
	@Override
	public float getMaxCharge() {
		return getMax();
	}
}
