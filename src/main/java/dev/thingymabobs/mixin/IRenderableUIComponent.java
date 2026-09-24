package dev.thingymabobs.mixin;

import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IRenderableUIComponent {
	@OnlyIn(Dist.CLIENT)
	public void renderUI(GuiGraphics ctx, ComponentFootprint that, int x, int y, boolean hovering);
}
