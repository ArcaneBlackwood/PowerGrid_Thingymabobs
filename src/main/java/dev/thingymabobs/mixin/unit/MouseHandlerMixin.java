package dev.thingymabobs.mixin.unit;
import com.llamalad7.mixinextras.sugar.Local;

import dev.thingymabobs.util.interaction.InteractionHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onPress",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0),
            cancellable = true)
    private void simulated$preOnPress(
			final long windowPointer, final int button, final int action, final int modifiers, final CallbackInfo ci,
			@Local(ordinal = 1, argsOnly = true) final int i, @Local(argsOnly = true, ordinal = 0) final long l
	) {
        if (InteractionHandler.onMousePress(button, action, modifiers))
			ci.cancel();
    }
}