package dev.thingymabobs.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SableUtils {
	public static boolean isLoaded;
	
	public static SubLevelAccess getSubLevel(BlockEntity block) {
		if (!isLoaded) return null;
		return SableCompanion.INSTANCE.getContaining(block);
	}

	private static Map<UUID, ThreadLocal<PoseMotion>> poseMotions = new HashMap<>();
	public static PoseMotion getPoseMotion(BlockEntity block) {
		if (!isLoaded) return null;
        final SubLevelAccess level = SableCompanion.INSTANCE.getContaining(block);
		if (level == null) return null;
		UUID uuid = level.getUniqueId();
		ThreadLocal<PoseMotion> thread = poseMotions.computeIfAbsent(uuid, 
			$ -> ThreadLocal.withInitial(() -> new PoseMotion(new WeakReference<>(level))));
		PoseMotion motion = thread.get();
		motion.markDirty();
		return motion;
	}

	public static void tick() {
		for (var iter = poseMotions.values().iterator(); iter.hasNext();) {
			PoseMotion motion = iter.next().get();
			SubLevelAccess level = motion.level.get();
			if (motion.tickCounter++ > 20 || level == null) iter.remove();
			motion.update(level, 20);
		}
	}

	public static Vector3d getGlobalPos(LevelAccessor world, BlockPos block, Vector3d output)  {
		output.set(block.getX()+0.5f, block.getY()+0.5f, block.getZ()+0.5f);
		if (!isLoaded) return output;
        final SubLevelAccess fromSublevel = SableCompanion.INSTANCE.getContaining((Level)world, new Vec3i(block.getX(), block.getY(), block.getZ()));
        if (fromSublevel == null) return output;
		fromSublevel.logicalPose().transformPosition(output);
        return output;
	}
	public static Vector3d getGlobalPos(BlockEntity block, Vector3d pos) {
		if (!isLoaded) return pos;
        final SubLevelAccess fromSublevel = SableCompanion.INSTANCE.getContaining(block);
        if (fromSublevel == null) return pos;
		fromSublevel.logicalPose().transformPosition(pos);
        return pos;
	}


	public final static class PoseMotion {///TODO: Add into SubLevel mixin
		private final Vector3f positionPrev = new Vector3f();
		private final Vector3f velocity = new Vector3f();
		private final Vector3f velocityPrev = new Vector3f();
		private final Vector3f acceleration = new Vector3f();

		private final Quaternionf orientationPrev = new Quaternionf();
		private final Quaternionf angularVelocity = new Quaternionf();
		private final Quaternionf angularVelocityPrev = new Quaternionf();
		private final Quaternionf angularAcceleration = new Quaternionf();
		public WeakReference<SubLevelAccess> level;

		private final Vector3f center = new Vector3f();

		protected PoseMotion(WeakReference<SubLevelAccess> level) {
			this.level = level;
		}
		protected void markDirty() {
			tickCounter = 0;
		}

		private byte state = 0;
		public int tickCounter = 0;

		public Vector3fc getVelocity() {
			return velocity;
		}
		public float getVelocity(Vector3f direc) {
			return velocity.dot(direc.x, direc.y, direc.z);
		}
		public Vector3fc getAcceleration() {
			return acceleration;
		}
		public float getAcceleration(Vector3f direc) {
			return acceleration.dot(direc.x, direc.y, direc.z);
		}
		private final Vector3f angularVelocityAng = new Vector3f();
		public Vector3fc getAngularVelocity() {
			return angularVelocity.getEulerAnglesYXZ(angularVelocityAng);
		}
		public float getAngularVelocity(Vector3f axis) {
			return getRotationAroundAxis(angularVelocity, axis);
		}
		private final Vector3f angularAccelerationAng = new Vector3f();
		public Vector3fc getAngularAcceleration() {
			return angularAcceleration.getEulerAnglesYXZ(angularAccelerationAng);
		}
		public float getAngularAcceleration(Vector3f axis) {
			return getRotationAroundAxis(angularAcceleration, axis);
		}
		public Vector3fc getCenter() {
			return positionPrev;
		}
		public Vector3f getDirectionGlobal(Direction direc) {
			return direc.step().rotate(orientationPrev);
		}
		public Vector3f getPositionGlobal(Vector3f pos) {
        	return orientationPrev.transform(pos.sub(center)).add(positionPrev);
		}

		private Vector3f temp0 = new Vector3f();
		public float getAcceleration(Vector3f direc, Vector3f pos) {
			float accel = acceleration.dot(direc);
			getAngularAccelerationPoint(pos, temp0);
			accel += temp0.dot(direc);
			getAngularCentrifugal(pos, temp0);
			accel += temp0.dot(direc);
			return accel;
		}
		
		private final Vector3f tempPoint = new Vector3f(); 
		public void getAngularVelocityPoint(Vector3f point, Vector3f dest) {
			final Quaternionf av = angularVelocity;
			Vector3f lp = point.sub(positionPrev, tempPoint);
			dest.set(av.x, av.y, av.z).cross(lp);
			if (dest.lengthSquared() < Mth.EPSILON) {
				dest.set(0, 0, 0);
				return;
			}
			dest.mul((float)Math.acos(av.w) * 2 / Mth.sqrt(av.x*av.x + av.y*av.y + av.z*av.z));
		}
		public void getAngularAccelerationPoint(Vector3f point, Vector3f dest) {
			final Quaternionf aa = angularAcceleration;
			Vector3f lp = point.sub(positionPrev, tempPoint);
			dest.set(aa.x, aa.y, aa.z).cross(lp);
			if (dest.lengthSquared() < Mth.EPSILON) {
				dest.set(0, 0, 0);
				return;
			}
			dest.mul((float)Math.acos(aa.w) * 2 / Mth.sqrt(aa.x*aa.x + aa.y*aa.y + aa.z*aa.z));
		}
		public void getAngularCentrifugal(Vector3f point, Vector3f dest) {
			final Quaternionf av = angularVelocity;
			point.sub(positionPrev, dest);
			tempPoint.set(av.x, av.y, av.z).cross(dest).cross(av.x, av.y, av.z, dest);
			if (dest.lengthSquared() < Mth.EPSILON) {
				dest.set(0, 0, 0);
				return;
			}
			float scale = 2.0f * (float)Math.acos(Math.abs(av.w))
				/ Mth.sqrt(av.x*av.x + av.y*av.y + av.z*av.z);
			dest.mul(-scale*scale);
		}



		public void update(SubLevelAccess level, float dtInv) {
			Pose3dc pose = level.logicalPose();
			Vector3dc position = pose.position();
			Quaterniondc orientation = pose.orientation();

			center.set(pose.rotationPoint());

			if (state != 2) {
				if (state == 1) {
					velocityPrev.set(position).sub(positionPrev).mul(dtInv);
				}
				positionPrev.set(position);
				orientationPrev.set(orientation);
				state = (byte)(state == 0 ? 1 : 2);
				return;
			}

			velocity.set(position).sub(positionPrev).mul(dtInv);
			acceleration.set(velocity).sub(velocityPrev).mul(dtInv);
			velocityPrev.set(velocity);
			positionPrev.set(position);
			//Thingymabobs.LOGGER.info(this+" acceleration: "+acceleration+", velocity: "+velocity+", "+position);

			angularVelocity.set((float)orientation.x(), (float)orientation.y(), (float)orientation.z(), (float)orientation.w())
				.mul(orientationPrev.invert()).normalize();
			scaleRotation(angularVelocity, dtInv);
			angularAcceleration.set(angularVelocity).mul(angularVelocityPrev.invert()).normalize();
			scaleRotation(angularAcceleration, dtInv);
			angularVelocityPrev.set(angularVelocity);
			orientationPrev.set(orientation);
		}
		private static void scaleRotation(Quaternionf q, float t) {
			float w = Mth.clamp(q.w, -1.0f, 1.0f);
			float theta = (float)Math.acos(w);

			if (Math.abs(theta) < 0.000001d) {
				q.set(0,0,0,1);
				return;
			}

			float k = Mth.sin(t * theta) / Mth.sin(theta);
			q.set(
				q.x * k, q.y * k, q.z * k,
				Mth.cos(t * theta)
			);
		}
		float getRotationAroundAxis(Quaternionf q, Vector3f axis) {
			float projected = q.x * axis.x + q.y * axis.y + q.z * axis.z;
			return 2.0f * (float)Mth.atan2(projected, q.w);
		}
	}
}
