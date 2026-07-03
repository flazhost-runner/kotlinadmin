package com.kotlinadmin.modules.access.routes

import com.kotlinadmin.core.helpers.respondJson
import com.kotlinadmin.core.helpers.respondView
import com.kotlinadmin.core.routing.*
import com.kotlinadmin.core.session.withErrors
import com.kotlinadmin.core.session.withFlash
import com.kotlinadmin.modules.access.dto.DeleteSelectedDto
import com.kotlinadmin.modules.access.dto.UserCreateDto
import com.kotlinadmin.modules.access.dto.UserUpdateDto
import com.kotlinadmin.modules.access.models.toMap
import com.kotlinadmin.modules.access.services.IRoleService
import com.kotlinadmin.modules.access.services.IUserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get

fun Application.accessUserWebModule() {
    val userService = get<IUserService>()
    val roleService = get<IRoleService>()

    routing {
        route("/admin/v1/access/user") {
            namedGet("admin.v1.access.user.index") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.index", "GET")
                val params = call.request.queryParameters
                val result = userService.index(params)
                call.respondView(
                    "access/users/index.ftl",
                    mapOf(
                        "datas" to result.items,
                        "paginate_data" to result.paginateData,
                        "filter" to params.entries().associate { it.key to it.value.firstOrNull() }
                    )
                )
            }

            namedGet("admin.v1.access.user.create", "/create") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.create", "GET")
                val roles = roleService.all()
                call.respondView("access/users/create.ftl", mapOf("roles" to roles))
            }

            namedPost("admin.v1.access.user.store", "/store") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.store", "POST")
                val params = call.receiveParameters()

                val errors = mutableMapOf<String, String>()
                val name = params["name"]?.trim() ?: ""
                val email = params["email"]?.trim() ?: ""
                val code = params["code"]?.trim() ?: ""
                val password = params["password"] ?: ""
                val passwordConfirmation = params["password_confirmation"] ?: params["passwordConfirm"] ?: ""
                val phone = params["phone"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val timezone = params["timezone"]?.trim() ?: "UTC"
                val roleIds = params.getAll("roles[]") ?: params.getAll("role_ids[]") ?: emptyList()

                if (name.isBlank()) errors["name"] = "Name is required"
                if (email.isBlank()) errors["email"] = "Email is required"
                if (password.isBlank()) errors["password"] = "Password is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf(
                                "name" to name,
                                "email" to email,
                                "code" to code,
                                "phone" to phone,
                                "status" to status
                            )
                        )
                    )
                    call.respondRedirect("/admin/v1/access/user/create")
                    return@namedPost
                }

                val dto = UserCreateDto(
                    name = name, email = email, code = code, password = password,
                    passwordConfirm = passwordConfirmation,
                    phone = phone.ifBlank { null }, status = status, timezone = timezone, roleIds = roleIds
                )
                userService.store(dto, session.userId)
                call.sessions.set(session.withFlash("success", "Create User Success."))
                call.respondRedirect("/admin/v1/access/user")
            }

            namedGet("admin.v1.access.user.edit", "/{id}/edit") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.edit", "GET")
                val id = call.parameters["id"] ?: return@namedGet call.respond(HttpStatusCode.BadRequest)
                val user = userService.edit(id)
                val roles = roleService.all()
                call.respondView(
                    "access/users/edit.ftl",
                    mapOf(
                        "data" to user.toMap(),
                        "roles" to roles
                    )
                )
            }

            namedPut("admin.v1.access.user.update", "/{id}/update") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.update", "PUT")
                val id = call.parameters["id"] ?: return@namedPut call.respond(HttpStatusCode.BadRequest)
                val params = call.receiveParameters()

                val errors = mutableMapOf<String, String>()
                val name = params["name"]?.trim() ?: ""
                val email = params["email"]?.trim() ?: ""
                val code = params["code"]?.trim() ?: ""
                val phone = params["phone"]?.trim() ?: ""
                val status = params["status"]?.trim() ?: "Active"
                val timezone = params["timezone"]?.trim() ?: "UTC"
                val password = params["password"]?.ifBlank { null }
                val roleIds = params.getAll("roles[]") ?: params.getAll("role_ids[]") ?: emptyList()

                if (name.isBlank()) errors["name"] = "Name is required"
                if (email.isBlank()) errors["email"] = "Email is required"

                if (errors.isNotEmpty()) {
                    call.sessions.set(
                        session.withErrors(
                            errors,
                            mapOf(
                                "name" to name,
                                "email" to email,
                                "code" to code,
                                "phone" to phone,
                                "status" to status
                            )
                        )
                    )
                    call.respondRedirect("/admin/v1/access/user/$id/edit")
                    return@namedPut
                }

                val dto = UserUpdateDto(
                    name = name,
                    email = email,
                    code = code,
                    password = password,
                    phone = phone.ifBlank { null },
                    status = status,
                    timezone = timezone,
                    roleIds = roleIds
                )
                userService.update(id, dto, session.userId)
                call.sessions.set(session.withFlash("success", "Update User Success."))
                call.respondRedirect("/admin/v1/access/user")
            }

            namedDelete("admin.v1.access.user.delete", "/{id}/delete") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.delete", "DELETE")
                val id = call.parameters["id"] ?: return@namedDelete call.respond(HttpStatusCode.BadRequest)
                userService.delete(id)
                call.sessions.set(session.withFlash("success", "Delete User Success."))
                call.respondRedirect("/admin/v1/access/user")
            }

            namedPost("admin.v1.access.user.delete_selected", "/delete_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.access.user.delete_selected", "POST")
                val params = call.receiveParameters()
                val ids = params.getAll("selected[]") ?: emptyList()
                if (ids.isEmpty()) {
                    call.sessions.set(session.withFlash("warning", "No users selected"))
                    call.respondRedirect("/admin/v1/access/user")
                    return@namedPost
                }
                userService.deleteSelected(ids)
                call.sessions.set(session.withFlash("success", "Delete User Success."))
                call.respondRedirect("/admin/v1/access/user")
            }
        }
    }
}

fun Application.accessUserApiModule() {
    val userService = get<IUserService>()

    routing {
        authenticate("api") {
            route("/api/v1/access/user") {
                namedGet("api.v1.access.user.index") {
                    val params = call.request.queryParameters
                    val result = userService.index(params)
                    call.respondJson(
                        data = mapOf(
                            "data" to result.items.map { it.toMap() },
                            "pagination" to result.paginateData.toMap()
                        )
                    )
                }

                namedPost("api.v1.access.user.store", "/store") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val dto = call.receive<UserCreateDto>()
                    val user = userService.store(dto, principal.subject!!)
                    call.respondJson(
                        HttpStatusCode.Created,
                        data = mapOf("message" to "User created", "data" to user.toMap())
                    )
                }

                namedGet("api.v1.access.user.edit", "/{id}/edit") {
                    val id = call.parameters["id"]!!
                    val user = userService.edit(id)
                    call.respondJson(data = mapOf("data" to user.toMap()))
                }

                namedPut("api.v1.access.user.update", "/{id}/update") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val id = call.parameters["id"]!!
                    val dto = call.receive<UserUpdateDto>()
                    val user = userService.update(id, dto, principal.subject!!)
                    call.respondJson(data = mapOf("message" to "User updated", "data" to user.toMap()))
                }

                namedDelete("api.v1.access.user.delete", "/{id}/delete") {
                    val id = call.parameters["id"]!!
                    userService.delete(id)
                    call.respondJson(data = mapOf("message" to "User deleted"))
                }

                namedPost("api.v1.access.user.delete_selected", "/delete_selected") {
                    val dto = call.receive<DeleteSelectedDto>()
                    userService.deleteSelected(dto.selected)
                    call.respondJson(
                        data = mapOf("message" to "${dto.selected.size} user(s) deleted", "count" to dto.selected.size)
                    )
                }
            }
        }
    }
}
