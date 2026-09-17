package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import dev.thingymabobs.registry.ModModels;
import dev.thingymabobs.registry.ModRenderTypes;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;
import org.patryk3211.powergrid.utility.Unit;

public class LEDComponent extends OrientableComponent implements IRenderedComponent, IGoggleLabel, ISynchronizedComponent {
	private static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
			3, 3, null, Thingymabobs.MOD_ID + ".component.led")
		.addPadSharedText(0, 1, 0, "0", "0.short")
		.addPadSharedText(2, 1, 1, "1", "1.short")
		.withItem().withOutline().build();

	public static String CONFIG_MIN_POWER = "power_min";
	protected static CProperties.Prop CONFIG = null;
	protected static SmallDiodeComponent.Config CONFIG_DIODE = null;
	protected static float POWER_MIN_OFFSET;
	protected static float POWER_DIFF_INV;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		POWER_MIN_OFFSET = prop.getFloat(CONFIG_MIN_POWER).get();
		POWER_DIFF_INV = 255 / (prop.getThermal().getPower() - POWER_MIN_OFFSET);

		CONFIG_DIODE = prop.get(SmallDiodeComponent.Config.class, SmallDiodeComponent.Config.KEY);
		BREAKDOWN_VOLTAGE.markDirty();
		RESISTANCE.markDirty();
		REVERSE_LEAKAGE.markDirty();
		POWER_MIN.markDirty();
		POWER.markDirty();
	}
	

	public static final LazyConstantProperty BREAKDOWN_VOLTAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "breakdown_voltage",
		() -> Unit.VOLTAGE.formatWithPrefixes(CONFIG_DIODE.getBreakdown()).string());
	public static final LazyConstantProperty RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(CONFIG.getResistance().get()).string());
	public static final LazyConstantProperty REVERSE_LEAKAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "reverse_leakage",
		() -> Unit.CURRENT.formatWithPrefixes(CONFIG_DIODE.getReverseLeakage(22)).string());

	public static final LazyConstantProperty POWER_MIN = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power_min",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getFloat(CONFIG_MIN_POWER).get()).string());
	public static final LazyConstantProperty POWER = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "power",
		() -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());

	
	public LEDComponent() {
		super(FOOTPRINT);
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(BREAKDOWN_VOLTAGE, RESISTANCE,
			REVERSE_LEAKAGE, POWER_MIN, POWER);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		var data = new FloatPair();
		placed.customData = data;

		var pnJunctionWire = CONFIG_DIODE.createJunction(
			CONFIG.getResistance().get(), builder.terminalNode(1), builder.terminalNode(0));
		builder.add(pnJunctionWire);
		placed.add(pnJunctionWire);
		CONFIG.getThermal().apply(thermals)
			.withTemperatureCallback(pnJunctionWire::setTemperatureCelsius)
			.addHeatSource(pnJunctionWire);
	}


	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		if(!placed.isClient()) return true;
	  	if (!(placed.customData != null && placed.customData instanceof FloatPair data)) return true;

		data.prev = data.current;
		data.current = data.next;
		return true;
	}
	@Override
	public void writeToSync(PlacedComponent placed, FriendlyByteBuf buf) {
		if (placed.wires.size() != 1) return;
		byte intensity = (byte)(Math.clamp(
				(((PNJunctionWire)placed.wires.get(0)).power() - POWER_MIN_OFFSET) * POWER_DIFF_INV,
			0, 255));
		buf.writeByte(intensity);
	}
	@Override
	public void readFromSync(PlacedComponent placed, FriendlyByteBuf buf) {
	  	if (!(placed.customData != null && placed.customData instanceof FloatPair data)) return;
		data.next = (buf.readByte() & 0xFF) / 255f;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, net.minecraft.client.renderer.MultiBufferSource bufferSource, int light, int overlay) {
			

		int a = 0;
		if(placed.customData instanceof FloatPair intensity) {
			int scale = (int) (intensity.lerped(partialTicks) * 255);
			a = scale;
		}
		if(a != 0) {
			CachedBuffers.partial(ModModels.LED_GLOW, be.getBlockState())
				.disableDiffuse()
				.color(a, a, a, 255)
				.light(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
				.renderInto(ms, bufferSource.getBuffer(ModRenderTypes.ADDITIVE_CULL));
		}
		a = 255-4*a;
		if (a > 0) {
			CachedBuffers.partial(ModModels.LED_GLASS, be.getBlockState())
				.light(light)
				.color(255, 255, 255, a)
				.renderInto(ms, bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.translucent()));
		}
	}
	
	@Override
	public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
		return ModModels.LED;
	}
	@Override
	public @NotNull Collection<ResourceLocation> requestedModels() {
		return List.of(
			ModModels.LED
		);
	}

	public static class FloatPair {
		public float prev;
		public float current;
		public float next;
		public FloatPair() { }
		public float lerped(float partial) {
			return Mth.lerp(partial, this.prev, this.current);
		}
   	}
}
