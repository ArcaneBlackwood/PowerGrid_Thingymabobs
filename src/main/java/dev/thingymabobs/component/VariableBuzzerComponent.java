package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.client.BuzzerSoundInstance;
import dev.thingymabobs.component.base.ABuzzerComponent;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.util.MetricScale;
import com.google.common.collect.ImmutableCollection;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;

public class VariableBuzzerComponent extends ABuzzerComponent {
	public static final String CONFIG_PITCH_VAR = "current_to_pitch";
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3,5, "component." + Thingymabobs.MOD_ID + ".buzzer", null)
		.addPad(0, 1, 0, "Volume", "V")
		.addPad(2, 1, 1, "Volume", "V")
		.addPad(0, 3, 2, "Pitch +", "P+")
		.addPad(2, 3, 3, "Pitch -", "P-")
		.withItem().withOutline().build();
	
	protected static CProperties.Prop CONFIG = null;
	protected static float VOLUME_POWER_MIN, VOLUME_POWER_MAX;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		VOLUME_POWER_MIN = prop.getFloat(CONFIG_VOLUME0).get();
		VOLUME_POWER_MAX = prop.getFloat(CONFIG_VOLUME1).get();
		PITCH.markDirty();
		POWER.markDirty();
		VOLUME_MIN_POWER.markDirty();
		VOLUME_MAX_POWER.markDirty();
		RESISTANCE.markDirty();
	}

	public static final DynamicFloatProperty PITCH = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "pitch.base", () -> CONFIG.getFloat(CONFIG_PITCH)).useMetrics();
	//Hz/A
	public static final DynamicFloatProperty PITCH_VAR = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "pitch.current", () -> CONFIG.getFloat(CONFIG_PITCH_VAR)).useMetrics();
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());
	public static final LazyConstantProperty VOLUME_MIN_POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "buzzer.volume_min_power",
		() -> MetricScale.format1D1K(VOLUME_POWER_MIN, "", 1));
	public static final LazyConstantProperty VOLUME_MAX_POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "buzzer.volume_max_power",
		() -> MetricScale.format1D1K(VOLUME_POWER_MAX, "", 1));
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());


	public VariableBuzzerComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL, PITCH, PITCH_VAR, POWER, VOLUME_MIN_POWER, VOLUME_MAX_POWER, RESISTANCE);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		State state = new State();
		state.buzzerWire = new ElectricWire(
			CONFIG.getResistance().get(), builder.terminalNode(0), builder.terminalNode(1));
		state.pitchWire = new ElectricWire(
			CONFIG.getResistance("pitch").get(), builder.terminalNode(2), builder.terminalNode(3));
		builder.add(state.buzzerWire);
		placed.add(state.buzzerWire);
		builder.add(state.pitchWire);
		placed.add(state.pitchWire);
		placed.customData = state;

		thermals.builder()
			.addHeatSource(state.buzzerWire)
			.addHeatSource(state.pitchWire);
	}


	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if (hasAudioSource) return true;
		if (placed.getWorld().isClientSide) tickClient(placed);
		return true;
	}
	@OnlyIn(Dist.CLIENT)
	protected void tickClient(@NotNull PlacedComponent placed) {
		if (getVolume(placed) > 0.01)
			net.minecraft.client.Minecraft.getInstance().getSoundManager().play(new BuzzerSoundInstance(placed));
	}
	
	@Override
	public float getVolume(PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return 0;
		float power = Mth.abs((float)state.buzzerWire.power());
		return Math.clamp((power-VOLUME_POWER_MIN)/(VOLUME_POWER_MAX-VOLUME_POWER_MIN), 0, 1);
	}
	@Override
	public float getPitch(PlacedComponent placed) {
		if (!(placed.customData instanceof State state)) return 0;
		float power = (float)state.pitchWire.current() * Math.abs((float) state.pitchWire.potentialDifference());
		return PITCH.limit(placed.get(PITCH) + power * placed.get(PITCH_VAR)) * SOUND_PITCH_INV;
	}

	protected static class State {
		public ElectricWire buzzerWire, pitchWire;
	}
}
