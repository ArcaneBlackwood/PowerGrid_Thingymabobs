package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.CouplingWireProxy;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.Thermal;
import dev.thingymabobs.registry.ModItems;
import dev.thingymabobs.registry.ModSounds;
import dev.thingymabobs.util.MetricScale;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.utility.Unit;
import java.util.List;

public class DryCellComponent extends OrientableComponent implements IComponentGoggleInformation, IInteractableComponent {
	public static final String CONFIG_REVERSE_DAMAGE = "reverse_damage";
	protected static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			5, 3, "component." + Thingymabobs.MOD_ID + ".dry_cell", null)
		.addPad(0, 1, 0, "Positive", "+")
		.addPad(4, 1, 1, "Negative", "-")
		.withItem().withOutline().build();

	protected static CProperties.Prop CONFIG = null;
	protected static BatterySpec SPEC;
	protected static Thermal THERMAL;
	protected static float REVERSE_DAMAGE_MULTIPLIER;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		SPEC = prop.getBattery();
		THERMAL = prop.getThermal();
		REVERSE_DAMAGE_MULTIPLIER = CONFIG.getFloat(CONFIG_REVERSE_DAMAGE).get();
		CAPACITY_AH.markDirty();
		POWER.markDirty();
	}

	public static final DynamicFloatProperty CAPACITY_AH = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "capacity",
		() -> SPEC.getInitialCharge(), () -> SPEC.getMaxCharge() / 20f, () -> SPEC.getMaxCharge()).useMetrics();
	public static final FloatProperty STATE_OF_CHARGE = (FloatProperty) new FloatProperty(
		Thingymabobs.MOD_ID, "soc", 1.0F, 0.0F, 1.0F).hidden().cast();
	public static final CalculatedProperty<Float> OPEN_VOLTAGE = new CalculatedProperty<>(
		Thingymabobs.MOD_ID, "open_voltage",
		placed -> SPEC.calculateVoltage(placed.get(STATE_OF_CHARGE)),
		value -> Unit.VOLTAGE.formatWithPrefixes(value).string());
	public static final CalculatedProperty<Float> INTERNAL_RESISTANCE = new CalculatedProperty<>(
		Thingymabobs.MOD_ID, "internal_resistance",
		placed -> SPEC.calculateResistance(placed.get(STATE_OF_CHARGE)),
		value -> MetricScale.format1D1K(value, "Ω", 1));
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(THERMAL.getPower()).string());

	public DryCellComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(CAPACITY_AH, OPEN_VOLTAGE, INTERNAL_RESISTANCE, STATE_OF_CHARGE, POWER);
	}
	@Override
	public void bake(
		@NotNull PlacedComponent placed,
		@NotNull ComponentCircuitBuilder builder,
		ThermalBuilder.@NotNull IEmitter thermals
	) {
		float soc = Mth.clamp(placed.get(STATE_OF_CHARGE),0,1);
		float resistance = SPEC.calculateResistance(soc);

		VoltageSourceCoupling source = builder.addInternalNode(
			VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), resistance);
		source.setVoltage(SPEC.calculateVoltage(soc));
		source.setResistance(resistance);
		placed.customData = source;

		THERMAL.apply(thermals)
			.addHeatSource(new CouplingWireProxy(source));
	}

	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (!(placed.customData instanceof VoltageSourceCoupling source)) return true;
		if (source == null || !source.isConverged()) return true;

		float current = (float)source.getCurrent();
		if (!Float.isFinite(current)) return true;

		float dischargeCurrent = Math.max(0.0f, -current);
		float reverseCurrent = Math.max(0.0f, current);
		float effectiveDrain = dischargeCurrent + REVERSE_DAMAGE_MULTIPLIER * reverseCurrent;
		float capacity = Math.max(0.1f, placed.get(CAPACITY_AH));

		float soc = Mth.clamp(placed.get(STATE_OF_CHARGE),0,1);
		soc = Mth.clamp(soc - effectiveDrain * 0.05f / capacity,0,1);
		placed.set(STATE_OF_CHARGE, (float) soc);

		updateSource(placed, soc);
		return true;
	}


	@Override
	public VoxelShape getShape(@NotNull PlacedComponent placed) {
		return IInteractableComponent.extrudedFootprint(placed, 2.0F / 16.0F);
	}

	@Override
	public InteractionResult use(
		CircuitBoardBlockEntity be,
		PlacedComponent placed,
		Player player
	) {
		ItemStack held = player.getMainHandItem();
		if (!held.is(ModItems.DRY_CELL.get())) {
			if (placed.get(STATE_OF_CHARGE) < 0.999f) {
				player.displayClientMessage(
						Component.translatable("thingymabobs.message.dry_cell.replace_required"),
						true
				);
			}
			return InteractionResult.PASS;
		}

		if (placed.get(STATE_OF_CHARGE) >= 0.999f) return InteractionResult.PASS;

		Level world = be.getLevel();
		if (world != null && world.isClientSide) {
			org.patryk3211.powergrid.circuits.components.Component.modelChanged(be.getBlockPos());
			return InteractionResult.SUCCESS;
		}

		if (!player.getAbilities().instabuild) held.shrink(1);

		placed.set(STATE_OF_CHARGE, 1.0f);
		placed.notifyClients(STATE_OF_CHARGE);
		updateSource(placed, 1.0f);
		be.setChanged();

		if (world != null && world.isClientSide) {
			world.playSound(
				null, be.getBlockPos(), ModSounds.BATTERY_REPLACE.get(), SoundSource.BLOCKS, 0.8f, 1f);
		}
		player.displayClientMessage(Component.translatable("thingymabobs.message.dry_cell.replaced"), true);
		return InteractionResult.SUCCESS;
	}

	private static void updateSource(PlacedComponent placed, float soc) {
		if (!(placed.customData instanceof VoltageSourceCoupling source)) return;
		source.setVoltage(SPEC.calculateVoltage(soc));
		source.setResistance(SPEC.calculateResistance(soc));
	}

	@Override
	public boolean addToGoggleTooltip(
		@NotNull PlacedComponent placed,
		@NotNull List<Component> tooltip,
		boolean isPlayerSneaking
	) {
		float soc = Mth.clamp(placed.get(STATE_OF_CHARGE),0,1);
		float voltage = SPEC.calculateVoltage(soc);
		tooltip.add(Component.translatable("thingymabobs.tooltip.dry_cell.soc", Math.round(soc * 100f)));
		tooltip.add(Component.translatable(
			"thingymabobs.tooltip.dry_cell.voltage",
			Unit.VOLTAGE.format(voltage)
		));
		if (soc < 0.999f) {
			tooltip.add(Component.translatable("thingymabobs.tooltip.dry_cell.replace"));
		}
		return true;
	}
}
