package dev.thingymabobs.mixin.unit.client;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.thingymabobs.mixin.IRenderableUIComponent;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(ComponentFootprint.class)
public abstract class ComponentFootprintMixin {
	@Inject(at = @At("HEAD"), method = "render", require = 1)
	public void render(@NotNull GuiGraphics ctx, @NotNull Component component, int x, int y, boolean hovering, CallbackInfo ci) {
		if (component instanceof IRenderableUIComponent compRender) {
			PoseStack ms = ctx.pose();
			ms.pushPose();
			compRender.renderUI(ctx, (ComponentFootprint)(Object)this, x, y, hovering);
			ms.popPose();
		}
	}
}
