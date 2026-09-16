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

public class MicroRelayDPST extends ARelay {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3, 2, null, Thingymabobs.MOD_ID + ".component.relay")
		.addPadSharedText(0, 0, 0, "c", "c.short")
		.addPadSharedText(0, 1, 1, "c", "c.short")
		.addPadSharedText(1, 0, 2, "a", "a.short")
		.addPadSharedText(1, 1, 3, "a", "a.short")
		.addPadSharedText(2, 0, 4, "b", "b.short")
		.addPadSharedText(2, 1, 5, "b", "b.short")
		.withItem().withOutline().build();

	public MicroRelayDPST() {
		super(FOOTPRINT);
	}


	@Override
	protected void addProperties(Builder<ComponentProperty<?>> properties) {
        initializeProperties();
		super.addProperties(properties);
		properties.add(THRESHOLD_VOLTAGE, ENERGIZED, THRESHOLD_CURRENT, POLARIZED, NORMALLY_CLOSEDA, NORMALLY_CLOSEDB, CURRENT);
	}
    @Override
    public int getCoilCount(PlacedComponent placed) {
        return 1;
    }
    @Override
    protected void createSwitches(PlacedComponent placed, ComponentCircuitBuilder builder, IEmitter thermals, SwitchBuilder switches) {
        switches.relay(2, 3, placed.get(NORMALLY_CLOSEDA));
        switches.wire(4, 5, placed.get(NORMALLY_CLOSEDB));
    }
    @Override
    protected void playSound(boolean energized, PlacedComponent placed) {
        Vec3 posExact = placed.getExactPos();
        Level world = placed.getWorld();
        world.playSound(
            (Player)null, posExact.x, posExact.y, posExact.z, energized ? ModSounds.RELAY_ON_SMALL.get() : ModSounds.RELAY_OFF_SMALL.get(),
            SoundSource.BLOCKS, 0.6f, 0.97f + world.random.nextFloat() * 0.06f);
    }
}
