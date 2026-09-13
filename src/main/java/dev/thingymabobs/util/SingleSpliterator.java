package dev.thingymabobs.util;

import java.util.Spliterator;
import java.util.function.Consumer;

public class SingleSpliterator<T> implements Spliterator<T>{
	public T value;

	public SingleSpliterator() {
		value = null;
	}
	public SingleSpliterator(T value) {
		this.value = value;
	}
	public static <T> SingleSpliterator<T> of(T value) {
		return new SingleSpliterator<>(value);
	}
	public static <T> SingleSpliterator<T> empty() {
		return new SingleSpliterator<>();
	}

	@Override
	public int characteristics() {
		return DISTINCT | IMMUTABLE | SIZED;
	}

	@Override
	public long estimateSize() {
		return value == null ? 0 : 1;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (value == null) return false;
		action.accept(value);
		value = null;
		return true;
	}

	@Override
	public Spliterator<T> trySplit() {
		return null;
	}
	
}
