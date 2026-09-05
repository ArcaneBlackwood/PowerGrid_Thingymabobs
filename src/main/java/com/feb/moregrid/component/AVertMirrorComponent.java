package com.feb.moregrid.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.MirrorableComponent;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import com.google.common.collect.ImmutableCollection;

public abstract class AVertMirrorComponent extends MirrorableComponent {
   public static final BooleanProperty VERTICAL = VerticallyOrientableComponent.VERTICAL;
   protected final ComponentFootprint verticalFootprint;

   public AVertMirrorComponent(ComponentFootprint vertical, ComponentFootprint horizontal) {
      super(horizontal);
      this.verticalFootprint = vertical;
   }

   protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
      super.addProperties(properties);
      properties.add(VERTICAL);
   }

   public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
      return placed != null && (Boolean)placed.get(VERTICAL) ? this.verticalFootprint.rotated((Orientation)placed.get(ORIENTATION)) : super.footprint(placed);
   }

   public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
      if (!counterClockwise) {
         if (!(Boolean)placed.get(VERTICAL)) {
            placed.set(VERTICAL, true);
            return true;
         }

         placed.set(VERTICAL, false);
      } else {
         if ((Boolean)placed.get(VERTICAL)) {
            placed.set(VERTICAL, false);
            return true;
         }

         placed.set(VERTICAL, true);
      }

      return super.rotate(placed, counterClockwise);
   }
}
