package dev.thingymabobs.mixin.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.patryk3211.powergrid.circuits.circuitboard.BakedCircuit;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.mixin.IDestroyComponent;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import dev.thingymabobs.mixin.PlacedComponentExt;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;

@Mixin(CircuitBoardBlockEntity.class)
public abstract class CircuitBoardBlockEntityMixin extends ElectricBlockEntity {
	public CircuitBoardBlockEntityMixin() {
		super(null, null, null);
	}

	@Shadow
	private BakedCircuit baked;
	
	@Inject(
		method = "writeToSync",
		at = @At("RETURN"),
		require = 1
	)
	private void thingymabobs$writeToSync(
		FriendlyByteBuf buffer,
		CallbackInfo cir
	) {
		if (baked == null) return;
		for (var placed : baked.tickedComponents) {
			if (!(placed.component instanceof ISynchronizedComponent sync)) continue;
			sync.writeToSync(placed, buffer);
		}
	}
	@Inject(
		method = "readFromSync",
		at = @At("RETURN")
	)
	private void thingymabobs$readFromSync(
		FriendlyByteBuf buffer,
		CallbackInfo cir
	) {
		if (baked == null) return;
		for (var placed : baked.tickedComponents) {
			if (!(placed.component instanceof ISynchronizedComponent sync)) continue;
			sync.readFromSync(placed, buffer);
		}
	}

	private List<INode> toEnable = new ArrayList<>();

	@Inject(method = "bakeCircuit", at = @At("TAIL"))
	private void thingymabobs$bakeCircuit(CallbackInfo ci) {
		Object2ObjectOpenHashMap<UUID, PlacedComponent> uuidMap = new Object2ObjectOpenHashMap<>(baked.tickedComponents.size());
		for (PlacedComponent placed : baked.tickedComponents)
			uuidMap.put(placed.uuid, placed);
		for (ThermalUnit thermal : baked.thermalUnits) {
			var placed = uuidMap.get(thermal.getId());
			if (!(placed instanceof PlacedComponentExt placedExt)) continue;
			placedExt.getThermalUnits().add(thermal);
		}
			
			
			
		if (baked == null) return;
		var level = ((CircuitBoardBlockEntity)(Object) this).getLevel();
		if (level == null) return;

		toEnable.clear();
		for (INode node : baked.internalNodes) {
			if (node instanceof ICouplingNode cnode && cnode.getNetwork() == null) {
				toEnable.add(cnode);
			}
		}
		if (toEnable.isEmpty()) return;
		Thingymabobs.LOGGER.info("Try add to network: "+toEnable);
		/*var eb = ((CircuitBoardBlockEntity)(Object) this).getElectricBehaviour();
		if (eb == null) return;

		var network = GlobalElectricNetworks.getWorldNetworks(level).newNetwork();
		for(INode node : toEnable) {
			network.addNode(node);
		}
		eb.tracedAdd(toEnable);*/
	}
	@Override
	public void destroy() {
		super.destroy();
		for (var placed : baked.tickedComponents) {
			if (!(placed.component instanceof IDestroyComponent sync)) continue;
			sync.onDestroy(placed);
		}
	}

	
}