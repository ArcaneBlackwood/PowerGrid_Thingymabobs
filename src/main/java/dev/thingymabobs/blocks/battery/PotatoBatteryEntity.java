package dev.thingymabobs.blocks.battery;

import dev.thingymabobs.registry.ModBlockEntities;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;

import java.util.List;

public class PotatoBatteryEntity extends BatteryBlockEntity {
    public PotatoBatteryEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POTATO_BATTERY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var b = ThermalBehaviour.simple(this, 0.2f, 0.01f, 100f);
        if(b != null)
            b.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
        return b;
    }

    @Override
    public float calculatePower() {
        // No recharging
        return Math.max(super.calculatePower(), 0);
    }

    @Override
    public void electricalTick() {
        if(getBlockState().getValue(PoisonousPotatoBattery.BAKED)) {
            sourceCoupling.setVoltage(0);
            sourceCoupling.setResistance(1e+6f);
            energy = 0;
            return;
        } else {
            super.electricalTick();
        }
        if(thermalBehaviour != null && thermalBehaviour.isOverheated() && !level.isClientSide) {
            thermalBehaviour.setTemperature(150);
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PotatoBatteryBlock.BAKED, true));
            notifyUpdate();
        }
    }
}
