package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.SwitchComponent;

import java.util.Collection;
import java.util.List;

public class SwitchDPSTComponent extends OrientableComponent implements IInteractableComponent, IGoggleLabel {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				4,3, "component." + MoreGrid.MOD_ID + ".switch_dpdt", null)
            .addPad(0, 0, 0, "Common", "C")
            .addPad(0, 2, 1, "Common", "C")
            .addPad(3, 0, 2, "Normally Open", "NO")
            .addPad(3, 2, 3, "Normally Open", "NO")
            .withItem().withOutline().build();

    public static final BooleanProperty STATE = SwitchComponent.STATE;

    public SwitchDPSTComponent() {
        super(FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(STATE, LABEL, current(16));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        var wireNO1 = builder.connectSwitch(0.1f, builder.terminalNode(0), builder.terminalNode(2), placed.get(STATE));
        var wireNO2 = builder.connectSwitch(0.1f, builder.terminalNode(1), builder.terminalNode(3), placed.get(STATE));
        placed.add(wireNO1);
        placed.add(wireNO2);
        thermals.builder()
                .setMaxCurrent(1.0f, 0.1f, 150).setThermalMass(0.04f)
                .addHeatSource(wireNO1).addHeatSource(wireNO2);
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 2 / 16f);
    }

    @Override
    public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
        var newState = !placed.get(STATE);
        placed.set(STATE, newState);

        if(be.getLevel().isClientSide) {
            Component.modelChanged(be.getBlockPos());
        } else {
            if(newState) {
                ModdedSoundEvents.MICROSWITCH_ON.playOnServer(be.getLevel(), be.getBlockPos());
            } else {
                ModdedSoundEvents.MICROSWITCH_OFF.playOnServer(be.getLevel(), be.getBlockPos());
            }
            placed.notifyClients(STATE);
            stateUpdated(placed);
        }
        be.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        if(placed.wires.isEmpty())
            return;
        ((SwitchedWire) placed.wires.get(0)).setState(placed.get(STATE));
        ((SwitchedWire) placed.wires.get(1)).setState(placed.get(STATE));
        placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return component.get(STATE)
                ? MoreGrid.asResource("switch_on")
                : MoreGrid.asResource("switch");
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                MoreGrid.asResource("switch"),
                MoreGrid.asResource("switch_on")
        );
    }
}
