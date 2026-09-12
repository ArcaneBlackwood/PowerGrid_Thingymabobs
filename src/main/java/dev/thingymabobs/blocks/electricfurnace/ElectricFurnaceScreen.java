package dev.thingymabobs.blocks.electricfurnace;

import org.joml.Matrix4f;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.registry.ModLang;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElectricFurnaceScreen extends AbstractSimiContainerScreen<ElectricFurnaceMenu> {
	public static final ResourceLocation SPRITE_SHEET = Thingymabobs.texture("gui/electric_furnace");

	public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		setWindowSize(176, 168);
	}

	private static Component TITLE = ModLang.translateDirect("gui.electric_furnace.title");
	@Override
	protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
		gui.drawCenteredString(font, TITLE, leftPos + 97, topPos + 4, 0x404040);
		gui.blit(SPRITE_SHEET, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		{
			int height = Mth.floor(menu.getTempNorm() * 72);
			if (height != 0)
				gui.blit(SPRITE_SHEET, leftPos+9, topPos+80 - height, imageWidth, 72-height, 5, height);
		} {
			boolean powered = menu.isPowered();
			gui.blit(SPRITE_SHEET, leftPos+36, topPos+39, imageWidth + (powered ? 0 : 10), 72, 10, 10);
		} {
			int slotTopStart = leftPos + 97 - ElectricFurnaceEntity.SLOTS_INPUT * 9;
			for (int i = 0, j = 0; i < ElectricFurnaceEntity.SLOTS_INPUT * 18; i+=18, j++) {
				gui.blit(SPRITE_SHEET, slotTopStart + i, topPos+17, imageWidth + 5, 0, 18, 36);
				int progress = Mth.floor(menu.getProgress(j) * 10);
				if (progress != 0)
					gui.blit(SPRITE_SHEET, slotTopStart + i + 3, topPos+39, imageWidth + 5, 54, 11, progress);
			}
				
		} {
			int slotTopStart = leftPos + 97 - ElectricFurnaceEntity.SLOTS_OUTPUT * 9;
			for (int i = 0; i < ElectricFurnaceEntity.SLOTS_OUTPUT * 18; i+=18)
				gui.blit(SPRITE_SHEET, slotTopStart + i, topPos+53, imageWidth + 5, 36, 18, 18);
		}
	}

	@Override
	protected void renderForeground(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(gui, mouseX, mouseY, partialTicks);
		{
			int slotTopStart = leftPos + 98 - ElectricFurnaceEntity.SLOTS_INPUT * 9;
			for (int i = 0, j = 0; i < ElectricFurnaceEntity.SLOTS_INPUT * 18; i+=18, j++) {
				float progressNorm = menu.getBurnProgress(true, j);
				int progress = Mth.floor(progressNorm * 16);
				if (progress == 0) continue;
				float alpha = 0.75f*progressNorm + 0.25f;
				blitColor(gui, SPRITE_SHEET, slotTopStart + i, topPos+33-progress, 300, imageWidth, 97-progress, 16, progress, 1, 1, 1, alpha);
			}
		} {
			int slotTopStart = leftPos + 98 - ElectricFurnaceEntity.SLOTS_OUTPUT * 9;
			for (int i = 0, j = 0; i < ElectricFurnaceEntity.SLOTS_OUTPUT * 18; i+=18, j++) {
				float progressNorm = menu.getBurnProgress(false, j);
				int progress = Mth.floor(progressNorm * 16);
				if (progress == 0) continue;
				float alpha = 0.75f*progressNorm + 0.25f;
				blitColor(gui, SPRITE_SHEET, slotTopStart + i, topPos+69-progress, 300, imageWidth, 97-progress, 16, progress, 1, 1, 1, alpha);
			}
		}
	}

	public static void blitColor(GuiGraphics gui, ResourceLocation resource, int x, int y, int z, int u, int v, int w, int h, float r, float g, float b, float a) {
		final float scale = 1/256f;
		blitColor(gui, resource, x, x+w, y, y+h, z, u*scale, (u+w)*scale, v*scale, (v+h)*scale, r, g, b, a);
	}
	public static void blitColor(GuiGraphics gui, ResourceLocation resource, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float r, float g, float b, float a) {
		RenderSystem.setShaderTexture(0, resource);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.enableBlend();
		Matrix4f matrix4f = gui.pose().last().pose();
		BufferBuilder bufferbuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)z).setUv(u1, v1).setColor(r, g, b, a);
		bufferbuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)z).setUv(u1, v2).setColor(r, g, b, a);
		bufferbuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)z).setUv(u2, v2).setColor(r, g, b, a);
		bufferbuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)z).setUv(u2, v1).setColor(r, g, b, a);
		BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
		RenderSystem.disableBlend();
   	}
}
