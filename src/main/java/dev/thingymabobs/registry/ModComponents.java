package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.AccelerometerComponent;
import dev.thingymabobs.component.BuzzerComponent;
import dev.thingymabobs.component.CeramicCapacitorComponent;
import dev.thingymabobs.component.DIPSwitchComponent;
import dev.thingymabobs.component.DryCellComponent;
import dev.thingymabobs.component.PoisonousPotatoBatteryComponent;
import dev.thingymabobs.component.PotatoBatteryComponent;
import dev.thingymabobs.component.SCRComponent;
import dev.thingymabobs.component.ShuntComponent;
import dev.thingymabobs.component.SmallDiodeComponent;
import dev.thingymabobs.component.SmallLightBulb;
import dev.thingymabobs.component.SmallResistorComponent;
import dev.thingymabobs.component.SwitchDPDTComponent;
import dev.thingymabobs.component.SwitchDPSTComponent;
import dev.thingymabobs.component.SwitchSPDTComponent;
import dev.thingymabobs.component.SwitchTPSTComponent;
import dev.thingymabobs.component.TallConnectorComponent;
import dev.thingymabobs.component.TransformerComponent;
import dev.thingymabobs.component.VariableBuzzerComponent;
import dev.thingymabobs.component.trancievers.DirectionalRecieverComponent;
import dev.thingymabobs.component.trancievers.DistanceRecieverComponent;
import dev.thingymabobs.component.trancievers.RecieverComponent;
import dev.thingymabobs.component.trancievers.TransmitterComponent;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

public final class ModComponents {

    private ModComponents() { }

    private static TransformerComponent buildTransformer() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
            5,
            4,
            "component." + Thingymabobs.MOD_ID + ".transformer",
            null
        )
            .addPad(0, 0, 0, "Primary 1", "P1")
            .addPad(0, 3, 1, "Primary 2", "P2")
            .addPad(4, 0, 2, "Secondary 1", "S1")
            .addPad(4, 3, 3, "Secondary 2", "S2")
            .withItem()
            .withOutline()
            .build();
        return new TransformerComponent(footprint);
    }

    private static SCRComponent buildScr() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
            4,
            3,
            "component." + Thingymabobs.MOD_ID + ".scr",
            null
        )
            .addPad(0, 1, 0, "Anode", "A")
            .addPad(3, 1, 1, "Cathode", "K")
            .addPad(1, 2, 2, "Gate", "G")
            .withItem()
            .withOutline()
            .build();
        return new SCRComponent(footprint);
    }

    private static DryCellComponent buildDryCell() {
        ComponentFootprint footprint = new ComponentFootprint.Builder(
            5,
            3,
            "component." + Thingymabobs.MOD_ID + ".dry_cell",
            null
        )
            .addPad(0, 1, 0, "Positive", "+")
            .addPad(4, 1, 1, "Negative", "-")
            .withItem()
            .withOutline()
            .build();
        return new DryCellComponent(footprint);
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ComponentRegistry.REGISTRY_KEY)) {
            return;
        }
        register(event, "transformer", buildTransformer());
        register(event, "scr", buildScr());
        register(event, "dry_cell", buildDryCell());
        register(event, "dip_switch", new DIPSwitchComponent());
        register(event, "ceramic_capacitor", new CeramicCapacitorComponent());
        register(event, "small_diode", new SmallDiodeComponent());
        register(event, "small_resistor", new SmallResistorComponent());
        register(event, "tall_connector", new TallConnectorComponent());
        register(event, "buzzer", new BuzzerComponent());
        register(event, "variable_buzzer", new VariableBuzzerComponent());
        register(event, "shunt", new ShuntComponent());
        register(event, "switch_dpdt", new SwitchDPDTComponent());
        register(event, "switch_spdt", new SwitchSPDTComponent());
        register(event, "switch_tpst", new SwitchTPSTComponent());
        register(event, "switch_dpst", new SwitchDPSTComponent());
        register(event, "potato_battery", new PotatoBatteryComponent());
        register(event, "poisonous_potato_battery", new PoisonousPotatoBatteryComponent());
        register(event, "small_bulb", new SmallLightBulb());
        register(event, "transmitter", new TransmitterComponent());
        register(event, "reciever", new RecieverComponent());
        register(event, "directional_reciever", new DirectionalRecieverComponent());
        register(event, "distance_reciever", new DistanceRecieverComponent());
        register(event, "accelerometer", new AccelerometerComponent());
    }

    private static void register(RegisterEvent event, String id, org.patryk3211.powergrid.circuits.components.Component component) {
        event.register(
            ComponentRegistry.REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(Thingymabobs.MOD_ID, id),
            () -> component
        );
    }
}
