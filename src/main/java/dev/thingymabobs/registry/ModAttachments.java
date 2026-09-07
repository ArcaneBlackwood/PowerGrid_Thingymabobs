package dev.thingymabobs.registry;

import java.util.function.Supplier;
import dev.thingymabobs.Thingymabobs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Thingymabobs.MOD_ID);

	public static final Supplier<AttachmentType<PhantomSleepReduction>> PHANTOM_SLEEP_REDUCTION =
		ATTACHMENTS.register("phantom_sleep_reduction", () ->
			AttachmentType.builder(() -> PhantomSleepReduction.DEFAULT)
				.serialize(PhantomSleepReduction.CODEC)
				.build()
		);
	
	public record PhantomSleepReduction(int value) {
		public static final PhantomSleepReduction DEFAULT = new PhantomSleepReduction(0);
		public static final Codec<PhantomSleepReduction> CODEC =
		RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("value").forGetter(PhantomSleepReduction::value)
		).apply(instance, PhantomSleepReduction::new));
		public static int get(ServerPlayer player) {
			if (!player.hasData(ModAttachments.PHANTOM_SLEEP_REDUCTION)) return 0;
			return player.getData(ModAttachments.PHANTOM_SLEEP_REDUCTION).value();
		}

		public static void set(ServerPlayer player, int value) {
			player.setData(ModAttachments.PHANTOM_SLEEP_REDUCTION,
				new PhantomSleepReduction(value));
  		}
	}
}
