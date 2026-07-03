package com.kotlinadmin.core.routing

import com.kotlinadmin.core.errors.ForbiddenError
import com.kotlinadmin.core.errors.UnauthorizedError
import com.kotlinadmin.core.plugins.EffectiveMethodKey
import com.kotlinadmin.core.session.UserSession
import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.RolesPermissions
import com.kotlinadmin.modules.access.models.Users
import com.kotlinadmin.modules.access.models.UsersRoles
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.namedGet(
    name: String,
    path: String = "",
    handler: suspend RoutingContext.() -> Unit
) {
    RouteRegistry.register(name, "GET", fullPath(path))
    if (path.isEmpty()) get { handler() } else get(path) { handler() }
}

fun Route.namedPost(
    name: String,
    path: String = "",
    handler: suspend RoutingContext.() -> Unit
) {
    RouteRegistry.register(name, "POST", fullPath(path))
    if (path.isEmpty()) post { handler() } else post(path) { handler() }
}

fun Route.namedPut(
    name: String,
    path: String = "",
    handler: suspend RoutingContext.() -> Unit
) {
    RouteRegistry.register(name, "PUT", fullPath(path))
    val block: Route.() -> Unit = {
        put { handler() }
        post {
            val eff = call.attributes.getOrNull(EffectiveMethodKey)
            if (eff == HttpMethod.Put) handler() else call.respond(HttpStatusCode.MethodNotAllowed)
        }
    }
    if (path.isEmpty()) block() else route(path) { block() }
}

fun Route.namedDelete(
    name: String,
    path: String = "",
    handler: suspend RoutingContext.() -> Unit
) {
    RouteRegistry.register(name, "DELETE", fullPath(path))
    val block: Route.() -> Unit = {
        delete { handler() }
        post {
            val eff = call.attributes.getOrNull(EffectiveMethodKey)
            if (eff == HttpMethod.Delete) handler() else call.respond(HttpStatusCode.MethodNotAllowed)
        }
    }
    if (path.isEmpty()) block() else route(path) { block() }
}

private fun Route.fullPath(relativePath: String): String {
    val parent = toString().removePrefix("(").removeSuffix(")")
    return if (relativePath.isEmpty()) parent else "$parent$relativePath"
}

suspend fun ApplicationCall.requireAuthenticated(): UserSession {
    return sessions.get<UserSession>() ?: throw UnauthorizedError()
}

suspend fun ApplicationCall.checkAccess(routeName: String, method: String) {
    val session = sessions.get<UserSession>() ?: throw UnauthorizedError()
    if (!hasAccess(session, routeName, method)) throw ForbiddenError()
}

fun ApplicationCall.hasAccess(session: UserSession, routeName: String, method: String): Boolean {
    if (session.roles.contains("Administrator")) return true
    return transaction {
        val userRoles = Users
            .innerJoin(UsersRoles)
            .innerJoin(Roles)
            .select(Roles.name)
            .where { UsersRoles.userId eq session.userId }
            .map { it[Roles.name] }

        if (userRoles.contains("Administrator")) return@transaction true

        Roles
            .innerJoin(RolesPermissions)
            .innerJoin(Permissions)
            .selectAll()
            .where {
                (Roles.name inList userRoles) and
                    (Permissions.name eq routeName) and
                    (Permissions.method eq method.uppercase())
            }
            .count() > 0
    }
}
