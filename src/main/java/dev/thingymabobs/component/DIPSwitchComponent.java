package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModSounds;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Unit;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.SwitchComponent;

import java.util.Collection;
import java.util.List;

public class DIPSwitchComponent extends OrientableComponent implements IInteractableComponent, IGoggleLabel {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			2,1, "component." + Thingymabobs.MOD_ID + ".dip_switch", null)
		.addPad(0, 0, 0)
		.addPad(1, 0, 1)
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

   
	public DIPSwitchComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL, STATE, MAX_CURRENT, RESISTANCE);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
		var wire = builder.connectSwitch(CONFIG.getResistance().get(),
			builder.terminalNode(0), builder.terminalNode(1), placed.get(STATE));
		placed.add(wire);
		CONFIG.getThermal().apply(thermals).addHeatSource(wire);
	}
	@Override
	public VoxelShape getShape(@NotNull PlacedComponent placed) {
		return IInteractableComponent.extrudedFootprint(placed, 2 / 16f);
	}

	
	@Override
	public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
		var newState = !placed.get(STATE);
		placed.set(STATE, newState);
		Level world = be.getLevel();

		if(world.isClientSide) {
			Component.modelChanged(be.getBlockPos());
		} else {
			placed.notifyClients(STATE);
			stateUpdated(placed);
		}
		if(newState) {
			world.playSound(
				null, placed.getPos(), ModSounds.DIP_SWITCH_ON.get(), SoundSource.BLOCKS, 0.6f, 
				0.95f + world.random.nextFloat() * 0.1f);
		} else {
			world.playSound(
				null, placed.getPos(), ModSounds.DIP_SWITCH_OFF.get(), SoundSource.BLOCKS, 0.6f, 
				0.95f + world.random.nextFloat() * 0.1f);
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
		placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
	}

	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
		return component.get(STATE)
				? Thingymabobs.asResource("dip_switch_on")
				: Thingymabobs.asResource("dip_switch");
	}

	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
				Thingymabobs.asResource("dip_switch"),
				Thingymabobs.asResource("dip_switch_on")
		);
	}
}
