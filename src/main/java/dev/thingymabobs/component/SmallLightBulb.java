package dev.thingymabobs.component;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModModels;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.RenderTypes;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IGoggleLabel;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.LightBulbComponent;
import org.patryk3211.powergrid.circuits.components.VerticallyOrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;

import java.util.Collection;
import java.util.List;

public class SmallLightBulb extends VerticallyOrientableComponent implements IRenderedComponent, IGoggleLabel {
    private static final ComponentFootprint FOOTPRINT_L = new ComponentFootprint.Builder(
            2,2, "component." + Thingymabobs.MOD_ID + ".light_bulb", null)
        .addPad(0, 0, 0)
        .addPad(1, 1, 1)
        .withItem().withOutline().build();
    private static final ComponentFootprint FOOTPRINT_S = new ComponentFootprint.Builder(
            2,1, "component." + Thingymabobs.MOD_ID + ".light_bulb", null)
        .addPad(0, 0, 0)
        .addPad(1, 0, 1)
        .withItem().withOutline().build();

    protected static CProperties.Prop CONFIG = null;
    protected static float RES_MAX;
    public static void configUpdated(CProperties.Prop prop) {
        CONFIG = prop;
        RES_MAX = CONFIG.getResistance().get();
        VOLTAGE.markDirty();
        POWER.markDirty();
    }
    
    public static final EnumProperty<DyeColor> COLOR = LightBulbComponent.COLOR;
    public static final LazyConstantProperty VOLTAGE = new LazyConstantProperty(
        Thingymabobs.MOD_ID, "voltage",
        () -> Unit.VOLTAGE.formatWithPrefixes(Mth.sqrt(CONFIG.getThermal().getPower() * RES_MAX)).string());
    public static final LazyConstantProperty POWER = new LazyConstantProperty(
        Thingymabobs.MOD_ID, "power",
        () -> Unit.POWER.formatWithPrefixes(CONFIG.getThermal().getPower()).string());

    
    public SmallLightBulb() {
        super(FOOTPRINT_L, FOOTPRINT_S);
    }
    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LABEL, COLOR, VOLTAGE, POWER);
    }
    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new ElectricWire(RES_MAX, builder.terminalNode(0), builder.terminalNode(1));
        builder.add(wire);
        placed.add(wire);

        var data = new FloatPair();
        placed.customData = data;
        CONFIG.getThermal().apply(thermals)
            .addHeatSource(wire)
            .withTemperatureCallback(T -> {
                if (T < 10) T = 10f;
                wire.setResistance(20f + (RES_MAX - 20f) / 1450f * T);
                var x = Mth.clamp((T - 600f) / (1400f - 600f), 0, 1);
                data.current = x * x;
            });
    }


    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(!placed.isClient())
            return false;
      	if (placed.customData != null && placed.customData instanceof FloatPair data)
			data.prev = data.current;
        return true;
    }
    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        int color = placed.get(COLOR).getTextureDiffuseColor();
        Orientation orientation = placed.get(ORIENTATION);
		float offsetAmount = placed.get(VERTICAL) ? -0.5f / 16f : 0;
		Vector3f offset = orientation.isX() ? new Vector3f(0, 0, offsetAmount) : new Vector3f(offsetAmount, 0 ,0);

        PartialModel glowModel = ModModels.SLB_GLOW_DYED;
        PartialModel bulbModel = ModModels.SLB_BULB_DYED;
        if (placed.get(COLOR) == DyeColor.WHITE) {
            glowModel = ModModels.SLB_GLOW;
            bulbModel = ModModels.SLB_BULB;
        }

        var red = (color >> 16) & 0xFF;
        var green = (color >> 8) & 0xFF;
        var blue = color & 0xFF;

        // Render the bulb here to avoid adding all circuit board quads to cutout layer.
        CachedBuffers.partial(bulbModel, be.getBlockState())
        	.color(red, green, blue, 255)
        	.light(light)
			.translate(offset).renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
        
        int a = 0, r = 0, g = 0, b = 0;
        if(placed.customData instanceof FloatPair temps) {
            a = (int) (temps.lerped(partialTicks) * 128);
            r = (int) (red * temps.lerped(partialTicks) * 128 / 256);
            g = (int) (green * temps.lerped(partialTicks) * 128 / 256);
            b = (int) (blue * temps.lerped(partialTicks) * 128 / 256);
        }
        if(a != 0) {
            CachedBuffers.partial(glowModel, be.getBlockState())
				.disableDiffuse()
				.color(r, g, b, 255)
				.light(LightTexture.FULL_BRIGHT)
				.translate(offset)
				.renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }
    }

    
    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent placed) {
        return placed.get(VERTICAL)
			? ModModels.SLB_BASE_S
			: ModModels.SLB_BASE_L;
    }
    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
			ModModels.SLB_BASE_L,
			ModModels.SLB_BASE_S
        );
    }

	public static class FloatPair {
		public float prev;
		public float current;
		public FloatPair() { }
		public float lerped(float partial) {
			return Mth.lerp(partial, this.prev, this.current);
		}
   }
}
