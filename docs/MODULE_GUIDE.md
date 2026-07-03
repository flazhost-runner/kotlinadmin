# Panduan Membuat Modul Baru — KotlinAdmin

Langkah konkret + template untuk menambah modul agar otomatis sejalan dengan pola, prinsip, security, dan testing yang ada. Contoh: modul **Product** (`name`, `price`, `status`). Ganti `Product`/`product` sesuai kebutuhan.

> Setelah selesai, WAJIB: `./gradlew detekt` → `./gradlew test` (semua hijau). Aturan: lihat `AGENTS.md`.

Cara cepat: `./gradlew makeModule -Pmodule=Product` (generate skeleton otomatis).

Struktur target:
```
src/main/kotlin/com/kotlinadmin/modules/product/
├── models/ProductTable.kt
├── dto/ProductDto.kt
├── services/IProductService.kt
├── services/ProductService.kt
└── routes/ProductRoutes.kt

src/main/resources/
├── db/migrations/V{N}__CreateProductTable.sql
└── templates/product/
    ├── index.ftl
    ├── create.ftl
    └── edit.ftl

src/test/kotlin/com/kotlinadmin/
└── integration/ProductServiceTest.kt
```

---

## 1. Entity (IdTable<String>)

```kotlin
// src/main/kotlin/com/kotlinadmin/modules/product/models/ProductTable.kt
package com.kotlinadmin.modules.product.models

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

object Products : IdTable<String>("products") {
    override val id = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)
    val name        = varchar("name", 100)
    val price       = long("price").default(0)
    val status      = varchar("status", 20).default("Active")
    val description = text("description").nullable()
    val createdBy   = varchar("created_by", 36).nullable()
    val updatedBy   = varchar("updated_by", 36).nullable()
    val createdAt   = long("created_at")
    val updatedAt   = long("updated_at")
}

class ProductEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ProductEntity>(Products)
    var name        by Products.name
    var price       by Products.price
    var status      by Products.status
    var description by Products.description
    var createdBy   by Products.createdBy
    var updatedBy   by Products.updatedBy
    var createdAt   by Products.createdAt
    var updatedAt   by Products.updatedAt
}
```

**Aturan entity:**
- `IdTable<String>` + `varchar("id", 36)` — BUKAN `UUIDTable` (uuid native memecah cross-port compatibility)
- Tipe kolom abstrak: `varchar`, `text`, `long`, `bool`, `timestamp`
- `status` = varchar(20) default `'Active'`, BUKAN enum native
- Kolom bernama `desc` di DB → property Kotlin bernama `description` (hindari konflik dengan Exposed DSL)

---

## 2. Migration (Flyway SQL)

```sql
-- src/main/resources/db/migrations/V7__CreateProductTable.sql
CREATE TABLE IF NOT EXISTS products (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'Active',
    description TEXT,
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36),
    created_at  BIGINT       NOT NULL,
    updated_at  BIGINT       NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS products__name ON products(name);
```

**Aturan migration:**
- SQL portabel: hindari `ENGINE=`, `AUTO_INCREMENT`, backtick, tipe vendor
- Nomor versi V harus berurutan dan unik
- Flyway auto-apply saat dev startup; produksi: migrasi eksplisit

---

## 3. DTO (whitelist field, anti mass-assignment)

```kotlin
// src/main/kotlin/com/kotlinadmin/modules/product/dto/ProductDto.kt
package com.kotlinadmin.modules.product.dto

data class ProductDto(
    val name: String,
    val price: Long = 0,
    val status: String = "Active",
    val description: String? = null
)
```

**Handler maping params ke DTO secara eksplisit** — jangan langsung pass `Parameters` ke service.

---

## 4. Interface Service

```kotlin
// src/main/kotlin/com/kotlinadmin/modules/product/services/IProductService.kt
package com.kotlinadmin.modules.product.services

import com.kotlinadmin.core.helpers.PaginationHelper.PaginateResult
import com.kotlinadmin.modules.product.dto.ProductDto
import com.kotlinadmin.modules.product.models.ProductEntity
import io.ktor.http.Parameters

interface IProductService {
    suspend fun index(params: Parameters): PaginateResult<ProductEntity>
    suspend fun store(dto: ProductDto, actorId: String): ProductEntity
    suspend fun edit(id: String): ProductEntity
    suspend fun update(id: String, dto: ProductDto, actorId: String): ProductEntity
    suspend fun delete(id: String)
    suspend fun deleteSelected(ids: List<String>)
}
```

---

## 5. Service Implementation

```kotlin
// src/main/kotlin/com/kotlinadmin/modules/product/services/ProductService.kt
package com.kotlinadmin.modules.product.services

import com.kotlinadmin.config.DatabaseConfig.dbQuery
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.helpers.PaginationHelper
import com.kotlinadmin.core.helpers.PaginationHelper.PaginateResult
import com.kotlinadmin.modules.product.dto.ProductDto
import com.kotlinadmin.modules.product.models.ProductEntity
import com.kotlinadmin.modules.product.models.Products
import io.ktor.http.Parameters
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.like
import org.jetbrains.exposed.sql.SortOrder
import java.util.UUID

class ProductService : IProductService {

    override suspend fun index(params: Parameters): PaginateResult<ProductEntity> = dbQuery {
        val page     = params["q_page"]?.toIntOrNull() ?: 1
        val pageSize = params["q_page_size"]?.toIntOrNull() ?: 10
        val qName    = params["q_name"]
        val qStatus  = params["q_status"]

        val query = Products.selectAll().apply {
            if (!qName.isNullOrBlank())   andWhere { Products.name.lowerCase() like "%${qName.lowercase()}%" }
            if (!qStatus.isNullOrBlank()) andWhere { Products.status eq qStatus }
        }.orderBy(Products.createdAt, SortOrder.DESC)

        val total = query.count()
        val items = ProductEntity.wrapRows(query.limit(pageSize, ((page - 1) * pageSize).toLong())).toList()
        PaginationHelper.build(items, total.toInt(), page, pageSize)
    }

    override suspend fun store(dto: ProductDto, actorId: String): ProductEntity = dbQuery {
        val exists = ProductEntity.find { Products.name eq dto.name }.firstOrNull()
        if (exists != null) throw ConflictError("Product '${dto.name}' already exists")
        val now = System.currentTimeMillis()
        ProductEntity.new(UUID.randomUUID().toString()) {
            name        = dto.name
            price       = dto.price
            status      = dto.status
            description = dto.description
            createdBy   = actorId
            updatedBy   = actorId
            createdAt   = now
            updatedAt   = now
        }
    }

    override suspend fun edit(id: String): ProductEntity = dbQuery {
        ProductEntity.findById(id) ?: throw NotFoundError("Product not found")
    }

    override suspend fun update(id: String, dto: ProductDto, actorId: String): ProductEntity = dbQuery {
        val item = ProductEntity.findById(id) ?: throw NotFoundError("Product not found")
        item.name        = dto.name
        item.price       = dto.price
        item.status      = dto.status
        dto.description?.let { item.description = it }
        item.updatedBy   = actorId
        item.updatedAt   = System.currentTimeMillis()
        item
    }

    override suspend fun delete(id: String) = dbQuery {
        val item = ProductEntity.findById(id) ?: throw NotFoundError("Product not found")
        item.delete()
    }

    override suspend fun deleteSelected(ids: List<String>) = dbQuery {
        ids.forEach { id -> ProductEntity.findById(id)?.delete() }
    }
}
```

**Aturan service:**
- SEMUA DB access dalam `dbQuery { }` (= `newSuspendedTransaction`)
- `throw AppException` (NotFoundError, ConflictError, ValidationError, dll) — TIDAK return null
- Logika bisnis hanya di sini, tidak di handler/route

---

## 6. Register Koin

```kotlin
// di/AppModule.kt — tambahkan baris ini
single<IProductService> { ProductService() }
```

---

## 7. Routes

```kotlin
// src/main/kotlin/com/kotlinadmin/modules/product/routes/ProductRoutes.kt
package com.kotlinadmin.modules.product.routes

import com.kotlinadmin.core.helpers.ViewHelper.respondView
import com.kotlinadmin.core.routing.AuthHelper.checkAccess
import com.kotlinadmin.core.routing.AuthHelper.requireAuthenticated
import com.kotlinadmin.core.routing.RouteDsl.namedDelete
import com.kotlinadmin.core.routing.RouteDsl.namedGet
import com.kotlinadmin.core.routing.RouteDsl.namedPost
import com.kotlinadmin.core.routing.RouteDsl.namedPut
import com.kotlinadmin.modules.product.dto.ProductDto
import com.kotlinadmin.modules.product.services.IProductService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get

fun Application.productModule() {
    val service = get<IProductService>()

    routing {
        route("/admin/v1/product") {
            namedGet("admin.v1.product.index") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.index", "GET")
                val result = service.index(call.request.queryParameters)
                call.respondView("product/index.ftl", mapOf(
                    "datas"        to result.items,
                    "paginate_data" to result.paginateData,
                    "filter"       to call.request.queryParameters
                ))
            }
            namedGet("admin.v1.product.create", "/create") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.create", "GET")
                call.respondView("product/create.ftl")
            }
            namedPost("admin.v1.product.store", "/store") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.store", "POST")
                val params = call.receiveParameters()
                val dto = ProductDto(
                    name        = params["name"] ?: "",
                    price       = params["price"]?.toLongOrNull() ?: 0,
                    status      = params["status"] ?: "Active",
                    description = params["description"]
                )
                service.store(dto, session.userId)
                call.sessions.set(session.withFlash("success", "Product created successfully"))
                call.respondRedirect("/admin/v1/product")
            }
            namedGet("admin.v1.product.edit", "/{id}/edit") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.edit", "GET")
                val data = service.edit(call.parameters["id"]!!)
                call.respondView("product/edit.ftl", mapOf("data" to data))
            }
            namedPut("admin.v1.product.update", "/{id}/update") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.update", "PUT")
                val params = call.receiveParameters()
                val dto = ProductDto(
                    name        = params["name"] ?: "",
                    price       = params["price"]?.toLongOrNull() ?: 0,
                    status      = params["status"] ?: "Active",
                    description = params["description"]
                )
                service.update(call.parameters["id"]!!, dto, session.userId)
                call.sessions.set(session.withFlash("success", "Product updated successfully"))
                call.respondRedirect("/admin/v1/product")
            }
            namedDelete("admin.v1.product.delete", "/{id}/delete") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.delete", "DELETE")
                service.delete(call.parameters["id"]!!)
                call.sessions.set(session.withFlash("success", "Product deleted successfully"))
                call.respondRedirect("/admin/v1/product")
            }
            namedPost("admin.v1.product.delete_selected", "/delete_selected") {
                val session = call.requireAuthenticated()
                call.checkAccess("admin.v1.product.delete_selected", "POST")
                val params = call.receiveParameters()
                val ids = params.getAll("selected[]") ?: emptyList()
                service.deleteSelected(ids)
                call.sessions.set(session.withFlash("success", "Selected products deleted"))
                call.respondRedirect("/admin/v1/product")
            }
        }
    }
}
```

---

## 8. Register Route di Application.kt

```kotlin
// Application.kt — tambahkan di configureRouting():
if (config.appMode == "full") {
    // ... existing modules ...
    productModule()
}
```

---

## 9. FreeMarker Templates

### index.ftl

```ftl
<#ftl output_format="HTML">
<#include "../layouts/head.ftl">
<#include "../layouts/sidebar.ftl">
<#include "../layouts/topbar.ftl">

<div class="md:ml-64 p-6">
<div class="tw-card p-0 overflow-hidden">
  <div class="px-6 py-4 border-b flex items-center justify-between">
    <h2 style="color:var(--primary)">Product List</h2>
    <div class="btn-group btn-sm">
      <a class="btn btn-success btn-sm" href="/admin/v1/product/create">
        <i class="fas fa-fw fa-plus"></i> Add Data
      </a>
      <button class="btn btn-danger btn-sm" form="selection"
              formaction="/admin/v1/product/delete_selected?_csrf=${_csrf}"
              data-confirm="Delete selected products?">
        <i class="fas fa-fw fa-times"></i> Delete Selected
      </button>
    </div>
  </div>
  <div class="p-4" style="overflow-x:auto">
    <table class="table table-bordered table-hover align-middle">
      <thead>
        <tr>
          <th></th>
          <th>
            <form id="searchform" method="GET" action="/admin/v1/product">
            <select name="q_page_size" class="form-control form-control-sm" onchange="this.form.submit()">
              <#list [10,20,50,100] as s>
                <option value="${s}" <#if (filter["q_page_size"]!"")==s?string>selected</#if>>${s}</option>
              </#list>
            </select>
          </th>
          <th><input class="form-control form-control-sm" name="q_name" placeholder="Name" value="${(filter["q_name"])!}"></th>
          <th>
            <select name="q_status" class="form-control form-control-sm">
              <option value="">All</option>
              <option value="Active" <#if (filter["q_status"]!"")==("Active")>selected</#if>>Active</option>
              <option value="Inactive" <#if (filter["q_status"]!"")==("Inactive")>selected</#if>>Inactive</option>
            </select>
          </th>
          <th>
            <div class="btn-group">
              <button class="btn btn-sm btn-success" type="submit" form="searchform">
                <i class="fas fa-search"></i>
              </button>
              <a class="btn btn-sm btn-danger" href="/admin/v1/product">
                <i class="fas fa-times"></i>
              </a>
            </div>
          </th>
        </tr>
        <tr>
          <th><input type="checkbox" id="checkall"></th>
          <th>No</th><th>Name</th><th>Status</th><th>Action</th>
        </tr>
      </thead>
      <tbody>
        <form id="selection" method="POST">
        <#list datas as item>
        <tr>
          <td><input type="checkbox" name="selected[]" value="${item.id.value}"></td>
          <td>${paginate_data.offset + item?index + 1}</td>
          <td>${item.name}</td>
          <td>
            <#if item.status == "Active">
              <i class="fas fa-check-circle text-green-500 text-xl"></i>
            <#else>
              <i class="fas fa-times-circle text-red-500 text-xl"></i>
            </#if>
          </td>
          <td>
            <div class="btn-group">
              <button class="btn btn-sm btn-primary dropdown-toggle" data-toggle-dd>Action</button>
              <div class="dropdown-menu dropdown-menu-end">
                <a class="dropdown-item" href="/admin/v1/product/${item.id.value}/edit">Edit</a>
                <div class="dropdown-divider"></div>
                <form method="POST" action="/admin/v1/product/${item.id.value}/delete?_method=DELETE&_csrf=${_csrf}">
                  <button class="dropdown-item text-red-600" data-confirm="Delete this product?">Delete</button>
                </form>
              </div>
            </div>
          </td>
        </tr>
        </#list>
        </form>
      </tbody>
    </table>
    <#include "../layouts/pagination.ftl">
  </div>
</div>
</div>

<#include "../layouts/foot.ftl">
<script>
$("#checkall").click(function(){ $('input:checkbox').not(this).prop('checked', this.checked); });
</script>
```

### create.ftl

```ftl
<#ftl output_format="HTML">
<#include "../layouts/head.ftl">
<#include "../layouts/sidebar.ftl">
<#include "../layouts/topbar.ftl">

<div class="md:ml-64 p-6">
<div class="tw-card">
  <h2 class="mb-4" style="color:var(--primary)">Add Product</h2>
  <form method="POST" action="/admin/v1/product/store?_csrf=${_csrf}">
    <div class="mb-3">
      <label class="form-label">Name <span class="text-red-500">*</span></label>
      <input type="text" name="name" class="form-control <#if errors.name??>is-invalid</#if>"
             value="${(old.name)!}">
      <#if errors.name??><div class="invalid-feedback">${errors.name}</div></#if>
    </div>
    <div class="mb-3">
      <label class="form-label">Price</label>
      <input type="number" name="price" class="form-control" value="${(old.price)!'0'}">
    </div>
    <div class="mb-3">
      <label class="form-label">Status</label>
      <select name="status" class="form-control">
        <option value="Active" <#if (old.status!'Active')=="Active">selected</#if>>Active</option>
        <option value="Inactive" <#if (old.status!"")==("Inactive")>selected</#if>>Inactive</option>
      </select>
    </div>
    <div class="mb-3">
      <label class="form-label">Description</label>
      <textarea name="description" class="form-control" rows="3">${(old.description)!}</textarea>
    </div>
    <button type="submit" class="btn btn-primary">Save</button>
    <a href="/admin/v1/product" class="btn btn-secondary ms-2">Cancel</a>
  </form>
</div>
</div>

<#include "../layouts/foot.ftl">
```

### edit.ftl (sama dengan create, tapi method PUT)

```ftl
<#ftl output_format="HTML">
<#include "../layouts/head.ftl">
<#include "../layouts/sidebar.ftl">
<#include "../layouts/topbar.ftl">

<div class="md:ml-64 p-6">
<div class="tw-card">
  <h2 class="mb-4" style="color:var(--primary)">Edit Product</h2>
  <form method="POST" action="/admin/v1/product/${data.id.value}/update?_method=PUT&_csrf=${_csrf}">
    <div class="mb-3">
      <label class="form-label">Name <span class="text-red-500">*</span></label>
      <input type="text" name="name" class="form-control <#if errors.name??>is-invalid</#if>"
             value="${(old.name)!data.name}">
      <#if errors.name??><div class="invalid-feedback">${errors.name}</div></#if>
    </div>
    <div class="mb-3">
      <label class="form-label">Price</label>
      <input type="number" name="price" class="form-control" value="${(old.price)!data.price?string}">
    </div>
    <div class="mb-3">
      <label class="form-label">Status</label>
      <select name="status" class="form-control">
        <option value="Active" <#if (old.status!data.status)=="Active">selected</#if>>Active</option>
        <option value="Inactive" <#if (old.status!data.status)=="Inactive">selected</#if>>Inactive</option>
      </select>
    </div>
    <div class="mb-3">
      <label class="form-label">Description</label>
      <textarea name="description" class="form-control" rows="3">${(old.description)!data.description!}</textarea>
    </div>
    <button type="submit" class="btn btn-primary">Update</button>
    <a href="/admin/v1/product" class="btn btn-secondary ms-2">Cancel</a>
  </form>
</div>
</div>

<#include "../layouts/foot.ftl">
```

---

## 10. Test

### Integration Test (Kotest)

```kotlin
// src/test/kotlin/com/kotlinadmin/integration/ProductServiceTest.kt
package com.kotlinadmin.integration

import com.kotlinadmin.config.DatabaseConfig
import com.kotlinadmin.config.DbConfig
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.modules.product.dto.ProductDto
import com.kotlinadmin.modules.product.models.Products
import com.kotlinadmin.modules.product.services.ProductService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class ProductServiceTest : DescribeSpec({
    beforeSpec {
        DatabaseConfig.setup(DbConfig("jdbc:sqlite::memory:", "org.sqlite.JDBC", "", ""))
        transaction { SchemaUtils.create(Products) }
    }

    val service = ProductService()

    describe("ProductService") {
        describe("store") {
            it("creates product successfully") {
                val dto = ProductDto(name = "Widget A", price = 1000)
                val result = service.store(dto, "actor-id")
                result.name shouldBe "Widget A"
                result.price shouldBe 1000
                result.status shouldBe "Active"
            }
            it("throws ConflictError on duplicate name") {
                val dto = ProductDto(name = "Duplicate")
                service.store(dto, "actor")
                shouldThrow<ConflictError> { service.store(dto, "actor") }
            }
        }
        describe("edit") {
            it("returns product by id") {
                val created = service.store(ProductDto(name = "FindMe"), "actor")
                val found = service.edit(created.id.value)
                found.name shouldBe "FindMe"
            }
            it("throws NotFoundError for missing id") {
                shouldThrow<NotFoundError> { service.edit("non-existent-id") }
            }
        }
        describe("update") {
            it("updates product fields") {
                val created = service.store(ProductDto(name = "Before"), "actor")
                service.update(created.id.value, ProductDto(name = "After", price = 999), "actor")
                val updated = service.edit(created.id.value)
                updated.name shouldBe "After"
                updated.price shouldBe 999
            }
        }
        describe("delete") {
            it("deletes product") {
                val created = service.store(ProductDto(name = "ToDelete"), "actor")
                service.delete(created.id.value)
                shouldThrow<NotFoundError> { service.edit(created.id.value) }
            }
        }
    }
})
```

### BDD Feature

```gherkin
# src/test/resources/features/product.feature
Feature: Product Management

  Background:
    Given the admin is logged in

  Scenario: Create a new product
    When I POST to "/admin/v1/product/store" with name="Test Product" price="500"
    Then I should be redirected to "/admin/v1/product"
    And the product "Test Product" should exist

  Scenario: Delete a product
    Given a product "ToDelete" exists
    When I DELETE "/admin/v1/product/{id}/delete"
    Then I should be redirected to "/admin/v1/product"
    And the product "ToDelete" should not exist
```

---

## 11. Verifikasi Akhir

```bash
./gradlew detekt   # harus 0 issues
./gradlew test     # hijau (termasuk ProductServiceTest)
```

Pastikan juga:
- `di/AppModule.kt` — `single<IProductService> { ProductService() }` sudah ada
- `Application.kt` — `productModule()` sudah dipanggil
- `README.md` dan `docs/API.md` sudah diperbarui
