package dev.thingymabobs.component;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Unit;
import com.google.common.collect.ImmutableCollection;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.VariantComponent.Variant;
import dev.thingymabobs.component.base.VariantOrientableComponent;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModSounds;
import dev.thingymabobs.util.interaction.InteractionHoldComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmallButtonComponent extends VariantOrientableComponent implements IInteractableComponent, IGoggleLabel, InteractionHoldComponent.Capable {
	protected static final Variant FOOTPRINT_SINGLE = new Variant(new ComponentFootprint.Builder(
			2,2, Thingymabobs.MOD_ID + ".component.button", null)
		.addPad(1, 0, 0)
		.addPad(0, 1, 1)
		.withItem().withOutline().build(), "_single");
	public static final int INDEX_DOUBLE = 1;
	protected static final Variant FOOTPRINT_DOUBLE = new Variant(new ComponentFootprint.Builder(
			2,2, Thingymabobs.MOD_ID + ".component.button_double", null)
		.addPad(0, 0, 0, "A", "A")
		.addPad(0, 1, 1, "A", "A")
		.addPad(1, 0, 2, "B", "B")
		.addPad(1, 1, 3, "B", "B")
		.withItem().withOutline().build(), "_double");
	protected static final Variant FOOTPRINT_SMALL = new Variant(new ComponentFootprint.Builder(
			1,2, Thingymabobs.MOD_ID + ".component.button", null)
		.addPad(0, 0, 0)
		.addPad(0, 1, 1)
		.withItem().withOutline().build(), "_small");

	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
	}


	public static final BooleanProperty NORMALLY_CLOSED = new BooleanProperty(
		Thingymabobs.MOD_ID, "button_nc");
	public static final BooleanProperty STATE = new BooleanProperty(
		Thingymabobs.MOD_ID, "switch_state").hidden().cast();
	public static final LazyConstantProperty MAX_CURRENT = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "current_max",
		() -> Unit.CURRENT.formatWithPrefixes(Mth.sqrt(CONFIG.getThermal().getPower() / CONFIG.getResistance().get())).string());
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());
	
	public SmallButtonComponent() {
		super(FOOTPRINT_SINGLE, FOOTPRINT_DOUBLE, FOOTPRINT_SMALL);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL, STATE, NORMALLY_CLOSED, MAX_CURRENT, RESISTANCE);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
		float resistance = CONFIG.getResistance().get();
		var switch1 = builder.connectSwitch(resistance, builder.terminalNode(0), builder.terminalNode(1), placed.get(STATE));
		placed.customData = Integer.valueOf(placed.get(VARIANT));
		if (placed.get(VARIANT) == INDEX_DOUBLE) {
			var switch2 = builder.connectSwitch(resistance, builder.terminalNode(2), builder.terminalNode(3), !placed.get(STATE));
			placed.add(switch1);
			placed.add(switch2);
			CONFIG.getThermal().apply(thermals)
				.addHeatSource(switch1).addHeatSource(switch2);
		} else {
			placed.add(switch1);
			CONFIG.getThermal().apply(thermals)
				.addHeatSource(switch1);
		}
	}
	@Override
	public VoxelShape getShape(@NotNull PlacedComponent placed) {
		return IInteractableComponent.extrudedFootprint(placed, 2 / 16f);
	}


	static int what = 0;
	@Override
	public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
		return interactTry(be, placed, player);
	}
	///TODO:  Ensure atleast one tick is with active.  If on and off single tick, schedule off for one tick later
	/// Maybe do in selfs main tick?  Counter for switch on, when interaction on, switch state on.
	@Override
	public boolean interactTick(PlacedComponent placed, InteractionHoldComponent interact) {
		return true;
	}
	@Override
	public void interactOnStart(PlacedComponent placed, InteractionHoldComponent interact, Player player, int newCount) {
		if(placed.getWorld().isClientSide) return;
		if (newCount != 1) return;

		placed.set(STATE, true);
		BlockPos pos = placed.getPos();
		Level world = placed.getWorld();
		if(placed.isClient()) {
			Component.modelChanged(pos);
		} else {
			Vec3 posExact = placed.getExactPos();
			world.playSound(
				(Player)null, posExact.x, posExact.y, posExact.z, ModSounds.BUTTON_OFF.get(), SoundSource.BLOCKS, 0.8f, 
				0.9f + world.random.nextFloat() * 0.2f);
			placed.notifyClients(STATE);
			stateUpdated(placed);
		}
		world.getBlockEntity(pos).setChanged();
	}
	@Override
	public void interactOnStop(PlacedComponent placed, InteractionHoldComponent interact, Player player, int oldCount) {
		if (oldCount != 1 && player != null) return;

		placed.set(STATE, false);
		BlockPos pos = placed.getPos();
		Level world = placed.getWorld();
		if(placed.isClient()) {
			Component.modelChanged(pos);
		} else {
			Vec3 posExact = placed.getExactPos();
			world.playSound(
				(Player)null, posExact.x, posExact.y, posExact.z, ModSounds.BUTTON_OFF.get(), SoundSource.BLOCKS, 0.8f, 
				0.9f + world.random.nextFloat() * 0.2f);
			placed.notifyClients(STATE);
			stateUpdated(placed);
		}
		world.getBlockEntity(pos).setChanged();
	}

	

	@Override
	public void stateUpdated(@NotNull PlacedComponent placed) {
		super.stateUpdated(placed);
		if(placed.wires.isEmpty()) return;

		int variant = placed.get(VARIANT);
		if ((Integer)placed.customData != variant)
			throw new IllegalStateException("Variant updated.  Requires re bake!");
		boolean closed = placed.get(STATE) != placed.get(NORMALLY_CLOSED);

		((SwitchedWire) placed.wires.get(0)).setState(closed);
		if (variant == INDEX_DOUBLE)
			((SwitchedWire) placed.wires.get(1)).setState(closed);
		if(placed.isClient()) {
			modelChanged(placed.getPos());
		}
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return Thingymabobs.asResource("switches/button")
			.withSuffix(getVariantSuffix(placed) + (placed.get(STATE) ? "_on" : "_off"));
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			Thingymabobs.asResource("switches/button_single_on"),
			Thingymabobs.asResource("switches/button_single_off"),
			Thingymabobs.asResource("switches/button_double_on"),
			Thingymabobs.asResource("switches/button_double_off"),
			Thingymabobs.asResource("switches/button_small_on"),
			Thingymabobs.asResource("switches/button_small_off")
		);
	}
}
