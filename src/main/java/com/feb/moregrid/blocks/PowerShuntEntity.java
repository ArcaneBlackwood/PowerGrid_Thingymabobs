package com.feb.moregrid.blocks;

import com.feb.moregrid.registry.ModBlockEntities;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.resistor.ResistorBoxTransform;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class PowerShuntEntity extends ElectricBlockEntity {
    protected PowerShuntValueBehaviour value;
    protected SwitchedWire wire;

    public PowerShuntEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_SHUNT.get(), pos, state);
    }

    protected PowerShuntValueBehaviour makeScroll() {
        return new PowerShuntValueBehaviour(Lang.translateDirect("devices.resistor.resistance"),
			this, new ResistorBoxTransform(), 4, 36);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this, 200f).behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        value = makeScroll();
        value.withResistanceCallback(R -> wire.setResistance(R));
        behaviours.add(value);
        wire.setResistance(value.getResistance());
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        BlockState state = getBlockState();
        wire = builder.connectSwitch(0.1f, builder.terminalNode(0), builder.terminalNode(1),
            !state.getValue(PowerShunt.BLOWN));
    }


    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
		super.getRequiredItems(state);
        if(state.getValue(PowerShunt.BLOWN))
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, AllItems.BRASS_SHEET.asStack());
        return ItemRequirement.NONE;
    }



    public void playBlowEffect() {
        if(level == null)
            return;
        var pos = this.worldPosition.getCenter();
        var facing = getBlockState().getValue(FuseHolderBlock.FACING);
        SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, facing.getOpposite(), 5);
        ModdedSoundEvents.FUSE_POPS.playAt(level, pos, 1.0f, 1.0f, false);
    }

	public void resetState() {
        BlockState state = getBlockState();
		if(state.getValue(PowerShunt.BLOWN) && !level.isClientSide) {
			ModdedSoundEvents.FUSE_INSTALL.playOnServer(level, worldPosition);
		}
        level.setBlockAndUpdate(worldPosition, state.setValue(PowerShunt.BLOWN, false));
		wire.setState(true);

		if (level.isClientSide) return;
		thermalBehaviour.resetTemperature();
        notifyUpdate();
	}
	public void blowState() {
		wire.setState(false);
		if (level.isClientSide) {
			playBlowEffect();
			return;
		}
		level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PowerShunt.BLOWN, true));
		thermalBehaviour.setTemperature(250);
		notifyUpdate();
	}

    @Override
    public void electricalTick() {
        applyPower(wire);
        BlockState state = getBlockState();
        if(!state.getValue(PowerShunt.BLOWN) && thermalBehaviour.isOverheated()) {
			blowState();
        }
    }

    public void setValue(double value) {
        wire.setResistance(value);
    }

    public double getValue() {
        return wire.getResistance();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        wire.setResistance(value.getResistance());
        BlockState state = getBlockState();
        boolean prevBlown = state.getValue(PowerShunt.BLOWN);
        boolean blown = tag.getBoolean("Blown");
        if(clientPacket && blown && !prevBlown)
            playBlowEffect();
        wire.setState(!blown);
		level.setBlockAndUpdate(worldPosition, state.setValue(PowerShunt.BLOWN, blown));
    }
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Blown", getBlockState().getValue(PowerShunt.BLOWN));
    }
    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putBoolean("Blown", getBlockState().getValue(PowerShunt.BLOWN));
    }
}
