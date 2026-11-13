package io.github.raghavsatyadev.openplayer.utils

import java.util.LinkedHashMap

class LruCache<K : Any, V : Any>(private val maxSize: Int) {
    init {
        require(maxSize > 0) { "maxSize <= 0" }
    }

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    fun get(key: K): V? = synchronized(this) {
        map[key]
    }

    fun put(key: K, value: V): V? = synchronized(this) {
        map.put(key, value)
    }

    fun remove(key: K): V? = synchronized(this) {
        map.remove(key)
    }

    fun evictAll(): Unit = synchronized(this) {
        map.clear()
    }

    fun size(): Int = synchronized(this) {
        map.size
    }

    fun maxSize(): Int = maxSize
}
