package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicIntProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import dev.thingymabobs.config.properties.CProperties.Builder;
import dev.thingymabobs.sim.TransformerRatedWire;
import com.google.common.collect.ImmutableCollection;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Unit;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class TransformerComponent extends OrientableComponent implements IComponentGoggleInformation {
	protected static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			5, 4, "component." + Thingymabobs.MOD_ID + ".transformer", null)
		.addPad(0, 0, 0, "Primary 1", "P1")
		.addPad(0, 3, 1, "Primary 2", "P2")
		.addPad(4, 0, 2, "Secondary 1", "S1")
		.addPad(4, 3, 3, "Secondary 2", "S2")
		.withItem().withOutline().build();

    protected static CProperties.Prop CONFIG = null;
	protected static Config CONFIG_TRANS;
    public static void configUpdated(CProperties.Prop prop) {
        CONFIG = prop;
		CONFIG_TRANS = CONFIG.get(Config.class, Config.KEY);
		TOTAL_TURNS.markDirty();
		PRIMARY_TURNS.markDirty();
		POWER.markDirty();
    }

	private static final Supplier<Integer> TURN_PROVIDER = () ->
		CONFIG_TRANS.getMaxTurns();
	private static final Supplier<Integer> TURN1_PROVIDER = () ->
		CONFIG_TRANS.getMaxTurns() - 1;
	private static final Supplier<Integer> RATIO_PROVIDER = () -> 
		Math.round(CONFIG_TRANS.getMaxTurns() * 0.8f);
	public static final DynamicIntProperty TOTAL_TURNS = new DynamicIntProperty(
		Thingymabobs.MOD_ID, "transformer_total_turns",
		TURN_PROVIDER, () -> 2, TURN_PROVIDER);
	public static final DynamicIntProperty PRIMARY_TURNS = new DynamicIntProperty(
		Thingymabobs.MOD_ID, "transformer_primary_turns",
		RATIO_PROVIDER, () -> 1, TURN1_PROVIDER);
	public static final CalculatedProperty<Integer> SECONDARY_TURNS = new CalculatedProperty<>(
		Thingymabobs.MOD_ID, "transformer_secondary_turns",
		placed -> placed.get(TOTAL_TURNS) - placed.get(PRIMARY_TURNS),
		Object::toString);
	public static final CalculatedProperty<Float> RATIO = new CalculatedProperty<>(
		Thingymabobs.MOD_ID, "transformer_ratio",
		placed -> (float)placed.get(SECONDARY_TURNS) / (float)placed.get(PRIMARY_TURNS),
		value -> String.format(Locale.ROOT, "%.4f", value));
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());

	public TransformerComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(TOTAL_TURNS, PRIMARY_TURNS, SECONDARY_TURNS, RATIO, POWER);
	}
	@Override
	public void bake(
		@NotNull PlacedComponent placed,
		@NotNull ComponentCircuitBuilder builder,
		ThermalBuilder.@NotNull IEmitter thermals
	) {
		TransformerEquivalent equivalent = CONFIG_TRANS.calculate(
			placed.get(PRIMARY_TURNS),
			placed.get(TOTAL_TURNS)
		);

		FloatingNode primaryInternal = builder.addInternalNode();
		FloatingNode secondaryInternal = builder.addInternalNode();

		IElectricNode primary1 = builder.terminalNode(0);
		IElectricNode primary2 = builder.terminalNode(1);
		IElectricNode secondary1 = builder.terminalNode(2);
		IElectricNode secondary2 = builder.terminalNode(3);

		TransformerRatedWire primaryStray = new TransformerRatedWire(
			equivalent.primaryStrayResistance(),
			primary1, primaryInternal, primary1, primary2, secondary1, secondary2
		);
		builder.add(primaryStray);

		ElectricWire secondaryStray = builder.connect(
			equivalent.secondaryStrayResistance(),
			secondaryInternal, secondary1
		);
		primaryStray.setSecondaryWinding(secondaryStray);

		ElectricWire magnetizingBranch = builder.connect(
			equivalent.magnetizingResistance(),
			primaryInternal, primary2
		);

		builder.couple(
			equivalent.ratio(),
			CONFIG_TRANS.getMinResistance(),
			primaryInternal, primary2, secondaryInternal, secondary2
		);

		placed.add(primaryStray);
		placed.add(secondaryStray);
		placed.add(magnetizingBranch);
		placed.customData = new RuntimeState(
			primary1, primary2, secondary1, secondary2, primaryStray, secondaryStray);

		CONFIG.getThermal().apply(thermals)
			.addHeatSource(primaryStray)
			.addHeatSource(secondaryStray);
	}

	@Override
	public boolean addToGoggleTooltip(
		@NotNull PlacedComponent placed,
		@NotNull List<Component> tooltip,
		boolean isPlayerSneaking
	) {
		int total = placed.get(TOTAL_TURNS);
		int primary = placed.get(PRIMARY_TURNS);
		int secondary = total - primary;
		float ratio = (float)secondary / (float)primary;
		tooltip.add(Component.translatable("thingymabobs.tooltip.transformer.turns", primary, secondary, total));
		tooltip.add(Component.translatable("thingymabobs.tooltip.transformer.ratio", String.format(Locale.ROOT, "%.4f", ratio)));
		tooltip.add(Component.translatable("thingymabobs.tooltip.transformer.rating", Math.round(CONFIG.getThermal().getPower())));

		if (!(placed.customData instanceof RuntimeState state)) return true;
		if (state != null && state.primaryWire().isConverged()
				&& state.secondaryWire().isConverged()) {
			double primaryPower = Math.abs(
				(state.primary1().getVoltage() - state.primary2().getVoltage())
					* state.primaryWire().current());
			double secondaryPower = Math.abs(
					(state.secondary1().getVoltage() - state.secondary2().getVoltage())
							* state.secondaryWire().current());
			tooltip.add(Component.translatable(
				"thingymabobs.tooltip.transformer.load",
				String.format(Locale.ROOT, "%.1f", Math.max(primaryPower, secondaryPower))));
		}

		if (placed.wires.size() >= 3
			&& placed.wires.get(0) instanceof ElectricWire primaryWire && primaryWire.isConverged()
			&& placed.wires.get(1) instanceof ElectricWire secondaryWire && secondaryWire.isConverged()
			&& placed.wires.get(2) instanceof ElectricWire magnetizingWire && magnetizingWire.isConverged()
		) {
			double totalCurrent = Math.abs(primaryWire.current())
				+ Math.abs(secondaryWire.current())
				+ Math.abs(magnetizingWire.current());
			double physicalCopperAndCoreLoss = primaryWire.current() * primaryWire.current() * primaryWire.getResistance()
				+ secondaryWire.internalPower()
				+ magnetizingWire.internalPower();
			tooltip.add(Component.translatable(
				"thingymabobs.tooltip.transformer.total_current",
				Unit.CURRENT.format(totalCurrent)));
			tooltip.add(Component.translatable(
				"thingymabobs.tooltip.transformer.loss",
				String.format(Locale.ROOT, "%.3f", physicalCopperAndCoreLoss)));
		}
		return true;
	}

	private record RuntimeState(
		IElectricNode primary1,
		IElectricNode primary2,
		IElectricNode secondary1,
		IElectricNode secondary2,
		TransformerRatedWire primaryWire,
		ElectricWire secondaryWire
	) {}
	public record TransformerEquivalent(
		int totalTurns, int primaryTurns, int secondaryTurns, float ratio,
		float primaryStrayResistance,
		float secondaryStrayResistance,
		float magnetizingResistance
    ) { }
	public static class Config extends ASubProp {
		public static final String KEY = "tf";
		public ConfigBase.ConfigInt maxTurns = null;
		public ConfigBase.ConfigFloat mutualMultiplier = null, minResistance = null,
			couplingK = null, coreAL = null;
		public int maxTurnsDef;
		public float mutualMultiplierDef, minResistanceDef,
			couplingKDef, coreALDef;

		public Config(int maxTurns, float mutualMultiplier, float minResistance,
			float couplingK, float coreAL) {
			this.maxTurnsDef = maxTurns;
			this.mutualMultiplierDef = mutualMultiplier;
			this.minResistanceDef = minResistance;
			this.couplingKDef = couplingK;
			this.coreALDef = coreAL;
		}

		@Override
		public Class<?> getType() {
			return Config.class;
		}
		public int getMaxTurns() {
			return maxTurns.get();
		}
		public float getMinResistance() {
			return minResistance.getF();
		}
		@Override
		public void register(String id, Builder builder) {
			maxTurns = builder.i(maxTurnsDef, 2, id+"_max_turns");
			mutualMultiplier = builder.f(mutualMultiplierDef, 0f, id+"_mutual_multiplier");
			minResistance = builder.f(minResistanceDef, 0f, id+"_min_resistance");
			couplingK = builder.f(couplingKDef, 0f, id+"_coupling_k");
			coreAL = builder.f(coreALDef, 0f, id+"_core_al");
		}
		public TransformerEquivalent calculate(int primaryTurns, int totalTurns) {
			int secondaryTurns = totalTurns - primaryTurns;
			double ratio = (double) secondaryTurns / (double) primaryTurns;
			double lp = primaryTurns * (double) primaryTurns * coreAL.get();
			double ls = secondaryTurns * (double) secondaryTurns * coreAL.get();
			double lm = couplingK.get() * lp;
			float min = minResistance.getF();

			return new TransformerEquivalent(
				totalTurns, primaryTurns, secondaryTurns, (float) ratio,
				safeResistance(lp - lm, min),
				safeResistance(ls - ratio * ratio * lm, min),
				safeResistance(lm * mutualMultiplier.get(), min)
			);
		}
		private static float safeResistance(double resistance, float min) {
			if (!Double.isFinite(resistance)) {
				return min;
			}
			return (float) Math.max(min, resistance);
		}
	}
}
