package com.feb.moregrid.blocks.switches;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;


public class MVSwitchSPDTBlock extends SwitchBlock {
    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 11.5, 0, 6.5, 13.5, 2, 9.5),
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 2.5, 0, 6.5, 4.5, 2, 9.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 0, 13.5, 9.5, 2, 15.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 0, 0.5, 9.5, 2, 2.5)
    };

    private static final VoxelShape SHAPE_DOWN = 
        box(4.5, 0, 2.5, 11.5, 3, 13.5);

    public MVSwitchSPDTBlock(Properties settings) {
        super(settings);
        this.maxVoltage = 640;
		this.zeroStateShift = true;
        this.terminalCount = DOWN_TERMINALS.length;
		this.switchStates = new SwitchStates(
            new SwitchStates.Connection[] {
                new SwitchStates.Connection(0, 2), new SwitchStates.Connection(1, 2),
                new SwitchStates.Connection(0, 3), new SwitchStates.Connection(1, 3)
            }, new SwitchStates.State[] {
                null,
                new SwitchStates.State(new int[]{2,3}),
                new SwitchStates.State(new int[]{0,1})
            }
        );
        setTerminalCollection(switchDownTerminals(this, DOWN_TERMINALS, SHAPE_DOWN));
    }

    @Override
    public void useSound(Level world, BlockPos pos, boolean open) {
        world.playSound(null, pos, ModdedSoundEvents.MV_SWITCH_CLICK.getMainEvent(), SoundSource.BLOCKS, 0.3F, open ? 1.25f : 1.5f);
    }
}
