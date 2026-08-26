package com.feb.moregrid.registry;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.utility.NumberFormats;
import org.patryk3211.powergrid.utility.Unit;

import com.feb.moregrid.MoreGrid;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ModLang extends Lang {
   public ModLang() {
   }

   public static MutableComponent translateDirect(String key, Object... args) {
      return builder().translate(key, args).component();
   }

   public static LangBuilder builder() {
      return builder(MoreGrid.MOD_ID);
   }

   public static LangBuilder translate(String langKey, Object... args) {
      return builder().translate(langKey, args);
   }
   
   public static LangBuilder translate(String langKey) {
      return builder().translate(langKey);
   }

   public static LangBuilder unit(String unit) {
      return builder().translate("generic.unit." + unit, new Object[0]);
   }

   public static LangBuilder unit(Unit unit) {
      return builder().translate(unit.getTranslationKey(), new Object[0]);
   }

   public static LangBuilder text(String literal) {
      return builder().text(literal);
   }

   public static LangBuilder number(double n) {
      return builder().text(NumberFormats.formatPrecise(n));
   }

   public static LangBuilder numberConstant(double n) {
      return builder().text(NumberFormats.formatConstant(n));
   }

   public static List<Component> translatedOptions(String prefix, String... keys) {
      List<Component> result = new ArrayList(keys.length);

      for(String key : keys) {
         result.add(translate((prefix != null ? prefix + "." : "") + key).component());
      }

      return result;
   }
}
