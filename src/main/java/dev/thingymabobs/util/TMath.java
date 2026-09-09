package dev.thingymabobs.util;

public class TMath {
	
	public static float fastPow2(float x) {
		return Float.intBitsToFloat((int)((x + 127.0f) * 8388608.0f));
	}
	private static final float _fastLog2Inv = 1f / 8388608.0f;
	public static float fastLog2(float x) {
		return (float)Float.floatToRawIntBits(x) * _fastLog2Inv - 127.0f;
	}
	protected static float softClamp(float x) {
		return 1f-fastLog2(fastPow2(-x)*(fastPow2(x)+1f));
	}
	public static class SoftMax {
		private float constant = 0, smooth = 1f;
		public SoftMax() { }
		public SoftMax(float smooth) {
			setSmooth(smooth);
		}
		public SoftMax setSmooth(float smooth) {
			constant = fastLog2(fastPow2(1/smooth)-1);
			this.smooth = smooth;
			return this;
		}
		public float compute(float x) {
			return (1 - fastLog2(1+fastPow2(-x/smooth + constant)))
				* smooth + 1 - smooth;
		}
		public float compute(float x, float scale) {
			if (x < 0) return -compute(- x / scale) * scale;
			else return compute(x / scale) * scale;
		}
	}
}
