package com.feb.moregrid.mixin.unit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.feb.moregrid.mixin.LevelTransformer;
import com.feb.moregrid.mixin.LinkBehaviourExt;
import com.feb.moregrid.mixin.RedstoneLinkNetworkHandlerExt;
import com.feb.moregrid.util.SableUtils;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;

@Mixin(RedstoneLinkNetworkHandler.class)
public abstract class RedstoneLinkNetworkHandlerMixin implements RedstoneLinkNetworkHandlerExt {
	@Unique
    protected final List<RedstoneLinkNetworkHandlerExt.Network> pending = new ArrayList<>();

    // shadow the existing method so we can call it from the overwrite below
    @Shadow
    public abstract Set<IRedstoneLinkable> getNetworkOf(LevelAccessor world, IRedstoneLinkable actor);
    @Shadow
	public AtomicInteger globalPowerVersion;


	private static final Vector3d temp1 = new Vector3d(), temp2 = new Vector3d();
	private static boolean moreGrid$withinRange(IRedstoneLinkable from, IRedstoneLinkable to, LevelAccessor level) {
		if (from == to)
			return true;
		Vector3d fromPos = SableUtils.getGlobalPos(level, from.getLocation(), temp1);
		Vector3d toPos = SableUtils.getGlobalPos(level, to.getLocation(), temp2);
		return fromPos.distanceSquared(toPos) <= Mth.square(AllConfigs.server().logistics.linkRange.get());
	}

    @Override
    public void tick() {
        
		for (Network network : pending) {
			for (Iterator<IRedstoneLinkable> iterator = network.links.iterator(); iterator.hasNext(); ) {
				IRedstoneLinkable other = iterator.next();
				if (!other.isAlive()) {
					iterator.remove();
					continue;
				}
			}
			for (Iterator<IRedstoneLinkable> destins = network.links.iterator(); destins.hasNext(); ) {
				IRedstoneLinkable destin = destins.next();
				if (!destin.isListening() || !destin.isAlive()) continue;

				LevelTransformer destinTransformer = destin instanceof LinkBehaviourExt ext ? ext.getTransform() : null;
				int power = 0;
				for (Iterator<IRedstoneLinkable> sources = network.links.iterator(); sources.hasNext(); ) {
					IRedstoneLinkable source = sources.next();
					if (source == destin || source.isListening() || !moreGrid$withinRange(destin, source, network.world)) continue;

					int powerOther = source.getTransmittedStrength();
					LevelTransformer transformer = source instanceof LinkBehaviourExt ext ? ext.getTransform() : null;
					if (transformer != null) powerOther = transformer.transform(powerOther, source, destin);
					if (destinTransformer != null) powerOther = destinTransformer.transform(powerOther, source, destin);
					power = Math.max(powerOther, power);
					if (power == 15) break;
				}

				if (destin instanceof LinkBehaviour linkBehaviour) linkBehaviour.newPosition = true;
				destin.setReceivedStrength(power);
			}
		}
		pending.clear();
    }

    @Inject(at = @At("HEAD"), cancellable = true, method = "updateNetworkOf", require = 1)
    public void moregrid$updateNetworkOf(LevelAccessor world, IRedstoneLinkable actor, CallbackInfo ci) {
		globalPowerVersion.incrementAndGet();
        Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
        if (network == null || network.size() < 2) {
			if (actor.isListening()) actor.setReceivedStrength(0);
			return;
		};
        pending.add(new RedstoneLinkNetworkHandlerExt.Network(network, world));
    	ci.cancel();
    }
}
