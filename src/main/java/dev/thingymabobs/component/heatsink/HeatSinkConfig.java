package dev.thingymabobs.component.heatsink;

import java.util.ArrayList;
import java.util.List;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.mojang.datafixers.util.Pair;
import dev.thingymabobs.config.properties.CProperties.Builder;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.resources.ResourceLocation;

public class HeatSinkConfig extends ASubProp {
	private static float EXTERNAL_MUL_DEFAULT;
	private static float CONDUCT_DEFAULT;
	private static final Reference2ObjectOpenHashMap<ResourceLocation, Float> CONDUCT_MAP_DEFAULT =
		new Reference2ObjectOpenHashMap<>();
	
	private static ConfigBase.ConfigFloat EXTERNAL_MUL_CONFIG;
	private static ConfigBase.ConfigFloat CONDUCT_DEFAULT_CONFIG;
	private static CProperties.ConfigArray<String> CONDUCT_MAP_CONFIG;
	private static final Object2FloatOpenHashMap<ResourceLocation> CONDUCT_MAP =
		new Object2FloatOpenHashMap<>();


	public static float getDefault() {
		return CONDUCT_DEFAULT_CONFIG == null ? CONDUCT_DEFAULT : CONDUCT_DEFAULT_CONFIG.getF();
	}
	public static void setDefault(ResourceLocation id, float value) {
		CONDUCT_MAP_DEFAULT.put(id, value);
	}
	public static float getDefault(ResourceLocation id) {
		float entry = CONDUCT_MAP_DEFAULT.getOrDefault(id, -1f);
		return entry < 0 ? getDefault() : entry;
	}
	public static float get(ResourceLocation id) {
		float def = getDefault(id);
		if (CONDUCT_MAP_CONFIG == null)
			return def;
		float entry = CONDUCT_MAP.getOrDefault(id, -1f);
		return entry < 0 ? def : entry;
	}

	public static float getDefault(Component comp) {
		return getDefault(ComponentRegistry.getId(comp));
	}
	public static float getDefault(PlacedComponent placed) {
		return getDefault(ComponentRegistry.getId(placed.component));
	}
	public static float get(Component comp) {
		return get(ComponentRegistry.getId(comp));
	}
	public static float get(PlacedComponent placed) {
		return get(ComponentRegistry.getId(placed.component));
	}
	public static float getExternalMul() {
		return EXTERNAL_MUL_CONFIG==null ? EXTERNAL_MUL_DEFAULT : EXTERNAL_MUL_CONFIG.getF();
	}


	@SafeVarargs
	public static ASubProp register(float externalMul, float def, Pair<ResourceLocation, Float>... pairs) {
		EXTERNAL_MUL_DEFAULT = externalMul;
		CONDUCT_DEFAULT = def;
		for (var pair : pairs) {
			CONDUCT_MAP.putIfAbsent(pair.getFirst(), pair.getSecond().floatValue());
		}
		return new HeatSinkConfig();
	}


	private static List<String> generateDefaults() {
		List<String> list = new ArrayList<>(CONDUCT_MAP_DEFAULT.size());
		for (var entry : CONDUCT_MAP_DEFAULT.entrySet())
			list.add(toString(entry.getKey(), entry.getValue()));
		return list;
	}
	public static Pair<ResourceLocation, Float> parse(String value) {
		String[] entry = value.split("=", 2);
		if (entry.length != 2) return null;
		try {
			return new Pair<>(ResourceLocation.parse(entry[0]), Float.parseFloat(entry[1]));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public static String toString(ResourceLocation id, Float val) {
		return id.toString()+"="+val.toString();
	}

	@Override
	public void onLoad() {
		for (var entry : CONDUCT_MAP_CONFIG.get()) {
			var parsed = parse(entry);
			if (parsed == null) continue;
			CONDUCT_MAP.put(parsed.getFirst(), parsed.getSecond().floatValue());
		}
		Thingymabobs.LOGGER.info("");
		super.onLoad();
	}
	@Override
	public Class<?> getType() {
		return HeatSinkConfig.class;
	}
	@Override
	public void register(String id, Builder builder) {
		id = id == null || id.isBlank() ? "" : id+"_";
		EXTERNAL_MUL_CONFIG = builder.f(EXTERNAL_MUL_DEFAULT, 0, id+"external_power_multiplier",
			"When a heat sink is connected to a external block instead of component, max power is multiplied by this unit.");
		CONDUCT_DEFAULT_CONFIG = builder.f(CONDUCT_DEFAULT, 0, 1, id+"conductance_default", 
			"Conductance represens how much heat can transfer between component and heat sink.  0 being none, 1 as high as possible.  Heat conduction calculated by power = (heat sink temp - component temp) * conductance");
		CONDUCT_MAP_CONFIG = builder.array(HeatSinkConfig::generateDefaults, "MOD:ID=0", id+"conductances", 
			"If conductance less than 0, default conductance used.  Format comma seperated values of \"MOD:ID=1.00\", for instance \"thingymabobs:small_resistor=0.5\"");
	}
}