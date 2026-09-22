package dev.thingymabobs.blocks.electricfurnace;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.client.SoundScapeSource;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.Thermal;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModLang;
import dev.thingymabobs.registry.ModSounds;
import dev.thingymabobs.registry.capabilities.ItemCapability;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.architectury.registry.menu.MenuRegistry;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Unit;
import java.util.ArrayList;
import java.util.List;

public class ElectricFurnaceEntity extends ElectricBlockEntity implements ItemCapability.Provider, IHaveGoggleInformation, MenuProvider, SoundScapeSource {
	protected static CProperties.PropDevice CONFIG = null;
	protected static Thermal THERMAL;
	protected static ElectricFurnaceConfig EF_CONFIG;
	protected static float DISSIPATOIN_DOOR_OPEN;
	protected static float BLOW_POWER;
	public static void configUpdated(CProperties.PropDevice prop) {
		CONFIG = prop;
		EF_CONFIG = prop.get(ElectricFurnaceConfig.class, "ef");
		THERMAL = prop.getThermal();
		Thingymabobs.LOGGER.info("ElectricFurnaceEntity config: "+prop+", thermals: "+THERMAL);
		DISSIPATOIN_DOOR_OPEN = ThermalBehaviour.dissipationFactor(THERMAL.getPower(), EF_CONFIG.getDoorOpenTemp()) 
			- ThermalBehaviour.dissipationFactor(THERMAL.getPower(), THERMAL.getTemp());
		BLOW_POWER = THERMAL.getPower() * 18 / 13 * EF_CONFIG.getCoilPowerMul();
	}

	public static final int SLOTS_INPUT = 4, SLOTS_OUTPUT = 4;


	protected SwitchedWire wire;
	protected FilteringBehaviour filtering;
	protected SmartInventory inputInventory;
	protected SmartInventory outputInventory;
	protected boolean contentsChanged = true;
	protected CombinedInvWrapper itemCapability;

	protected ElectricFurnaceRecipe.Either recipe = null;
	protected float progress = 0;
	protected boolean[] isProcessing = new boolean[SLOTS_INPUT];
	protected float[] burnTimeOutputs = new float[SLOTS_OUTPUT];
	protected boolean recipeFits = false;
	protected boolean goingToBurn = false;
	
	protected float cachedAmbientTemperature = -1000;
	public float doorState = 0f;
	



	public ElectricFurnaceEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state);

		inputInventory = new SmartInventory(SLOTS_INPUT, this).whenContentsChanged($ -> contentsChanged = true);
		outputInventory = new SmartInventory(SLOTS_OUTPUT, this).whenContentsChanged($ -> contentsChanged = true).forbidInsertion();
		itemCapability = new CombinedInvWrapper(inputInventory, outputInventory);
		
		setLazyTickRate(5);
	}
	@Override
	public @Nullable ThermalBehaviour specifyThermalBehaviour() {
		return THERMAL.createBehaviour(this)
			.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES | ThermalBehaviour.OVERHEAT_EXPLOSION);
	}
	@Override
	public void initialize() {
		super.initialize();
		if (level.isClientSide) addSoundScape();
	}
	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		filtering = new FilteringBehaviour(this, new FilterValueBox())
			.withCallback(newFilter -> contentsChanged = true)
			.forRecipes();
		behaviours.add(filtering);
		wire.setResistance(resistance());
	}
	@Override
	public IItemHandler getItemCapability(Direction direction) {
		return itemCapability;
	}
	@Override
	public void buildCircuit(CircuitBuilder builder) {
		builder.setTerminalCount(2);
		BlockState state = getBlockState();
		wire = builder.connectSwitch(1000, builder.terminalNode(0), builder.terminalNode(1),
			!state.getValue(ElectricFurnace.BLOWN) && state.getValue(ElectricFurnace.BUTTON));
	}




	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		super.getRequiredItems(state);
		if(state.getValue(ElectricFurnace.BLOWN))
			return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ModdedItems.RESISTIVE_COIL.asStack(3));
		return ItemRequirement.NONE;
	}
	public boolean useItemOn(Player player, BlockHitResult hit, InteractionHand hand, ItemStack stack) {
		BlockState state = getBlockState();
		AABB button = ElectricFurnace.getButtonShape(state).inflate(0.03125);
		if (button.contains(hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())))) {
			setPowered(!state.getValue(ElectricFurnace.BUTTON));
			return true;
		}

		if (tryItemIteract(player, hit, hand, stack)) return true;
		
		if (player.isShiftKeyDown() || AllItems.WRENCH.isIn(player.getItemInHand(hand))) return false;

		if(level.isClientSide) return true;
		MenuRegistry.openExtendedMenu((ServerPlayer) player, this, buf -> this.sendToMenu(
			new RegistryFriendlyByteBuf(buf, level.registryAccess(), ConnectionType.OTHER)));
		return true;
	}
	protected boolean tryItemIteract(Player player, BlockHitResult hit, InteractionHand hand, ItemStack stack) {
		BlockState state = getBlockState();
		if (!state.getValue(ElectricFurnace.BLOWN) || stack.isEmpty()) return false;
		if (!stack.is(ModdedItems.RESISTIVE_COIL.get())) return false;
		if (stack.getCount() < 4) return false;
		stack.shrink(4);
		setBlown(false);
		return true;
	}


	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (itemCapability == null) return false;
		ModLang.translate("tooltip.electric_furnace.title")
			.forGoggles(tooltip);
		
		BlockState state = getBlockState();
		if (state.getValue(ElectricFurnace.BLOWN))
			ModLang.translate("tooltip.electric_furnace.blown")
				.style(ChatFormatting.RED)
				.forGoggles(tooltip);
		if (thermalBehaviour != null)
			ModLang.translate("tooltip.electric_furnace.temp",
					Unit.TEMPERATURE.format(thermalBehaviour.getTemperature()))
				.style(ChatFormatting.GOLD)
				.forGoggles(tooltip);
		
		ModLang.translate("tooltip.power", Unit.POWER.format(wire.power()))
			.style(ChatFormatting.YELLOW)
			.forGoggles(tooltip);
		
		for (int i = 0; i < itemCapability.getSlots(); i++) {
			ItemStack stackInSlot = itemCapability.getStackInSlot(i);
			if (stackInSlot.isEmpty())
				continue;
			CreateLang.text("")
				.add(Component.translatable(stackInSlot.getDescriptionId())
					.withStyle(ChatFormatting.GRAY))
				.add(CreateLang.text(" x" + stackInSlot.getCount())
					.style(ChatFormatting.GREEN))
				.forGoggles(tooltip, 1);
		}
		

		return true;
	}





	private ThreadLocal<List<ItemStack>> inventoryShadow = new ThreadLocal<>();
	private List<ItemStack> getShadow() {
		List<ItemStack> shadow = inventoryShadow.get();
		if (shadow == null) inventoryShadow.set(shadow = new ArrayList<>(SLOTS_INPUT));
		shadow.clear();
		for (int i = 0; i < SLOTS_INPUT; i++)
			shadow.add(inputInventory.getItem(i));
		return shadow;
	}
	private List<ItemStack> getShadowCopy() {
		List<ItemStack> shadow = inventoryShadow.get();
		if (shadow == null) inventoryShadow.set(shadow = new ArrayList<>(SLOTS_INPUT));
		shadow.clear();
		for (int i = 0; i < SLOTS_INPUT; i++)
			shadow.add(inputInventory.getItem(i).copy());
		return shadow;
	}


	@Override
	public void electricalTick() {
		if (CONFIG == null) return;
		applyPower(wire);
		BlockState state = getBlockState();
		if(!state.getValue(ElectricFurnace.BLOWN) && (wire.power() > BLOW_POWER)) {
			setBlown(true);
		}
	}
	@Override
	public void tick() {
		super.tick();
		if (CONFIG == null) return;
		if (level.isClientSide) {
			doorState = ElectricFurnaceRenderer.approach(doorState, isDoorOpen() ? 1 : 0,
				ElectricFurnaceRenderer.DOOR_SPEED * 0.05f);
			return;
		}
		if (thermalBehaviour == null) return;
		if (cachedAmbientTemperature < -500) 
			cachedAmbientTemperature = AThermalBehaviour.getAmbientTemperature(level, getBlockPos());
		if (isDoorOpen())
			thermalBehaviour.applyTickPower(-DISSIPATOIN_DOOR_OPEN * (thermalBehaviour.getTemperature() - cachedAmbientTemperature));
		if (contentsChanged) {
			contentsChanged = false;
			if (recipe == null)
				findRecipe();
			else if (recipe.testRecipe(level, getShadow(), EF_CONFIG.getItemsPerProcess(), null) != recipe.getMultiplier() || recipe.getMultiplier() == 0)
				findRecipe();
			notifyUpdate();
		}

		float temp = thermalBehaviour.getTemperature();
		for (int i = 0; i < SLOTS_OUTPUT; i++) {
			ItemStack slot = outputInventory.getItem(i);
			if (slot.isEmpty()) {
				burnTimeOutputs[i] = 0;
				continue;
			}
			float burnTemp = ElectricFurnaceRecipe.Either.getBurnTemp(slot);
			if (temp <= burnTemp) {
				burnTimeOutputs[i] -= 1;
				if (burnTimeOutputs[i] < 0) burnTimeOutputs[i] = 0;
				continue;
			}
			float burnSpeed = Math.max(0, (temp - burnTemp) * EF_CONFIG.getOverburnSpeedMul());
			burnTimeOutputs[i] += burnSpeed;
			if (burnTimeOutputs[i] >= EF_CONFIG.getBurnTicks()) {
				burnTimeOutputs[i] = 0;
				List<ItemStack> outputs = ElectricFurnaceRecipe.Either.getBurntOutputItem(level.random, EF_CONFIG.getItemsPerProcess(), slot);
				Thingymabobs.LOGGER.info("Burnt output: "+i+" "+slot+" into #"+outputs.size());
				for (ItemStack output : outputs) {
					Thingymabobs.LOGGER.info("   item: "+output);
					addToOutput(output, i);
				}
			}
		}
		if (recipe == null) return;

		float minTemp = recipe.getMinTemp();
		float maxTemp = recipe.getMaxTemp();
		goingToBurn = recipe.shouldBurnInput(temp);
		float progressSpeed = Math.max(0, (temp - minTemp) * EF_CONFIG.getMaxSpeedMul() / (maxTemp - minTemp));
		if (minTemp <= temp && recipeFits) {
			progress += progressSpeed;
			if (progress >= recipe.getProcessingTime()) applyRecipe();
		} else {
			progress -= 1f;
			if (progress < 0) progress = 0;
		}
	}
	@Override
	public void lazyTick() {
		super.lazyTick();
		if (CONFIG == null) return;
		if (level.isClientSide) return;
		checkMenuCount();
		if (recipe != null) {
			notifyUpdate();
			return;
		}
		for (int i = 0; i < SLOTS_OUTPUT; i++) {
			if (burnTimeOutputs[i] < 0.5f) continue;
			notifyUpdate();
			return;
		}
	}
	private void applyRecipe() {
		List<ItemStack> outputs = recipe.applyRecipe(level, getShadow(), EF_CONFIG.getItemsPerProcess(), thermalBehaviour.getTemperature());
		progress = 0;
		recipe = null;
		contentsChanged = true;
		for (ItemStack output : outputs)
			addToOutput(output, -1);
	}
	private void addToOutput(ItemStack item, int preferSlot) {
		if (item.isEmpty()) return;
		int count = item.getCount();
		if (preferSlot >= 0) {
			ItemStack slot = outputInventory.getItem(preferSlot);
			if (slot.isEmpty() || ItemStack.isSameItem(item, slot)) {
				int transfer = Math.min(count, slot.getMaxStackSize() - slot.getCount());
				count -= transfer;
				if (count == 0) return;
			}
		}
		if (count == 0) return;
		for (int i = 0; i < SLOTS_OUTPUT; i++) {
			ItemStack slot = outputInventory.getItem(i);
			if (slot.isEmpty() || !ItemStack.isSameItem(item, slot)) continue;
			int transfer = Math.min(count, slot.getMaxStackSize() - slot.getCount());
			count -= transfer;
			slot.grow(transfer);
			if (count == 0) return;
		}
		item.setCount(count);
		for (int i = 0; i < SLOTS_OUTPUT; i++) {
			ItemStack slot = outputInventory.getItem(i);
			if (!slot.isEmpty()) continue;
			outputInventory.setItem(i, item);
			return;
		}
		spawnAtLocation(item, 0.52f);
	}
	private void findRecipe() {
		progress = 0;
		List<ItemStack> shadow = getShadow();
		recipe = ElectricFurnaceRecipe.tryMatch(level, shadow);
		if (recipe == null) {
			for (int i = 0; i < SLOTS_INPUT; i++) isProcessing[i] = false;
			return;
		}
		recipe.realizeRecipe(level, shadow, EF_CONFIG.getItemsPerProcess(), isProcessing);
		List<ItemStack> outputs = recipe.getResults();
		List<ItemStack> slots = getShadowCopy();
		mainLoop: for (ItemStack output : outputs) {
			if (output.isEmpty()) continue;
			int count = output.getCount();
			for (ItemStack slot : slots) {
				if (slot.isEmpty() || !ItemStack.isSameItem(output, slot)) continue;
				int transfer = Math.min(count, slot.getMaxStackSize() - slot.getCount());
				count -= transfer;
				slot.grow(transfer);
				if (count == 0) continue mainLoop;
			}
			if (count == 0) continue;
			for (ItemStack slot : slots) {
				if (!slot.isEmpty()) continue;
				continue mainLoop;
			}
			recipeFits = false;
			return;
		}
		recipeFits = true;
	}
	public ItemEntity spawnAtLocation(ItemStack stack, float offset) {
		if (stack.isEmpty()) {
			return null;
		} else if (this.level.isClientSide) {
			return null;
		} else {
			Vec3 center = getBlockPos().getCenter();
			ItemEntity itementity = new ItemEntity(this.level, center.x, center.y + offset, center.z, stack);
			itementity.setDefaultPickUpDelay();
			level.addFreshEntity(itementity);

			return itementity;
		}
	}



	@Override
	public Vec3 getSoundScapePos() {
		return this.getBlockPos().getCenter();
	}
	@Override
	public boolean isSoundScapeValid() {
		return !this.isRemoved();
	}
	@Override
	public float getSoundScapeVolume() {
		return 0.6f;
	}
	@Override
	public boolean getSoundScapeState() {
		float powerNorm = Mth.abs((float)(wire.power()/ THERMAL.getPower()));
		return powerNorm >= 0.05f;
	}
	@OnlyIn(Dist.CLIENT)
	protected void addSoundScape() {
		dev.thingymabobs.registry.ModSoundScapes.ELECTRIC_FURNACE_FAN.get().addSource(this);
	}
	@OnlyIn(Dist.CLIENT)
	protected void removeSoundScape() {
		dev.thingymabobs.registry.ModSoundScapes.ELECTRIC_FURNACE_FAN.get().removeSource(this);
	}

	protected boolean doorOpen = false;
	protected void onClose() {
		if (!level.isClientSide) notifyUpdate();
		if (!doorOpen) return;
		doorOpen = false;
		if (level.isClientSide)
			level.playSound(null, getBlockPos(), ModSounds.ELECTRIC_FURNACE_CLOSE.get(), SoundSource.BLOCKS, .5f, 1f);
	}
	protected void onOpen() {
		if (!level.isClientSide) notifyUpdate();
		if (doorOpen) return;
		doorOpen = true;
		if (level.isClientSide)
			level.playSound(null, getBlockPos(), ModSounds.ELECTRIC_FURNACE_OPEN.get(), SoundSource.BLOCKS, .5f, 1f);
	}
	@OnlyIn(Dist.CLIENT)
	public void playBlowEffect() {
		if(level == null)
			return;
		var pos = this.worldPosition.getCenter();
		var facing = getBlockState().getValue(FuseHolderBlock.FACING);
		SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, facing.getOpposite(), 5);
		level.playSound(null, getBlockPos(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 0.8F, 1.6F);
	}
	@OnlyIn(Dist.CLIENT)
	public void playRepairEffect() {
		level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, .2f,
			1f + level.getRandom().nextFloat());
	}
	@OnlyIn(Dist.CLIENT)
	public void playButtonEffect(boolean press) {
		level.playSound(null, getBlockPos(), press ? ModSounds.ELECTRIC_FURNACE_ON.get() : ModSounds.ELECTRIC_FURNACE_OFF.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
	}
	public void setBlown(boolean blown) {
		BlockState state = getBlockState();
		if (state.getValue(ElectricFurnace.BLOWN) == blown) return;
		level.setBlockAndUpdate(worldPosition, state.setValue(ElectricFurnace.BLOWN, blown));
		wire.setState(!blown && state.getValue(ElectricFurnace.BUTTON));

		if (level.isClientSide) {
			if (blown) playBlowEffect();
			else playRepairEffect();
			return;
		}
		notifyUpdate();
	}
	public void setPowered(boolean power) {
		BlockState state = getBlockState();
		if (state.getValue(ElectricFurnace.BUTTON) == power) return;
		level.setBlockAndUpdate(worldPosition, state.setValue(ElectricFurnace.BUTTON, power));
		wire.setState(power && !state.getValue(ElectricFurnace.BLOWN));

		if (level.isClientSide) {
			playButtonEffect(power);
			return;
		}
		notifyUpdate();
	}
	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inputInventory);
		ItemHelper.dropContents(level, worldPosition, outputInventory);
	}



	public boolean isDoorOpen() {
		return menuCount > 0;
	}
	public int getCoilLevel() {
		if (CONFIG == null) return 0;
		float powerNorm = Mth.abs((float)(wire.power()/ THERMAL.getPower()));
		if (powerNorm < 0.05) return 0;
		return Math.min(Mth.ceil(powerNorm * 4), 3);
	}
	public int getTempLevel() {
		if (thermalBehaviour == null || CONFIG == null) return 0;
		float temp = thermalBehaviour.getTemperature();
		if (temp < EF_CONFIG.getTempSmokeMin()) return 0;
		if (temp < EF_CONFIG.getTempSmokeMax()) return 1;
		if (temp < EF_CONFIG.getTempSmeltMin()) return 2;
		return 3;
	}
	public float getTempNorm() {
		if (thermalBehaviour == null || CONFIG == null) return 0f;
		return thermalBehaviour.getTemperature() / THERMAL.getOverheat();
	}
	public float getProgress(int slot) {
		return goingToBurn || !isProcessing[slot] ? 0 : recipe == null ? progress : progress / recipe.getProcessingTime();
	}
	public float getBurnProgress(boolean input, int slot) {
		if (CONFIG == null) return 0;
		if (input) return goingToBurn ? getProgress(slot) : 0f;
		else return burnTimeOutputs[slot] / EF_CONFIG.getBurnTicks();
	}




	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("InputItems", inputInventory.serializeNBT(registries));
		tag.put("OutputItems", outputInventory.serializeNBT(registries));
		tag.putBoolean("Blown", getBlockState().getValue(ElectricFurnace.BLOWN));
		tag.putBoolean("Power", getBlockState().getValue(ElectricFurnace.BUTTON));
		if (recipe != null && progress > 0.5f) {
			tag.putByte("Prog", (byte)Math.round(progress / recipe.getProcessingTime() * 255));
		}
		if (!clientPacket) return;

		if (recipe != null && progress > 0.5f) {
			tag.putBoolean("BurnI", goingToBurn);
			byte inputs = 0;
			for (int i = 0; i < SLOTS_INPUT; i++)
				if (isProcessing[i]) inputs |= 1 << i;
			tag.putByte("Inputs", inputs);
		}
		boolean burningItems = false;
		for (int i = 0; i < SLOTS_OUTPUT; i++) {
			if (burnTimeOutputs[i] < 0.5f) continue;
			burningItems = true;
			break;
		}
		if (burningItems) {
			List<Byte> burnings = new ArrayList<>();
			float burnScale = 255 / EF_CONFIG.getBurnTicks();
			for (int i = 0; i < SLOTS_OUTPUT; i++) burnings.add((byte)(burnTimeOutputs[i] * burnScale));
			tag.putByteArray("BurnO", burnings);
		}
		if (menuCount > 0) tag.putByte("Opens", (byte)menuCount);
	}
	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		inputInventory.deserializeNBT(registries, tag.getCompound("InputItems"));
		outputInventory.deserializeNBT(registries, tag.getCompound("OutputItems"));
		setBlown(tag.getBoolean("Blown"));
		setPowered(tag.getBoolean("Power"));
		if (tag.contains("Prog")) {
			progress = (tag.getByte("Prog") & 0xFF) / 255f;
		} else {
			progress = 0;
		}
		if (!clientPacket) return;

		if (tag.contains("Prog")) {
			goingToBurn = tag.getBoolean("BurnI");
			byte inputs = tag.getByte("Inputs");
			for (int i = 0; i < SLOTS_INPUT; i++)
				isProcessing[i] = ((inputs >> i) & 1) == 1;
		}
		if (tag.contains("BurnO")) {
			byte[] burnings = tag.getByteArray("BurnO");
			float burnScale = EF_CONFIG.getBurnTicks() / 255;
			for (int i = 0; i < SLOTS_OUTPUT; i++) burnTimeOutputs[i] = (burnings[i] & 0xFF) * burnScale;
		} else {
			for (int i = 0; i < SLOTS_OUTPUT; i++) burnTimeOutputs[i] = 0;
		}
		int newMenuCount = tag.contains("Opens") ? tag.getByte("Opens") : 0;
		if (newMenuCount != menuCount) {
			if (menuCount == 0) onOpen();
			else if (newMenuCount == 0) onClose();
			menuCount = newMenuCount;
		}
	}
	@Override
	public void invalidate() {
		super.invalidate();
		if (hasLevel() && level.isClientSide) removeSoundScape();
	}


	static class FilterValueBox extends ValueBoxTransform.Sided {
		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8, 6, 16.03125);
		}
		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction.getOpposite() == state.getValue(ElectricFurnace.HORIZONTAL_FACING);
		}
	}


	protected int menuCount = 0;
	protected boolean clientMenuCount = false;
	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new ElectricFurnaceMenu(id, inventory, this);
	}
	@Override
	public Component getDisplayName() {
		return ModLang.translate("gui.electric_furnace.title").component();
	}
	public void onMenuAdded() {
		if (level.isClientSide) {
			clientMenuCount = true;
			if (menuCount <= 1) onOpen();
		} else {
			if (menuCount == 0) onOpen();
			menuCount += 1;
		}

	}
	public void onMenuRemoved() {
		if (level.isClientSide) {
			if (menuCount <= 1) onClose();
			clientMenuCount = false;
		} else { 
			if (menuCount == 1) onClose();
			menuCount -= 1;
		}
	}
	public void checkMenuCount() {//server only
		AABB aabb = (new AABB(getBlockPos())).inflate(Player.DEFAULT_BLOCK_INTERACTION_RANGE + 4.0F);
		List<Player> list = level.getEntities(EntityTypeTest.forClass(Player.class), aabb, this::isOwnContainer);

		int playerCount = list.size();
		if (menuCount != playerCount) {
			if (playerCount > 0 && menuCount == 0) {
				onOpen();
			} else if (playerCount == 0 && menuCount > 0) {
				onClose();
			}
			menuCount = 0;
		}
	}
	protected boolean isOwnContainer(Player player) {
		if (!(player.containerMenu instanceof ElectricFurnaceMenu menu)) return false;
		return menu.contentHolder == this;
	}
	/*@Override
	public void sendToMenu(RegistryFriendlyByteBuf buffer) {
		super.sendToMenu(buffer);
	}*/
}
