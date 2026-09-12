package dev.thingymabobs.blocks.battery;

import javax.annotation.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import dev.thingymabobs.client.ISoundSource;
import dev.thingymabobs.client.PotatoElectrocuteSoundInstance;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PotatoBatteryBlockEntity extends MultiBlockBatteryEntity implements ElectricBehaviour.SyncAppender, ISoundSource {
	protected float rechargePower;
	protected float electrocuteVolume = 0f;
	public boolean hasAudioSource = false;
	protected boolean isPoison;

	protected static CProperties.Prop CONFIG_POTATO = null, CONFIG_POISON = null;
	protected static BatterySpec SPEC_POTATO, SPEC_POISON;
	public static void configUpdatedPotato(CProperties.Prop prop) {
		CONFIG_POTATO = prop;
		SPEC_POTATO = prop.getBattery();
	}
	public static void configUpdatedPoison(CProperties.Prop prop) {
		CONFIG_POISON = prop;
		SPEC_POISON = prop.getBattery();
	}
	protected CProperties.Prop getConfig() {
		return isPoison ? CONFIG_POISON : CONFIG_POTATO;
	}

	public PotatoBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POTATO_BATTERY_BLOCK.get(), pos, state);
		if (isPoison = state.getBlock() instanceof PoisonousPotatoBatteryArray) {
			float maxPower = spec.calculateVoltage(1);
			maxPower = maxPower * maxPower / spec.calculateResistance(1);
			rechargePower = maxPower * 0.1f;
		} else {
			rechargePower = 0f;
		}
	}
	@Override
	public void initialize() {
		super.initialize();
		electricBehaviour.setSyncAppender(this);
	}
	@Override
	public @Nullable ThermalBehaviour specifyThermalBehaviour() {
		var b = getConfig().getThermal().createBehaviour(this);
		if(b != null)
			b.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
		return b;
	}


	@Override
	public float calculatePower() {
		// No recharging, but has passive recharge
		if (isController())
			return Math.max(super.calculatePower(), 0) - rechargePower;
		var controller = getControllerBE();
		if (controller == null) return 0;
		return controller.calculatePower();
	}
	public void resetState() {
		setEnergy(getCapacity());
		updateParameters();
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PotatoBatteryBlock.BAKED, false));

		if (level.isClientSide) return;
		thermalBehaviour.resetTemperature();
		notifyUpdate();
	}
	public void resetState(double energy) {
		setEnergy(energy);
		updateParameters();
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PotatoBatteryBlock.BAKED, false));

		if (level.isClientSide) return;
		thermalBehaviour.resetTemperature();
		notifyUpdate();
	}

	public float getVolume() {
		hasAudioSource = true;
		return isController() ? electrocuteVolume : 0;
	}
	@Override
	public Vec3 getPosition() {
		return getBlockPos().getCenter();
	}
	@Override
	public float getPitch() {
		return 1;
	}
	@Override
	public void onStop() {
		hasAudioSource = false;
	}
	@Override
	public RandomSource getRandom() {
		return level.random;
	}
	public boolean use(Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
		if(!(getControllerBE() instanceof PotatoBatteryBlockEntity be)) return false;
		BlockState state = getBlockState();
		IPotatoBattery block = (IPotatoBattery)state.getBlock();
		int maxPotatos = be.getSize()*24;
		double usage = be.getEnergy() / be.getCapacity();
		boolean baked = state.getValue(APotatoBatteryArray.BAKED).booleanValue();
		int usedPotatos = baked ? maxPotatos : (int)(maxPotatos+0.9999 - maxPotatos*usage);
		boolean isRequired = stack.is(block.getReplaceItem());
		boolean hasItems = isRequired && stack.getCount() >= usedPotatos;
		if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
			if(!player.isCreative() || !player.isShiftKeyDown()) {
				int processedPotatos = (int)(maxPotatos+0.4999 - maxPotatos*usage);
				stack.shrink(usedPotatos);
				ItemStack potatos = null;
				if (baked) {
					Item bakedItem = block.getBakedItem();
					if (bakedItem != null) potatos = new ItemStack(Items.BAKED_POTATO, 8);
				}
				if (!baked &&processedPotatos > 0)
					potatos = new ItemStack(block.getUsedItem(), processedPotatos);
				if (potatos != null)
					if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
			}
			be.resetState();
			if (be.getLevel() != null) {
				be.getLevel().playSound(
					null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
			}
			return true;
		} else if (isRequired) {
			usedPotatos = stack.getCount();
			stack.shrink(usedPotatos);
			double energyPerPotato = spec.getMaxCharge() / 24.0d;
			if (baked) be.resetState(energyPerPotato * usedPotatos);
			else be.setEnergy(be.getEnergy() + energyPerPotato * usedPotatos);
			if (be.getLevel() != null) {
				be.getLevel().playSound(
					null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
			}
			return true;
		}
		player.displayClientMessage(
				ModLang.translateDirect("thingymabobs.message.potato_battery_block.replace_required",
					block.getReplaceItem().getDescription()),
				true
		);
		return false;
	}
	@OnlyIn(Dist.CLIENT)
	private void playRemoveEffect() {
		if (level == null) return;
		level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
	}

	
	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide && isController()) tickClient();
	}
	@OnlyIn(Dist.CLIENT)
	private void tickClient() {
		if (!hasAudioSource && electrocuteVolume > 0.01) {
			net.minecraft.client.Minecraft.getInstance().getSoundManager().play(new PotatoElectrocuteSoundInstance(this));
		}
	}
	@Override
	public void electricalTick() {
		if (!isController() || sourceCoupling == null) {
			electrocuteVolume = 0;
			super.electricalTick();
			return;
		}
		electrocuteVolume = Math.clamp((Math.abs(super.calculatePower()) / (thermalBehaviour.maxPower() * this.getSize()) - 0.5f) * 2f, 0, 1);
		if(getBlockState().getValue(APotatoBatteryArray.BAKED)) {
			sourceCoupling.setVoltage(0);
			sourceCoupling.setResistance(1e+6f);
			energy = 0;
			return;
		} else {
			super.electricalTick();
			if(thermalBehaviour != null && thermalBehaviour.isOverheated() && !level.isClientSide) {
				level.setBlockAndUpdate(worldPosition, getBlockState().setValue(APotatoBatteryArray.BAKED, true));
				notifyUpdate();
			}
		}
	}

	@Override
	public void writeToSync(FriendlyByteBuf buffer) {
		buffer.writeInt(checksumBasic(this.getSize()));
		buffer.writeFloat(electrocuteVolume);
	}
	@Override
	public void readFromSync(FriendlyByteBuf buffer) {
		if (buffer.readInt() != checksumBasic(this.getSize())) return;
		electrocuteVolume = buffer.readFloat();
	}
	public static int checksumBasic(int value) {
		int b = value & 0xFF;
		int a = Integer.reverse(b) >>> 24;
		int c = ~b & 0xFF;
		return (b << 24) | (a << 16) | (c << 8) | (b ^ 0xA5);
	}
}
