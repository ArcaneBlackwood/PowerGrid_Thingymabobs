package com.feb.moregrid;

import com.feb.moregrid.registry.ModBlockEntities;
import com.feb.moregrid.registry.ModBlocks;
import com.feb.moregrid.registry.ModItems;
import com.feb.moregrid.registry.ModSounds;
import com.feb.moregrid.registry.Resistances;
import com.feb.moregrid.registry.Thermals;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.config.ThermalValues;
import org.slf4j.Logger;

@Mod(MoreGrid.MOD_ID)
public final class MoreGrid {
    public static final String MOD_ID = "moregrid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoreGrid(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModItems.CREATIVE_TABS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ResistanceValues.register(Resistances.INSTANCE);
        ThermalValues.register(Thermals.INSTANCE);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation texture(String path) {
        return asResource("textures/" + path + ".png");
    }
}
