package com.feb.moregrid.blocks;

import com.feb.moregrid.client.ISoundSource;
import com.feb.moregrid.client.PotatoElectrocuteSoundInstance;
import com.feb.moregrid.client.PotatoThermalBehaviour;
import com.feb.moregrid.registry.ModBlockEntities;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PotatoBatteryArrayEntity extends BatteryBlockEntity implements ElectricBehaviour.SyncAppender, ISoundSource {
	protected float rechargePower;
	protected float electrocuteVolume = 0f;
	public boolean hasAudioSource = false;
	
    public PotatoBatteryArrayEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POTATO_BATTERY_ARRAY.get(), pos, state);
		electricBehaviour.setSyncAppender(this);
		if (state.getBlock() instanceof PoisonousPotatoBatteryArray) {
			float maxPower = spec.calculateVoltage(1);
			maxPower = maxPower * maxPower / spec.calculateResistance(1);
			rechargePower = maxPower * 0.1f;
		} else {
			rechargePower = 0f;
		}
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var b = new PotatoThermalBehaviour(this, 100f, 0.04f, 100f);
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

	@Override
	public void tick() {
		super.tick();
		if (!this.level.isClientSide) return;
		((PotatoThermalBehaviour)thermalBehaviour).sparks = electrocuteVolume;
		if (!hasAudioSource && electrocuteVolume > 0.01) {
            Minecraft.getInstance().getSoundManager().play(new PotatoElectrocuteSoundInstance(this));
			
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
        }
        if(thermalBehaviour != null && thermalBehaviour.isOverheated() && !level.isClientSide) {
            thermalBehaviour.setTemperature(150);
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(APotatoBatteryArray.BAKED, true));
            notifyUpdate();
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
