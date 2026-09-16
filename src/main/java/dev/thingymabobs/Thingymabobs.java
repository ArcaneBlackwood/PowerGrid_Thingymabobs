package dev.thingymabobs;

import java.io.StringWriter;
import org.joml.Vector3f;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.slf4j.Logger;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.thingymabobs.blocks.lavalamp.LavaLampEntity;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.RedstoneLinkNetworkHandlerExt;
import dev.thingymabobs.registry.ModAttachments;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModBlocks;
import dev.thingymabobs.registry.ModComponents;
import dev.thingymabobs.registry.ModConfigs;
import dev.thingymabobs.registry.ModDataComponents;
import dev.thingymabobs.registry.ModItems;
import dev.thingymabobs.registry.ModMenus;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.registry.ModRecipies;
import dev.thingymabobs.registry.ModSoundScapes;
import dev.thingymabobs.registry.ModSounds;
import dev.thingymabobs.registry.network.PacketManager;
import dev.thingymabobs.util.IDirectionSocketElectric;
import dev.thingymabobs.util.MetricScale;
import dev.thingymabobs.util.SableUtils;
import dev.thingymabobs.util.interaction.InteractionManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Thingymabobs.MOD_ID)
public final class Thingymabobs {
	public static final String MOD_ID = "thingymabobs";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final boolean DIST_CLIENT = FMLLoader.getDist() == Dist.CLIENT;
	public static ModContainer CONTAINER;

	public static final float RESISTANCE_CLAMP = 0.00001f;

	static {
		MixinExtrasBootstrap.init();
	}
	///TODO: Add mixin to circuit editor moving discard somewhere else


	public Thingymabobs(IEventBus modBus, ModContainer modContainer) {
		CONTAINER = modContainer;
		modBus.addListener(Thingymabobs::onCommon);
		NeoForge.EVENT_BUS.addListener(Thingymabobs::tickGlobal);
		LifecycleEvent.SETUP.register(Thingymabobs::setup);

		PacketManager.register(modBus);
		InteractionManager.register(modBus);
		MetricScale.register(modBus);
		ModComponents.register(modBus);
		ModDataComponents.DATA_COMPONENTS.register(modBus);
		ModItems.register(modBus);
		ModSounds.register(modBus);
		ModBlocks.register(modBus);
		ModBlockEntities.register(modBus);
		ResistanceValues.register(CProperties.INSTANCE);
		ThermalValues.register(CProperties.INSTANCE);
		ModAttachments.ATTACHMENTS.register(modBus);
		LavaLampEntity.registerLoading(modBus);
		ModRecipies.register(modBus);
		ModMenus.register(modBus);
		CordItem.PLACEMENT_HANDLERS.add(new IDirectionSocketElectric.Handler());
		if (FMLLoader.getDist() == Dist.CLIENT) registerClient(modBus);
		ModConfigs.register(modBus);
	}
	@OnlyIn(Dist.CLIENT)
	private void registerClient(IEventBus modBus) {
		InteractionManager.registerClient(modBus);
		ModModels.registerClient();
		ModItems.registerClient(modBus);
		ModBlockEntities.registerClient(modBus);
		ModMenus.registerClient(modBus);
		ModSoundScapes.registerClient(modBus);
	}
	public static void onCommon(FMLCommonSetupEvent event) {
		if (FMLLoader.getDist() == Dist.CLIENT) onCommonClient(event);
	}
	@OnlyIn(Dist.CLIENT)
	public static void onCommonClient(FMLCommonSetupEvent event) {
		ModBlocks.postRegisterClient();
	}
	



	public static void setup() {
		SableUtils.isLoaded = ModList.get().isLoaded("sable");
	}
	public static void tickGlobal(ServerTickEvent.Post tick) {
		if (Create.REDSTONE_LINK_NETWORK_HANDLER != null)
			((RedstoneLinkNetworkHandlerExt)Create.REDSTONE_LINK_NETWORK_HANDLER).tick();
	}


	public static void logStackTrace(int limit) {
		Thread thread = Thread.currentThread();
		StringWriter sw = new StringWriter();
		for (var trace : thread.getStackTrace()) {
			sw.append("\n\tat ").append(trace.toString());
			if (limit-- == 0) break;
		}
		Thingymabobs.LOGGER.debug(sw.toString());
	}



	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
	public static ResourceLocation texture(String path) {
		return asResource("textures/" + path + ".png");
	}
	public static void renderLine(PoseStack poseStack, VertexConsumer consumer, Vector3f pos, Vector3f size, int color) {
	  PoseStack.Pose pose = poseStack.last();
	  consumer.addVertex(pose, pos).setColor(color).setNormal(pose, size.x, size.y, size.z);
	  consumer.addVertex(pose, size.add(pos)).setColor(color).setNormal(pose, size.x, size.y, size.z);
   }
}
