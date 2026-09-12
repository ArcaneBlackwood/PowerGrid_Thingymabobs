package dev.thingymabobs.registry;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.jarjar.nio.util.Lazy;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public final class ModSoundScapes {
	public static final Lazy<dev.thingymabobs.client.SoundScape> ELECTRIC_FURNACE_FAN = Lazy.of(() -> 
		new dev.thingymabobs.client.SoundScape(ModSounds.ELECTRIC_FURNACE_FAN_START.get(), ModSounds.ELECTRIC_FURNACE_FAN_LOOP.get(), 
			ModSounds.ELECTRIC_FURNACE_FAN_STOP.get(), 16f));
	
	public static final List<Lazy<dev.thingymabobs.client.SoundScape>> SOUND_SCAPES = List.of(
		ELECTRIC_FURNACE_FAN);

	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(dev.thingymabobs.client.SoundScape.TrackedSound::onPlaySound);
		NeoForge.EVENT_BUS.addListener(ModSoundScapes::onRenderLevel);
	}

	@OnlyIn(Dist.CLIENT)
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (Minecraft.getInstance().isPaused()) return;
		for (Lazy<dev.thingymabobs.client.SoundScape> lazyScape : SOUND_SCAPES) {
			dev.thingymabobs.client.SoundScape scape = lazyScape.orElse(null);
			if (scape == null) continue;
			scape.tickRender(event);
		}
	}
}
