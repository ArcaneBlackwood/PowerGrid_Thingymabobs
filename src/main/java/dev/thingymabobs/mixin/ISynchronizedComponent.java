package dev.thingymabobs.mixin;

import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import net.minecraft.network.FriendlyByteBuf;

public interface ISynchronizedComponent {
   public void writeToSync(PlacedComponent placed, FriendlyByteBuf var1);
   public void readFromSync(PlacedComponent placed, FriendlyByteBuf var1);
}
