package dev.thingymabobs.mixin.unit;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.thingymabobs.mixin.ThermalEffector;
import dev.thingymabobs.mixin.ThermalExt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(ThermalBehaviour.class)
public abstract class ThermalBehaviourMixin implements ThermalExt {
	@Shadow
	private float temperature;
	@Shadow
	private float cachedAmbientTemperature;
	@Shadow
	private float thermalMass;
	@Shadow
	private float dissipationFactor;
	@Shadow
	public abstract BehaviourType<?> getType();
	@Shadow
	public abstract boolean isOverheated();
	@Shadow
   	private BlockPos trackedBehaviour;

	@Unique
	private float thermalMassDefault;
	@Unique
	private float dissipationFactorDefault;
	@Unique
	private ArrayList<ThermalEffector> effectors = new ArrayList<>();

	Object customData;

	@Inject(
        method = "<init>",  // the jvm bytecode signature for the constructor
        at = @At("RETURN")  // signal that this void should be run at the method HEAD, meaning the first opcode
    )
	public void constructorHead(CallbackInfo ci) {
        thermalMassDefault = thermalMass;
        dissipationFactorDefault = dissipationFactor;
    }
	@Inject(
		method = "tick",
		at = @At(
			value = "FIELD",
			target = "Lorg/patryk3211/powergrid/electricity/base/ThermalBehaviour;temperature:F",
			opcode = Opcodes.PUTFIELD,
			ordinal = 2,
			shift = At.Shift.AFTER
		)
	)
	private void onTemperatureSet(CallbackInfo ci) {
		for (var iter = effectors.iterator(); iter.hasNext(); ) {
			if (iter.next().effect(this)) continue;
			iter.remove();
		}
	}
	@Override
	public void addEffector(ThermalEffector effector) {
		Level world = ((BlockEntityBehaviour)(Object)this).getWorld();
		ThermalBehaviour tracked = this.trackedBehaviour != null ? ThermalBehaviour.getThermal(world, this.trackedBehaviour) : null;
		if (tracked == null)
			effectors.add(effector);
		else
			((ThermalBehaviourMixin)(Object)tracked).effectors.add(effector);
	}
	@Override
	public boolean removeEffector(ThermalEffector effector) {
		Level world = ((BlockEntityBehaviour)(Object)this).getWorld();
		ThermalBehaviour tracked = this.trackedBehaviour != null ? ThermalBehaviour.getThermal(world, this.trackedBehaviour) : null;
		if (tracked == null)
			return effectors.remove(effector);
		else
			return ((ThermalBehaviourMixin)(Object)tracked).effectors.remove(effector);
	}


	public boolean isRemoved() {
		return ((BlockEntityBehaviour)(Object)this).blockEntity.isRemoved();
	}
	@Override
	public boolean isBlown() {
		return isOverheated();
	}
	public float getAmbientTemperature() {
		return cachedAmbientTemperature;
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