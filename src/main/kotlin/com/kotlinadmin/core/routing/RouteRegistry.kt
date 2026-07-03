package com.kotlinadmin.core.routing

import java.util.concurrent.CopyOnWriteArrayList

object RouteRegistry {
    data class RouteEntry(val name: String, val method: String, val path: String)

    private val _routes = CopyOnWriteArrayList<RouteEntry>()

    fun register(name: String, method: String, path: String) {
        if (_routes.none { it.name == name && it.method == method }) {
            _routes.add(RouteEntry(name, method, path))
        }
    }

    fun findByPathAndMethod(path: String, method: String): RouteEntry? {
        return _routes.find { entry ->
            entry.method.equals(method, ignoreCase = true) &&
                pathMatches(entry.path, path)
        }
    }

    fun findByName(name: String): RouteEntry? = _routes.find { it.name == name }

    fun findByNameAndMethod(name: String, method: String): RouteEntry? =
        _routes.find { it.name == name && it.method.equals(method, ignoreCase = true) }

    fun all(): List<RouteEntry> = _routes.toList()

    fun url(name: String, params: Map<String, String> = emptyMap()): String {
        val entry = findByName(name) ?: return "/"
        var path = entry.path
        params.forEach { (key, value) ->
            path = path.replace(":$key", value).replace("{$key}", value)
        }
        return path
    }

    fun clear() = _routes.clear()

    private fun pathMatches(pattern: String, actual: String): Boolean {
        val patternParts = pattern.trimEnd('/').split("/")
        val actualParts = actual.split("?").first().trimEnd('/').split("/")
        if (patternParts.size != actualParts.size) return false
        return patternParts.zip(actualParts).all { (p, a) ->
            p.startsWith(":") || p.startsWith("{") || p == a
        }
    }
}
