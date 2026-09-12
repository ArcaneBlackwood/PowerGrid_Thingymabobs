package dev.thingymabobs.blocks.switches;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.Prop;


public class LVSwitchDPDTBlock extends SwitchBlock {
	private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
			new TerminalBoundingBox(IDecoratedTerminal.COMMON, 12.5, 0, 6.5, 14.5, 2, 9.5),
			new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 8.5, 0, 11.5, 11.5, 2, 13.5),
			new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 8.5, 0, 2.5, 11.5, 2, 4.5),
			new TerminalBoundingBox(IDecoratedTerminal.COMMON, 1.5, 0, 6.5, 3.5, 2, 9.5),
			new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 0, 11.5, 7.5, 2, 13.5),
			new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4.5, 0, 2.5, 7.5, 2, 4.5)
	};

	protected static CProperties.Prop CONFIG = null;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
	}
	@Override
	protected Prop getConfig() {
		return CONFIG;
	}

	private static final VoxelShape SHAPE_DOWN = Shapes.or(
			box(3.5, 0, 4.5, 12.5, 2, 11.5),
			box(3.5, 2, 5.5, 12.5, 5, 10.5)
	);

	public LVSwitchDPDTBlock(Properties settings) {
		super(settings);
		this.zeroStateShift = true;
		this.terminalCount = DOWN_TERMINALS.length;
		this.switchStates = new SwitchStates(
			new SwitchStates.Connection[] {
				new SwitchStates.Connection(0, 1), new SwitchStates.Connection(3, 4),
				new SwitchStates.Connection(0, 2), new SwitchStates.Connection(3, 5)
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
		world.playSound(null, pos, ModdedSoundEvents.LV_SWITCH_CLICK.getMainEvent(), SoundSource.BLOCKS, 0.3F, open ? 0.65f : 0.75f);
	}
}
