package dev.thingymabobs;

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
import dev.thingymabobs.blocks.LavaLampEntity;
import dev.thingymabobs.mixin.RedstoneLinkNetworkHandlerExt;
import dev.thingymabobs.registry.ModAttachments;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModBlocks;
import dev.thingymabobs.registry.ModComponents;
import dev.thingymabobs.registry.ModDataComponents;
import dev.thingymabobs.registry.ModItems;
import dev.thingymabobs.registry.ModMenus;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.registry.ModPackets;
import dev.thingymabobs.registry.ModRecipies;
import dev.thingymabobs.registry.ModSoundScapes;
import dev.thingymabobs.registry.ModSounds;
import dev.thingymabobs.registry.Resistances;
import dev.thingymabobs.registry.Thermals;
import dev.thingymabobs.util.IDirectionSocketElectric;
import dev.thingymabobs.util.SableUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
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
    static {
        MixinExtrasBootstrap.init();
    }

    public Thingymabobs(IEventBus modBus) {
        modBus.addListener(Thingymabobs::onCommon);
        modBus.addListener(ModPackets::registerPayloadHandlers);
        modBus.addListener(ModComponents::onRegister);
        NeoForge.EVENT_BUS.addListener(Thingymabobs::tickGlobal);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ResistanceValues.register(Resistances.INSTANCE);
        ThermalValues.register(Thermals.INSTANCE);
        ModAttachments.ATTACHMENTS.register(modBus);
        LavaLampEntity.registerLoading(modBus);
        ModRecipies.register(modBus);
        ModMenus.register(modBus);
        CordItem.PLACEMENT_HANDLERS.add(new IDirectionSocketElectric.Handler());
        if (FMLLoader.getDist() == Dist.CLIENT) registerClient(modBus);
        LifecycleEvent.SETUP.register(Thingymabobs::setup);
    }
    @OnlyIn(Dist.CLIENT)
    public void registerClient(IEventBus modBus) {
        ModModels.registerClient();
        ModItems.registerClient(modBus);
        ModBlockEntities.registerClient(modBus);
        ModMenus.registerClient(modBus);
        ModSoundScapes.registerClient(modBus);
    }
    public static void setup() {
        SableUtils.isLoaded = ModList.get().isLoaded("sable");
    }

    public static void tickGlobal(ServerTickEvent.Post tick) {
        if (Create.REDSTONE_LINK_NETWORK_HANDLER != null)
            ((RedstoneLinkNetworkHandlerExt)Create.REDSTONE_LINK_NETWORK_HANDLER).tick();
    }

    public static void onCommon(FMLCommonSetupEvent event) {
        //ModBlocks.postRegister();
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
