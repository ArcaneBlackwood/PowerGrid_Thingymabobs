package com.feb.moregrid.mixin.unit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.feb.moregrid.mixin.LevelTransformer;
import com.feb.moregrid.mixin.LinkBehaviourExt;
import com.simibubi.create.content.redstone.link.LinkBehaviour;

@Mixin(LinkBehaviour.class)
public abstract class LinkBehaviourMixin implements LinkBehaviourExt {
	@Unique
    public LevelTransformer transformer;

	@Override
	public void setTransformer(LevelTransformer transformer) {
		this.transformer = transformer;
	}

    @Override
    public LevelTransformer getTransform() {
		return transformer;
	}
}
