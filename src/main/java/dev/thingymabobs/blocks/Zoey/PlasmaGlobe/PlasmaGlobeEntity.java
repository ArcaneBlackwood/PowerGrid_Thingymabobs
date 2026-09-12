package dev.thingymabobs.blocks.Zoey.PlasmaGlobe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredItem;

public class PlasmaGlobeEntity extends ElectricBlockEntity implements ElectricBehaviour.SyncAppender {
	
	protected SwitchedWire wire;
	protected int colorBase, colorGlass, colorPlasma;
	protected int state;

	protected static CProperties.Prop CONFIG = null;
	public static float IDEAL_TEMPERATURE;
	public static float RESISTANCE_MIN;
	public static float RESISTANCE_MAX;
	public static float RATED_VOLTAGE;
	public static float MIN_VOLTAGE;
	public static float MAX_VOLTAGE;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		float power = prop.getThermal().getPower();
		IDEAL_TEMPERATURE = prop.getThermal().getTemp();
		RATED_VOLTAGE = prop.getFloat(CProperties.VOLTAGE).get();
		RESISTANCE_MIN = RATED_VOLTAGE*RATED_VOLTAGE/(power*2);
		RESISTANCE_MAX = RATED_VOLTAGE*RATED_VOLTAGE/power;
		MIN_VOLTAGE = RATED_VOLTAGE * 0.7f;
		MAX_VOLTAGE = RATED_VOLTAGE * 1.3f;
	};

	public static final DeferredItem<Item> TRANSFORMER = ModItems.TRANSFORMER;

	public PlasmaGlobeEntity(BlockPos pos, BlockState state){
		super(ModBlockEntities.PLASMA_GLOBE.get(), pos, state);
		this.state = state.getValue(PlasmaGlobe.STATE);
		setLazyTickRate(100);
	};

	@Override
	public void initialize() {
		super.initialize();
		electricBehaviour.setSyncAppender(this);
	};

	@Override
	public @Nullable ThermalBehaviour specifyThermalBehaviour() {
		return CONFIG.getThermal().createBehaviour(this);
	};

	@Override
	public void buildCircuit(CircuitBuilder builder) {
		builder.setTerminalCount(2);
		BlockState blockState = getBlockState();
		wire = builder.connectSwitch(
			getGlobeResistance(), builder.terminalNode(0), builder.terminalNode(1),
			PlasmaGlobe.hasFunctionalTransformer(blockState.getValue(PlasmaGlobe.STATE)));
	};


	// TRANSFORMER SEGMENT
	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		super.getRequiredItems(state);
		if (state.getValue(PlasmaGlobe.STATE) == PlasmaGlobe.STATE_EMPTY){
			return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, TRANSFORMER.get());
		};
		return ItemRequirement.NONE;
	};

	public boolean replaceTransformer(Player player, ItemStack usedStack, float hitY){
		replaceTransformerInternal(player,usedStack,hitY);
		return state != -1;
	};

	private void replaceTransformerInternal(Player player, ItemStack usedStack, float hitY) {
		if (usedStack == null || usedStack.isEmpty()){ return; };
		if (usedStack.is(TRANSFORMER.get()) && (usedStack.getCount() > 1 || player.isCreative())){
			playInteractTransformerSound();
			if (!level.isClientSide) {
				((ThermalBehaviour)thermalBehaviour).resetTemperature();
				if (!PlasmaGlobe.hasFunctionalTransformer(state) && !player.isCreative()){ 
					usedStack.shrink(1);
				};
			};
			state = PlasmaGlobe.STATE_OFF;
			notifyUpdate();
		} else if (usedStack.getItem() instanceof DyeItem dye) { // Fix heights for dye'ing
			playInteractDyeSound();
			int newColor = dye.getDyeColor().getTextureDiffuseColor();

			if (hitY < 6/16f){
				colorBase = newColor;
			} else if (hitY < 11/16f){
				colorPlasma = newColor;
			} else {
				colorGlass = newColor;
			};
			//notifyUpdate();
		};
	};


	//Interaction Sounds
	public void playInteractTransformerSound() {
		if (level == null || level.isClientSide){ return; };
		level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
	};

	public void playInteractDyeSound() {
		if (level == null || level.isClientSide) { return; };
		level.playSound(null, worldPosition, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.30F, 1.0F);
	};


	// Boom boom
	public void playBlowEffect() {
		if (level == null) return;
		var pos = this.worldPosition.getCenter();
		if (level.isClientSide()) {
			SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, Direction.UP, 5);
		} else {
			ModdedSoundEvents.FUSE_POPS.playAt(level, pos, 1.0f, 1.0f, false);
		};
	};


	// Updates the local state of the lava lamp
	protected void updateState() {
		BlockState blockState = getBlockState();
		if(!level.isClientSide) {
			ThermalBehaviour thermal = (ThermalBehaviour)thermalBehaviour;
			int oldState = blockState.getValue(PlasmaGlobe.STATE);
			boolean functionalTransformer = PlasmaGlobe.hasFunctionalTransformer(state);

			meep : {
				if (!functionalTransformer || thermal.isOverheated()) {
					state = PlasmaGlobe.STATE_BLOWN;
					break meep;
				};

				wire.setResistance(getGlobeResistance());
				double voltage = wire.potentialDifference();
				if (voltage < MIN_VOLTAGE){
					state = PlasmaGlobe.STATE_OFF;
				} else if (voltage < RATED_VOLTAGE){
					state = PlasmaGlobe.STATE_ON_LOW;
				} else if (voltage < MAX_VOLTAGE){
					state = PlasmaGlobe.STATE_ON;
				} else {
					state = PlasmaGlobe.STATE_ON_HIGH;
				};
			};

			if (oldState != state) { // Send only on state changes
				wire.setState(functionalTransformer);
				if (!functionalTransformer) { thermal.resetTemperature(); };
				if (state == PlasmaGlobe.STATE_BLOWN) { playBlowEffect(); };
				level.setBlock(worldPosition, blockState.setValue(PlasmaGlobe.STATE, state), Block.UPDATE_ALL_IMMEDIATE);
				notifyUpdate();
			};
		};
	};


	// Tick stuff
	@Override
	public void tick() {
		super.tick();
		updateState();
		if (!level.isClientSide) { return; };
		updateParticles(1/20f);
		spawnParticles();
	};

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide) { return; };
	};




	private final List<PlasmaTendril> tendrils = new ArrayList<>(); // All active tendrils
	public static final double ElectrodePoint = 7 / 16.0; // the center bulb
	public static final double DistToGlass = 3.5 / 16.0; // dist up from electrode to glass
	public static final int MaxTendrils = 5; // Count of max tendrils allowed at once
	// ^ maybe make config able??

	// Particles (Implimented later)
	@OnlyIn(Dist.CLIENT)
	public void updateParticles(float deltaTime) {
		tendrils.removeIf(PlasmaTendril::Step);
	};

	@OnlyIn(Dist.CLIENT)
	public void spawnParticles() {
		if(tendrils.size() >= MaxTendrils){
			return;
		} else {
			tendrils.add(new PlasmaTendril());
		};
	};

	public List<PlasmaTendril> getTendrils() {
		return tendrils;
	};






	// Electrical stuff
	@Override
	public void electricalTick() {
		applyPower(wire);
	};

	@Override
	protected void applyPower(@Nullable AbstractElectricWire wire) {
		super.applyPower(wire);
		if (thermalBehaviour != null) {
			((ThermalBehaviour)thermalBehaviour).applyWirePower(wire);
		};
	};

	public float getGlobeResistance() {
		if (thermalBehaviour == null) { return RESISTANCE_MIN; };
		return (RESISTANCE_MIN + ((RESISTANCE_MAX - RESISTANCE_MIN) / 200f) * ((ThermalBehaviour)thermalBehaviour).getTemperature());
	};

	public boolean isActive() {
		return PlasmaGlobe.isPowered(getBlockState().getValue(PlasmaGlobe.STATE));
	};


	// Save && Write Stuff
	@Override
	protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("ColorB", colorBase);
		tag.putInt("ColorG", colorGlass);
		tag.putInt("ColorW", colorPlasma);
	};

	@Override
	protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		colorBase = tag.getInt("ColorB");
		colorGlass = tag.getInt("ColorG");
		colorPlasma = tag.getInt("ColorW");
	};

	@Override
	public void writeToSync(FriendlyByteBuf buff) {
		if (thermalBehaviour == null) {
			buff.writeFloat(0);
			buff.writeFloat(0);
		} else {
			ThermalBehaviour thermal = (ThermalBehaviour)thermalBehaviour;
			buff.writeFloat(thermal.getTemperature());
		};
	};

	@Override
	public void readFromSync(FriendlyByteBuf buff) {
		if (thermalBehaviour == null) {
			buff.readFloat();
			buff.readFloat();
		} else {
			ThermalBehaviour thermal = (ThermalBehaviour)thermalBehaviour;
			thermal.setTemperature(buff.readFloat());
		};
	};

};
