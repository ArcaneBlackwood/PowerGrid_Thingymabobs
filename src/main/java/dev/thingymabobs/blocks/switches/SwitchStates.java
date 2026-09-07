package dev.thingymabobs.blocks.switches;

public class SwitchStates {
	public Connection[] connections;
	public State[] states;


	public static class State {
		public int[] connections;
		public State(int[] connections) {
			this.connections = connections;
		}
	}
	public static class Connection {
		public int a;
		public int b;
		public boolean state;
		public Connection(int a, int b) {
			this.a = a;
			this.b = b;
		}
	}
	public SwitchStates(Connection[] connections, State[] states) {
		this.connections = connections;
		this.states = states;
	}
}
