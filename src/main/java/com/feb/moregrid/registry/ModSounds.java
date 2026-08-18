package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
  public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
      DeferredRegister.create(Registries.SOUND_EVENT, MoreGrid.MOD_ID);

  public static final DeferredHolder<SoundEvent, SoundEvent> BUZZER =
      SOUND_EVENTS.register("buzzer", () ->
          SoundEvent.createVariableRangeEvent(
              ResourceLocation.fromNamespaceAndPath(MoreGrid.MOD_ID, "buzzer")
          )
      );
}