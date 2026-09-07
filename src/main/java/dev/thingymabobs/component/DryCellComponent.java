package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.registry.ModItems;
import dev.thingymabobs.util.ThingymabobsMath;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;
import java.util.Locale;

/** A non-rechargeable zinc-carbon dry-cell battery pack. */
public final class DryCellComponent extends OrientableComponent
        implements IComponentGoggleInformation, IInteractableComponent {
    private static final double REVERSE_DAMAGE_MULTIPLIER = 5.0D;

    public static final FloatProperty CAPACITY_AH = new FloatProperty(
        Thingymabobs.MOD_ID, "dry_cell_capacity", 2.0F, 0.1F, 2.0F
    );

    public static final FloatProperty STATE_OF_CHARGE = (FloatProperty) new FloatProperty(
        Thingymabobs.MOD_ID, "dry_cell_soc", 1.0F, 0.0F, 1.0F
    ).hidden().cast();

    public static final CalculatedProperty<Float> OPEN_VOLTAGE = new CalculatedProperty<>(
        Thingymabobs.MOD_ID, "dry_cell_open_voltage",
        placed -> (float) ThingymabobsMath.dryCellOpenVoltage(
                placed.get(STATE_OF_CHARGE)
        ), value -> Unit.VOLTAGE.formatWithPrefixes(value).string()
    );

    public static final CalculatedProperty<Float> INTERNAL_RESISTANCE = new CalculatedProperty<>(
        Thingymabobs.MOD_ID, "dry_cell_internal_resistance",
        placed -> (float) ThingymabobsMath.dryCellInternalResistance(
                placed.get(STATE_OF_CHARGE)
        ), value -> Unit.RESISTANCE.formatWithPrefixes(value).string()
    );

    public DryCellComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(CAPACITY_AH);
        properties.add(OPEN_VOLTAGE);
        properties.add(INTERNAL_RESISTANCE);
        properties.add(STATE_OF_CHARGE);
        properties.add(power(5.0F));
    }

    @Override
    public void bake(
            @NotNull PlacedComponent placed,
            @NotNull ComponentCircuitBuilder builder,
            ThermalBuilder.@NotNull IEmitter thermals
    ) {
        double soc = ThingymabobsMath.clamp01(placed.get(STATE_OF_CHARGE));
        float resistance = (float) ThingymabobsMath.dryCellInternalResistance(soc);

        VoltageSourceCoupling source = builder.addInternalNode(
                VoltageSourceCoupling.class,
                builder.terminalNode(0),
                builder.terminalNode(1),
                resistance
        );
        source.setVoltage((float) ThingymabobsMath.dryCellOpenVoltage(soc));
        source.setResistance(resistance);
        placed.customData = source;
        thermals.builder()
            .setDissipationFactor(ThermalBehaviour.dissipationFactor(5, 150))
            .setThermalMass(0.2f).setOverheatTemperature(175)
            .addHeatSource(new CouplingWireProxy(source));
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (!(placed.customData instanceof VoltageSourceCoupling source)) return true;
        if (source == null || !source.isConverged()) {
            return true;
        }

        double current = source.getCurrent();
        if (!Double.isFinite(current)) {
            return true;
        }

        double dischargeCurrent = Math.max(0.0D, -current);
        double reverseCurrent = Math.max(0.0D, current);
        double effectiveDrain = dischargeCurrent + REVERSE_DAMAGE_MULTIPLIER * reverseCurrent;
        double capacity = Math.max(0.1D, placed.get(CAPACITY_AH));

        double soc = ThingymabobsMath.clamp01(placed.get(STATE_OF_CHARGE));
        soc = ThingymabobsMath.clamp01(soc - effectiveDrain * 0.05f / capacity);
        placed.set(STATE_OF_CHARGE, (float) soc);

        updateSource(placed, soc);
        return true;
    }


    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 2.0F / 16.0F);
    }

    /** Replace the installed pack with a fresh Thingymabobs dry-cell item. */
    @Override
    public InteractionResult use(
            CircuitBoardBlockEntity be,
            PlacedComponent placed,
            Player player
    ) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.DRY_CELL.get())) {
            if (placed.get(STATE_OF_CHARGE) < 0.999F) {
                player.displayClientMessage(
                        Component.translatable("thingymabobs.message.dry_cell.replace_required"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        if (placed.get(STATE_OF_CHARGE) >= 0.999F) {
            return InteractionResult.PASS;
        }

        if (be.getLevel() != null && be.getLevel().isClientSide) {
            org.patryk3211.powergrid.circuits.components.Component.modelChanged(be.getBlockPos());
            return InteractionResult.SUCCESS;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        placed.set(STATE_OF_CHARGE, 1.0F);
        placed.notifyClients(STATE_OF_CHARGE);
        updateSource(placed, 1.0D);
        be.setChanged();

        if (be.getLevel() != null) {
            be.getLevel().playSound(
                    null,
                    be.getBlockPos(),
                    SoundEvents.ANVIL_USE,
                    SoundSource.BLOCKS,
                    0.30F,
                    1.65F
            );
        }
        player.displayClientMessage(Component.translatable("thingymabobs.message.dry_cell.replaced"), true);
        return InteractionResult.SUCCESS;
    }

    private static void updateSource(PlacedComponent placed, double soc) {
        if (!(placed.customData instanceof VoltageSourceCoupling source)) return;
        source.setVoltage((float) ThingymabobsMath.dryCellOpenVoltage(soc));
        source.setResistance((float) ThingymabobsMath.dryCellInternalResistance(soc));
    }

    @Override
    public boolean addToGoggleTooltip(
            @NotNull PlacedComponent placed,
            @NotNull List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        double soc = ThingymabobsMath.clamp01(placed.get(STATE_OF_CHARGE));
        double voltage = ThingymabobsMath.dryCellOpenVoltage(soc);
        tooltip.add(Component.translatable("thingymabobs.tooltip.dry_cell.soc", Math.round(soc * 100.0D)));
        tooltip.add(Component.translatable(
                "thingymabobs.tooltip.dry_cell.voltage",
                String.format(Locale.ROOT, "%.2f", voltage)
        ));
        if (soc < 0.999D) {
            tooltip.add(Component.translatable("thingymabobs.tooltip.dry_cell.replace"));
        }
        return true;
    }
}
