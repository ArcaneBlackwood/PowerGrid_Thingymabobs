package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.circuits.components.Component;

import java.util.List;

public class TallConnectorComponent extends Component {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				3,3, "component." + Thingymabobs.MOD_ID + ".tall_connector", null)
			.addPad(1, 1, 0)
			.withItem().withOutline().build();

	private static final List<TerminalBoundingBox> TERMINALS = List.of(
			new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 1f, 9.0f, 1f, 2f, 10.0f, 2f)
	);

	public TallConnectorComponent() {
		super(FOOTPRINT);
	}

	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(LABEL);
	}

	@Override
	public boolean emitExternalTerminals() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed) {
		if(placed.customData instanceof List) {
			return (List<TerminalBoundingBox>) placed.customData;
		} else {
			var label = placed.get(LABEL);
			if(label.isEmpty()) {
				placed.customData = TERMINALS;
				return TERMINALS;
			} else {
				var list = List.of(new TerminalBoundingBox(
						net.minecraft.network.chat.Component.literal(label),
						1, 9, 1, 2, 10, 2
				));
				placed.customData = list;
				return list;
			}
		}
	}

	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
	}
}
