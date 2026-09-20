package dev.thingymabobs.mixin.unit;

import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.mixin.ThermalExt;

@Mixin(ThermalUnit.class)
public abstract class ThermalUnitMixin implements ThermalExt {
	@Shadow
	private float thermalMass;
	@Shadow
	private float dissipationFactor;

	@Unique
	private float thermalMassDefault;
	@Unique
	private float dissipationFactorDefault;

	Object customData;

	@Inject(
        method = "<init>",  // the jvm bytecode signature for the constructor
        at = @At("RETURN")  // signal that this void should be run at the method HEAD, meaning the first opcode
    )
	public void constructorHead(CallbackInfo ci) {
        thermalMassDefault = thermalMass;
        dissipationFactorDefault = dissipationFactor;
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