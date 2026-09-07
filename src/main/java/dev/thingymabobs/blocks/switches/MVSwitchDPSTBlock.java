package dev.thingymabobs.blocks.switches;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;


public class MVSwitchDPSTBlock extends SwitchBlock {
    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 8.5, 0, 11.5, 11.5, 2, 13.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 8.5, 0, 2.5, 11.5, 2, 4.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 0, 11.5, 7.5, 2, 13.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 0, 2.5, 7.5, 2, 4.5)
    };

    private static final VoxelShape SHAPE_DOWN = 
        box(2.5, 0, 4.5, 13.5, 3, 11.5);

    public MVSwitchDPSTBlock(Properties settings) {
        super(settings);
        this.maxVoltage = 640;
		this.zeroStateShift = false;
        this.terminalCount = DOWN_TERMINALS.length;
		this.switchStates = new SwitchStates(
            new SwitchStates.Connection[] {
                new SwitchStates.Connection(0, 1),
                new SwitchStates.Connection(2, 3)
            }, new SwitchStates.State[] {
                null,
                null,
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
