package dev.thingymabobs.registry;

import dev.thingymabobs.Thingymabobs;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModModels {
	public static final PartialModel PPBB_MODEL = block("battery/poisonous_potato_battery_block");
	public static final PartialModel PPBB_MODEL_BAKED = block("battery/baked_poisonous_potato_battery_block");
	public static final PartialModel PPBA_MODEL = block("battery/poisonous_potato_battery_array_v");
	public static final PartialModel PPBA_MODEL_BAKED = block("battery/baked_poisonous_potato_battery_array_v");
	public static final PartialModel PBB_MODEL = block("battery/poisonous_potato_battery_block");
	public static final PartialModel PBB_MODEL_BAKED = block("battery/baked_poisonous_potato_battery_block");
	public static final PartialModel PBA_MODEL = block("battery/potato_battery_array_v");
	public static final PartialModel PBA_MODEL_BAKED = block("battery/baked_potato_battery_array_v");

	public static final PartialModel SHUNT_MODEL = block("shunt_v");
	public static final PartialModel SHUNT_MODEL_BLOWN = block("shunt_blown_v");


	public static final PartialModel LL_BASE = block("lava_lamp/base_cutout");
	public static final PartialModel LL_BASE_EMPTY = block("lava_lamp/base_cutout_empty");
	public static final PartialModel LL_BASE_ON = block("lava_lamp/base_cutout_on");
	public static final PartialModel LL_BASE_TRANS = block("lava_lamp/base_translucent_off");
	public static final PartialModel LL_BASE_TRANS_ON = block("lava_lamp/base_translucent");

	public static final PartialModel LL_GLOB_BOTTOM = block("lava_lamp/glob_bottom");
	public static final PartialModel LL_GLOB_BOTTOM_OFF = block("lava_lamp/glob_bottom_off");
	public static final PartialModel LL_GLOB_TOP = block("lava_lamp/glob_top");
	public static final PartialModel LL_GLOB_TOP_OFF = block("lava_lamp/glob_top_off");
	public static final PartialModel LL_GLOB_TOP_LARGE = block("lava_lamp/glob_top_large");
	public static final PartialModel LL_GLOB_TOP_LARGE_OFF = block("lava_lamp/glob_top_large_off");

	public static final PartialModel LL_GLOB_SMALL = block("lava_lamp/glob_small");
	public static final PartialModel LL_GLOB_SMALL_OFF = block("lava_lamp/glob_small_off");
	public static final PartialModel LL_GLOB_LARGE = block("lava_lamp/glob_large");
	public static final PartialModel LL_GLOB_LARGE_OFF = block("lava_lamp/glob_large_off");

	public static final ResourceLocation SLB_BASE_L = Thingymabobs.asResource("small_light_bulb/base_l");
	public static final ResourceLocation SLB_BASE_S = Thingymabobs.asResource("small_light_bulb/base_s");
	public static final PartialModel SLB_BULB = component("small_light_bulb/bulb");
	public static final PartialModel SLB_BULB_DYED = component("small_light_bulb/bulb_dyed");
	public static final PartialModel SLB_GLOW = component("small_light_bulb/glow");
	public static final PartialModel SLB_GLOW_DYED = component("small_light_bulb/glow_dyed");

	public static final ResourceLocation TRANS_BASE = Thingymabobs.asResource("transceivers/transmitter");
	public static final ResourceLocation RECV_BASE = Thingymabobs.asResource("transceivers/reciever");
	public static final ResourceLocation DIRECT_BASE_H = Thingymabobs.asResource("transceivers/directional_reciever_h");
	public static final ResourceLocation DIRECT_BASE_V = Thingymabobs.asResource("transceivers/directional_reciever_v");
	public static final PartialModel TRANS_ANTENNA = component("transceivers/transmitter_antenna");
	public static final PartialModel RECV_ANTENNA = component("transceivers/reciever_antenna");
	public static final PartialModel DIRECT_ANTENNA_H = component("transceivers/directional_reciever_antenna_h");
	public static final PartialModel DIRECT_ANTENNA_V = component("transceivers/directional_reciever_antenna_v");


	public static final PartialModel[] FURN_INTERNAL = {
		block("electric_furnace/internal"),	block("electric_furnace/internal1-0"), block("electric_furnace/internal2-0"), block("electric_furnace/internal3-0"),
		block("electric_furnace/internal0-1"), block("electric_furnace/internal1-1"), block("electric_furnace/internal2-1"), block("electric_furnace/internal3-1"),
		block("electric_furnace/internal0-2"), block("electric_furnace/internal1-2"), block("electric_furnace/internal2-2"), block("electric_furnace/internal3-2"),
		block("electric_furnace/internal0-3"), block("electric_furnace/internal1-3"), block("electric_furnace/internal2-3"), block("electric_furnace/internal3-3"),
	};
	public static final PartialModel[] FURN_GLOW = {
		null, block("electric_furnace/glow1"), block("electric_furnace/glow2"), block("electric_furnace/glow3"),
	};
	public static final PartialModel FURN_BASE_LEFT = block("electric_furnace/base_left");
	public static final PartialModel FURN_BASE_RIGHT = block("electric_furnace/base_right");
	public static final PartialModel FURN_BASE_TOP = block("electric_furnace/base_top");
	public static final PartialModel FURN_DOOR = block("electric_furnace/door");
	public static final PartialModel FURN_DOOR_CLOSE = block("electric_furnace/door_closed");
	public static final PartialModel FURN_BUTTON = block("electric_furnace/button");
	public static final PartialModel FURN_BUTTON_PRESS = block("electric_furnace/button_pressed");
	public static final PartialModel FURN_BUTTON_ON = block("electric_furnace/button_on");
	public static final PartialModel FURN_TEMPERATURE = block("electric_furnace/temperature");

	public static final PartialModel PLASMA_GLOBE = block("plasma_globe/plasmaglobe");
	
	@OnlyIn(Dist.CLIENT)
	public static void registerClient() {

	}
	public static PartialModel block(String path) {
		return PartialModel.of(Thingymabobs.asResource("block/"+path));
	}
	public static PartialModel component(String path) {
		return PartialModel.of(Thingymabobs.asResource("component/"+path));
	}
}
