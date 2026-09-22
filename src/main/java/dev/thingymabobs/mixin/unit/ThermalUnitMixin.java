package dev.thingymabobs.mixin.unit;

import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.mixin.ThermalEffector;
import dev.thingymabobs.mixin.ThermalExt;

@Mixin(ThermalUnit.class)
public abstract class ThermalUnitMixin implements ThermalExt {
	@Shadow
	private float temperature;
	@Shadow
	private float ambientTemperature;
	@Shadow
	private float thermalMass;
	@Shadow
	private float dissipationFactor;
	@Shadow
	public abstract boolean hasOverheated();

	@Unique
	private float thermalMassDefault;
	@Unique
	private float dissipationFactorDefault;
	@Unique
	private Object customData;
	@Unique
	private ArrayList<ThermalEffector> effectors = new ArrayList<>();

	@Inject(
        method = "<init>",  // the jvm bytecode signature for the constructor
        at = @At("RETURN")  // signal that this void should be run at the method HEAD, meaning the first opcode
    )
	public void constructorHead(CallbackInfo ci) {
        thermalMassDefault = thermalMass;
        dissipationFactorDefault = dissipationFactor;
    }
	@Inject(
		method = "tick(F)V",
		at = @At(
			value = "FIELD",
			target = "Lorg/patryk3211/powergrid/circuits/thermal/ThermalUnit;temperature:F",
			opcode = Opcodes.PUTFIELD,
			ordinal = 0,
			shift = At.Shift.AFTER
		)
	)
	private void onTemperatureSet(float dissipationMultiplier, CallbackInfo ci) {
		for (var iter = effectors.iterator(); iter.hasNext(); ) {
			if (iter.next().effect(this)) continue;
			iter.remove();
		}
	}
	@Override
	public void addEffector(ThermalEffector effector) {
		effectors.add(effector);
	}
	@Override
	public boolean removeEffector(ThermalEffector effector) {
		return effectors.remove(effector);
	}

	
	public boolean isRemoved() {
		return false;
	}
	@Override
	public boolean isBlown() {
		return hasOverheated();
	}
	public float getAmbientTemperature() {
		return ambientTemperature;
	}
	public float getTemperature() {
		return temperature;
	}
	public void setTemperature(float temp) {
		temperature = temp;
	}
	public float getDefaultMass() {
		return thermalMassDefault;
	}
	public float getMass() {
		return thermalMass;
	}
	public void setMass(float mass) {
		thermalMass = mass;
	}
	public float getDefaultDissipation() {
		return dissipationFactorDefault;
	}
	public float getDissipation() {
		return dissipationFactor;
	}
	public void setDissipation(float factor) {
		dissipationFactor = factor;
	}
	public Object getCustomData() {
		return customData;
	}
	public void setCustomData(Object customData) {
		this.customData = customData;
	}
}