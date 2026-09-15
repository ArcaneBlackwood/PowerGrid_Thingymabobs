package dev.thingymabobs.mixin.unit;

import java.util.ArrayList;
import java.util.List;
import org.patryk3211.powergrid.circuits.circuitboard.BakedCircuit;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import net.minecraft.network.FriendlyByteBuf;

@Mixin(CircuitBoardBlockEntity.class)
public class CircuitBoardBlockEntityMixin {
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
		/*if (baked == null) return;
		var level = ((CircuitBoardBlockEntity)(Object) this).getLevel();
		if (level == null) return;

		toEnable.clear();
		for (INode node : baked.internalNodes) {
			if (node instanceof ICouplingNode cnode && cnode.getNetwork() == null) {
				toEnable.add(cnode);
			}
		}
		if (toEnable.isEmpty()) return;
		var eb = ((CircuitBoardBlockEntity)(Object) this).getElectricBehaviour();
		if (eb == null) return;

		var network = GlobalElectricNetworks.getWorldNetworks(level).newNetwork();
		for(INode node : toEnable) {
			network.addNode(node);
		}
		eb.tracedAdd(toEnable);*/
	}
}