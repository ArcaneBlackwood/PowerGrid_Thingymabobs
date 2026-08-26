package com.feb.moregrid;

import com.feb.moregrid.blocks.LavaLampEntity;
import com.feb.moregrid.registry.ModAttachments;
import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModBlocks;
import com.feb.moregrid.registry.ModDataComponents;
import com.feb.moregrid.registry.ModItems;
import com.feb.moregrid.registry.ModModels;
import com.feb.moregrid.registry.ModPackets;
import com.feb.moregrid.registry.ModSounds;
import com.feb.moregrid.registry.Resistances;
import com.feb.moregrid.registry.Thermals;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
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
        modBus.addListener(ModBlockEntities::registerClientExtensions);
        modBus.addListener(ModPackets::registerPayloadHandlers);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModItems.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ResistanceValues.register(Resistances.INSTANCE);
        ThermalValues.register(Thermals.INSTANCE);
        ModModels.register();
        ModAttachments.ATTACHMENTS.register(modBus);
        LavaLampEntity.registerLoading(modBus);
        NeoForge.EVENT_BUS.addListener(MoreGrid::onPlayerLogin);
    }
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModAttachments.PhantomSleepReduction.set(player, -80000);
            LOGGER.info("MoreGrid.onPlayerLogin "+player);
        }
    }

    public static void onCommon(FMLCommonSetupEvent event) {
        ModBlocks.postRegister();
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    public static ResourceLocation texture(String path) {
        return asResource("textures/" + path + ".png");
    }
}
