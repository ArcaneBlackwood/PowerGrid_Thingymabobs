package com.feb.moregrid.client;

import java.util.HashMap;
import java.util.Map;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.feb.moregrid.mixin.SoundBufferExt;
import com.feb.moregrid.mixin.SoundEngineExt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

@OnlyIn(Dist.CLIENT)
public class SoundScape extends AbstractTickableSoundInstance {
	private Map<BlockPos, SourceState> sources = new HashMap<>();
	private float invSoundDistance;

	private SoundEvent start, end;

	public SoundScape(SoundEvent start, SoundEvent loop, SoundEvent end, float soundDistance) {
		super(loop, SoundSource.AMBIENT, RandomSource.create());
		this.invSoundDistance = 1f / soundDistance;
		this.start = start;
		this.end = end;
		this.pitch = 1f;
		this.volume = 0f;
		looping = true;
		stopped = true;
	}
	private void resetAndPlay() {
		looping = true;
		stopped = false;
		sound = null;
		tickRender(null);
		Minecraft.getInstance().getSoundManager().play(this);
	}
	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() { }
	public void tickRender(RenderLevelStageEvent event) {
		if (this.random == null) return;
		if (sources.size() == 0) {
			stop();
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		volume = 0f;
		if (player == null) return;
		Vec3 playerPos = player.getEyePosition();
		Vector3f averagePos;

		//calculate average weighted position & spread (handle sable position condition)
		double totalWeight = 0, totalInfluence = 1;
		{
			Vector3d averagePosD = new Vector3d(0);
			
			for (var iter = sources.values().iterator(); iter.hasNext();) {
				SourceState state = iter.next();
				if (!state.block.isSoundScapeValid()) {
					iter.remove();
					continue;
				}
				float volume = updateState(state);
				state.weight = -1;
				if (volume <= 0) continue;
				Vec3 rel = state.block.getSoundScapePos().subtract(playerPos);
				float influence = Mth.clamp(volume
					* (1f - Mth.sqrt((float)(rel.x*rel.x + rel.y*rel.y + rel.z*rel.z)) * invSoundDistance),
					0, 1);
				state.localPos.set(rel.x, rel.z);
				float weight = influence*influence;
				averagePosD.add(state.localPos.x * weight, rel.y * weight, state.localPos.y * weight);
				totalWeight += weight;
				state.weight = weight;
				totalInfluence *= 1f - (influence);
			}
			totalInfluence = 1f - totalInfluence;
			//if below certain volume, stop sound and exit
			if (totalInfluence < 0.01f) return;
			averagePosD.mul(1f / totalWeight);
			averagePos = new Vector3f((float)averagePosD.x, (float)averagePosD.y, (float)averagePosD.z);
		}
		if (sources.size() == 0) {
			stop();
			return;
		}
		renderPoint(event, new Vec3(averagePos).add(playerPos),0,1,0);

		Vector2f averagePos2 = new Vector2f(averagePos.x, averagePos.z);
		float spread = 0;
		float averageLengthSq = averagePos2.lengthSquared();
		float piInv = 1f / Mth.PI;
		float angleToCenter = (float)Mth.atan2(averagePos2.x, -averagePos2.y);

		if (averageLengthSq > 0.0001f) {
			for (SourceState state : sources.values()) {
				if (state.weight <= 0) continue;

				float lengthSq = state.localPos.lengthSquared();
				if (lengthSq <= 0.000001f) {
					spread += state.weight;
					continue;
				}

				float angle = (float)Mth.atan2(state.localPos.x, -state.localPos.y);

				spread += state.weight * Mth.abs(angle - angleToCenter) * piInv;
			}
			spread = Mth.lerp(Mth.clamp(averageLengthSq, 0, 1), 1f, (float)(spread / totalWeight));
		} else {
			spread = 1.0f;
		}
		//Rotate calculated point around player to be either behind or infront based on spread
		float playerRot = player.getViewYRot(0);
		float audioRot = (float)Mth.atan2(averagePos.x, -averagePos.z) * Mth.RAD_TO_DEG;

		float frontRot = playerRot;
		float backRot = playerRot + 180.0f;

		float frontDelta = Mth.wrapDegrees(audioRot - frontRot);
		float backDelta = Mth.wrapDegrees(audioRot - backRot);

		float targetRot = Math.abs(frontDelta) <= Math.abs(backDelta)
			? frontRot
			: backRot;

		float rotatedRot = Mth.rotLerp(spread, audioRot, targetRot) * Mth.DEG_TO_RAD;

		float distance = Mth.sqrt((float)(averagePos.x * averagePos.x + averagePos.z * averagePos.z));
		averagePos.x = Mth.sin(rotatedRot) * distance;
		averagePos.z = -Mth.cos(rotatedRot) * distance;
		this.x = averagePos.x + playerPos.x;
		this.y = averagePos.y + playerPos.y;
		this.z = averagePos.z + playerPos.z;
		this.volume = Mth.clamp((float)totalInfluence, 0, 2);
		renderPoint(event, new Vec3(averagePos).add(playerPos), 1, 0, 0);
	}
	public static void renderPoint(RenderLevelStageEvent event, Vec3 position, float r, float g, float b) {
		if (event == null) return;
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		PoseStack poseStack = event.getPoseStack();
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

		Vec3 cameraPos = camera.getPosition();

		poseStack.pushPose();
		poseStack.translate(
			position.x - cameraPos.x,
			position.y - cameraPos.y,
			position.z - cameraPos.z
		);

		AABB box = new AABB(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1);

		MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

		LevelRenderer.renderLineBox(
			poseStack,
			buffer.getBuffer(RenderType.lines()),
			box,
			r, g, b, 1f
		);

		buffer.endBatch(RenderType.lines());
		poseStack.popPose();
	}
	private float updateState(SourceState state) {
		boolean doPlay = state.block.getSoundScapeState();
		float progress = state.sound == null ? 0 : state.sound.getProgress();
		float volume = state.state == State.STOPPED ? 0 : state.block.getSoundScapeVolume();
		switch (state.state) {
			case STARTING:
				if (progress < 0.875f) {
					state.sound.setVolume(volume);
					return -1;
				}
				progress = (progress-0.875f) * 8;
				state.sound.setVolume(Mth.cos((progress) * Mth.HALF_PI) * volume);
				if (progress == 1) {
					state.state = State.LOOPING;
					state.sound = null;
				}
				return Mth.sin(progress * Mth.HALF_PI) * volume;
			case ENDING:
				if (progress < 0.125f) {
					progress = (progress) * 8;
					state.sound.setVolume(Mth.sin((progress) * Mth.HALF_PI) * volume);
					return Mth.cos(progress * Mth.HALF_PI) * volume;
				}
				state.sound.setVolume(volume);
				if (progress == 1) {
					state.state = State.STOPPED;
					state.sound = null;
				}
				return -1;
			case LOOPING:
				if (!doPlay) {
					onEnd(state);
					state.state = State.ENDING;
				}
				return volume;
			case STOPPED:
				if (doPlay) {
					onStart(state);
					state.state = State.STARTING;
				}
				return -1;
			default:
				return -1;
		}
	}
	private void onStart(SourceState state) {
		TrackedSound sound = new TrackedSound(start, source, random);
		sound.setPosition(state.block.getSoundScapePos());
		Minecraft.getInstance().getSoundManager().play(sound);
		state.sound = sound;
	}
	private void onEnd(SourceState state) {
		TrackedSound sound = new TrackedSound(end, source, random);
		sound.setPosition(state.block.getSoundScapePos());
		Minecraft.getInstance().getSoundManager().play(sound);
		state.sound = sound;
	}

	public <T extends SmartBlockEntity & SoundScapeSource> void addSource(T block) {
		SourceState state = new SourceState(block);
		state.state = block.getSoundScapeState() ? State.LOOPING : State.STOPPED;
		sources.put(block.getBlockPos(), state);
		if (!isStopped()) return;
		resetAndPlay();
	}
	public <T extends SmartBlockEntity & SoundScapeSource> void removeSource(T block) {
		sources.remove(block.getBlockPos());
	}



	private static enum State {
		STARTING, LOOPING, ENDING, STOPPED
	}
	private static class SourceState {
		public Vector2f localPos = new Vector2f();
		public float weight;
		public State state = State.STARTING;
		public SoundScapeSource block;
		public TrackedSound sound;
		public SourceState(SoundScapeSource block) {
			this.block = block;
		}
	}
	@OnlyIn(Dist.CLIENT)
	public class TrackedSound extends AbstractSoundInstance {
		protected TrackedSound(SoundEvent event, SoundSource source, RandomSource random) {
			super(event, source, random);
			looping = false;
			volume = 1f;
		}
		private long startTime;
		private float duration = 1;
		public static void onPlaySound(PlaySoundSourceEvent event) {
			if (event.getSound() instanceof TrackedSound sound) {
				sound.startTime = System.nanoTime();
				((SoundEngineExt)event.getEngine()).getSoundBuffer(sound.sound).thenAccept(buffer -> {
					sound.duration = ((SoundBufferExt)buffer).getDuration();
				});
			}
		}
		public void setPosition(Vec3 pos) {
			x = pos.x;
			y = pos.y;
			z = pos.z;
		}
		public void setVolume(float volume) {
			this.volume = volume;
		}

		public float getProgress() {
			if (duration < 0) return 0;
			float time = (System.nanoTime() - startTime) / 1_000_000_000f;
			float progress = Math.min(1f, time * pitch / duration);
			return progress;
		}
	}
}
