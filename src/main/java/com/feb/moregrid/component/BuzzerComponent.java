package com.feb.moregrid.component;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.client.BuzzerSoundInstance;
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

public class BuzzerComponent extends ABuzzerComponent {
    private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3,3, "component." + MoreGrid.MOD_ID + ".buzzer", null)
            .addPad(0, 1, 0)
            .addPad(2, 1, 1)
            .withItem().withOutline().build();
    
    public static final FloatProperty PITCH = new FloatProperty(MoreGrid.MOD_ID, "pitch", 1000.0f, 20.0f, 20000.0f);
	private static final float RESISTANCE = 20f;

	private ElectricWire buzzerWire;
	private float pitch = 1f;

    public BuzzerComponent() {
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
        builder.add(buzzerWire);
        placed.add(buzzerWire);

        thermals.builder()
                .addHeatSource(buzzerWire)
                .setThermalMass(0.004f)
                .setMaxPower(4.0f, 80f)
                .setOverheatTemperature(100f);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
		pitch = placed.get(PITCH) / 1516.0f;
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
		return pitch;
	}
}
