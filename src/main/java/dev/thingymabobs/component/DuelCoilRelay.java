package dev.thingymabobs.component;

import com.google.common.collect.ImmutableCollection.Builder;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.base.ARelay;
import dev.thingymabobs.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder.IEmitter;

public class DuelCoilRelay extends ARelay {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			4, 3, null, Thingymabobs.MOD_ID + ".component.relay")
		.addPadSharedText(0, 0, 0, "ca", "ca.short")
		.addPadSharedText(0, 2, 1, "ca", "ca.short")
		.addPadSharedText(1, 0, 2, "cb", "cb.short")
		.addPadSharedText(1, 2, 3, "cb", "cb.short")
		.addPadSharedText(3, 0, 4, "no", "no.short")
		.addPadSharedText(3, 1, 5, "cc", "cc.short")
		.addPadSharedText(3, 2, 6, "nc", "nc.short")
		.withItem().withOutline().build();

	public DuelCoilRelay() {
		super(FOOTPRINT);
	}


	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
        initializeProperties();
		super.addProperties(properties);
		properties.add(THRESHOLD_VOLTAGE, ENERGIZED, THRESHOLD_CURRENT, POLARIZED, CURRENT);
	}
    @Override
    public int getCoilCount(PlacedComponent placed) {
        return 2;
    }
    @Override
    protected void createSwitches(PlacedComponent placed, ComponentCircuitBuilder builder, IEmitter thermals, SwitchBuilder switches) {
        switches.relay(4, 5, false);
        switches.wire(6, 5, true);
    }
    @Override
    protected void playSound(boolean energized, PlacedComponent placed) {
        Vec3 posExact = placed.getExactPos();
        Level world = placed.getWorld();
        world.playSound(
            (Player)null, posExact.x, posExact.y, posExact.z, energized ? ModSounds.RELAY_ON.get() : ModSounds.RELAY_OFF.get(),
            SoundSource.BLOCKS, 0.8f, 0.97f + world.random.nextFloat() * 0.06f);
    }
}
