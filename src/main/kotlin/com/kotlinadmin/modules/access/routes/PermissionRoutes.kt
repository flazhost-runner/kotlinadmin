package com.kotlinadmin.modules.access.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.*
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.access.dto.DeleteSelectedDto
import com.kotlinadmin.modules.access.dto.PermissionDto
import com.kotlinadmin.modules.access.models.toMap
import com.kotlinadmin.modules.access.services.IPermissionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get

fun Application.accessPermissionWebModule() {
    val permissionService = get<IPermissionService>()

    routing {
        route("/admin/v1/access/permission") {
            namedGet("admin.v1.access.permission.index") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.index", "GET")

                // Lazy sync: scan RouteRegistry → upsert permissions
                permissionService.syncFromRouteRegistry()

                val params = call.request.queryParameters
                val result = permissionService.index(params)
                call.respondView(
                    "access/permission/index.ftl",
                    mapOf(
                        "datas" to result.items,
                        "paginate_data" to result.paginateData,
                        "filter" to params.entries().associate { it.key to it.value.firstOrNull() }
                    )
                )
            }

            namedGet("admin.v1.access.permission.create", "/create") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.create", "GET")
                call.respondView("access/permission/create.ftl")
            }

            namedPost("admin.v1.access.permission.store", "/store") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.store", "POST")
                val params = call.receiveParameters()

                val name = params["name"]?.trim() ?: ""
                val method = params["method"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val description = params["description"]?.trim() ?: ""
                val guardName = params["guard_name"]?.trim() ?: "web"

                val errors = mutableMapOf<String, String>()
                if (name.isBlank()) errors["name"] = "Name is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf(
                                "name" to name,
                                "method" to method,
                                "status" to status,
                                "description" to description
                            )
                        )
                    )
                    call.respondRedirect("/admin/v1/access/permission/create")
                    return@namedPost
                }

                permissionService.store(
                    PermissionDto(
                        name = name,
                        method = method.ifBlank { null },
                        status = status,
                        description = description.ifBlank { null },
                        guardName = guardName
                    ),
                    session.userId
                )
                call.sessions.set(session.withFlash("success", "Create Permission Success."))
                call.respondRedirect("/admin/v1/access/permission")
            }

            namedGet("admin.v1.access.permission.edit", "/{id}/edit") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.edit", "GET")
                val id = call.parameters["id"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)
                val perm = permissionService.edit(id)
                call.respondView("access/permission/edit.ftl", mapOf("data" to perm.toMap()))
            }

            namedPut("admin.v1.access.permission.update", "/{id}/update") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.update", "PUT")
                val id = call.parameters["id"] ?: return@namedPut call.respond(HttpStatusCode.BadRequest)
                val params = call.receiveParameters()

                val name = params["name"]?.trim() ?: ""
                val method = params["method"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val description = params["description"]?.trim() ?: ""
                val guardName = params["guard_name"]?.trim() ?: "web"

                val errors = mutableMapOf<String, String>()
                if (name.isBlank()) errors["name"] = "Name is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf(
                                "name" to name,
                                "method" to method,
                                "status" to status,
                                "description" to description
                            )
                        )
                    )
                    call.respondRedirect("/admin/v1/access/permission/$id/edit")
                    return@namedPut
                }

                permissionService.update(
                    id,
                    PermissionDto(
                        name = name,
                        method = method.ifBlank { null },
                        status = status,
                        description = description.ifBlank { null },
                        guardName = guardName
                    ),
                    session.userId
                )
                call.sessions.set(session.withFlash("success", "Update Permission Success."))
                call.respondRedirect("/admin/v1/access/permission")
            }

            namedDelete("admin.v1.access.permission.delete", "/{id}/delete") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.delete", "DELETE")
                val id = call.parameters["id"] ?: return@namedDelete call.respond(HttpStatusCode.BadRequest)
                permissionService.delete(id)
                call.sessions.set(session.withFlash("success", "Delete Permission Success."))
                call.respondRedirect("/admin/v1/access/permission")
            }

            namedPost("admin.v1.access.permission.delete_selected", "/delete_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.permission.delete_selected", "POST")
                val params = call.receiveParameters()
                val ids = params.getAll("selected[]") ?: emptyList()
                if (ids.isEmpty()) {
                    call.sessions.set(session.withFlash("warning", "No permissions selected"))
                    call.respondRedirect("/admin/v1/access/permission")
                    return@namedPost
                }
                permissionService.deleteSelected(ids)
                call.sessions.set(session.withFlash("success", "Delete Permission Success."))
                call.respondRedirect("/admin/v1/access/permission")
            }
        }
    }
}

fun Application.accessPermissionApiModule() {
    val permissionService = get<IPermissionService>()

    routing {
        authenticate("api") {
            route("/api/v1/access/permission") {
                namedGet("api.v1.access.permission.index") {
                    permissionService.syncFromRouteRegistry()
                    val params = call.request.queryParameters
                    val result = permissionService.index(params)
                    call.respondJson(
                        data = mapOf(
                            "data" to result.items.map { it.toMap() },
                            "pagination" to result.paginateData.toMap()
                        )
                    )
                }

                namedPost("api.v1.access.permission.store", "/store") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val dto = call.receive<PermissionDto>()
                    val perm = permissionService.store(dto, principal.subject!!)
                    call.respondJson(
                        HttpStatusCode.Created,
                        data = mapOf("message" to "Permission created", "data" to perm.toMap())
                    )
                }

                namedGet("api.v1.access.permission.edit", "/{id}/edit") {
                    val id = call.parameters["id"]!!
                    val perm = permissionService.edit(id)
                    call.respondJson(data = mapOf("data" to perm.toMap()))
                }

                namedPut("api.v1.access.permission.update", "/{id}/update") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val id = call.parameters["id"]!!
                    val dto = call.receive<PermissionDto>()
                    val perm = permissionService.update(id, dto, principal.subject!!)
                    call.respondJson(data = mapOf("message" to "Permission updated", "data" to perm.toMap()))
                }

                namedDelete("api.v1.access.permission.delete", "/{id}/delete") {
                    val id = call.parameters["id"]!!
                    permissionService.delete(id)
                    call.respondJson(data = mapOf("message" to "Permission deleted"))
                }

                namedPost("api.v1.access.permission.delete_selected", "/delete_selected") {
                    val dto = call.receive<DeleteSelectedDto>()
                    permissionService.deleteSelected(dto.selected)
                    call.respondJson(
                        data = mapOf(
                            "message" to "${dto.selected.size} permission(s) deleted",
                            "count" to dto.selected.size
                        )
                    )
                }
            }
        }
    }
}
