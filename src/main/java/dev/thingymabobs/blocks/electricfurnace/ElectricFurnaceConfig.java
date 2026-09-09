package dev.thingymabobs.blocks.electricfurnace;

import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import net.createmod.catnip.config.ConfigBase;

public class ElectricFurnaceConfig extends ASubProp {
	public ConfigBase.ConfigInt itemsPerProcess = null;
	public ConfigBase.ConfigFloat defaultTimePerProcess = null, vanillaProcessScale = null,
		tempSmokeMin = null, tempSmokeBurn = null, tempSmokeMax = null, tempSmeltMin = null,
		maxSpeedMul = null, overburnSpeedMul = null, burnTicks = null,
		doorOpenTemp = null, coilPowerMul = null;

	public int itemsPerProcessDef;
	public float defaultTimePerProcessDef, vanillaProcessScaleDef,
		tempSmokeMinDef, tempSmokeBurnDef, tempSmokeMaxDef, tempSmeltMinDef,
		maxSpeedMulDef, overburnSpeedMulDef, burnTicksDef,
		doorOpenTempDef, coilPowerMulDef;

	protected boolean unloaded = false;

	public ElectricFurnaceConfig(int itemsPerProcess, float defaultTimePerProcess, float floatvanillaProcessScale,
			float tempSmokeMin, float tempSmokeBurn, float tempSmokeMax, float tempSmeltMin,
			float maxSpeedMul, float overburnSpeedMul, float burnTicks,
			float doorOpenTempDef, float coilPowerMul) {
		this.itemsPerProcessDef = itemsPerProcess;
		this.defaultTimePerProcessDef = defaultTimePerProcess;
		this.vanillaProcessScaleDef = floatvanillaProcessScale;

		this.tempSmokeMinDef = tempSmokeMin;
		this.tempSmokeBurnDef = tempSmokeBurn;
		this.tempSmokeMaxDef = tempSmokeMax;
		this.tempSmeltMinDef = tempSmeltMin;

		this.maxSpeedMulDef = maxSpeedMul;
		this.overburnSpeedMulDef = overburnSpeedMul;
		this.burnTicksDef = burnTicks;

		this.doorOpenTempDef = doorOpenTempDef;
		this.coilPowerMulDef = coilPowerMul;
	}
	public int getItemsPerProcess() {
		if (unloaded) return itemsPerProcessDef;
		return itemsPerProcess.get();
	}
	public float getDefaultTimePerProcess() {
		if (unloaded) return defaultTimePerProcessDef;
		return itemsPerProcess.get();
	}
	public float getVanillaProcessScale() {
		if (unloaded) return vanillaProcessScaleDef;
		return vanillaProcessScale.getF();
	}

	public float getTempSmokeMin() {
		if (unloaded) return tempSmokeMinDef;
		return tempSmokeMin.getF();
	}
	public float getTempSmokeBurn() {
		if (unloaded) return tempSmokeBurnDef;
		return tempSmokeBurn.getF();
	}
	public float getTempSmokeMax() {
		if (unloaded) return tempSmokeMaxDef;
		return tempSmokeMax.getF();
	}
	public float getTempSmeltMin() {
		if (unloaded) return tempSmeltMinDef;
		return tempSmeltMin.getF();
	}

	public float getMaxSpeedMul() {
		if (unloaded) return maxSpeedMulDef;
		return maxSpeedMul.getF();
	}
	public float getOverburnSpeedMul() {
		if (unloaded) return overburnSpeedMulDef;
		return overburnSpeedMul.getF();
	}
	public float getBurnTicks() {
		if (unloaded) return burnTicksDef;
		return burnTicks.getF();
	}

	public float getDoorOpenTemp() {
		if (unloaded) return doorOpenTempDef;
		return doorOpenTemp.getF();
	}
	public float getCoilPowerMul() {
		if (unloaded) return coilPowerMulDef;
		return coilPowerMul.getF();
	}
	
	@Override
	public void onLoad() {
		unloaded = false;
	}
	@Override
	public Class<?> getType() {
		return ElectricFurnaceConfig.class;
	}
		public static final String CONFIG_SPEED_MAX = "";
		public static final String CONFIG_BURN_TICKS = "";
		//How much to add to base speed per every degreee above burn temp
		public static final String CONFIG_SPEED_BURN = "";
		public static final String CONFIG_DOOR_OPEN_TEMP = "";
		public static final String CONFIG_BLOW_COIL = "";
	@Override
	public void register(String id, CProperties.Builder builder) {
		if (!id.isEmpty()) id = id+"_";
		itemsPerProcess = builder.i(itemsPerProcessDef, 0, id+"items_per_process",
			"Maximum amount of input items to process at once");
		defaultTimePerProcess = builder.f(defaultTimePerProcessDef, 0, id+"default_process_time",
			"Default processing time for electric furnace recipies when undefined");
		vanillaProcessScale = builder.f(vanillaProcessScaleDef, 0, id+"vanilla_process_scale",
			"Scale for the processing time converting vanilla recipies to this.  Default vanilla items take 200 ticks for one item.");


		tempSmokeMin = builder.f(tempSmokeMinDef, 0, id+"temp_smoke_min",
			"Minimum temperature to process smoking vanila recipies");
		tempSmokeBurn = builder.f(tempSmokeBurnDef, 0, id+"temp_smoke_burn",
			"Minimum temperature at which to start burning smoking vanila recipie outputs");
		tempSmokeMax = builder.f(tempSmokeMaxDef, 0, id+"temp_smoke_max",
			"Maximum temperature before smoking vanila recipies only outputs burnt results");
		tempSmeltMin = builder.f(tempSmeltMinDef, 0, id+"temp_smelt_min",
			"Minimum temperature to process smelt vanila recipies");

		maxSpeedMul = builder.f(maxSpeedMulDef, 0, id+"speed_at_max",
			"The maximum input processing speed by reaching the recipies max temp");
		overburnSpeedMul = builder.f(overburnSpeedMulDef, 0, id+"overburn_speed_multiplier",
			"The speed multiplier of items burning is calculated by (current temp - items burn temp) * value below.");
		burnTicks = builder.f(burnTicksDef, 0, id+"burn_time",
			"The base time in ticks it takes to burn an output item.");

		doorOpenTemp = builder.f(doorOpenTempDef, 0, id+"door_open_temp",
			"Sets thermal dissipation when door is open.  A furnace at rated voltage will approach the temperature below when the door is opened.");
		coilPowerMul = builder.f(coilPowerMulDef, 0, id+"coil_power_multiplier",
			"Used to calculate the power at which the coil blows at.  Calculated by thermal max power * value below.");
	}

}