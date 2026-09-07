package dev.thingymabobs.blocks;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.function.Consumer;
import java.text.NumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.utility.Lang;

public class PowerShuntValueBehaviour extends ScrollValueBehaviour {
   private static final NumberFormat precise = NumberFormat.getInstance();
   static {
      precise.setMaximumFractionDigits(6);
      precise.setMinimumFractionDigits(0);
      precise.setGroupingUsed(true);
   }
   private final int minOffset;

   public PowerShuntValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot, int minOffset, int max) {
      super(label, be, slot);
      this.minOffset = minOffset;
      this.between(0, max);
      this.withFormatter((i) -> precise.format(exponentialValue(minOffset, i)).replace(" ", " "));
   }

   public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
      ImmutableList<Component> rows = ImmutableList.of(Lang.translateDirect("generic.unit.ohm", new Object[0]));
      ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
      return new ValueSettingsBoard(this.label, this.max, 9, rows, formatter);
   }

   public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings valueSetting, boolean ctrlHeld) {
      int value = Math.max(0, valueSetting.value());
      if (!valueSetting.equals(this.getValueSettings())) {
         this.playFeedbackSound(this);
      }

      this.setValue(value);
   }

   public ScrollValueBehaviour withResistanceCallback(Consumer<Float> resistanceCallback) {
      return super.withCallback((i) -> resistanceCallback.accept(exponentialValue(this.minOffset, i)));
   }

   public static float exponentialValue(int min, int i) {
      int number = i % 9 + 1;
      double mult = Math.pow((double)10.0F, (double)(i / 9 - min));
      return (float)((double)number * mult);
   }

   public MutableComponent formatSettings(ValueSettingsBehaviour.ValueSettings settings) {
      return Lang.text(precise.format((double)exponentialValue(this.minOffset, settings.value()))).component();
   }

   public float getResistance() {
      return exponentialValue(this.minOffset, this.value);
   }
}
