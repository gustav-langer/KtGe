package de.thecommcraft.ktge

import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import io.michaelrocks.bimap.MutableBiMap

interface NamedList<E, N : Any> : List<E> {
    operator fun get(name: N): E?
    fun nameOf(index: Int): N?
    fun indexOfName(name: N): Int?
    fun toMutable(): MutableNamedList<E, N>
}

interface MutableNamedList<E, N : Any> : NamedList<E, N>, MutableList<E> {
    fun add(element: E, name: N): Boolean
    fun addNullable(element: E, name: N?): Boolean = if (name == null) add(element) else add(element, name)

    fun toImmutable(): NamedList<E, N>
}

private fun <N : Any> biMapFromList(names: List<N?>): MutableBiMap<N, Int> =
    HashBiMap.create(names.mapIndexedNotNull { index: Int, name: N? -> name?.let { it to index } }.toMap())

class MapNamedList<E, N : Any>(private val items: List<E>, private val names: BiMap<N, Int>) : NamedList<E, N>,
    List<E> by items {
    constructor(items: List<E>, names: List<N?>) : this(items, biMapFromList(names))

    override fun toMutable(): MutableNamedList<E, N> =
        MapMutableNamedList(items.toMutableList(), HashBiMap.create(names))

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]
}

class MapMutableNamedList<E, N : Any>(private val items: MutableList<E>, private val names: MutableBiMap<N, Int>) :
    MutableNamedList<E, N>, MutableList<E> by items {
    constructor(items: MutableList<E>, names: List<N?>) : this(items, biMapFromList(names))

    override fun toImmutable(): NamedList<E, N> = MapNamedList(items, names)
    override fun toMutable(): MutableNamedList<E, N> = MapMutableNamedList(items, names)

    override fun get(name: N): E? = names[name]?.let { items[it] }
    override fun nameOf(index: Int): N? = names.inverse[index]
    override fun indexOfName(name: N): Int? = names[name]

    override fun add(element: E, name: N): Boolean {
        if (names.contains(name)) {
            return false
        } else {
            names[name] = items.size
            items.add(element)
            return true
        }
    }

    override fun clear() {
        items.clear()
        names.clear()
    }
}

fun <E, N : Any> namedListOf(vararg items: Pair<E, N?>) = MapNamedList(items.map { it.first }, items.map { it.second })
fun <E, N : Any> mutableNamedListOf(vararg items: Pair<E, N?>) =
    MapMutableNamedList(items.map { it.first }.toMutableList(), items.map { it.second })

val <E> E.noName
    get() = this to null
