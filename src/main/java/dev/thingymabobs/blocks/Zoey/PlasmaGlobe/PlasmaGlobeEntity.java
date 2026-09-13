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
	public static final String CONFIG_MAX_TENDRILS = "max_tendrils";
	public static final String CONFIG_TENDRIL_LIFE = "tendril_lifetime";
	public static final String CONFIG_TENDRIL_LIFE_VARY = "tendril_lifetime_vary";
	protected static float IDEAL_TEMPERATURE;
	protected static float RESISTANCE_MIN;
	protected static float RESISTANCE_MAX;
	protected static float RATED_VOLTAGE;
	protected static float MIN_VOLTAGE;
	protected static float MAX_VOLTAGE;
	protected static final float ELECTRODE_POINT_Y = 7.0f / 16.0f; // the center bulb
	protected static final float ELECTRODE_GLASS_DISTANCE = 3.5f / 16.0f; // dist up from electrode to glass
	protected static int MAX_TENDRILS = 10;
	protected static int TRNDRIL_LIFE = 220;
	protected static int TRNDRIL_LIFE_VARY = 180;
	protected static int RANDOM_INT_MAX;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;

		float power = prop.getThermal().getPower();
		IDEAL_TEMPERATURE = prop.getThermal().getTemp();
		RATED_VOLTAGE = prop.getFloat(CProperties.VOLTAGE).get();
		RESISTANCE_MIN = RATED_VOLTAGE*RATED_VOLTAGE/(power*2);
		RESISTANCE_MAX = RATED_VOLTAGE*RATED_VOLTAGE/power;
		MIN_VOLTAGE = RATED_VOLTAGE * 0.7f;
		MAX_VOLTAGE = RATED_VOLTAGE * 1.3f;

		MAX_TENDRILS = prop.getInt(CONFIG_MAX_TENDRILS).get();
		TRNDRIL_LIFE = prop.getInt(CONFIG_TENDRIL_LIFE).get();
		TRNDRIL_LIFE_VARY = prop.getInt(CONFIG_TENDRIL_LIFE_VARY).get();
		RANDOM_INT_MAX = TRNDRIL_LIFE * MAX_TENDRILS;
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


	// Boom boom boom boom, I want you in my room
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
		} else {
			int oldState = blockState.getValue(PlasmaGlobe.STATE);

			if (oldState != state) { // Send only on state changes
				if (state == PlasmaGlobe.STATE_BLOWN) { playBlowEffect(); };
				level.setBlock(worldPosition, blockState.setValue(PlasmaGlobe.STATE, state), Block.UPDATE_ALL_IMMEDIATE);
			};
		}
	};


	// Tick stuff
	@Override
	public void tick() {
		super.tick();
		updateState();
		if (!level.isClientSide) { return; };
		if (PlasmaGlobe.isPowered(state)) {
			updateParticles(1/20f);
			int spawnChance = 2*(MAX_TENDRILS-tendrils.size());
			if (level.random.nextInt(RANDOM_INT_MAX) < spawnChance)
				spawnParticles();
		} else if (!tendrils.isEmpty()) {
			ClearTendrils();
		};
	};

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide) { return; };
	};




	private final List<PlasmaTendril> tendrils = new ArrayList<>(); // All active tendrils

	@OnlyIn(Dist.CLIENT)
	public void updateParticles(float deltaTime) {
		tendrils.removeIf(PlasmaTendril::Step);
	};

	@OnlyIn(Dist.CLIENT)
	public void ClearTendrils(){
		tendrils.clear();
	};

	@OnlyIn(Dist.CLIENT)
	public void spawnParticles() {
		if(tendrils.size() >= MAX_TENDRILS){
			return;
		} else {
			tendrils.add(new PlasmaTendril(level.random));
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
		tag.putByte("State", (byte)state);
	};

	@Override
	protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		colorBase = tag.getInt("ColorB");
		colorGlass = tag.getInt("ColorG");
		colorPlasma = tag.getInt("ColorW");
		state = tag.getByte("State");
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
