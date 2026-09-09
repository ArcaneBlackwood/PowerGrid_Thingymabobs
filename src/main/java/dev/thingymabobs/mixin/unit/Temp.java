package dev.thingymabobs.mixin.unit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.thingymabobs.Thingymabobs;

@Mixin(targets = "net.neoforged.neoforge.common.ModConfigSpec$BuilderContext")
public abstract class Temp {
	@Shadow 
	private List<String> comment;
	@Inject(at = @At("HEAD"), method = "ensureEmpty", require = 1)
    public void thingymabobs$build(CallbackInfo ci) {
        Thread thread = Thread.currentThread();
		StringWriter sw = new StringWriter();
		for (var trace : thread.getStackTrace())
			sw.append("\n\tat ").append(trace.toString());
		Thingymabobs.LOGGER.info("Message last: '"+String.join(", ", comment)+"'"+sw.toString());
	}
	@Inject(at = @At("HEAD"), method = "addComment", require = 1)
    public void thingymabobs$addComment(String value, CallbackInfo ci) {
        Thread thread = Thread.currentThread();
		StringWriter sw = new StringWriter();
		for (var trace : thread.getStackTrace())
			sw.append("\n\tat ").append(trace.toString());
		Thingymabobs.LOGGER.info("Add comment: '"+value+"'\n\t"+sw.toString());
	}
	
}
