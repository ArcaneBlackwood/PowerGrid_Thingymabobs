package dev.thingymabobs.blocks.electricfurnace;

import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.util.BakedQuadEditor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class ElectricFurnaceRenderer extends SafeBlockEntityRenderer<ElectricFurnaceEntity> {
	public static final float DOOR_SPEED = 15;
	public static int QUALITY_RENDER_DISTANCE = 32;
	public ElectricFurnaceRenderer(BlockEntityRendererProvider.Context context) {
		super();
	}
	
	@Override
	protected void renderSafe(ElectricFurnaceEntity be, float partialTicks, PoseStack matrices, MultiBufferSource consumer, int light, int overlay) {
		LocalPlayer player = Minecraft.getInstance().player;
		boolean qualityRender = be.getBlockPos().distToCenterSqr(player.getEyePosition())
			< QUALITY_RENDER_DISTANCE*QUALITY_RENDER_DISTANCE;

		matrices.pushPose();
		try {
			BlockState blockState = be.getBlockState();
			rotateToFacing(matrices, blockState.getValue(ElectricFurnace.HORIZONTAL_FACING));
			if (!qualityRender) {
				CachedBuffers.partial(
						ModModels.FURN_DOOR_CLOSE, blockState)
					.light(light)
					.renderInto(matrices, consumer.getBuffer(RenderType.solid()));
				return;
			}

			boolean buttonPress = blockState.getValue(ElectricFurnace.BUTTON);
				int coilLevel = be.getCoilLevel();
			CachedBuffers.partial(
					buttonPress ? (coilLevel == 0 ? ModModels.FURN_BUTTON_PRESS : ModModels.FURN_BUTTON_ON) : 
					ModModels.FURN_BUTTON, blockState)
				.light(light)
				.renderInto(matrices, consumer.getBuffer(RenderType.solid()));
			
			float tempNorm = be.getTempNorm();
			renderTemperature(tempNorm, be, matrices, consumer, light);

			
			boolean doorOpen = be.isDoorOpen();
			if (be.doorState > 0.005f) {
				float doorState = approach(be.doorState, doorOpen ? 1 : 0, DOOR_SPEED * partialTicks / 20f);
				int tempLevel = be.getTempLevel();
				CachedBuffers.partial(ModModels.FURN_INTERNAL[coilLevel + Mth.floor(tempLevel) * 4], blockState)
					.light(light).renderInto(matrices, consumer.getBuffer(RenderType.solid()));

				tempLevel = Math.min(Mth.floor(tempNorm * 4), 3);
				if (tempLevel > 0)
					CachedBuffers.partial(ModModels.FURN_GLOW[tempLevel], blockState)
						.color(255, 255, 255, Mth.floor(tempNorm * 255)).light(light)
						.renderInto(matrices, consumer.getBuffer(RenderTypes.additive()));

				float doorAngle = doorState * Mth.PI * 0.75f;
				Vector3f doorHinge = new Vector3f(2,0,2).mul(1/16f);
				CachedBuffers.partial(ModModels.FURN_DOOR, blockState)
					.light(light)
					.translate(doorHinge).rotate(Axis.Y, doorAngle).translateBack(doorHinge)
					.renderInto(matrices, consumer.getBuffer(RenderType.solid()));
			} else {
				CachedBuffers.partial(
						ModModels.FURN_DOOR_CLOSE, blockState)
					.light(light)
					.renderInto(matrices, consumer.getBuffer(RenderType.solid()));
			}
		} finally {
			matrices.popPose();
		}

	}
	public static float approach(float a, float b, float time) {
		return (b*time+a)/(time+1);
	}
	public void rotateToFacing(PoseStack pose, Direction facing) {
		if (facing == Direction.NORTH) return;
		pose.translate(0.5f, 0, 0.5f);
		pose.mulPose(switch (facing) {
			case EAST -> new Quaternionf().rotateY(Mth.PI * -0.5f);
			case SOUTH -> new Quaternionf().rotateY(Mth.PI);
			case WEST -> new Quaternionf().rotateY(Mth.PI * 0.5f);
			default -> new Quaternionf();
		});
		pose.translate(-0.5f, 0, -0.5f);
	}
	protected static void renderTemperature(float value, BlockEntity be, PoseStack matrices, MultiBufferSource consumer, int light) {
		BakedModel model = ModModels.FURN_TEMPERATURE.get();
		BakedQuadEditor quad;
		{
			List<BakedQuad> quads = model.getQuads(
				be.getBlockState(), null, be.getLevel().random, ModelData.EMPTY, null
			);
			if (quads.size() == 0) return;
			quad = BakedQuadEditor.fromClone(quads.get(0), DefaultVertexFormat.BLOCK);
		}
		
		quad.setUV(2, quad.getUV(1).lerp(quad.getUV(2), value));
		quad.setUV(3, quad.getUV(0).lerp(quad.getUV(3), value));
		quad.setPosition(2, quad.getPosition(1).lerp(quad.getPosition(2), value));
		quad.setPosition(3, quad.getPosition(0).lerp(quad.getPosition(3), value));
		consumer.getBuffer(RenderType.solid()).putBulkData(matrices.last(), quad.compile(), 1f,1f,1f,1f, light, OverlayTexture.NO_OVERLAY);
	}
}
