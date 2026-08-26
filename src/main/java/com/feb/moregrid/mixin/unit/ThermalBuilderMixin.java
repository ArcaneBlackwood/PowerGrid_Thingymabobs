package com.feb.moregrid.mixin.unit;

import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.feb.moregrid.mixin.ThermalBuilderExtension;

import java.util.function.Consumer;

@Mixin(ThermalBuilder.class)
public abstract class ThermalBuilderMixin implements ThermalBuilderExtension {
  @Unique
  private Consumer<ThermalUnit> buildCallback;

  @Override
  public ThermalBuilder withBuildCallback(Consumer<ThermalUnit> buildCallback) {
    this.buildCallback = buildCallback;
    return (ThermalBuilder) (Object) this;
  }
  
	@Inject(
		method = "build",
		at = @At("RETURN")
	)
	private void moregrid$onBuild(
		CallbackInfoReturnable<ThermalUnit> cir
	) {
		if (this.buildCallback == null) return;
		this.buildCallback.accept(cir.getReturnValue());
	}
}