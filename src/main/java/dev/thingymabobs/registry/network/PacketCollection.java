package dev.thingymabobs.registry.network;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PacketCollection<T> {
	protected final Entry<T>[] packets;
	protected final Object2IntMap<Class<?>> packetIndices;

	@SafeVarargs
	public PacketCollection(final Entry<T>... packets) {
		this.packets = packets;
		packetIndices = new Object2IntOpenHashMap<>(packets.length);
		for (int i=0; i < packets.length; i++)
			packetIndices.put(packets[i].getPacketType(), i);
	}

	public int get(Class<T> packetClass) {
		return packetIndices.getInt(packetClass);
	}
	public int get(T packet) {
		return packetIndices.getInt(packet.getClass());
	}


	public @Nullable T get(int index) {
		if (index < 0 || index >= packets.length) return null;
		return packets[index].resolve();
	}
	public boolean isInstance(int index) {
		if (index < 0 || index >= packets.length) return false;
		return packets[index].isInstance();
	}



	public static final class Entry<K> {
		private Class<?> type;
		private @Nullable Supplier<K> constructor;
		private @Nullable K instance;


		private Entry(Class<K> type, @Nullable Supplier<K> constructor, @Nullable K instance) {
			if (instance == null && (type == null || constructor == null)) {
				throw new IllegalArgumentException("Must have one instance or a type and constructor.");
			}
			if (instance != null && type == null)
				this.type = instance.getClass();
			else
				this.type = type;
			this.constructor = constructor;
			this.instance = instance;
		}
		public Entry(Class<K> type, Supplier<K> constructor) {
			this(type, constructor, null);
		}
		public Entry(K instance) {
			this(null, null, instance);
		}

		public K resolve() {
			return instance == null ? constructor.get() : instance;
		}
		public boolean isInstance() {
			return instance != null;
		}
		public Class<?> getPacketType() {
			return type;
		}
		public Entry<K> asInstance() {
			if (!isInstance()) instance = constructor.get();
			return this;
		}
	}
}
