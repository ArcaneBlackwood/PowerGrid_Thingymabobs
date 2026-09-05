package com.feb.moregrid;

import com.feb.moregrid.blocks.LavaLampEntity;
import com.feb.moregrid.mixin.RedstoneLinkNetworkHandlerExt;
import com.feb.moregrid.registry.ModAttachments;
import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModBlocks;
import com.feb.moregrid.registry.ModComponents;
import com.feb.moregrid.registry.ModDataComponents;
import com.feb.moregrid.registry.ModItems;
import com.feb.moregrid.registry.ModMenus;
import com.feb.moregrid.registry.ModModels;
import com.feb.moregrid.registry.ModPackets;
import com.feb.moregrid.registry.ModRecipies;
import com.feb.moregrid.registry.ModSoundScapes;
import com.feb.moregrid.registry.ModSounds;
import com.feb.moregrid.registry.Resistances;
import com.feb.moregrid.registry.Thermals;
import com.feb.moregrid.util.IDirectionSocketElectric;
import com.feb.moregrid.util.SableUtils;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import dev.architectury.event.events.common.LifecycleEvent;
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
import org.joml.Vector3f;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.slf4j.Logger;

@Mod(MoreGrid.MOD_ID)
public final class MoreGrid {
    public static final String MOD_ID = "moregrid";
    public static final Logger LOGGER = LogUtils.getLogger();
    static {
        MixinExtrasBootstrap.init();
    }

    public MoreGrid(IEventBus modBus) {
        modBus.addListener(MoreGrid::onCommon);
        modBus.addListener(ModPackets::registerPayloadHandlers);
        modBus.addListener(ModComponents::onRegister);
        NeoForge.EVENT_BUS.addListener(MoreGrid::tickGlobal);
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
        LifecycleEvent.SETUP.register(MoreGrid::setup);
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
