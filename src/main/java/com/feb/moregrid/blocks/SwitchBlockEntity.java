package com.feb.moregrid.blocks;

import com.feb.moregrid.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class SwitchBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private float maxVoltage;
    private int switchState;
    private Float overvoltResistance;
    private boolean isButton;
	private SwitchStates switchStates;
    private boolean isNormallyClosed;
    private int buttonTimeout = 0;
    private boolean playEffect = false;
    private SwitchedWire[] wires;

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH.get(), pos, state);
        isButton = ((SwitchBlock) state.getBlock()).isButton;
        switchStates = ((SwitchBlock) state.getBlock()).switchStates;
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    private void overvoltEffect() {
        var pos = worldPosition.getCenter();
        var face = getBlockState().getValue(SwitchBlock.FACING);
        SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, face.getOpposite(), 7);
        ModdedSoundEvents.COMPONENT_EXPLODE.playAt(level, pos, 1, 1, true);
    }

    @Override
    public void electricalTick() {
        for(int i = 0; i < wires.length; i++) {
            var wire = wires[i];
            applyPower(wire);
            if(wire.isConverged() && Math.abs(wire.potentialDifference()) > maxVoltage && overvoltResistance == null) {
                wire.setState(true);
                // Pick a random resistance for failed switches to spice things up.
                overvoltResistance = level.random.nextFloat() * 1000f;
                wire.setResistance(overvoltResistance);
                playEffect = true;
                notifyUpdate();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if(isButton && buttonTimeout > 0) {
            --buttonTimeout;
            if(buttonTimeout <= 0) {
                SwitchBlock block = (SwitchBlock) getBlockState().getBlock();
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SwitchBlock.STATE, 0));
                block.useSound(level, worldPosition, true);
                setState(0);
            }
        }
    }

    public void setState(int state) {
        switchState = state;
        if(isButton && state == 1)
            buttonTimeout = 10;
        updateWires();
        if(!level.isClientSide)
            notifyUpdate();
    }
    private void updateWires() {
        for(int i = 0; i < switchStates.connections.length; i++) {
            SwitchStates.Connection con = switchStates.connections[i];
            con.state = false;
        }
        SwitchStates.State wireStates = switchStates.states[switchState];
        if (wireStates != null)
            for(int i = 0; i < wireStates.connections.length; i++) {
                switchStates.connections[wireStates.connections[i]].state = true;
            }
        for(int i = 0; i < switchStates.connections.length; i++) {
            SwitchStates.Connection con = switchStates.connections[i];
            if(overvoltResistance == null)
                wires[i].setState(con.state != isNormallyClosed);
        }
    }
    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(!(getBlockState().getBlock() instanceof SwitchBlock block))
            throw new IllegalArgumentException("Blocks with SwitchBlockEntity must inherit from SwitchBlock");
        builder.setTerminalCount(block.terminalCount);
        maxVoltage = block.getMaxVoltage();
        switchState = getBlockState().getValue(SwitchBlock.STATE);
        if (switchStates==null) switchStates =  block.switchStates;
        wires = new SwitchedWire[switchStates.connections.length];
        for(int i = 0; i < switchStates.connections.length; i++) {
            SwitchStates.Connection con = switchStates.connections[i];
            wires[i] = builder.connectSwitch(resistance(), builder.terminalNode(con.a), builder.terminalNode(con.b), false);
            if(overvoltResistance != null) {
                wires[i].setResistance(overvoltResistance);
                wires[i].setState(true);
            }
        }
        updateWires();
    }


    public void setNormallyClosed(boolean normallyClosed) {
        isNormallyClosed = normallyClosed;
    }

    public boolean isNormallyClosed() {
        return isNormallyClosed;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if(isButton) {
            buttonTimeout = tag.getByte("Timeout");
            isNormallyClosed = tag.getBoolean("NormallyClosed");
        }
        //if(clientPacket) {
        //    switchState = tag.getBoolean("State");
        //    wire.setState(switchState);
        //}
        if(tag.contains("Overvolted")) {
            overvoltResistance = tag.getFloat("Overvolted");
            if(overvoltResistance <= 0)
                overvoltResistance = 1f;
            for(int i = 0; i < wires.length; i++) {
                wires[i].setResistance(overvoltResistance);
                wires[i].setState(true);
            }
            if(tag.getBoolean("Effect"))
                overvoltEffect();
        } else {
            switchState = tag.getInt("State");
            updateWires();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if(clientPacket) {
            tag.putInt("State", switchState);
        }
        if(overvoltResistance != null) {
            tag.putFloat("Overvolted", overvoltResistance);
            if(playEffect) {
                tag.putBoolean("Effect", true);
                playEffect = false;
            }
        }
        if(isButton) {
            tag.putByte("Timeout", (byte) buttonTimeout);
            tag.putBoolean("NormallyClosed", isNormallyClosed);
        }
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(overvoltResistance == null)
            return false;
        Lang.translate("gui.damage_header")
                .forGoggles(tooltip);
        Lang.translate("gui.switch.overvolted")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }
}
