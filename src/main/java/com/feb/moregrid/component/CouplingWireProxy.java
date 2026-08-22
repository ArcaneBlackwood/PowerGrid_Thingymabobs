package com.feb.moregrid.component;

import java.util.Collection;
import java.util.List;

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;

import com.feb.moregrid.MoreGrid;

public class CouplingWireProxy extends AbstractElectricWire {
	public VoltageSourceCoupling coupling;

	public CouplingWireProxy(VoltageSourceCoupling coupling) {
		super(null, null);
		this.coupling = coupling;
	}

	

	@Override
	public double potentialDifference() {
		if (this.node1 == null) {
			return -this.node2.getVoltage();
		} else {
			return this.node2 == null ? this.node1.getVoltage() : this.node1.getVoltage() - this.node2.getVoltage();
		}
	}
	@Override
	public double conductance() {
		return 0d;
	}
	@Override
	public double current() {
		return coupling.getCurrent();
	}
	@Override
	public double power() {
		return Math.abs(coupling.getVoltage() * coupling.getCurrent());
	}
	@Override
	public double internalPower() {
		return coupling.getVoltage() * coupling.getCurrent();
	}
	@Override
	public boolean isConverged() {
		return coupling.isConverged();
	}
	@Override
	public boolean isSource() {
		return coupling.isSource();
	}

	@Override
	public void prepare(int multiTicks) { }
	@Override
	public void postMicroTick() { }
	@Override
	public void stamp(IAdmittanceAdder admittance, double change) { }
	@Override
	public Collection<IElectricNode> coupledNodes() { return null; }
	@Override
	public List<INode> affectedNodes() { return null; }
	@Override
	public void valueChange(double x, double x0, int ticks) { }
	@Override
	protected void valueChange(double x, double x0) { }
	@Override
	public void setNode1(IElectricNode node1) { }
	@Override
	public void setNode2(IElectricNode node2) { }
	@Override
	public void flipNodes() { }
}
