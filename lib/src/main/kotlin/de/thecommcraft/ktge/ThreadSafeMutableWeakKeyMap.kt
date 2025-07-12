package de.thecommcraft.ktge
import java.util.WeakHashMap
import java.util.Collections

class ThreadSafeMutableWeakKeyMap<K, V> : MutableMap<K, V> {
    private val lock = Any()
    private val map = WeakHashMap<K, V>()

    override val size: Int
        get() = synchronized(lock) { map.size }

    override fun isEmpty(): Boolean = synchronized(lock) { map.isEmpty() }

    override fun containsKey(key: K): Boolean = synchronized(lock) { map.containsKey(key) }

    override fun containsValue(value: V): Boolean = synchronized(lock) { map.containsValue(value) }

    override fun get(key: K): V? = synchronized(lock) { map[key] }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = Collections.synchronizedSet(map.entries)

    override val keys: MutableSet<K>
        get() = Collections.synchronizedSet(map.keys)

    override val values: MutableCollection<V>
        get() = Collections.synchronizedCollection(map.values)

    override fun put(key: K, value: V): V? = synchronized(lock) { map.put(key, value) }

    override fun remove(key: K): V? = synchronized(lock) { map.remove(key) }

    override fun putAll(from: Map<out K, V>) = synchronized(lock) { map.putAll(from) }

    override fun clear() = synchronized(lock) { map.clear() }
}
