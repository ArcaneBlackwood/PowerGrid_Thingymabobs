package dev.thingymabobs.mixin;

import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import java.util.function.Consumer;

public interface ThermalBuilderExt {
  ThermalBuilder withBuildCallback(Consumer<ThermalUnit> buildCallback);
}