package dev.thingymabobs.mixin.unit.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.util.interaction.InteractionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {
	@Shadow
	@Final
	public ModelPart body;

	@Inject(method = "setupAnim*", at = @At("RETURN"))
	private void simulated$afterSetupAnim(final T pEntity, final float pLimbSwing, final float pLimbSwingAmount, final float pAgeInTicks, final float pNetHeadYaw, final float pHeadPitch, final CallbackInfo callbackInfo) {
		if (!(pEntity instanceof final AbstractClientPlayer player))
			return;
		if (Minecraft.getInstance().isPaused()) return;
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		if (InteractionHandler.getActive(player) == null) return;

		model.leftArm.zRot = 0.0f;
		model.leftArm.zRot = 0.0f;

		model.leftArm.xRot = (float) Math.toRadians(-80.0f)+ model.head.xRot;
		model.rightArm.xRot = (float) Math.toRadians(-80.0f)+ model.head.xRot;

		model.rightArm.yRot = (float) Math.toRadians(-15);
		model.leftArm.yRot = (float) Math.toRadians(15);
	}
}
