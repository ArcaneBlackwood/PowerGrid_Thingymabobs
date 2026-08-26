package com.feb.moregrid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class CustomModelItemRenderer extends CustomRenderedItemModelRenderer {
    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		Block block = ((BlockItem)stack.getItem()).getBlock();
		BakedModel replaceModel = null;
        if ((block instanceof Provider provider))
		 	replaceModel = provider.getModel(stack).get();
		if (replaceModel == null)
			replaceModel = model.getOriginalModel();
    	renderer.render(replaceModel, light);
    }
	public static interface Provider {
		public PartialModel getModel(ItemStack stack);
	}
}
