package com.feb.moregrid.mixin.unit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import com.feb.moregrid.mixin.LevelTransformer;
import com.feb.moregrid.mixin.LinkBehaviourExt;
import com.feb.moregrid.mixin.RedstoneLinkNetworkHandlerExt;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;

import net.minecraft.world.level.LevelAccessor;

@Mixin(RedstoneLinkNetworkHandler.class)
public abstract class RedstoneLinkNetworkHandlerMixin implements RedstoneLinkNetworkHandlerExt {
	@Unique
    protected final List<Set<IRedstoneLinkable>> pending = new ArrayList<>();

    // shadow the existing method so we can call it from the overwrite below
    @Shadow
    public abstract Set<IRedstoneLinkable> getNetworkOf(LevelAccessor world, IRedstoneLinkable actor);

    @Override
    public void tick() {
        
		for (Set<IRedstoneLinkable> network : pending) {
			for (Iterator<IRedstoneLinkable> iterator = network.iterator(); iterator.hasNext(); ) {
				IRedstoneLinkable other = iterator.next();
				if (!other.isAlive()) {
					iterator.remove();
					continue;
				}
			}
			for (Iterator<IRedstoneLinkable> destins = network.iterator(); destins.hasNext(); ) {
				IRedstoneLinkable destin = destins.next();
				if (!destin.isListening() || !destin.isAlive()) continue;

				LevelTransformer destinTransformer = destin instanceof LinkBehaviourExt ext ? ext.getTransform() : null;
				int power = 0;
				for (Iterator<IRedstoneLinkable> sources = network.iterator(); sources.hasNext(); ) {
					IRedstoneLinkable source = sources.next();
					if (source == destin || source.isListening() || !RedstoneLinkNetworkHandler.withinRange(destin, source)) continue;

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

    @Overwrite
    public void updateNetworkOf(LevelAccessor world, IRedstoneLinkable actor) {
        Set<IRedstoneLinkable> network = getNetworkOf(world, actor);
        if (network == null || network.size() < 2) {
			if (actor.isListening()) actor.setReceivedStrength(0);
			return;
		};
        pending.add(network);
    }
}
