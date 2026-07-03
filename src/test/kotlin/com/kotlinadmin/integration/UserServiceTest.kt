package com.kotlinadmin.integration

import com.kotlinadmin.config.DatabaseConfig
import com.kotlinadmin.config.DbConfig
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.modules.access.dto.UserCreateDto
import com.kotlinadmin.modules.access.dto.UserUpdateDto
import com.kotlinadmin.modules.access.services.UserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

class UserServiceTest : DescribeSpec({

    beforeSpec {
        // DB file temporer unik per run: skema selalu segar sehingga migrasi Flyway
        // dan uji duplikat-email deterministik (URL ":memory:?..." lama justru membuat
        // file literal yang persisten antar-run → checksum Flyway basi).
        val dbFile = Files.createTempFile("kotlinadmin-userservice-test-", ".db").toFile()
        dbFile.deleteOnExit()
        DatabaseConfig.setup(
            DbConfig(
                url = "jdbc:sqlite:${dbFile.absolutePath}",
                driver = "org.sqlite.JDBC",
                user = "",
                password = ""
            )
        )
    }

    val service = UserService()

    describe("UserService.store") {
        it("creates user and returns entity") {
            val result = runBlocking {
                service.store(
                    UserCreateDto(
                        code = "TST001",
                        name = "Test User",
                        email = "testuser@example.com",
                        phone = "081234567890",
                        password = "password123",
                        passwordConfirm = "password123",
                        status = "Active",
                        timezone = "Asia/Jakarta"
                    ),
                    actorId = "system"
                )
            }
            result.name shouldBe "Test User"
            result.email shouldBe "testuser@example.com"
            result.status shouldBe "Active"
        }

        it("throws ConflictError on duplicate email") {
            shouldThrow<ConflictError> {
                runBlocking {
                    service.store(
                        UserCreateDto(
                            code = "TST002",
                            name = "Duplicate",
                            email = "testuser@example.com",
                            phone = null,
                            password = "password123",
                            passwordConfirm = "password123"
                        ),
                        actorId = "system"
                    )
                }
            }
        }

        it("throws ConflictError on duplicate code") {
            shouldThrow<ConflictError> {
                runBlocking {
                    service.store(
                        UserCreateDto(
                            code = "TST001",
                            name = "Another",
                            email = "another@example.com",
                            phone = null,
                            password = "password123",
                            passwordConfirm = "password123"
                        ),
                        actorId = "system"
                    )
                }
            }
        }
    }

    describe("UserService.edit") {
        it("returns user by id") {
            val created = runBlocking {
                service.store(
                    UserCreateDto(
                        code = "EDT001",
                        name = "Edit Me",
                        email = "edit@example.com",
                        phone = null,
                        password = "password123",
                        passwordConfirm = "password123"
                    ),
                    actorId = "system"
                )
            }
            val found = runBlocking { service.edit(created.id.value) }
            found shouldNotBe null
            found!!.email shouldBe "edit@example.com"
        }

        it("throws NotFoundError for non-existent id") {
            shouldThrow<NotFoundError> {
                runBlocking { service.edit("00000000-0000-0000-0000-000000000000") }
            }
        }
    }

    describe("UserService.update") {
        it("updates user fields") {
            val created = runBlocking {
                service.store(
                    UserCreateDto(
                        code = "UPD001",
                        name = "Update Me",
                        email = "update@example.com",
                        phone = null,
                        password = "password123",
                        passwordConfirm = "password123"
                    ),
                    actorId = "system"
                )
            }
            val updated = runBlocking {
                service.update(
                    created.id.value,
                    UserUpdateDto(name = "Updated Name", phone = "089999999999"),
                    actorId = "system"
                )
            }
            updated.name shouldBe "Updated Name"
            updated.phone shouldBe "089999999999"
        }
    }

    describe("UserService.delete") {
        it("deletes user and throws NotFoundError on subsequent edit") {
            val created = runBlocking {
                service.store(
                    UserCreateDto(
                        code = "DEL001",
                        name = "Delete Me",
                        email = "deleteme@example.com",
                        phone = null,
                        password = "password123",
                        passwordConfirm = "password123"
                    ),
                    actorId = "system"
                )
            }
            runBlocking { service.delete(created.id.value) }
            shouldThrow<NotFoundError> {
                runBlocking { service.edit(created.id.value) }
            }
        }
    }

    describe("UserService.index") {
        it("returns paginated results") {
            val result = runBlocking {
                service.index(
                    Parameters.build {
                        append("page", "1")
                        append("page_size", "10")
                    }
                )
            }
            result.paginateData.page shouldBe 1
        }

        it("filters by email (case-insensitive)") {
            val result = runBlocking {
                service.index(Parameters.build { append("q_email", "TESTUSER") })
            }
            result.items.all { it.email.contains("testuser", ignoreCase = true) } shouldBe true
        }
    }
})
