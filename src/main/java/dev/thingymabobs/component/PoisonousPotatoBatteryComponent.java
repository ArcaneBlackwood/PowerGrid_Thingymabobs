package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.ThermalBuilderExt;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.circuits.thermal.ThermalUnit;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Dont look in here, its messy :(. */
public final class PoisonousPotatoBatteryComponent extends OrientableComponent implements IInteractableComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				6,4, "component." + Thingymabobs.MOD_ID + ".potato_battery", null)
            .addPad(1, 1, 0, "Positive", "+")
            .addPad(4, 2, 1, "Negative", "-")
            .withItem().withOutline().build();


    protected static CProperties.Prop CONFIG = null;
	protected static BatterySpec SPEC;
    public static void configUpdated(CProperties.Prop prop) {
        CONFIG = prop;
		SPEC = prop.getBattery();
    }

	public static CalculatedProperty<Float> CAPACITY_AH = new CalculatedProperty<>(
        Thingymabobs.MOD_ID, "capacity",
        placed -> SPEC.getMaxCharge(),
        value -> Unit.ENERGY.formatWithPrefixes(value).string()
    );
	public static DynamicFloatProperty STATE_OF_CHARGE = new DynamicFloatProperty(
        Thingymabobs.MOD_ID, "soc",
        () -> SPEC.getInitialCharge(),
        () -> 0.0F,
        () -> SPEC.getMaxCharge()
    ).hidden().cast();
	public static final IntProperty STATE = new IntProperty(
        Thingymabobs.MOD_ID,"potato.baked", 0, 0, 2).hidden().cast();
	public static final CalculatedProperty<Float> OPEN_VOLTAGE = new CalculatedProperty<>(
        Thingymabobs.MOD_ID, "open_voltage",
        placed -> (float) SPEC.calculateVoltage(placed.get(STATE_OF_CHARGE) / SPEC.getMaxCharge()),
        value -> String.format(Locale.ROOT, "%.2f V", value)
	);
	public static final CalculatedProperty<Float> INTERNAL_RESISTANCE = new CalculatedProperty<>(
        Thingymabobs.MOD_ID, "internal_resistance",
        placed -> (float) SPEC.calculateResistance(placed.get(STATE_OF_CHARGE) / SPEC.getMaxCharge()),
        value -> Unit.RESISTANCE.formatWithPrefixes(value).string()
	);
	public static final CalculatedProperty<Float> MAX_POWER = new CalculatedProperty<>(
        PowerGrid.MOD_ID, "power",
        placed -> calculateMaxPower(),
        value -> Unit.POWER.formatWithPrefixes(value).string()
	);


    public static final int STATE_POTATO = 0;
    public static final int STATE_BAKED = 1;
    public static final int STATE_REMOVED = 2;

	
	public PoisonousPotatoBatteryComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(CAPACITY_AH);
		properties.add(OPEN_VOLTAGE);
		properties.add(INTERNAL_RESISTANCE);
		properties.add(STATE_OF_CHARGE);
		properties.add(STATE);
		properties.add(MAX_POWER);
	}
	public static float calculateMaxPower() {
		float v = SPEC.calculateVoltage(1);
		return v*v/SPEC.calculateResistance(1);
	}
	@Override
	public void bake(
        @NotNull PlacedComponent placed,
        @NotNull ComponentCircuitBuilder builder,
        ThermalBuilder.@NotNull IEmitter thermals
	) {
		float soc = Mth.clamp(SPEC.getInitialCharge() / SPEC.getMaxCharge(), 0, 1);
		float resistance = (float) SPEC.calculateResistance(soc);
		
		VoltageSourceCoupling source = builder.addInternalNode(
            VoltageSourceCoupling.class,
            builder.terminalNode(0),
            builder.terminalNode(1),
            resistance
		);
		updateSource(placed, soc);
        placed.customData = new CustomData(source);

		ThermalBuilder thermal = thermals.builder()
			.setDissipationFactor(0.01f)
			.setThermalMass(0.2f)
			.addHeatSource(new CouplingWireProxy(source))
            .setOverheatTemperature(200)
            .withTemperatureCallback((temp) -> thermalCallback(placed, temp));
        ((ThermalBuilderExt)thermal).withBuildCallback(
            (thermalUnit) -> {
                if (placed.customData instanceof CustomData data) data.thermal = thermalUnit;
            });
	}

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof CustomData data)) return true;
        VoltageSourceCoupling source = data.source;
		if (source == null || !source.isConverged())
			return true;
        

        int state = placed.get(STATE);
        float power = (float)(-source.getCurrent() * source.getVoltage());
		if(state == STATE_BAKED || state == STATE_REMOVED) {
			source.setVoltage(0);
			source.setResistance(1e+6f);
            if (state == STATE_REMOVED) data.thermal.setTemperature(0);
			placed.set(STATE_OF_CHARGE, 0f);
		} else if (state == STATE_POTATO) {
			if (!Double.isFinite(source.getCurrent()))
				return true;

			float capacity = SPEC.getMaxCharge();
			float soc = Mth.clamp(placed.get(STATE_OF_CHARGE).floatValue(), 0, capacity);
			soc = Mth.clamp(soc - Math.max(0.0f, power) * 0.05f, 0, capacity);
			placed.set(STATE_OF_CHARGE, soc);
			updateSource(placed, soc / SPEC.getMaxCharge());
		}

        if(placed.isClient() && data.thermal != null) {
            var world = placed.getWorld();
            var random = world.random;
            var pos = data.thermal.getPosition();
            Vector3f offset = new Vector3f((random.nextFloat() - 0.5f) / 8.0f, (random.nextFloat() - 0.5f) / 8.0f, (random.nextFloat() - 0.5f) / 8.0f);
            BlockState boardState = placed.getWorld().getBlockState(placed.getPos());
            Direction normal = switch (boardState.getValue(CircuitBoardBlock.ROTATION)) {
                case 0 -> Direction.UP;
                case 2 -> Direction.WEST;
                default -> boardState.getValue(CircuitBoardBlock.HORIZONTAL_FACING).getOpposite();
            };
            offset.add(normal.step().mul(6f/16f));
            if (data.thermal.getTemperature() >= 100 - 50f) {
                float chance = data.thermal.getTemperature() / 100;
                if (random.nextFloat() < chance) {
                    world.addParticle(ParticleTypes.SMOKE, 
                        pos.x()+offset.x, pos.y()+offset.y, pos.z()+offset.z, 0.0f, 0.05f, 0.0f);
                }
            }
        }

		return true;
	}
    public void thermalCallback(PlacedComponent placed, float temp) {
        if(temp > 100 && placed.get(STATE) == STATE_POTATO) {
			setState(placed, STATE_BAKED);
        }
    }
    public void setState(PlacedComponent placed, int state) {
        placed.set(STATE, state);

        if(placed.isClient()) {
            Component.modelChanged(placed.getPos());
        } else {
            placed.notifyClients(STATE);
            stateUpdated(placed);
        }
        if (placed.getWorld() == null) return;
        BlockEntity be = placed.getWorld().getBlockEntity(placed.getPos());
        be.setChanged();
    }
    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
    }


	@Override
	public VoxelShape getShape(@NotNull PlacedComponent placed) {
		return IInteractableComponent.extrudedFootprint(placed, 5.0F / 16.0F);
	}

	/** Replace the installed pack with a fresh Thingymabobs dry-cell item. */
	@Override
	public InteractionResult use(
			CircuitBoardBlockEntity be,
			PlacedComponent placed,
			Player player
	) {
		if (!(placed.customData instanceof CustomData data)) return InteractionResult.PASS;
		ItemStack stack = player.getMainHandItem();
        int state = placed.get(STATE);
        double usage = placed.get(STATE_OF_CHARGE) / SPEC.getMaxCharge();
        boolean hasItems = stack.is(Items.POISONOUS_POTATO) && stack.getCount() >= 1;

        if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
            if(!player.isCreative() || !player.isShiftKeyDown()) {
                stack.shrink(1);
                ItemStack potatos = null;
                if (state == STATE_POTATO && usage < 0.5)
                    potatos = new ItemStack(Items.BONE_MEAL, 1);
                if (potatos != null)
                    if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
            }
            placed.set(STATE_OF_CHARGE, SPEC.getInitialCharge());
            updateSource(placed, 1.0f);
            data.thermal.setTemperature(0);
            setState(placed, STATE_POTATO);
            if (be.getLevel() != null) {
                be.getLevel().playSound(
                    null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.65F);
            }
            if(player.isCreative() && player.isShiftKeyDown())
                return InteractionResult.CONSUME;
            else
                return InteractionResult.SUCCESS;
        } else if (stack.isEmpty() && state != STATE_REMOVED) {
            ItemStack potatos;
            if (state == STATE_BAKED)
                potatos = null;
            if (usage < 0.5)
                potatos = new ItemStack(Items.BONE_MEAL, 1);
            else
                potatos = new ItemStack(Items.POISONOUS_POTATO, 1);
            if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
            placed.set(STATE_OF_CHARGE, 0f);
            data.thermal.setTemperature(0);
            updateSource(placed, 0f);
            setState(placed, STATE_REMOVED);
            if (be.getLevel() != null) {
                be.getLevel().playSound(
                    null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.65F);
            }
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("thingymabobs.message.poisonous_potato_battery.replace_required"),
            true
        );
        return InteractionResult.PASS;
	}

	private static void updateSource(PlacedComponent placed, float soc) {
		if (!(placed.customData instanceof CustomData data)) return;
        if (data.source == null) return;
        if (placed.get(STATE) != STATE_POTATO) return;
		data.source.setVoltage(SPEC.calculateVoltage(soc));
		data.source.setResistance(SPEC.calculateResistance(soc));
	}

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return switch (component.get(STATE)) {
            case STATE_POTATO -> Thingymabobs.asResource("poisonous_potato_battery");
            case STATE_BAKED -> Thingymabobs.asResource("poisonous_potato_battery_baked");
            case STATE_REMOVED -> Thingymabobs.asResource("poisonous_potato_battery_removed");
            default -> Thingymabobs.asResource("poisonous_potato_battery_removed");
        };
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                Thingymabobs.asResource("poisonous_potato_battery"),
                Thingymabobs.asResource("poisonous_potato_battery_baked"),
                Thingymabobs.asResource("poisonous_potato_battery_removed")
        );
    }

    public static class CustomData {
        VoltageSourceCoupling source;
        ThermalUnit thermal;
        public CustomData(VoltageSourceCoupling source) {
            this.source = source;
        }
    }
}
