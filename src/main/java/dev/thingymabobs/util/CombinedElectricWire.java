package dev.thingymabobs.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;


/**
 * Mainly built for {@link #RelaySwitchWire}
 */
public class CombinedElectricWire extends ElectricWire {
	public final List<AbstractElectricWire> children;

	public CombinedElectricWire(List<AbstractElectricWire> children) {
		super(1, null, null);
		this.children = children;
	}
	public CombinedElectricWire() {
		super(1, null, null);
		this.children = new ArrayList<>();
	}


	public double potentialDifference() {
		double total = 0;
		for (AbstractElectricWire wire : children)
			if (wire.isConverged())
				total += wire.potentialDifference();
		return total;
	}
	public double current() {
		double total = 0;
		for (AbstractElectricWire wire : children)
			if (wire.isConverged())
				total += wire.current();
		return total;
	}
	public double power() {
		double total = 0;
		for (AbstractElectricWire wire : children)
			if (wire.isConverged())
				total += wire.power();
		return total;
	}
	public double internalPower() {
		double total = 0;
		for (AbstractElectricWire wire : children)
			if (wire.isConverged())
				total += wire.internalPower();
		return total;
	}
	public boolean isConverged() {
		for (AbstractElectricWire wire : children)
			if (wire.isConverged()) return true;
		return false;
	}


	public String toString() {
		return String.format("ElectricWire(R=%g)", this.resistance);
	}



	public void setResistance(double resistance) { }
	public void valueChange(double x, double x0, int ticks) { }
	public void setNetwork(ElectricalNetwork network) { }
	public void remove() { }
	public void setNode1(IElectricNode node1) { }
	public void setNode2(IElectricNode node2) { }
	public void flipNodes() { }
	public void prepare(int multiTicks) { }
	public void postMicroTick() { }

	@Deprecated(forRemoval = true)
	public double getResistance() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public double conductance() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public ElectricalNetwork getNetwork() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public IElectricNode getNode1() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public IElectricNode getNode2() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public void stamp(IAdmittanceAdder admittance, double change) {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public Collection<IElectricNode> coupledNodes() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
	@Deprecated(forRemoval = true)
	public List<INode> affectedNodes() {
		throw new UnsupportedOperationException("Cannot call getResistance in CombinedElectricWire");
	}
}