package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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

public class SwitchDPSTComponent extends OrientableComponent implements IInteractableComponent, IGoggleLabel {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			4,3, Thingymabobs.MOD_ID + ".component.switch_dpst", null)
		.addPad(0, 0, 0, "Common", "C")
		.addPad(0, 2, 1, "Common", "C")
		.addPad(3, 0, 2, "Normally Open", "NO")
		.addPad(3, 2, 3, "Normally Open", "NO")
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


	public SwitchDPSTComponent() {
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
		var wireNO1 = builder.connectSwitch(resistance, builder.terminalNode(0), builder.terminalNode(2), placed.get(STATE));
		var wireNO2 = builder.connectSwitch(resistance, builder.terminalNode(1), builder.terminalNode(3), placed.get(STATE));
		placed.add(wireNO1);
		placed.add(wireNO2);
		CONFIG.getThermal().apply(thermals)
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
			Vec3 posExact = placed.getExactPos();
			if(newState) {
				ModdedSoundEvents.MICROSWITCH_ON.play(be.getLevel(), (Player)null, posExact.x, posExact.y, posExact.z, 1, 1);
			} else {
				ModdedSoundEvents.MICROSWITCH_OFF.play(be.getLevel(), (Player)null, posExact.x, posExact.y, posExact.z, 1, 1);
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
			? Thingymabobs.asResource("switches/switch_on")
			: Thingymabobs.asResource("switches/switch");
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			Thingymabobs.asResource("switches/switch"),
			Thingymabobs.asResource("switches/switch_on")
		);
	}
}
