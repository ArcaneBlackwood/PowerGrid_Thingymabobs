package dev.thingymabobs.mixin;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public interface IDestroyComponent {
	public void onDestroy(@NotNull PlacedComponent placed);
}
