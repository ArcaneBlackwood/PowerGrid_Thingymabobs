package dev.thingymabobs.blocks.battery;

import dev.thingymabobs.client.ISoundSource;
import dev.thingymabobs.client.PotatoElectrocuteSoundInstance;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModLang;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import java.util.List;
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

public class PotatoBatteryArrayEntity extends BatteryBlockEntity implements ElectricBehaviour.SyncAppender, ISoundSource {
	protected float rechargePower;
	protected float electrocuteVolume = 0f;
	public boolean hasAudioSource = false;
	
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

	public PotatoBatteryArrayEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POTATO_BATTERY_ARRAY.get(), pos, state);
		if (state.getBlock() instanceof PoisonousPotatoBatteryArray) {
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
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
	}
	@Override
	public @Nullable ThermalBehaviour specifyThermalBehaviour() {
		var b = PotatoThermalBehaviour.fromConfig(this, 100f);
		if(b != null)
			b.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
		return b;
	}


	public void resetState() {
		setEnergy(getCapacity());
		updateParameters();
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PotatoBatteryArray.BAKED, false));

		if(level.isClientSide) return;
		thermalBehaviour.resetTemperature();
		notifyUpdate();
	}
	@Override
	public float calculatePower() {
		// No recharging, but has passive recharge
		return Math.max(super.calculatePower(), 0) - rechargePower;
	}
	@Override
	public float getVolume() {
		hasAudioSource = true;
		return electrocuteVolume;
	}
	@Override
	public Vec3 getPosition() {
		return this.getBlockPos().getCenter();
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
		boolean hasItems = stack.is(Items.POISONOUS_POTATO) && stack.getCount() >= 8;
		BlockState state = getBlockState();
		IPotatoBattery block = (IPotatoBattery)state.getBlock();
		if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
			if(!player.isCreative() || !player.isShiftKeyDown()) {
				double usage = getEnergy() / getCapacity();
				boolean baked = state.getValue(APotatoBatteryArray.BAKED).booleanValue();
				int usedPotatos = baked ? 8 : (int)(8.9999 - 8*usage);
				int processedPotatos = (int)(8.4999 - 8*usage);
				stack.shrink(usedPotatos);
				ItemStack potatos = null;
				if (baked) {
					Item bakedItem = block.getBakedItem();
					if (bakedItem != null) potatos = new ItemStack(Items.BAKED_POTATO, 8);
				}
				if (potatos == null && !baked && processedPotatos > 0)
					potatos = new ItemStack(Items.BONE_MEAL, processedPotatos);
				if (potatos != null)
					if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
			}
			resetState();
			if (level.isClientSide) playRemoveEffect();
			return true;
		}
		player.displayClientMessage(
			ModLang.translateDirect("message.poisonous_potato_battery.replace_required",
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
		if (this.level.isClientSide) tickClient();;
	}
	@OnlyIn(Dist.CLIENT)
	private void tickClient() {
		((PotatoThermalBehaviour)thermalBehaviour).sparks = electrocuteVolume;
		if (!hasAudioSource && electrocuteVolume > 0.01) {
			net.minecraft.client.Minecraft.getInstance().getSoundManager().play(new PotatoElectrocuteSoundInstance(this));
			
		}
	}
	@Override
	public void electricalTick() {
		electrocuteVolume = Math.clamp((Math.abs(super.calculatePower()) - thermalBehaviour.maxPower() * 0.5f) * 0.5f, 0, 1);
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
		buffer.writeFloat(electrocuteVolume);
	}
	@Override
	public void readFromSync(FriendlyByteBuf buffer) {
		electrocuteVolume = buffer.readFloat();
	}

}
