package com.feb.moregrid.component.trancievers;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

public class CleanupElectricWire extends ElectricWire {
	protected Runnable removeCallback = null;
	public CleanupElectricWire(double resistance, IElectricNode node1, IElectricNode node2) {
		super(resistance, node1, node2);
	}
	@Override
	public void remove() {
		super.remove();
		if (removeCallback != null) removeCallback.run();
	}
	public CleanupElectricWire onRemove(Runnable callback) {
		this.removeCallback = callback;
		return this;
	}
}
