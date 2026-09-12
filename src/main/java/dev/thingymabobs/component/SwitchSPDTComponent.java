package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import org.patryk3211.powergrid.utility.Unit;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.SwitchComponent;

import java.util.Collection;
import java.util.List;

public class SwitchSPDTComponent extends OrientableComponent implements IInteractableComponent, IGoggleLabel {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			5,3, "component." + Thingymabobs.MOD_ID + ".switch_spdt", null)
		.addPad(0, 1, 0, "Common", "C")
		.addPad(2, 1, 1, "Normally Open", "NO")
		.addPad(4, 1, 2, "Normally Closed", "NC")
		.withItem().withOutline().build();
	
	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		MAX_CURRENT.markDirty();
		RESISTANCE.markDirty();
	}

	public static final BooleanProperty STATE = SwitchComponent.STATE;
	public static final LazyConstantProperty MAX_CURRENT = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "current_max",
		() -> Unit.CURRENT.formatWithPrefixes(Mth.sqrt(CONFIG.getThermal().getPower() / CONFIG.getResistance().get())).string());
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());
		

	public SwitchSPDTComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL, STATE, MAX_CURRENT, RESISTANCE);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
		float resistance = CONFIG.getResistance().get();
		var wireNO = builder.connectSwitch(resistance, builder.terminalNode(0), builder.terminalNode(1), placed.get(STATE));
		var wireNC = builder.connectSwitch(resistance, builder.terminalNode(0), builder.terminalNode(2), !placed.get(STATE));
		placed.add(wireNO);
		placed.add(wireNC);
		CONFIG.getThermal().apply(thermals)
			.addHeatSource(wireNO).addHeatSource(wireNC);
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
		((SwitchedWire) placed.wires.get(1)).setState(!placed.get(STATE));
		placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
		return component.get(STATE)
				? Thingymabobs.asResource("switches/switch_long_on")
				: Thingymabobs.asResource("switches/switch_long");
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
				Thingymabobs.asResource("switches/switch_long"),
				Thingymabobs.asResource("switches/switch_long_on")
		);
	}
}
