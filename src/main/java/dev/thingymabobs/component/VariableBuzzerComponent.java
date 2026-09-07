package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.client.BuzzerSoundInstance;
import com.google.common.collect.ImmutableCollection;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;

public class VariableBuzzerComponent extends ABuzzerComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3,5, "component." + Thingymabobs.MOD_ID + ".buzzer", null)
            .addPad(0, 1, 0)
            .addPad(2, 1, 1)
            .addPad(0, 3, 2, "Pitch", "P")
            .addPad(2, 3, 3, "Pitch", "P")
            .withItem().withOutline().build();
    
    //Hz/A
    public static final FloatProperty PITCH = new FloatProperty(Thingymabobs.MOD_ID, "pitch.current", 20000.0f, 5000.0f, 200000.0f);
	private static final float RESISTANCE = 20f;
	private static final float PITCH_RESISTANCE = 1f;

	private ElectricWire buzzerWire, pitchWire;
    private float currnetToPitch = 0;

    public VariableBuzzerComponent() {
        super(FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LABEL, PITCH, power(0.5f),
			new ConstantProperty(PowerGrid.MOD_ID, "resistance", Unit.RESISTANCE.formatWithPrefixes((double)RESISTANCE).component()));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        buzzerWire = new ElectricWire(RESISTANCE, builder.terminalNode(0), builder.terminalNode(1));
        pitchWire = new ElectricWire(PITCH_RESISTANCE, builder.terminalNode(2), builder.terminalNode(3));
        builder.add(buzzerWire);
        placed.add(buzzerWire);
        builder.add(pitchWire);
        placed.add(pitchWire);

        thermals.builder()
                .addHeatSource(buzzerWire)
                .addHeatSource(pitchWire)
                .setThermalMass(0.004f)
                .setMaxPower(5.0f, 200f);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        currnetToPitch = placed.get(PITCH);
		if (hasAudioSource) return true;
		if (placed.getWorld().isClientSide) tickClient(placed);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickClient(@NotNull PlacedComponent placed) {
		if (getVolume(placed) > 0.01)
            Minecraft.getInstance().getSoundManager().play(new BuzzerSoundInstance(placed));
    }
	
    @Override
	public float getVolume(PlacedComponent placed) {
		return Math.clamp((float)buzzerWire.power() * 4.0f - 1.0f, 0, 1);
	}
    @Override
	public float getPitch(PlacedComponent placed) {
		return Math.clamp((float)Math.abs(pitchWire.current()) * currnetToPitch, 20, 20000) / 1516.0f;
	}
}
