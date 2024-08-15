package de.thecommcraft.ktge

import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import io.michaelrocks.bimap.MutableBiMap

interface NamedList<E, N : Any> : List<E> {
    operator fun get(name: N): E?
    fun nameOf(index: Int): N?
    fun indexOfName(name: N): Int?
}

interface MutableNamedList<E, N : Any> : NamedList<E, N>, MutableList<E> {
    fun add(element: E, name: N)
    fun addNullable(element: E, name: N?) = if (name == null) {
        add(element); Unit
    } else add(element, name)
}

class MapNamedList<E, N : Any>(private val items: List<E>, names: List<N>) : NamedList<E, N>, List<E> by items {
    private val names: BiMap<N, Int> = HashBiMap.create(names.mapIndexed { index: Int, n: N -> n to index }.toMap())

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]
}

class MapMutableNamedList<E, N : Any>(private val items: MutableList<E>, names: List<N?>) : MutableNamedList<E, N>,
    MutableList<E> by items {
    private val names: MutableBiMap<N, Int> =
        HashBiMap.create(names.mapIndexedNotNull { index: Int, n: N? -> n?.let { n to index } }.toMap())

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]

    override fun add(element: E, name: N) {
        names[name] = items.size
        items.add(element)
    }
}

fun <E, N : Any> emptyMutableNamedList() = MapMutableNamedList<E, N>(mutableListOf(), emptyList())