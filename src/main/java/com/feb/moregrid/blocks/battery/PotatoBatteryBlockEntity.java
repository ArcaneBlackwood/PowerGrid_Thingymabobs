package com.feb.moregrid.blocks.battery;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.client.ISoundSource;
import com.feb.moregrid.client.PotatoElectrocuteSoundInstance;
import com.feb.moregrid.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PotatoBatteryBlockEntity extends MultiBlockBatteryEntity implements ElectricBehaviour.SyncAppender, ISoundSource  {
	protected float rechargePower;
	protected float electrocuteVolume = 0f;
	public boolean hasAudioSource = false;

	public PotatoBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POTATO_BATTERY_BLOCK.get(), pos, state);
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
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var b = ThermalBehaviour.fromConfig(this, 100f);
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

	@Override
	public void tick() {
		super.tick();
		if (!this.level.isClientSide || !isController()) return;
		if (!hasAudioSource && electrocuteVolume > 0.01) {
            Minecraft.getInstance().getSoundManager().play(new PotatoElectrocuteSoundInstance(this));
			MoreGrid.LOGGER.info("Summon sound at "+this.getBlockPos());
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
