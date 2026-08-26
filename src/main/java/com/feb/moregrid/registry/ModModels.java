package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

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
	
	public static void register() {

	}
	public static PartialModel block(String path) {
		return PartialModel.of(MoreGrid.asResource("block/"+path));
	}
}
