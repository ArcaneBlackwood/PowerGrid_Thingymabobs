package dev.thingymabobs.blocks.lavalamp;

import dev.thingymabobs.registry.ModModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class LavaLampRenderer extends SafeBlockEntityRenderer<LavaLampEntity> {
    public LavaLampRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }
	
    @Override
    protected void renderSafe(LavaLampEntity be, float partialTicks, PoseStack matrices, MultiBufferSource consumer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        int state = blockState.getValue(LavaLamp.STATE);
        Direction facing = blockState.getValue(LavaLamp.HORIZONTAL_FACING);
        boolean isPowered = LavaLamp.isPowered(state);

        float waxTop = be.getVisibleWax(true), waxBottom = be.getVisibleWax(false);
        for (LavaLampEntity.Glob glob : be.globs) {
            float progress = Math.min(1f, glob.progress + partialTicks / 20f * glob.speed);
            progress = glob.movingUp ? progress : 1f - progress;
            float bounds = Math.max(0f, (2f - progress*1.1f - glob.size*0.5f)/16f);
            float x = glob.x*bounds + 0.5f;
            float y = (progress*10f + 5)/16f;
            float z = glob.z*bounds + 0.5f;
            boolean large = glob.size > 1.5f;
            float scale = 1;

            if (progress < 0.2) {
                scale = progress*5;
                waxBottom += glob.getVolume()*(1f - scale);
            } else if (progress > 0.7) {
                float norm = (progress-0.7f)/0.3f;
                scale = 1f - norm;
                waxTop += glob.getVolume()*(norm);
            }

            rotateToFacing(CachedBuffers.partial(
                    large ? (isPowered ? ModModels.LL_GLOB_LARGE : ModModels.LL_GLOB_LARGE_OFF)
                     : (isPowered ? ModModels.LL_GLOB_SMALL : ModModels.LL_GLOB_SMALL_OFF),
                blockState), facing).translate(x, y, z).scale(scale * (large ? glob.size / 2f : glob.size))
                .color(be.colorWax).light(light).renderInto(matrices, consumer.getBuffer(RenderType.solid()));
           
        }
        //Render fixed waxes
        if (waxBottom > 0.02f) {
            rotateToFacing(CachedBuffers.partial(
                    isPowered ? ModModels.LL_GLOB_BOTTOM : ModModels.LL_GLOB_BOTTOM_OFF, blockState),
                facing).color(be.colorWax).light(light).translate(0.5f, 5f/16f, 0.5f)
                .scale(1, waxBottom, 1).renderInto(matrices, consumer.getBuffer(RenderType.solid()));
        }
        if (waxTop > 8f/18f) {
            rotateToFacing(CachedBuffers.partial(
                    isPowered? ModModels.LL_GLOB_TOP_LARGE : ModModels.LL_GLOB_TOP_LARGE_OFF, blockState),
                facing).color(be.colorWax).light(light).translate(0.5f, 13.9f/16f, 0.5f)
                .scale(1, waxTop - 4f/18f, 1).renderInto(matrices, consumer.getBuffer(RenderType.solid()));
        } else if (waxTop > 0.02f) {
            rotateToFacing(CachedBuffers.partial(
                    isPowered ? ModModels.LL_GLOB_TOP : ModModels.LL_GLOB_TOP_OFF, blockState),
                facing).color(be.colorWax).light(light).translate(0.5f, 15f/16f, 0.5f)
                .scale(1, waxTop * 18f/8f, 1).renderInto(matrices, consumer.getBuffer(RenderType.solid()));
        }

        // Render base model
        rotateToFacing(CachedBuffers.partial(
                LavaLamp.hasBulb(state) ? (isPowered ? ModModels.LL_BASE_ON : ModModels.LL_BASE) : ModModels.LL_BASE_EMPTY, blockState),
            facing).color(be.colorBase).light(light).renderInto(matrices, consumer.getBuffer(RenderType.cutout()));
        rotateToFacing(CachedBuffers.partial(
                isPowered ? ModModels.LL_BASE_TRANS_ON : ModModels.LL_BASE_TRANS, blockState),
            facing).color(be.colorGlass).light(light).renderInto(matrices, consumer.getBuffer(RenderType.translucent()));
    }

    public SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        return switch (facing) {
            case SOUTH -> buffer;
			case EAST -> buffer.rotateCentered((float) Math.PI * 0.5f, Axis.Y);
			case NORTH -> buffer.rotateCentered((float) Math.PI, Axis.Y);
			case WEST -> buffer.rotateCentered((float) Math.PI * -0.5f, Axis.Y);
            default -> null;
        };
    }
    public SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Rotation rotation) {
        return switch (rotation) {
            case NONE -> buffer;
            case CLOCKWISE_90 -> buffer.rotateCentered((float) Math.PI * 0.5f, Axis.Y);
            case CLOCKWISE_180 -> buffer.rotateCentered((float) Math.PI, Axis.Y);
            case COUNTERCLOCKWISE_90 -> buffer.rotateCentered((float) Math.PI * -0.5f, Axis.Y);
            default -> null;
        };
    }
}
