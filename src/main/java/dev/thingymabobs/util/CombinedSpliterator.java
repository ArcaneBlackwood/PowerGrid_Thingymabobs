package dev.thingymabobs.util;

import java.util.Spliterator;
import java.util.function.Consumer;

public class CombinedSpliterator<T> implements Spliterator<T> {
	protected Spliterator<T>[] children;
	protected int count, index;
	protected long totalSize;
	public int characteristics;

	public CombinedSpliterator() {
		count = 0;
		precompute();
	}
	public CombinedSpliterator(Spliterator<T>[] children) {
		index = 0;
		this.children = children;
		count = children.length;
		precompute();
	}
	@SafeVarargs
	public static <T> CombinedSpliterator<T> of(Spliterator<T>... children) {
		return new CombinedSpliterator<>(children);
	}
	public static <T> CombinedSpliterator<T> empty() {
		return new CombinedSpliterator<>();
	}


	@Override
	public int characteristics() {
		return characteristics;
	}
	@Override
	public long estimateSize() {
		return totalSize;
	}


	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (count == 0) return false;
		for (;index < count; index++) {
			if (children[index].tryAdvance(action)) return true;
		}
		return false;
	}
	@Override
	public Spliterator<T> trySplit() {
		if (count == 0) return null;
		count--;
		if (index >= count) index = count-1;
		precompute();
		return children[count];
	}


	protected void precompute() {
		totalSize = 0;
		for (int i=0; i<count; i++) {
			if (children[i].hasCharacteristics(SIZED)) totalSize += children[i].estimateSize();
			else {
				totalSize = Long.MAX_VALUE;
				break;
			}
		}

		int allHave = CONCURRENT | DISTINCT | NONNULL | SUBSIZED;
		characteristics = IMMUTABLE | ORDERED | allHave;
		allHave = ~allHave;
		for (int i=0; i<count; i++) {
			int cr = children[i].characteristics();
			characteristics &= allHave | cr;
		}
		if (totalSize != Long.MAX_VALUE) characteristics &= ~(SIZED | SUBSIZED);
	}
}
