package dev.thingymabobs.util;

import java.util.LinkedList;
import java.util.List;

public class SpecialStringBuilder {
	protected int currentSize = 0;
	protected List<byte[]> entries = new LinkedList<>();
	public SpecialStringBuilder() { }
	public SpecialStringBuilder append(char value, int repeat) {
		currentSize += repeat;
		byte[] t = new byte[repeat];
		for (int i=0; i <repeat; i++) t[i] = (byte)value;
		entries.add(t);
		return this;
	}
	public SpecialStringBuilder append(char value) {
		currentSize += 1;
		entries.add(new byte[] {(byte)value});
		return this;
	}
	public SpecialStringBuilder append(byte[] value) {
		currentSize += value.length;
		entries.add((byte[])value);
		return this;
	}
	public SpecialStringBuilder append(String value) {
		currentSize += value.length();
		entries.add(value.getBytes());
		return this;
	}
	public SpecialStringBuilder compile() {
		if (entries.size() < 2) return this;
		byte[] newEntry = new byte[currentSize];
		int index = 0;
		for (byte[] entry : entries) {
			for (int i = 0; i < entry.length; i++)
				newEntry[index++] = entry[i];
		}
		entries.clear();
		entries.add(newEntry);
		return this;
	}
	public SpecialStringBuilder reset() {
		currentSize = 0;
		entries.clear();
		return this;
	}
	@Override
	public String toString() {
		compile();
		return entries.size() == 1 ? new String(entries.getFirst()) : new String();
	}
}
