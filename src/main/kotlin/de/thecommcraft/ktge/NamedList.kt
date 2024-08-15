package de.thecommcraft.ktge

import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import io.michaelrocks.bimap.MutableBiMap

interface NamedList<E, N : Any> : List<E> {
    operator fun get(name: N): E?
    fun nameOf(index: Int): N?
    fun indexOfName(name: N): Int?
    fun itemsAndValues(): Pair<List<E>, List<N>>
    fun toMutable(): MutableNamedList<E, N>
}

interface MutableNamedList<E, N : Any> : NamedList<E, N>, MutableList<E> {
    fun add(element: E, name: N)
    fun toImmutable(): NamedList<E, N>
}

class MapNamedList<E, N : Any>(private val items: List<E>, names: List<N>) : NamedList<E, N>, List<E> by items {
    private val _names = names
    private val names: BiMap<N, Int> = HashBiMap.create(names.mapIndexed { index: Int, n -> n to index }.toMap())

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]
    override fun itemsAndValues(): Pair<List<E>, List<N>> {
        return (items to _names)
    }
    override fun toMutable(): MutableNamedList<E, N> {
        val values = itemsAndValues()
        return MapMutableNamedList(values.first.toMutableList(), values.second)
    }
}

class MapMutableNamedList<E, N : Any>(private val items: MutableList<E>, names: List<N>) : MutableNamedList<E, N>,
    MutableList<E> by items {
    private val _names = names.toMutableList()
    private val names: MutableBiMap<N, Int> =
        HashBiMap.create(names.mapIndexed { index: Int, n: N -> n to index }.toMap())

    override fun toImmutable(): NamedList<E, N> {
        val values = itemsAndValues()
        return MapNamedList(values.first, values.second)
    }

    override fun toMutable(): MutableNamedList<E, N> {
        val values = itemsAndValues()
        return MapMutableNamedList(values.first.toMutableList(), values.second)
    }

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]

    override fun add(element: E, name: N) {
        names[name] = items.size
        items.add(element)
        _names.add(name)
    }

    override fun itemsAndValues(): Pair<MutableList<E>, List<N>> {
        return (items to _names.toList())
    }
}

fun <E, N : Any> emptyMutableNamedList() = MapMutableNamedList<E, N>(mutableListOf(), emptyList())