package dev.thingymabobs.blocks.Zoey.PlasmaGlobe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import org.joml.Vector3f;

public class PlasmaGlobeEntityRenderer implements BlockEntityRenderer<PlasmaGlobeEntity> {

	private static final Vector3f ELECTRODE_POINT = new Vector3f(0f, 0f, 0f);
	private static final float R = 0.4f, G = 0.9f, B = 1.0f, A = 1.0f;

	public PlasmaGlobeEntityRenderer(BlockEntityRendererProvider.Context context) {
		// nothing to stash yet - required so this can be used as a renderer factory
	};

	@Override
	public void render(PlasmaGlobeEntity entity, float partialTick, PoseStack poseStack,
						MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

		poseStack.pushPose();

		// Tendril math lives in -1..1 space centered on the electrode, NOT the block center -
		// anchor to the real bulb height and scale to the real electrode->glass distance.
		poseStack.translate(0.5, PlasmaGlobeEntity.ElectrodePoint, 0.5);
		float glassDist = (float) PlasmaGlobeEntity.DistToGlass;
		poseStack.scale(glassDist, glassDist, glassDist);

		VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
		PoseStack.Pose pose = poseStack.last();

		for (PlasmaTendril tendril : entity.getTendrils()) {
			Vector3f tip = tendril.GetCurrentPoint();
			drawLine(buffer, pose, ELECTRODE_POINT, tip);
		};

		poseStack.popPose();
	};

	private static void drawLine(VertexConsumer buffer, PoseStack.Pose pose, Vector3f a, Vector3f b) {
		buffer.addVertex(pose, a.x, a.y, a.z)
			.setColor(R, G, B, A)
			.setNormal(pose, 0, 1, 0);

		buffer.addVertex(pose, b.x, b.y, b.z)
			.setColor(R, G, B, A)
			.setNormal(pose, 0, 1, 0);
	};
};