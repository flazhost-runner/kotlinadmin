package com.kotlinadmin.modules.access.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.*
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.access.dto.BulkPermissionDto
import com.kotlinadmin.modules.access.dto.DeleteSelectedDto
import com.kotlinadmin.modules.access.dto.RoleDto
import com.kotlinadmin.modules.access.models.toMap
import com.kotlinadmin.modules.access.services.IPermissionService
import com.kotlinadmin.modules.access.services.IRoleService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get

fun Application.accessRoleWebModule() {
    val roleService = get<IRoleService>()
    val permissionService = get<IPermissionService>()

    routing {
        route("/admin/v1/access/role") {
            namedGet("admin.v1.access.role.index") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.index", "GET")
                val params = call.request.queryParameters
                val result = roleService.index(params)
                call.respondView(
                    "access/roles/index.ftl",
                    mapOf(
                        "datas" to result.items,
                        "paginate_data" to result.paginateData,
                        "filter" to params.entries().associate { it.key to it.value.firstOrNull() }
                    )
                )
            }

            namedGet("admin.v1.access.role.create", "/create") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.create", "GET")
                call.respondView("access/roles/create.ftl")
            }

            namedPost("admin.v1.access.role.store", "/store") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.store", "POST")
                val params = call.receiveParameters()

                val name = params["name"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val description = params["description"]?.trim() ?: ""

                val errors = mutableMapOf<String, String>()
                if (name.isBlank()) errors["name"] = "Name is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf("name" to name, "status" to status, "description" to description)
                        )
                    )
                    call.respondRedirect("/admin/v1/access/role/create")
                    return@namedPost
                }

                roleService.store(
                    RoleDto(name = name, status = status, description = description.ifBlank { null }),
                    session.userId
                )
                call.sessions.set(session.withFlash("success", "Create Role Success."))
                call.respondRedirect("/admin/v1/access/role")
            }

            namedGet("admin.v1.access.role.edit", "/{id}/edit") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.edit", "GET")
                val id = call.parameters["id"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)
                val role = roleService.edit(id)
                call.respondView("access/roles/edit.ftl", mapOf("data" to role.toMap()))
            }

            namedPut("admin.v1.access.role.update", "/{id}/update") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.update", "PUT")
                val id = call.parameters["id"] ?: return@namedPut call.respond(HttpStatusCode.BadRequest)
                val params = call.receiveParameters()

                val name = params["name"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val description = params["description"]?.trim() ?: ""

                val errors = mutableMapOf<String, String>()
                if (name.isBlank()) errors["name"] = "Name is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf("name" to name, "status" to status, "description" to description)
                        )
                    )
                    call.respondRedirect("/admin/v1/access/role/$id/edit")
                    return@namedPut
                }

                roleService.update(
                    id,
                    RoleDto(name = name, status = status, description = description.ifBlank { null }),
                    session.userId
                )
                call.sessions.set(session.withFlash("success", "Update Role Success."))
                call.respondRedirect("/admin/v1/access/role")
            }

            namedDelete("admin.v1.access.role.delete", "/{id}/delete") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.delete", "DELETE")
                val id = call.parameters["id"] ?: return@namedDelete call.respond(HttpStatusCode.BadRequest)
                roleService.delete(id)
                call.sessions.set(session.withFlash("success", "Delete Role Success."))
                call.respondRedirect("/admin/v1/access/role")
            }

            namedPost("admin.v1.access.role.delete_selected", "/delete_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.delete_selected", "POST")
                val params = call.receiveParameters()
                val ids = params.getAll("selected[]") ?: emptyList()
                if (ids.isEmpty()) {
                    call.sessions.set(session.withFlash("warning", "No roles selected"))
                    call.respondRedirect("/admin/v1/access/role")
                    return@namedPost
                }
                roleService.deleteSelected(ids)
                call.sessions.set(session.withFlash("success", "Delete Role Success."))
                call.respondRedirect("/admin/v1/access/role")
            }

            // ── Permission management per role ────────────────────────────
            namedGet("admin.v1.access.role.permission", "/{id}/permission") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.permission", "GET")
                val id = call.parameters["id"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)

                // Lazy sync: scan RouteRegistry → upsert permissions
                permissionService.syncFromRouteRegistry()

                val params = call.request.queryParameters
                val role = roleService.edit(id)
                val result = roleService.getPermissions(id, params)
                call.respondView(
                    "access/roles/permission.ftl",
                    mapOf(
                        "role" to role.toMap(),
                        "datas" to result.items.map { it.toMap() },
                        "paginate_data" to result.paginateData,
                        "filter" to params.entries().associate { it.key to it.value.firstOrNull() }
                    )
                )
            }

            namedGet("admin.v1.access.role.permission.assign", "/{id}/permission/{permId}/assign") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.permission.assign", "GET")
                val id = call.parameters["id"]!!
                val permId = call.parameters["permId"]!!
                roleService.assignPermission(id, permId)
                call.sessions.set(session.withFlash("success", "Assign Permission Success."))
                call.respondRedirect("/admin/v1/access/role/$id/permission")
            }

            namedPost("admin.v1.access.role.permission.assign_selected", "/{id}/permission/assign_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.permission.assign_selected", "POST")
                val id = call.parameters["id"]!!
                val params = call.receiveParameters()
                val permIds = params.getAll("selected[]") ?: emptyList()
                roleService.assignPermissions(id, permIds)
                call.sessions.set(session.withFlash("success", "Assign Permission Success."))
                call.respondRedirect("/admin/v1/access/role/$id/permission")
            }

            namedGet("admin.v1.access.role.permission.unassign", "/{id}/permission/{permId}/unassign") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.permission.unassign", "GET")
                val id = call.parameters["id"]!!
                val permId = call.parameters["permId"]!!
                roleService.unassignPermission(id, permId)
                call.sessions.set(session.withFlash("success", "Unassign Permission Success."))
                call.respondRedirect("/admin/v1/access/role/$id/permission")
            }

            namedPost("admin.v1.access.role.permission.unassign_selected", "/{id}/permission/unassign_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.role.permission.unassign_selected", "POST")
                val id = call.parameters["id"]!!
                val params = call.receiveParameters()
                val permIds = params.getAll("selected[]") ?: emptyList()
                roleService.unassignPermissions(id, permIds)
                call.sessions.set(session.withFlash("success", "Unassign Permission Success."))
                call.respondRedirect("/admin/v1/access/role/$id/permission")
            }
        }
    }
}

fun Application.accessRoleApiModule() {
    val roleService = get<IRoleService>()
    val permissionService = get<IPermissionService>()

    routing {
        authenticate("api") {
            route("/api/v1/access/role") {
                namedGet("api.v1.access.role.index") {
                    val params = call.request.queryParameters
                    val result = roleService.index(params)
                    call.respondJson(
                        data = mapOf(
                            "data" to result.items.map { it.toMap() },
                            "pagination" to result.paginateData.toMap()
                        )
                    )
                }

                namedPost("api.v1.access.role.store", "/store") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val dto = call.receive<RoleDto>()
                    val role = roleService.store(dto, principal.subject!!)
                    call.respondJson(
                        HttpStatusCode.Created,
                        data = mapOf("message" to "Role created", "data" to role.toMap())
                    )
                }

                namedGet("api.v1.access.role.edit", "/{id}/edit") {
                    val id = call.parameters["id"]!!
                    val role = roleService.edit(id)
                    call.respondJson(data = mapOf("data" to role.toMap()))
                }

                namedPut("api.v1.access.role.update", "/{id}/update") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val id = call.parameters["id"]!!
                    val dto = call.receive<RoleDto>()
                    val role = roleService.update(id, dto, principal.subject!!)
                    call.respondJson(data = mapOf("message" to "Role updated", "data" to role.toMap()))
                }

                namedDelete("api.v1.access.role.delete", "/{id}/delete") {
                    val id = call.parameters["id"]!!
                    roleService.delete(id)
                    call.respondJson(data = mapOf("message" to "Role deleted"))
                }

                namedPost("api.v1.access.role.delete_selected", "/delete_selected") {
                    val dto = call.receive<DeleteSelectedDto>()
                    roleService.deleteSelected(dto.selected)
                    call.respondJson(
                        data = mapOf("message" to "${dto.selected.size} role(s) deleted", "count" to dto.selected.size)
                    )
                }

                // Permission management
                namedGet("api.v1.access.role.permission", "/{id}/permission") {
                    val id = call.parameters["id"]!!
                    permissionService.syncFromRouteRegistry()
                    val params = call.request.queryParameters
                    val result = roleService.getPermissions(id, params)
                    call.respondJson(
                        data = mapOf("data" to result.items.map { it.toMap() }, "pagination" to result.paginateData)
                    )
                }

                namedGet("api.v1.access.role.permission.assign", "/{id}/permission/{permId}/assign") {
                    val id = call.parameters["id"]!!
                    val permId = call.parameters["permId"]!!
                    roleService.assignPermission(id, permId)
                    call.respondJson(data = mapOf("message" to "Permission assigned"))
                }

                namedPost("api.v1.access.role.permission.assign_selected", "/{id}/permission/assign_selected") {
                    val id = call.parameters["id"]!!
                    val dto = call.receive<BulkPermissionDto>()
                    roleService.assignPermissions(id, dto.selected)
                    call.respondJson(data = mapOf("message" to "${dto.selected.size} permission(s) assigned"))
                }

                namedGet("api.v1.access.role.permission.unassign", "/{id}/permission/{permId}/unassign") {
                    val id = call.parameters["id"]!!
                    val permId = call.parameters["permId"]!!
                    roleService.unassignPermission(id, permId)
                    call.respondJson(data = mapOf("message" to "Permission unassigned"))
                }

                namedPost("api.v1.access.role.permission.unassign_selected", "/{id}/permission/unassign_selected") {
                    val id = call.parameters["id"]!!
                    val dto = call.receive<BulkPermissionDto>()
                    roleService.unassignPermissions(id, dto.selected)
                    call.respondJson(data = mapOf("message" to "${dto.selected.size} permission(s) unassigned"))
                }
            }
        }
    }
}
