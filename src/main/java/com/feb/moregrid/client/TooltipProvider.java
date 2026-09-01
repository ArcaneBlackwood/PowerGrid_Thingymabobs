package com.feb.moregrid.client;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TooltipProvider implements SimpleRegistry.Provider<Item, TooltipModifier> {
	public Map<Item, TooltipModifier> items = new IdentityHashMap<>();
	@Override
	public @Nullable TooltipModifier get(Item item) {
		return items.get(item);
	}
}