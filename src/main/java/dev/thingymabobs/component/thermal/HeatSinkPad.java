package dev.thingymabobs.component.thermal;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.util.BakedQuadEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class HeatSinkPad implements BakedModel {
	public ArrayList<BakedQuad> preComutedQuads = new ArrayList<>();
	public void clear() {
		preComutedQuads.clear();
	}
	public void addPad(float depth, float x, float y, float w, float h) {
		w -= 1;
		h -= 1;
		List<BakedQuad> quads = ModModels.HEAT_PAD.get().getQuads(null, null, null, ModelData.EMPTY, RenderType.SOLID);
		preComutedQuads.ensureCapacity(preComutedQuads.size() + quads.size());
		final float norm1 = 1 / 16f;
		for (BakedQuad quad : quads) {
			var edit = BakedQuadEditor.fromClone(quad, DefaultVertexFormat.BLOCK);
			for (int i=0; i<4; i++) {
				Vector3f pos = edit.getPosition(i).mul(16);
				Vector2f uv = edit.getUV(i);
				pos.x += depth;
				if (pos.z > 0.5) pos.z += w;
				if (pos.y > 0.5) pos.y += h;
				pos.z += x;
				pos.y += y;
				boolean doUVX = uv.x > 15f, doUVY = uv.y > 7f;
				if (doUVX) uv.x -= 16;
				if (doUVX && uv.x > 0.25) uv.x += w;
				if (doUVY) uv.y -= 8;
				if (doUVY && uv.y > 0.25) uv.y += h;
				edit.setPosition(i, pos.mul(norm1));
				edit.setUV(i, uv);
			}
			preComutedQuads.add(edit.compile());
		}
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}
	@Override
	public boolean isGui3d() {
		return false;
	}
	@Override
	public boolean usesBlockLight() {
		return false;
	}
	@Override
	public boolean isCustomRenderer() {
		return false;
	}
	@Override
	public TextureAtlasSprite getParticleIcon() {
		return null;
	}
	@Override
	public ItemTransforms getTransforms() {
		return ItemTransforms.NO_TRANSFORMS;
	}
	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		return null;
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
		return List.of();
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
		return preComutedQuads;
	}

	public void render(PoseStack pose, MultiBufferSource buffer, RenderType renderType, int light, int overlay) {
		ModelBlockRenderer renderer = Minecraft.getInstance()
					.getBlockRenderer()
			.getModelRenderer();
		renderer.renderModel(
			pose.last(), buffer.getBuffer(renderType), null,
			this, 1.0F, 1.0F, 1.0F,
			light, overlay, ModelData.EMPTY, renderType
		);
	}
}
