package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.ABuzzerComponent;
import dev.thingymabobs.component.AccelerometerComponent;
import dev.thingymabobs.component.BuzzerComponent;
import dev.thingymabobs.component.CeramicCapacitorComponent;
import dev.thingymabobs.component.DIPSwitchComponent;
import dev.thingymabobs.component.DryCellComponent;
import dev.thingymabobs.component.GyroscopeComponent;
import dev.thingymabobs.component.PoisonousPotatoBatteryComponent;
import dev.thingymabobs.component.PotatoBatteryComponent;
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
import dev.thingymabobs.component.trancievers.ATrancieverComponent;
import dev.thingymabobs.component.trancievers.DirectionalRecieverComponent;
import dev.thingymabobs.component.trancievers.DistanceRecieverComponent;
import dev.thingymabobs.component.trancievers.RecieverComponent;
import dev.thingymabobs.component.trancievers.TransmitterComponent;
import dev.thingymabobs.config.properties.CProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;

public final class ModComponents {
    private static List<ComponentEntry> COMPONENT_QUEUE = new ArrayList<>();

    private ModComponents() { }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModComponents::onRegister);
        registerComponents();
    }

    public static void registerComponents() {
        CProperties.register(register("transformer", new TransformerComponent()))
			.registerThermal(0.1f, 120f)
            .register(TransformerComponent.Config.KEY, new TransformerComponent.Config(
                30, 10f, 1.0E-4f, 0.9999f, 1.5f))
			.complete(TransformerComponent::configUpdated);
		CProperties.register(register("dry_cell", new DryCellComponent()))
            .registerBattery(2f, 2f, 
                0.9f, 1.6f, 0.15f, 5f, 0.5f)
			.registerThermal(0.2f, 5f)
			.registerFloat(DryCellComponent.CONFIG_REVERSE_DAMAGE, 5f, 
                "Drain done to the battery with reverse current(charging attempt).  Calculated by drain current * value below.")
			.complete(DryCellComponent::configUpdated);
		CProperties.register(register("shunt", new ShuntComponent()))
            .registerFloat("resistance", 0.01f, 0.0001f, 0.1f)
			.registerThermal(5f, 50f, 150f, 200f)
			.complete(ShuntComponent::configUpdated);
        register("tall_connector", new TallConnectorComponent());


		CProperties.register(register("dip_switch", new DIPSwitchComponent()))
            .registerResistance(0.1f)
			.registerThermal(0.01f, 0.025f)
			.complete(DIPSwitchComponent::configUpdated);
		CProperties.register(register("ceramic_capacitor", new CeramicCapacitorComponent()))
            .registerResistance(0.01f)
            .registerFloat("capacitance", 0.0001f, 1e-11f, 0.005f)
			.registerThermal(0.05f, 100f)
			.complete(CeramicCapacitorComponent::configUpdated);
		CProperties.register(register("small_diode", new SmallDiodeComponent()))
            .registerResistance(0.075f)
            .register(SmallDiodeComponent.Config.KEY, new SmallDiodeComponent.Config(
                1.783f, 500f, 1e-6f, 5.47e-9f))
			.registerThermal(0.05f, 100f)
			.complete(SmallDiodeComponent::configUpdated);
		CProperties.register(register("small_resistor", new SmallResistorComponent()))
            .registerFloat("resistance", 10000f, 100f, 100_000_000f)
			.registerThermal(0.05f, 1f)
			.complete(SmallResistorComponent::configUpdated);
		CProperties.register(register("small_bulb", new SmallLightBulb()))
            .registerResistance(12*12/0.5f)
			.registerThermal(0.0001f, 0.5f, 1450f, 1850f)
			.complete(SmallLightBulb::configUpdated);


		CProperties.register(register("buzzer", new BuzzerComponent()))
            .registerFloat(ABuzzerComponent.CONFIG_SOUND_PITCH, 1516.0f, 
                "The pitch of the sound file asset used for the buzzer.")
            .registerResistance(20f)
            .registerFloat(ABuzzerComponent.CONFIG_PITCH, 1000.0f, 20.0f, 20000.0f)
            .registerFloat(ABuzzerComponent.CONFIG_VOLUME1, Mth.sqrt(4f/20f),
                "The input power required to reach max volume")
            .registerFloat(ABuzzerComponent.CONFIG_VOLUME0, Mth.sqrt(4f/20f),
                "Minimum input power")
			.registerThermal(0.005f, 4.0f, 70f, 100f)
			.complete(BuzzerComponent::configUpdated);
		CProperties.register(register("variable_buzzer", new VariableBuzzerComponent()))
            .registerResistance(20f)
            .registerResistance(ABuzzerComponent.CONFIG_PITCH, 1f)
            .registerFloat(ABuzzerComponent.CONFIG_PITCH, 1000.0f, 20.0f, 20000.0f)
            .registerFloat(VariableBuzzerComponent.CONFIG_PITCH_VAR, 1000.0f, 20.0f, 20000.0f,
                "How many Hz/W on the pitch input to vary the pitch.")
            .registerFloat(ABuzzerComponent.CONFIG_VOLUME1, Mth.sqrt(4f/20f),
                "The input power required to reach max volume")
            .registerFloat(ABuzzerComponent.CONFIG_VOLUME0, Mth.sqrt(4f/20f),
                "Minimum input power")
			.registerThermal(0.005f, 5.0f, 70f, 100f)
			.complete(VariableBuzzerComponent::configUpdated);

		CProperties.register(register("switch_dpdt", new SwitchDPDTComponent()))
            .registerResistance(0.1f)
			.registerThermal(0.04f, 0.1f)
			.complete(SwitchDPDTComponent::configUpdated);
		CProperties.register(register("switch_spdt", new SwitchSPDTComponent()))
            .registerResistance(0.1f)
			.registerThermal(0.04f, 0.1f)
			.complete(SwitchSPDTComponent::configUpdated);
		CProperties.register(register("switch_tpst", new SwitchTPSTComponent()))
            .registerResistance(0.1f)
			.registerThermal(0.04f, 0.1f)
			.complete(SwitchTPSTComponent::configUpdated);
		CProperties.register(register("switch_dpst", new SwitchDPSTComponent()))
            .registerResistance(0.1f)
			.registerThermal(0.04f, 0.1f)
			.complete(SwitchDPSTComponent::configUpdated);


		CProperties.register(register("transmitter", new TransmitterComponent()))
            .alsoForId(register("reciever", new RecieverComponent()))
            .alsoForId(register("directional_reciever", new DirectionalRecieverComponent()))
            .alsoForId(register("distance_reciever", new DistanceRecieverComponent()))
            .registerThermal(0.2f, 5.1f, 40f, 175f)
            .register(ATrancieverComponent.Config.KEY, new ATrancieverComponent.Config(
                5f, 48f, 
                100_000f, 1_000f, 16_000f, 1_000f,
                5_000f, 0.1f))
			.complete(ATrancieverComponent::configUpdated);

		CProperties.register(register("accelerometer", new AccelerometerComponent()))
            .registerResistance(48*48/5f)
            .registerFloat(AccelerometerComponent.CONFIG_SENSITIVITY, 1f, 0f, 10f)
            .registerFloat(AccelerometerComponent.CONFIG_FALLOFF, 0.3f)
			.registerThermal(0.2f, 5f)
			.complete(AccelerometerComponent::configUpdated);
		CProperties.register(register("gyroscope", new GyroscopeComponent()))
            .registerResistance(48*48/5f)
            .registerResistance(GyroscopeComponent.CONFIG_MOTOR, 18*18/20f)
            .registerFloat(GyroscopeComponent.CONFIG_MOTOR_POWER, 20f, 
                "Minimum power required for the gyroscope")
            .registerFloat(GyroscopeComponent.CONFIG_SENSITIVITY, 1f, 0f, 10f, 
                "Sensitivity converts rotations per second to voltage.  So its measured in V/RPS.")
            .registerFloat(GyroscopeComponent.CONFIG_FALLOFF, 0.3f, 
                "How sharply the voltage clamps to its max voltage.  Lower values are sharper.  Follows a curve similar to 1-log2(1+2^-x)")
            .registerFloat(GyroscopeComponent.CONFIG_PARTICLES, 3/20f,
                "Chance of emitting a visual indicator particle when powered on.")
			.registerThermal(0.4f, 25f)
			.complete(GyroscopeComponent::configUpdated);


        register("potato_battery", new PotatoBatteryComponent());
        register("poisonous_potato_battery", new PoisonousPotatoBatteryComponent());
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ComponentRegistry.REGISTRY_KEY)) return;
        if (COMPONENT_QUEUE == null) {
            Thingymabobs.LOGGER.warn("ModComponents.onRegister running more than once, re-creating component queue!");
            COMPONENT_QUEUE = new ArrayList<>();
            registerComponents();
        }
        for (var comp : COMPONENT_QUEUE)
            event.register(
                ComponentRegistry.REGISTRY_KEY, comp.res,
                () -> comp.component
            );
        COMPONENT_QUEUE = null;
    }

    private static ResourceLocation register(String id, Component component) {
        ResourceLocation res = ResourceLocation.fromNamespaceAndPath(Thingymabobs.MOD_ID, id);
        COMPONENT_QUEUE.add(new ComponentEntry(component, res));
        return res;
    }
    public static class ComponentEntry {
        public Component component;
        public ResourceLocation res;
        public ComponentEntry(Component component, ResourceLocation res) {
            this.component = component;
            this.res = res;
        }
    }
}
